# 本体动作链与 Workflow 改造方案

> 基于当前 `datamaster-ontology` 实现的核对结果整理。
> 日期：2026-09-05
>
> 2026-09-05 更新：可靠单动作触发 V1 已按
> `docs/本体动作可靠触发V1实施方案.md` 落地。本文后续内容继续作为 Workflow 阶段设计，
> 不把 Decision 描述为 Palantir 官方本体原语。
>
> 2026-10-14 更新：Workflow 定义层第一版已实现，包括 Workflow / Node / Edge 持久化、
> DAG 校验、草稿/发布接口和本体工作台“动作编排”配置页。Workflow Run / Workflow Step、
> 条件求值、持久化异步运行、动作节点失败重试、可恢复的并行拆分/汇聚和安全超时对账已接入 Workflow Runtime V2；当前并行分支按隔离上下文逐个执行，不是多线程同时执行。失败或需人工对账的运行可人工发起逆序补偿计划，补偿动作只进入人工审批/执行链；自动补偿和补偿结果自动收敛仍未实现。
>
> 2026-09-06 图形化改造：动作节点改为从已注册动作列表直接拖入 X6 画布，控制节点从节点库拖入，
> 端口连线后在右侧配置节点或分支条件。Workflow 增加触发概念，条件不再允许任意手写表达式，
> 而是保存“动态值来源 + 字段 + 操作符 + 固定值/动态引用”的结构化 JSON。

> 2026-09-06 交互语义补充：动作编排不在画布里临时创建动作。`ACTION` 节点必须从当前本体已有动作中拖入，
> 或在节点属性面板中重新选择已有动作；条件、并行拆分和并行汇聚属于控制节点。条件引用上游输出时，
> 只能选择当前条件节点之前已经执行的动作节点，后端也会校验其上游关系。

> 2026-09-06 开始/结束节点调整：Workflow 编码由服务端自动生成，不再暴露给用户填写；开始触发节点和
> 结束守护节点都从控制节点库拖入。开始节点必须选择一个已有动作作为触发来源，Workflow 的
> `TRIGGER_CONCEPT_ID` 从该动作的 `conceptId`（函数动作则取 `sourceConceptId`）自动推导，不再单独选择。
> 结束节点不执行业务动作，作为唯一守护终点，禁止继续连接后续节点。

## 1. 结论

当前本体模块已经实现了带持久队列 Worker 的“可靠单动作运行时（Action Runtime）”，并已增加动作编排定义层和 Workflow Runtime V2。V2 支持已发布 ACTION DAG 的启动、结构化条件分支、持久化异步运行、可恢复的并行拆分/汇聚、待审批、失败续跑、取消、按节点策略重试和超时人工对账；并行目前为逐分支 fan-out/fan-in，不是多线程同时执行。失败终态已经支持人工发起补偿计划，但补偿动作必须经过人工审批，不会自动执行。

审批驳回和 `RECONCILIATION_REQUIRED` 会终止对应 Workflow 节点，不会在续跑时自动重新提交或重放动作。

当前已有：

- `Concept / Property / Relation` 及物理表、字段、关系映射；
- `CREATE / UPDATE / DELETE / QUERY / FUNCTION / WEBHOOK` 动作；其中 WEBHOOK 通过现有审批、幂等和执行记录链路调用受控外部 HTTP 接口，不能绕过执行引擎直接发请求。
- SQL 生成、dry-run 预览、直接执行或一次人工确认后执行；
- 执行前后快照、CRUD 回退和错误记录；
- `submissionCriteria` 前置检查（提交时与真正执行前各检查一次）；
- 每个动作最多一次可选人工确认，意见可选；
- `triggerRef` 数据到达触发；
- 函数读取来源概念、处理结果并写回目标概念；
- Webhook 通知和动作血缘记录。
- 数据事件 ID、动作级幂等、`APPROVED → RUNNING` 抢占、自动执行 Worker；
- 超时执行锁转 `RECONCILIATION_REQUIRED`，停止盲目重放。
- `ONT_ACTION_WORKFLOW / NODE / EDGE` 编排定义、DAG 校验、草稿/发布和前端配置页；

当前仍没有：

