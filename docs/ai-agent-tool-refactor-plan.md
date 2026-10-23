# AI 模块 Tool 化与自主调度改造规划

## 1. 改造目标

本次改造不是删除现有 AI 生成功能，也不是把所有请求交给 DB-GPT，而是把平台能力统一抽象为可被 Agent 调度的 Tool：

```text
配置的大模型 Agent
        |
        | 自主选择、调用、串联 Tool
        v
DataMaster Tool 层
  数据源 / 元数据探查 / 资产 / 本体 / 动作 / Skill / 审批 / 流程

DB-GPT
  数据问答 / 数据分析 / 数据报告
```

保留现有固定流程，作为标准数据接入流程、稳定性兜底和批量场景入口；新增 Agent 模式后，由配置的大模型根据用户目标和 Tool 返回结果自主决定下一步。

关键边界：

- DB-GPT 继续负责数据查询、数据分析和数据报告，不作为平台流程总调度器。
- 本体、动作、Skill 的生成能力继续保留，先包装为 Tool，不重复实现生成逻辑。
- Agent 可以自主规划和调用 Tool，但不能绕过 DataMaster 的权限、条件、审批和执行引擎直接改数据。
- 所有写操作必须返回可审计的操作结果，并根据风险等级进入自动执行、待确认或审批状态。

## 2. 当前固定流程的真实链路

当前入口是：

```text
POST /ai/agent/operation/start
  -> PlatformAgentOperationService.start()
  -> CompletableFuture.runAsync(() -> execute())
```

流程实例会写入 `AI_AGENT_OPERATION`，前端通过 `/ai/agent/operation/{id}` 查询状态，通过 `/page` 查询历史记录；服务重启时运行中的实例会被标记为 `INTERRUPTED`，之后可以继续或重试。

### 2.1 解析和匹配数据源

`start()` 首先执行 `completeConnectionFromGoal()`：

1. 从请求和自然语言目标中补齐数据库类型、地址、端口、数据库名、用户名和密码。
2. 支持 `owner`、`onwer`、`user`、`username`、`password`、`pwd` 等连接信息写法。
3. 密码进入受控数据源服务前会加密保存。

然后执行 `resolveDatasource()`，匹配顺序是：

1. 按 `datasourceId` 查找。
2. 按数据源名称或数据库名称匹配。
3. 按 IP、端口、数据库名匹配。

如果没有匹配到数据源且连接信息不完整，流程进入 `NEED_INPUT`，要求用户补充信息；流程复用按“同一用户 + 空间 + 数据源 + operationKey/conversationId/goal”执行，同一数据源上的不同目标可以并行运行，同时不泄露其他用户的流程状态。

### 2.2 新增或复用数据源

在 `execute()` 中：

- 已找到数据源：直接复用。
- 未找到数据源：组装 `AssetsDatasourceSaveReqVO`，调用 `datasourceService.createDatasource()` 创建。

创建内容包括数据源名称、数据库类型、IP、端口、连接配置、描述和空间绑定。创建或匹配完成后，通过 `ensureDatasourceSpaceRelation()` 确保数据源已经绑定当前空间。

这一步是平台服务调用，不经过 DB-GPT，也不允许模型直接执行任意 SQL。

### 2.3 新增或修正探查任务

通过 `findTask(datasourceId, spaceId, spaceCode)` 查找名称以 `数据智能体自动探查-` 开头的任务。

如果没有任务，则调用 `catalogTaskService.createCatalogTask()` 创建：

- 只探查请求指定的数据库，不使用“全部库”模式。
- 默认收集范围为单库。
- 任务初始状态为未发布。
- 探查范围绑定当前数据源、数据库、schema 和空间。
- PostgreSQL 等未填写模式名时，后续解析默认使用 `public`。

如果发现历史任务是全库模式、空间不一致或未绑定空间，会先调用 `updateCatalogTask()` 修正后复用，而不是再次创建任务。

### 2.4 发布并执行探查任务

固定流程目前会自动尝试：

```text
updateReleaseJobTask(status=1)
        -> runDaDiscoveryTask(taskId)
```

状态进度大致为：

- 12：准备元数据采集任务。
- 20：任务已发布，开始执行一次探查。

如果任务本来已经是发布状态，发布更新失败时会跳过重复发布，直接执行一次探查。如果任务尚未发布且自动发布/执行失败，当前实现会将流程置为 `NEED_CONFIRMATION`，记录 `PUBLISH_METADATA_TASK` 确认类型，等待人工确认后再发布执行。

人工确认接口是：

```text
POST /ai/agent/operation/{id}/confirm-metadata
```

确认后重新发布、执行探查，并继续后续流程。

探查任务执行本身会创建任务实例、写执行日志、记录成功/失败表数量，并使用 Redis 锁防止同一任务并发执行。探查成功后还会检查目录中是否真的生成了表元数据；只有任务返回成功但没有表元数据时，流程也会失败，不会盲目进入本体生成。

### 2.5 同步数据资产和授权

探查确认产生表元数据后：

1. 调用 `assetSyncService.sync()`，把目录元数据同步为数据资产。
2. 使用当前数据库、schema、资产目录和空间信息进行同步。
3. 调用 `authorizeAssets()`，绑定当前空间的数据资产和资产字段。

进度约为 35（同步资产）和 52（完成空间授权）。

### 2.6 生成本体、动作和 Skill

进入 `executeAiStages()` 后，调用的是配置的大模型能力，不是 DB-GPT：

1. `ontologyGenerateService.generate()`：根据已采集资产生成本体模型，进度 62。
2. `ontologyGenerateService.generateActionsPreview()`：生成动作预览。
3. `persistActions()`：逐个保存动作，默认需要审批，进度 74。
4. `aiSkillService.generateOntologyDecisionSkill()`：生成并发布本体决策 Skill，进度 86。
5. `skillSyncService.syncAllSkills()` 和 `datasourceSyncService.syncById()`：同步 Skill 和数据源能力，进度 94。

全部完成后，流程写入数据源 ID、本体 ID、动作数量、资产同步结果等结果信息，进度 100，状态为 `SUCCEEDED`；核心步骤完成但 Skill 生成或能力同步降级时状态为 `PARTIAL`，具体原因写入 `result.warnings`。

### 2.7 流程状态、失败和恢复

固定流程已经具备流程实例持久化：

- 主记录：操作 ID、目标、数据源、空间、状态、进度、当前步骤。
- 结果信息：探查任务 ID、本体 ID、动作数量、资产同步消息、结果 JSON。
- 过程信息：步骤列表、输出日志、确认类型和确认消息。
- 恢复能力：`resume` 补充连接信息，`retry` 从失败流程重新执行，`cancel` 取消流程并向正在执行的元数据探查任务发送停止请求。

因此新 Agent 模式不应另起一套不可恢复的内存状态，而应复用同一套操作记录和状态模型。

## 3. 目标架构

### 3.1 分层

```text
Controller / Chat API
        |
        +-- FixedFlowFacade                标准固定流程
        |
        +-- AutonomousAgentService         Tool 自主调度
                |
                +-- LangChain4j Agent
                |      - ChatModel（配置的大模型）
                |      - Tool Calling
                |      - ChatMemory
                |      - 结构化决策结果
                |
                +-- AgentToolCatalog（仅目录查询）
                       |
                       +-- DataSourceTools
                       +-- MetadataTools
                       +-- AssetTools
                       +-- OntologyTools
                       +-- ActionTools
                       +-- SkillTools
                       +-- ApprovalTools
                       +-- DbGptDataTools
                       +-- OperationTools
```

### 3.2 模块落位

建议将通用抽象和 Agent 编排能力放在 `datamaster-ai`：

- `com.datamaster.module.ai.agent.tool`：Tool 注解、请求 DTO、上下文、结果、目录查询。
- `com.datamaster.module.ai.agent.runtime`：LangChain4j Agent、记忆、调度循环。
- `com.datamaster.module.ai.agent.policy`：权限、风险和确认策略。
- `com.datamaster.module.ai.service.dbgpt`：DB-GPT 数据能力适配。

依赖具体业务实现的适配器通过现有 `*-interface` 注入，避免 AI 模块反向依赖各业务 Core；需要直接使用服务器编排服务的工具适配器放在 `datamaster-server`，由 Spring 组件扫描后交给 LangChain4j `AiServices`。