- 多线程并行度控制、自动补偿和补偿结果自动收敛；节点级超时已采用人工对账模式，尚未提供可中断的硬超时；
- 外部通知渠道（邮件、Webhook、企业 IM）；当前已接入站内 Workflow 告警，统一运行监控已通过监控接口和工作站入口接入；
- 条件求值时的对象版本快照；
- 外部通知渠道（邮件、Webhook、企业 IM）；当前只提供站内 Workflow 告警和运行监控。

动作节点重试策略使用 JSON 保存，例如 `{"maxAttempts":3,"backoffMs":1000}`；`maxAttempts` 包含首次执行，
后端限制为 1～10 次，退避时间采用指数增长并限制在 5 分钟内。节点 `timeoutMs` 字段由 Workflow Worker 监控，超时后将底层执行和 Workflow 运行转为 `RECONCILIATION_REQUIRED`，停止自动重试并保留人工对账边界；不会通过线程中断强行终止数据库/函数调用，避免把已产生副作用的动作误判为可安全重放。

因此，现状应准确描述为：

```text
Ontology + Action Runtime + Optional Human Confirmation
```

目标应演进为：

```text
Ontology + Action Runtime + Workflow Orchestration + Decision Runtime
```

## 2. 当前实现核对

核心实现位于：

- `datamaster-ontology/datamaster-ontology-core/src/main/java/com/datamaster/module/ontology/dal/dataobject/ActionDO.java`
- `datamaster-ontology/datamaster-ontology-core/src/main/java/com/datamaster/module/ontology/service/impl/ActionExecutionServiceImpl.java`
- `datamaster-ontology/datamaster-ontology-core/src/main/java/com/datamaster/module/ontology/service/impl/ActionApprovalServiceImpl.java`
- `datamaster-ontology/datamaster-ontology-core/src/main/java/com/datamaster/module/ontology/service/impl/ObjectInstanceOperateServiceImpl.java`

### 2.1 当前动作生命周期

```text
提交动作
    ↓
生成 SQL / 函数执行计划 / dry-run
    ↓
提交判定
    ↓
免审批 → APPROVED
需审批 → PENDING_APPROVAL
    ↓
人工审批
    ↓
数据触发 → Worker 自动领取执行
人工提交 → 用户确认后执行
    ↓
快照、血缘、Webhook、结果记录
```

### 2.2 当前 `triggerRef` 不是动作链

当前数据到达触发会查询所有匹配 `triggerRef` 的动作，然后逐个提交：

```text
triggerRef
   ├─ submit Action A
   ├─ submit Action B
   └─ submit Action C
```

这属于“触发扇出”，不能表达：

```text
A 成功
  → 条件判断
  → B、C 并行
  → 等待 B、C
  → 执行 D
```

现在同一个受信任数据到达入口还会匹配配置了相同 `triggerRef` 的已发布且启用 Workflow，按 `eventId + workflow` 幂等地异步入队；原有动作扇出仍保留，因此 `triggerRef` 同时可以作为动作入口和 Workflow 入口，但 Workflow 内部的条件、并行和审批关系仍由 DAG 定义负责。

### 2.3 审批链不等于动作执行链

当前 `ONT_ACTION_REQUEST / ONT_ACTION_TASK / ONT_ACTION_REVIEWER` 解决的是：

```text
谁批准、批准几级、每级需要几个人同意
```

它不解决：

```text
批准后执行哪些动作、动作之间如何依赖、失败后如何继续或补偿
```

两者必须分层。

## 3. 必须优先修正的问题

### 3.1 对象状态条件已接入，但对象版本留痕仍待补齐

动作前置条件和 Workflow 条件分支现在都会通过 `ObjectStateConditionResolver` 回查目标对象物理表，解析异常按失败处理；当前仍未记录条件求值时的对象版本快照。

改造要求：

1. 根据 `conceptId + objectKey` 查询对象当前状态（已完成）；
2. 只允许访问概念属性白名单；
3. 对象不存在、字段不存在、表达式错误不得默认通过（已完成）；
4. 区分 `REJECT`、`EVALUATION_ERROR` 和系统异常；
5. 记录条件求值时的对象版本（待补齐）。

### 3.2 `objectKey` 必须使用真实对象主键