当前根项目是 Java 8、Spring Boot 2.5，因此不直接引入要求 Java 17 的 Spring AI 版本。LangChain4j 选择支持 Java 8 的兼容版本，并先以最小依赖接入；最终版本在落地阶段根据依赖树确认。

## 4. Tool 注解与执行边界

Tool 不再实现统一接口，而是作为 Spring Bean 使用两层注解声明：

- LangChain4j 的 `dev.langchain4j.agent.tool.Tool` 标注 `execute` 方法，提供框架层的工具名称和模型描述。
- 平台自定义的 `@AgentTool` 标注类，提供风险等级、确认策略等治理元数据。

```java
import dev.langchain4j.agent.tool.Tool;

@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class CreateDatasourceTool {

    @Resource
    private IAssetsDatasourceService datasourceService;

    @Tool(
        name = "create_datasource",
        value = "创建受控数据源并绑定当前空间"
    )
    public AgentToolResult execute(AgentToolRequest.CreateDatasource input) {
        // 只做参数适配，调用已有 Service；权限、确认和审计由切面统一处理
        return ...;
    }
}
```

`AgentToolCatalog` 只为目录接口和管理端人工调用提供元数据；自主 Agent 的 Tool Calling、
参数校验和调度全部由 LangChain4j `AiServices` 完成。由于当前项目启用了 Spring AOP，
代理对象可能无法直接暴露目标类上的 `@Tool` 方法，因此 Catalog 仅提供
`ToolSpecification + ToolExecutor` 兼容适配，不实现第二套调度或参数协议。平台治理元数据
仍来自 `@AgentTool`，框架注解缺少名称时启动即失败，审计和确认记录也统一使用同一个
框架工具名称。

Tool Bean 直接交给 LangChain4j 的自动执行器；`AgentToolContext` 通过线程上下文由平台注入，
不暴露给模型；类型化请求 DTO 由框架转换后进入治理切面。LangChain4j 注解负责框架元数据，
`AgentToolAspect` 负责安全上下文、确认、审计和异常边界。
确认、参数脱敏、审计、结果归一化由 `AgentToolAspect` 统一处理；入口权限由
Controller 的权限注解校验，空间成员资格由 `ISpaceApi.hasUserSpace()` 统一校验，
最终的数据权限和业务资源归属仍由 Service/资源守卫负责。DB-GPT Tool 使用独立
的 Tool DTO，不把 `spaceId`、`spaceCode` 等可信上下文字段暴露给模型。

上下文至少包含：

- `operationId`
- `userId`、`userName`
- `spaceId`、`spaceCode`
- 当前数据源和数据库上下文
- 当前本体、探查任务和 Skill 上下文
- 权限快照和幂等键

标准结果至少包含：

```json
{
  "success": true,
  "status": "SUCCEEDED|PARTIAL|NEED_INPUT|NEED_CONFIRMATION|FAILED|CANCELED",
  "data": {},
  "message": "可读结果",
  "nextHints": [],
  "operationId": "..."
}
```

Tool 不直接返回异常堆栈给模型；异常要转成可继续规划的错误码，例如 `DATASOURCE_NOT_FOUND`、`CONNECTION_INVALID`、`NO_TABLE_METADATA`、`APPROVAL_REQUIRED`。

## 5. 第一批 Tool 划分

### 5.1 数据源 Tool

```text
create_datasource       新增受控数据源
get_datasource          查询数据源详情
list_datasources        查询当前空间可见数据源
update_datasource       修改数据源配置
delete_datasource       删除/停用数据源
bind_datasource_space   绑定数据源和空间
```

创建和修改必须复用现有数据源服务的加密、类型校验和连接校验；删除默认需要人工确认。

### 5.2 元数据探查 Tool

```text
create_probe_task       新增指定库/表范围的探查任务
get_probe_task          查询探查任务
update_probe_task       修改范围、schema、空间等配置
publish_probe_task      发布探查任务
run_probe_task          执行一次探查
```

`get_probe_task` 已包含任务和最近实例状态；`publish_probe_task` 和 `run_probe_task` 要使用幂等键，已经发布的任务不能重复创建，执行中任务不能并发执行。

### 5.3 资产和授权 Tool

```text
sync_assets             将目录元数据同步为资产
list_assets             查询资产/字段
authorize_assets        绑定资产和空间
```