当前对象行操作将整行输入 JSON 作为 `objectKey`。这会影响对象条件、审批定位、幂等和血缘。

应从概念的主键属性提取稳定对象标识：

```json
{"tenantId":"T001","orderId":"O1001"}
```

单主键使用字符串；联合主键使用规范化 JSON，保证同一对象始终得到相同键。

### 3.3 提交时锁定动作版本

提交审批后，执行记录必须保存：

```text
actionVersion
functionVersion
functionHash
compiledPlan
```

否则函数在“提交 → 审批 → 执行”期间被修改，批准后可能执行的不是提交时预览的逻辑。

### 3.4 增加幂等和并发控制

需要防止重复点击、重复回调和并发执行。建议增加：

```text
idempotencyKey
expectedVersion
attemptNo
lockTime
```

执行入口使用条件更新或数据库锁，确保同一动作实例不会被并发执行两次。

### 3.5 不把快照回退当成通用补偿

CRUD 可以使用 before/after 快照回退，但函数、ERP API、消息和通知等外部副作用不能天然回滚。

需要为有副作用的动作声明：

```text
compensationActionId
```

例如：

```text
createPurchaseOrder → cancelPurchaseOrder
switchSupplier      → restoreSupplier
```

## 4. 目标架构

```text
Ontology
  ├─ Concept / Property / Relation
  ├─ Action Definition
  └─ Workflow Definition

Runtime
  ├─ Action Executor       执行一个业务命令
  ├─ Workflow Orchestrator 编排多个动作
  ├─ Approval Runtime      处理人工审批
  ├─ External Adapter      调用 ERP/WMS/API/消息系统
  └─ Audit / Lineage       记录结果和血缘
```

设计原则：

- **Action 是一个业务命令**；
- **Workflow 是多个 Action 的执行关系**；
- **Proposal 是待确认的执行计划**；
- **Ontology 是动作操作的业务对象和关系**。

不要继续把流程编排堆进 `ActionExecutionServiceImpl`。

```text
ActionExecutionService
    负责单个 Action 的提交、审批、执行、审计

WorkflowOrchestrator
    负责 Action 之间的顺序、条件、并行、等待和失败处理
```

## 5. 建议新增的数据模型

### 5.1 Workflow 定义

新增 `ONT_ACTION_WORKFLOW`：

| 字段 | 说明 |
|---|---|
| `ID` | 主键 |
| `ONTOLOGY_ID` | 所属本体 |
| `CODE` | 稳定编码 |
| `NAME` | 流程名称 |
| `TRIGGER_CONCEPT_ID` | 当前触发对象所属概念；条件读取 `OBJECT` 时必填 |
| `VERSION` | 发布版本 |
| `STATUS` | `DRAFT / PUBLISHED / ARCHIVED` |
| `TRIGGER_REF` | 事件触发标识 |
| `INPUT_SCHEMA` | 输入结构 |
| `OUTPUT_SCHEMA` | 输出结构 |
| `FAILURE_POLICY` | `STOP / RETRY / COMPENSATE / MANUAL` |
| `ENABLED` | 是否启用 |

### 5.2 Workflow 节点

新增 `ONT_ACTION_WORKFLOW_NODE`：

```text
NODE_TYPE = START / ACTION / CONDITION / APPROVAL /
            PARALLEL_SPLIT / PARALLEL_JOIN / END
```

关键字段：

```text
WORKFLOW_ID
NODE_KEY
NODE_TYPE
ACTION_ID
CONFIG_JSON
TIMEOUT_MS
RETRY_POLICY
COMPENSATION_ACTION_ID
```

### 5.3 Workflow 边

新增 `ONT_ACTION_WORKFLOW_EDGE`：

```text
WORKFLOW_ID
FROM_NODE_KEY
TO_NODE_KEY
CONDITION_EXPR
PRIORITY
```

`CONDITION_EXPR` 第一版保存结构化比较条件，而不是 SQL、SpEL 或任意脚本文本：

```json
{
  "type": "COMPARE",
  "left": { "source": "OBJECT", "path": "stock" },
  "operator": "GT",
  "right": { "mode": "FIXED", "value": 0 }
}
```

动态值来源限定为：

- `OBJECT`：`TRIGGER_CONCEPT_ID` 对应当前对象的已注册属性；
- `INPUT`：本次 Workflow 启动输入；
- `STEP_OUTPUT`：已经执行完成的上游动作标准输出；
- `CONTEXT`：受控运行上下文，如 `objectKey/currentUser/now/eventId`。

前端配置时不再让用户猜测动态值来源：

| 来源 | 值在运行时从哪里取得 | 前端选择方式 |
|---|---|---|
| `OBJECT` | `TRIGGER_CONCEPT_ID + objectKey` 定位到的当前对象 | 从触发概念的属性列表选择 |
| `INPUT` | 启动 Workflow 时传入的 `input` | 从 `INPUT_SCHEMA` 字段选择，也允许输入扩展路径 |
| `STEP_OUTPUT` | 已完成上游动作的标准 `resultContext` | 先选择上游动作节点，再选择 `status/objectKey/affectedRows/rowCount/output/...` |
| `CONTEXT` | Workflow 运行时生成的受控上下文 | 从 `objectKey/currentUser/now/eventId` 中选择 |

图形化编辑器的职责边界：

```text
左侧已有动作 ──拖入──> ACTION 节点
左侧控制节点 ──拖入──> START / CONDITION / PARALLEL_SPLIT / PARALLEL_JOIN / END
START 节点 ──选择动作──> 自动推导触发概念
节点右侧端口 ──拉线──> 下一个节点左侧端口
点击节点/连线 ───────> 右侧属性面板配置
```

当前保存的是可发布的 DAG 定义；Workflow Runtime V2 已支持动作输入、前序节点输出注入、节点输出留痕、`INPUT`、`STEP_OUTPUT`、`OBJECT`、`CONTEXT` 条件求值以及并行拆分/汇聚。
超时已完成安全对账运行时；补偿已支持从失败/需人工对账运行按已执行步骤逆序提交，并根据关联动作执行记录汇总 `REQUESTED / PARTIAL / COMPENSATED / FAILED` 状态，但仍只进入人工审批链。自动执行补偿、外部副作用判定和原 Workflow 状态自动收敛仍未实现，因此仍不能描述为完整的 Saga 流程引擎。

第一版只支持 DAG，禁止环；循环需求通过事件重新触发 Workflow 实现。

### 5.4 Workflow 运行实例和步骤实例

新增：

```text
ONT_ACTION_WORKFLOW_RUN
ONT_ACTION_WORKFLOW_STEP
```

运行实例记录：

```text
WORKFLOW_ID
WORKFLOW_VERSION
OBJECT_KEY
TRIGGER_SOURCE
INPUT_CONTEXT
OUTPUT_CONTEXT
STATUS
IDEMPOTENCY_KEY
START_TIME
END_TIME
ERROR_MESSAGE
```

步骤实例记录：

```text
RUN_ID
NODE_ID
ACTION_EXECUTION_ID
STATUS
ATTEMPT_NO
INPUT_CONTEXT
OUTPUT_CONTEXT
START_TIME
END_TIME
ERROR_MESSAGE
```

现有 `ONT_ACTION_EXECUTION` 增加：

```text
WORKFLOW_RUN_ID
WORKFLOW_STEP_ID
PARENT_EXECUTION_ID
IDEMPOTENCY_KEY
ACTION_VERSION
FUNCTION_VERSION
```

现有审批请求和审批任务增加 `WORKFLOW_RUN_ID`、`WORKFLOW_STEP_ID`，使审批节点可以回到正确的流程实例。

## 6. 执行语义

供应商切换可以表达为：

```text
数据到达
    ↓
计算供应风险
    ↓
风险是否为 HIGH？
    ├─ 否 → END
    └─ 是
         ↓
      生成提案
         ↓
      人工审批
         ├─ 驳回 → END
         └─ 通过
              ↓
       ┌──────┼──────┐
       ↓      ↓      ↓
   创建采购单 更新库存 更新排产
       └──────┼──────┘
              ↓
         汇聚执行结果
              ↓
          通知相关方
              ↓
          关闭风险事件
```

运行时规则：