`sync_assets` 前必须校验表元数据存在；原始资产数据预览继续走 DB-GPT 的权限、脱敏和查询链路，不开放通用原始数据读取 Tool。

### 5.4 本体和 Skill Tool

```text
generate_ontology       根据资产生成本体
get_ontology            查询本体及对象、关系
generate_actions        生成动作预览
generate_skill          生成本体决策 Skill
generate_report_template 生成报告模板 JSON
sync_agent_capability   同步 Skill 和数据源到智能体
```

这些 Tool 只包装现有生成服务：

- 本体生成继续调用 `ontologyGenerateService`。
- 动作生成继续调用 `generateActionsPreview` 和现有持久化逻辑。
- Skill 生成继续调用 `aiSkillService`。

模型只决定调用顺序和参数，不直接生成并执行未经校验的数据库写操作。

### 5.5 动作、审批和执行 Tool

```text
list_actions            查询已有动作
submit_action           提交动作执行请求
execute_action          调用现有动作执行引擎
get_action_execution    查询执行状态和结果
list_pending_approvals  查询待人工审批动作
```

风险策略：

- 查询和分析：可自动执行。
- 只读本体查询：可自动执行。
- 更新、删除、批量写入：默认 `NEED_CONFIRMATION` 或进入审批。
- Agent 永远不能跳过动作条件、主客体引用值解析、权限校验和审批等级。

### 5.6 DB-GPT 数据能力 Tool

```text
dbgpt_query             数据问答/查询
dbgpt_report             生成报告内容
```

`dbgpt_query` 的 `responseMode` 覆盖问答和决策分析；DB-GPT 返回的 Markdown/HTML 仍由平台负责安全渲染。它是数据能力提供者，不暴露为平台 Agent 的产品名称，也不负责创建数据源、发布探查任务或执行本体动作。

## 6. Agent 调度流程

### 6.1 标准数据接入请求

```text
用户：接入订单库，探查数据，生成本体和 Skill
        |
        v
Agent 判断缺少/已有数据源
        |
        +-- create_datasource
        +-- create_probe_task
        +-- publish_probe_task
        +-- run_probe_task
        +-- sync_assets
        +-- authorize_assets
        +-- generate_ontology
        +-- generate_actions
        +-- generate_skill
        +-- sync_agent_capability
        |
        v
返回结果、过程日志和可继续的 operationId
```

每次 Tool 调用都写入流程主记录和步骤记录；Agent 可以从中断位置继续，而不是刷新页面后丢失上下文。

### 6.2 数据分析和经营决策请求

```text
用户：分析哪些货物比较好卖，并给出库存分配建议
        |
        +-- dbgpt_query
        +-- dbgpt_analyze / dbgpt_report
        +-- 配置模型整理关键发现、建议、影响和风险
        |
        v
用户确认后才允许进入：
        preview_action -> approval -> execute_action
```

分析建议和本体动作执行必须分开。建议不等于执行，模型不能因为生成了建议就自动修改业务数据。

### 6.3 缺少信息和人工确认

Agent 遇到以下情况必须暂停并返回结构化状态：

- 数据库连接参数不完整：`NEED_INPUT`。
- 探查发布或执行失败：`NEED_CONFIRMATION` 或 `FAILED`，按策略决定。
- 写操作需要审批：`NEED_CONFIRMATION`。
- 结果缺少必要表元数据：`FAILED`，提示用户先完成探查。

前端显示当前步骤、动态输出、Tool 调用结果、待确认按钮和历史流程记录；刷新或离开页面后通过 operationId 恢复。

## 7. 固定流程与 Agent 流程共存

不删除 `PlatformAgentOperationService`，而是把它逐步拆成可复用的阶段服务：

```text
DatasourceStage
ProbeStage
AssetStage
OntologyStage
SkillStage
CapabilitySyncStage
```

两类入口共用这些阶段服务：

```text
固定流程入口
  严格按 Datasource -> Probe -> Asset -> Ontology -> Skill 执行

Agent Tool 入口
  由模型选择阶段或跳过已经满足条件的阶段
```

当 Agent 无法规划、模型超时、Tool 调度次数超过上限或用户选择“标准流程”时，可以调用总 Tool：

```text
run_standard_data_onboarding
```

该总 Tool 内部复用现有固定流程，保证标准接入不受 Agent 波动影响。