1. 每个节点默认只执行一次，重试必须增加 `ATTEMPT_NO`；
2. Action 节点复用现有动作提交、审批和执行逻辑；
3. Condition 节点只判断，不直接修改业务数据；
4. Parallel Split 产生多个可执行 Step；
5. Parallel Join 等待依赖分支满足汇聚策略；
6. 每个动作输出合并进 Workflow Context；
7. 失败按节点策略执行重试、停止、补偿或人工介入；
8. 外部调用采用幂等键和最终一致性，不假设跨系统全局事务。

## 7. 建议的代码结构

新增：

```text
service/
  IActionWorkflowService.java
  IWorkflowRunService.java
  IWorkflowOrchestrator.java
  IActionExecutor.java

service/impl/
  ActionWorkflowServiceImpl.java
  WorkflowRunServiceImpl.java
  WorkflowOrchestratorImpl.java
  CrudActionExecutor.java
  FunctionActionExecutor.java
  ExternalActionExecutor.java

dal/dataobject/
  ActionWorkflowDO.java
  ActionWorkflowNodeDO.java
  ActionWorkflowEdgeDO.java
  WorkflowRunDO.java
  WorkflowStepDO.java
```

建议的执行器边界：

```text
CrudActionExecutor       CREATE / UPDATE / DELETE / QUERY
FunctionActionExecutor   FUNCTION
WebhookActionExecutor    WEBHOOK（受控 HTTP，含 SSRF/超时/对账边界）
ExternalActionExecutor   ERP / WMS / 消息 / DolphinScheduler
```

## 8. 分阶段改造计划

### 阶段一：修正单动作运行时

1. 修复 `objectKey`（已完成）；
2. 实现对象状态条件查询（已完成）；
3. 条件解析失败改为拒绝或错误（已完成）；
4. 增加动作和函数版本冻结（已完成）；
5. 增加幂等键、乐观锁和重复执行保护（已完成）；
6. 删除动作前检查执行记录和流程引用；
7. 统一 Action 执行结果结构（已完成）。

### 阶段二：增加最小 Workflow

当前进度：定义层、节点/连线配置、DAG 校验和发布已完成；运行时 V2 已完成 ACTION DAG 启动、结构化条件求值、持久化异步运行、节点步骤持久化、底层动作执行结果关联、并行拆分/汇聚、失败续跑、取消、安全超时对账、统一运行监控、人工补偿计划和补偿状态汇总。运行监控通过 `GET /ont/workflow/monitor?limit=20` 提供当前用户可见运行样本的状态汇总与最近运行记录，工作站动作编排页已提供监控入口。补偿动作必须属于当前本体并配置人工审批，通过 `POST /ont/workflow/runs/{runId}/compensate` 逆序提交；自动补偿、外部系统补偿和补偿结果自动收敛仍待实现。
Workflow `inputSchema` 已在手动启动、数据到达触发和异步执行前执行基础 JSON Schema 校验；运行输入根节点要求为 object，目前支持必填字段、类型、枚举、数组 `items` 和禁止额外字段，复杂 JSON Schema 关键字仍不在运行时范围内。
Workflow `outputSchema` 同样支持在发布时校验定义，并在 Workflow 到达结束节点时校验最终业务输出上下文；运行游标、对象/空间/事件元数据和当前用户等内部字段不会参与 `additionalProperties=false` 校验，输出不符合时运行标记为失败。

先支持：

```text
START → ACTION → ACTION → END
```

然后增加：

```text
CONDITION
APPROVAL
PARALLEL_SPLIT
PARALLEL_JOIN
```

### 阶段三：外部动作和自动补偿闭环

增加 HTTP/API、消息、DolphinScheduler、Webhook 等适配器；在现有人工补偿计划基础上，补齐外部副作用确认、补偿执行状态和原 Workflow 收敛规则。

### 阶段四：Proposal 和 AI 调度

AI 只负责读取对象上下文、分析风险、生成 Proposal 和推荐 Action/Workflow；实际执行必须经过已注册动作、权限、审批和 Workflow Runtime。

不能让大模型直接拼 SQL 或调用任意接口。

## 9. 最终判断

当前模块不需要重做本体模型。正确改造顺序是：

```text
先修正对象条件、版本、幂等和安全问题
    ↓
抽离单动作执行器
    ↓
新增 Workflow 定义和运行时
    ↓
把现有审批链作为 Workflow 节点复用
    ↓
增加并行、条件、补偿和外部系统动作
```