## 8. 记忆、幂等和持久化

### 8.1 记忆

- LangChain4j `ChatMemory` 管理当前 Agent 会话的短期上下文。
- 使用持久化 `ChatMemoryStore` 对接现有 AI 会话/消息表。
- `operationId` 作为流程执行上下文，不能只放在前端内存。
- 每次 Tool 调用保存名称、参数摘要、结果摘要、耗时和错误码。

### 8.2 幂等

以下操作必须支持幂等：

- 数据源创建：同空间 + 同连接指纹只能有一个有效数据源。
- 探查任务创建：同数据源 + 数据库 + schema + 空间只能复用一个平台任务。
- 发布和执行：同一任务执行中拒绝并发；已完成的请求按幂等键返回原结果。
- 本体、动作、Skill 生成：同一 operationId 重试时复用已有 ID，避免重复生成。

### 8.3 事务边界

Tool 不把长流程包在一个数据库事务里。每个阶段独立提交状态，阶段之间通过 operationId 和业务 ID 关联，支持失败重试和人工恢复。

## 9. 分阶段实施计划

### 阶段一：抽象和观测（先做）

1. 增加 `@AgentTool`、`AgentToolContext`、`AgentToolResult`、`AgentToolCatalog` 和 `AgentToolAspect`。
2. 增加 Tool 调用记录和统一错误码，不改变现有固定流程行为。
3. 把固定流程中的数据源、探查、资产、本体、Skill 阶段提取为可复用服务。
4. 增加 operationId、阶段状态和幂等键校验。

验收：固定流程结果与改造前一致，流程记录可完整查看每个阶段和错误。

### 阶段二：先接入只读和低风险 Tool

1. `get/list_datasource`。
2. `get_probe_task`、`get_probe_task_status`。
3. `list_assets`、`preview_asset`。
4. `get_ontology`、`list_actions`。
5. `dbgpt_query`、`dbgpt_analyze`、`dbgpt_report`。

落地时合并了语义重复的入口：`get_probe_task` 已返回任务状态，不再单独注册
`get_probe_task_status`；`dbgpt_query` 通过 `responseMode` 同时覆盖查询和分析，
不再注册仅改名称的 `dbgpt_analyze`。`list_assets` 只返回资产摘要；涉及真实数据行的
预览统一走 DB-GPT 查询链路和平台数据权限、脱敏逻辑，不提供可绕过治理的通用
`preview_asset` 原始数据读取 Tool。

验收：配置模型可以自主选择查询、分析和状态查询 Tool；不产生写入副作用。

### 阶段三：接入标准接入写 Tool

1. `create_datasource`、`bind_datasource_space`。
2. `create_probe_task`、`update_probe_task`。
3. `publish_probe_task`、`run_probe_task`。
4. `sync_assets`、`authorize_assets`。
5. `generate_ontology`、`generate_actions`、`generate_skill`、`sync_agent_capability`。

验收：用户一句话可以完成标准数据接入；每一步可暂停、恢复、重试；同一数据库不会重复创建流程和探查任务。

### 阶段四：接入动作预览、审批和执行

1. `preview_action` 解析触发对象、引用值和动作参数。
2. 对更新/删除/批量写入统一进入确认或审批。
3. `execute_action` 只调用既有动作执行引擎。
4. 完善审计、失败补偿和人工接管。

动作定义预览由 `generate_actions` 提供，动作执行请求预览由 `submit_action` 提供，
避免再增加一个与两者职责重叠的 `preview_action`。

验收：Agent 可以提出决策并生成执行预览，但没有用户确认或审批时不能修改业务数据。

### 阶段五：前端工作站和兼容收口

1. 对话页显示 Agent 当前计划、Tool 调用、进度和待确认项。
2. 右侧保留流程记录列表，支持恢复历史 operationId。
3. 固定流程和 Agent 流程共用结果展示。
4. 增加“标准流程/自主调度”模式标识和切换。

## 10. 失败处理和降级策略

```text
Tool 调用失败
  -> 记录错误码和原始原因
  -> 判断是否可重试
       |-- 可重试：指数退避，限制次数
       |-- 需补充信息：NEED_INPUT
       |-- 需人工确认：NEED_CONFIRMATION
       |-- 不可恢复：FAILED

Agent 规划失败/超时/循环
  -> 保存当前上下文
  -> 可选回退 run_standard_data_onboarding
  -> 否则保留流程记录，允许 retry/resume
```