**审批链解决“谁批准”，动作链解决“接下来执行哪些动作、按什么顺序执行、哪些可以并行以及失败后怎么办”。**

---

## 10. DataMaster 自有动作执行方案

本方案是 DataMaster 的目标设计，Palantir 只作为参考对象，不追求原语或内部实现一比一复刻。

### 10.1 V1 不单独引入 Decision 原语

第一版不新增独立的 `Decision` 核心对象。需要“决策”的场景先由以下能力组合实现：

```text
Action
  + Submission Criteria
  + Workflow Condition
  + Approval
  + Audit / Lineage
```

如果后续确实需要“只做判定、不产生业务变更”的场景，再增加 `Decision Definition`；它应作为 DataMaster 的业务扩展，而不是声称与 Palantir 的官方原语对应。

### 10.2 DataMaster 的四类核心对象

```text
Ontology Object
  业务对象、属性、关系

Action Definition
  一个可授权、可校验、可审计的业务命令

Workflow Definition
  多个 Action 的顺序、条件、并行和审批关系

Execution / Workflow Run
  一次实际运行及其每个步骤的结果
```

Proposal 不作为第一版必须的独立表。需要人工确认时，直接使用 `ActionExecution` 或 `WorkflowStep` 的 `PENDING_APPROVAL` 状态承载提案；未来需要多方案比较时再抽取 `Proposal`。

### 10.3 Action 定义扩展

在现有 `ONT_ACTION` 基础上补充：

```text
ACTION_CODE
VERSION
STATUS                 DRAFT / PUBLISHED / ARCHIVED
EXECUTOR_TYPE          CRUD / FUNCTION / WEBHOOK / MESSAGE / DS
INPUT_SCHEMA
OUTPUT_SCHEMA
PRECONDITION_EXPR
APPROVAL_POLICY
RETRY_POLICY
TIMEOUT_MS
COMPENSATION_ACTION_ID
```

现有 `actionType` 保留作为兼容字段，但新代码应通过 `EXECUTOR_TYPE` 和动作配置决定执行器。

### 10.4 ActionExecution 只负责单动作

现有 `ONT_ACTION_EXECUTION` 继续作为单动作执行记录，增加：

```text
ACTION_VERSION
FUNCTION_VERSION
IDEMPOTENCY_KEY
WORKFLOW_RUN_ID
WORKFLOW_STEP_ID
PARENT_EXECUTION_ID
ATTEMPT_NO
RESULT_CONTEXT
ERROR_CODE
```

单动作生命周期：

```text
SUBMITTED
  ↓
PRECHECKED
  ↓
PENDING_APPROVAL / APPROVED / REJECTED
  ↓
RUNNING
  ↓
SUCCEEDED / FAILED / COMPENSATING / COMPENSATED
```

现有 `DRAFT / PENDING_APPROVAL / APPROVED / REJECTED / EXECUTED / FAILED / ROLLED_BACK` 作为兼容状态保留，新增状态逐步迁移。

### 10.5 Workflow 负责编排，不负责直接写业务表

Workflow Runtime 只做以下事情：

1. 找到可执行节点；
2. 根据边条件选择下一步；
3. 创建 ActionExecution；
4. 等待审批、重试或并行分支；
5. 汇总步骤输出；
6. 推进后续节点；
7. 处理失败、补偿和人工介入。

真正的业务写入必须经过 `IActionExecutor`：

```text
CrudActionExecutor
FunctionActionExecutor
HttpActionExecutor
MessageActionExecutor
DolphinSchedulerActionExecutor
```

### 10.6 事务和外部副作用边界

当前实现通过 `DataSourceFactory / DbQuery` 访问概念绑定的物理库，而 `ActionExecution`、审批和审计记录写在 DataMaster 管理库中。因此不能默认认为“对象变更 + 执行记录”属于同一个本地事务。

只有在以下条件同时满足时，才可以使用一个本地事务：

1. 目标对象库和执行记录库是同一个数据库；
2. 使用同一个 Spring `TransactionManager`；
3. 目标写入使用同一个事务连接，而不是另行打开 JDBC 连接。