DB-GPT 超时只影响问答、分析或报告 Tool，不应把已经完成的数据源和探查流程回滚；本体生成模型超时则停在对应阶段，允许从该阶段重试。

## 11. 暂不纳入本轮

- 不引入新的数据集成、ETL、调度编排平台能力。
- 不让 DB-GPT 直接调用数据源管理或动作执行接口。
- 不重写现有本体、动作、Skill 生成算法。
- 不删除固定流程和现有流程 API。
- 不允许模型直接生成任意 SQL 执行写操作。

## 12. 最终验收场景

1. 用户提供完整连接信息：Agent 自动创建/复用数据源，创建/复用探查任务，发布执行，确认有表元数据后同步资产、授权并生成本体、动作和 Skill。
2. 用户只提供数据库名称：Agent 返回 `NEED_INPUT`，补充连接后从原 operationId 继续。
3. 同一用户和空间内重复请求：复用现有数据源、探查任务和流程，不产生重复数据，也不读取其他用户的流程记录。
4. 探查发布失败：流程停在可解释的确认状态，人工确认后继续。
5. DB-GPT 查询超时：保留前面已完成阶段，问答 Tool 可重试，不丢失流程记录。
6. 经营分析提出库存调整建议：只生成建议和动作预览，未经确认/审批不执行更新。
7. 刷新或离开页面：重新打开流程工作站仍能看到历史 operationId、步骤、日志和当前状态。

## 13. 第一阶段已落地内容

本次开始改造已经加入第一版 Tool 基础设施：

- `@AgentTool`：声明平台 Tool 的风险等级和确认策略。
- LangChain4j `@Tool`：声明 `execute` 方法的框架级模型调用名称和描述；当前 29 个 Tool 均已双注解。
- `AgentToolContext`：统一传递用户、空间、operationId 和扩展上下文。
- `AgentToolResult`：统一返回成功、运行中、待补充、待确认和失败状态。
- `AgentToolResult.toolName`：记录实际被调用的 Tool，映射到流程步骤展示。
- `AgentToolCatalog`：只提供管理端 Tool 目录和受控人工调用；自主 Agent 的 Tool Calling、参数 Schema 和调度由 LangChain4j `AiServices` 完成，Catalog 仅做 Spring AOP 代理兼容适配。
- LangChain4j `DefaultToolExecutor`：负责框架的 DTO 参数转换；`AgentToolAspect` 在执行前后统一处理确认、审计和 `NEED_INPUT`。
- `AgentToolAspect`：统一处理确认、脱敏、审计、结果归一化和异常边界。
- `AgentToolResourceGuard`：集中校验数据源、本体、动作、执行记录和流程是否属于当前空间，Tool 不信任模型传入的资源归属字段。
- `AgentToolAuditService`：将每次 Tool 调用的脱敏参数、状态、结果摘要和耗时写入 `AI_AGENT_TOOL_CALL`。
- `run_standard_data_onboarding`：将现有固定数据接入流程包装成 Tool，内部仍调用 `PlatformAgentOperationService`。
- `get_datasource`：查询数据源基本信息，明确过滤密码和连接配置。
- `list_datasources`：查询当前空间数据源目录，只返回受控摘要。
- `list_assets`：查询当前空间可见的数据资产摘要，支持数据源、表名和资产名称筛选。
- `get_probe_task`：查询探查任务状态和范围摘要。
- `publish_probe_task`：复用现有发布服务发布探查任务。
- `run_probe_task`：复用现有探查执行服务执行一次探查。
- `create_datasource`：创建或复用受控数据源，并支持当前空间绑定。
- `update_datasource`：更新当前空间数据源且保留原空间关系。
- `delete_datasource`：删除当前空间数据源，必须确认同一个 datasourceId。
- `create_probe_task`：新增指定数据库/模式的探查任务，默认模式按 PostgreSQL 使用 `public`。
- `update_probe_task`：完整读取后更新探查任务，保留原采集范围和质量规则，发布执行仍由独立 Tool 控制。
- `sync_assets`：把已采集目录元数据同步为资产。
- `generate_ontology`：调用现有本体 AI 生成服务。
- `generate_actions`：调用现有动作预览生成服务，只生成预览不执行动作。
- `generate_skill`：调用现有本体决策 Skill 生成服务。
- `dbgpt_query`：把 DB-GPT 数据问答/分析能力作为只读 Tool 暴露给 Agent。
- `dbgpt_report`：把 DB-GPT 报告能力作为只读 Tool 暴露给 Agent。
- `list_actions`：查询本体动作定义。
- `get_ontology`：查询当前空间可见的本体详情。
- `submit_action`：提交动作并生成执行预览，复用动作条件和审批判定。
- `get_action_execution`：查询动作执行记录。
- `execute_action`：执行已审批动作，默认强制人工确认。
- `sync_agent_capability`：同步 Skill 和数据源能力到数据智能体运行时。
- `get_agent_operation`：查询固定流程或自主流程的持久化状态。
- `list_pending_approvals`：查询待人工审批动作；Agent 不能代替审批人批准。
- `generate_report_template`：保留现有报告模板 AI 生成能力。
- `update_datasource`、`delete_datasource`：数据源维护 Tool，删除要求精确资源确认。
- `create_probe_task`、`update_probe_task`：探查任务维护 Tool，创建同库同模式任务时复用已有任务。
- `/ai/agent/tools`：提供只读 Tool 目录和受控人工调用入口；自主 Agent 由 LangChain4j `AiServices` 自动发现同一批 Spring Tool Bean。
- 自主 Agent 入口不会默认把目标提交视为写操作确认；只有继续请求明确携带 `confirmed: true` 才会打开确认开关，`confirmedExecutionId`/`confirmedDatasourceId` 只用于进一步校验对应资源，不能单独授权其他 Tool。
- 对话工作站新增“流程编排”模式：问答、决策建议、报告继续走 DB-GPT；流程编排走 `/ai/agent/autonomous/start`，在消息中显示 operationId 和后台进度。