跨数据库、跨数据源或调用外部系统时，采用状态机、幂等命令和补偿/对账，不承诺全局原子性：

```text
管理库事务：执行记录状态 + 审计 + Outbox/命令日志
目标库事务：对象数据变更
外部系统：API / 消息 / ERP / DolphinScheduler
```

外部调用不放在长事务里，而是写入 Outbox 或命令日志：

```text
本地事务提交
    ↓
ONT_ACTION_OUTBOX
    ↓
异步投递 HTTP / 消息 / ERP / DolphinScheduler
    ↓
成功、重试、死信或人工处理
```

建议新增 `ONT_ACTION_OUTBOX`：

```text
ID
EXECUTION_ID
WORKFLOW_RUN_ID
EVENT_TYPE
PAYLOAD
IDEMPOTENCY_KEY
STATUS
ATTEMPT_NO
NEXT_RETRY_TIME
ERROR_MESSAGE
```

对于管理库和 Outbox 在同一个数据库中的场景，这可以避免“管理库已提交但异步通知任务没有记录”的问题；但如果目标物理库是另一个数据库，仍然存在目标库已提交而管理库更新失败的情况，必须依靠幂等重试、状态查询和对账任务收敛，不能声称完全原子。

执行状态建议至少区分：

```text
PREPARED
APPLYING
APPLIED
OUTBOX_PENDING
SUCCEEDED
FAILED_RETRYABLE
FAILED_FINAL
RECONCILIATION_REQUIRED
```

当前 `ActionExecutionServiceImpl` 中捕获异常后直接设置 `FAILED`，也不能替代跨库事务；后续应将可重试失败、最终失败和需要人工对账的失败分开。

### 10.7 V1 执行约束

为了控制复杂度，第一版采用以下约束：

- Workflow 只允许 DAG，禁止环；
- 每个 Action 节点绑定一个 Action Definition；
- Condition 只能读取已授权的对象属性、输入参数和上游输出；
- 并行只允许分支之间无写冲突，或显式配置冲突策略；
- Join 默认等待全部分支成功；
- 外部调用必须有幂等键；
- 自动回退只适用于明确声明补偿动作的节点；
- AI 只能选择已注册 Action，不能直接执行 SQL 或任意 HTTP。

### 10.8 推荐 API

保留现有单动作 API：

```text
POST /ont/action/execution/submit
POST /ont/action/execution/approve
POST /ont/action/execution/reject
POST /ont/action/execution/run/{id}
POST /ont/action/execution/rollback/{id}
```

新增 Workflow API（当前实际路径）：

```text
GET  /ont/workflow/page
GET  /ont/workflow/{id}
POST /ont/workflow
PUT  /ont/workflow
POST /ont/workflow/{id}/validate
POST /ont/workflow/{id}/publish
POST /ont/workflow/{id}/disable
POST /ont/workflow/{id}/run
GET  /ont/workflow/runs/{runId}
GET  /ont/workflow/{id}/runs/page?pageNum=1&pageSize=10&status=FAILED
GET  /ont/workflow/{id}/versions
GET  /ont/workflow/{id}/versions/{version}
POST /ont/workflow/{id}/versions/{version}/rollback
POST /ont/workflow/runs/{runId}/resume
POST /ont/workflow/runs/{runId}/cancel
POST /ont/workflow/runs/{runId}/compensate
POST /ont/workflow/runs/{runId}/compensate/retry
GET  /ont/workflow/monitor?limit=20
```

### 10.9 实施顺序

```text
第一步：修复 objectKey、对象状态条件、版本冻结和幂等
    ↓
第二步：将 ActionExecutionService 拆出 IActionExecutor
    ↓
第三步：增加 Workflow Definition / Node / Edge
    ↓
第四步：实现串行 Action 链
    ↓
第五步：增加 Condition、Approval、Parallel Split/Join
    ↓
第六步：增加 Outbox、重试、死信和补偿
    ↓
第七步：再考虑 Proposal、多方案决策和 AI Agent 调度
```

这套方案的定位是：**用现有本体和动作能力做基础，补上可审计、可恢复、可编排的业务执行层；不依赖 Palantir 的未公开内部实现，也不把未经确认的 Decision 当作外部平台事实。**