LangChain4j 自主调度入口为：

```text
POST /ai/agent/autonomous/run
{
  "goal": "接入订单库，探查数据并生成本体和 Skill",
  "conversationId": "可选，会话记忆 ID",
  "spaceId": 1,
  "spaceCode": "school"
}
```

长流程优先使用后台入口，接口立即返回 `operationId`：

```text
POST /ai/agent/autonomous/start
```

后台入口适合数据源创建、探查、本体生成等可能耗时数分钟的任务；前端按 `operationId` 轮询流程主记录，不需要保持当前页面或 HTTP 请求一直等待。

自主流程专用查询接口：

```text
GET /ai/agent/autonomous/{operationId}?spaceId=1&spaceCode=school
```

如果 Agent 委托了 `run_standard_data_onboarding`，该查询会同步返回被委托固定流程的最新进度和状态。

用户补充信息或确认单个动作后，使用：

```text
POST /ai/agent/autonomous/{operationId}/continue
```

请求可以携带新的 `goal`、`conversationId`、`spaceId`、`spaceCode`、`confirmed` 和 `confirmedExecutionId`；只有用户明确确认时才传 `confirmed: true`，资源确认只对对应执行记录或数据源生效。

对话工作站在流程处于 `NEED_INPUT` 时，下一条用户消息会自动作为同一 `operationId` 的补充目标继续执行，不会重新创建流程。

该入口使用 `ai.skill-model` 配置的大模型；模型调用 Tool 时由 LangChain4j `AiServices` 自动发现和执行。入口会先创建或复用 `AI_AGENT_OPERATION` 主记录，并把自主调度的状态、Tool 结果和最终响应持续写回，前端可以通过 `operationId` 查询历史、恢复会话。异步固定流程返回 `RUNNING` 后仍继续使用原有流程查询、恢复、确认和重试接口。

本次已接入 LangChain4j 0.35.0（Java 8 兼容）最小调度运行时：使用 `ai.skill-model` 配置创建 OpenAI 兼容 ChatModel，向模型注册当前 Tool Schema，按模型返回的 Tool Calling 串行执行，并使用 LangChain4j `MessageWindowChatMemory` 配合 `PersistentAgentChatMemoryStore` 管理可恢复会话记忆。当前仍没有改变固定流程的执行逻辑；下一阶段将继续完善持久化流程步骤和审批策略。
