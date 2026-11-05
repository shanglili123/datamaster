# LangChain4j 框架监控与路由增强（2026-11-02）

## 范围与验证状态

在现有统一请求网关上增强，不创建第二套计量系统。源 POM 的 LangChain4j 从 0.35.0 改为 1.23.0，保留 Java 17 / Boot 2.7.18。版本和接口依据 Maven Central 发布元数据与对应 sources.jar 核对，而非猜测 API。

**本轮未运行编译、构建、测试、依赖解析、数据库迁移、服务启动或真实模型调用。此前 Java 17 编译/打包成功属于旧 SDK，不能证明本轮 1.23.0 改动已验证。**

## 关机交接快照（2026-11-02，下次先读）

### 已完成与当前停点

- 用户升级目标：复用框架增强已有能力，重点为 DecisionServices/Models、Model Routing 和统一监控；结构化输出、决策主 Agent 与分析/规划子 Agent 之前已经实现，不应重复建设。
- 当前分支 `dev_1029_2.0`。先前 Java17/Boot2.7 改动已出现提交 `cd0b31be`（用户侧提交，标题“升级sdk”），但当时 LangChain4j 仍为 0.35.0。本会话后续的 1.23.0、监听器、路由与 DecisionServices 代码保存在工作区，未由本助手提交。恢复时先检查实际 git status，不覆盖用户的新修改。
- 旧 SDK 阶段：JDK17.0.19 + Maven3.8.1 下主服务及依赖36个反应堆项目编译、跳过测试打包成功；db-init编译成功。随后隔离启动因数据库连接失败中断。用户已明确解释：**依赖服务器没有开启**，不能归因于 Java17；测试进程已清理，18989/12138端口在清理时均释放。
- **当前新 SDK 阶段：源配置已改 LangChain4j1.23.0 + Jackson BOM2.22.4，已适配 ChatModel/AiServices/工具Schema，接入框架监听、规则路由及可选 DecisionServices 语义模型等级分诊。尚未编译、测试或启动。旧 SDK 成功结果不适用于这些新改动。**
- 保留原统一 AiInvocationGateway / 统计表 / 监控页，不另建监控系统。DecisionServices 经 ChatBackedDecisionModel 复用原聊天模型与监控；不要求新供应商或 /decisions。业务决策主 Agent、Java测算、权限、人工确认、执行幂等保持原样。
- 模型路由、语义决策路由和供应商 strict Schema 均默认关闭，需要受控配置后才启用；不在关机前自动开启或调用外部模型。

### 下次优先事项

> 2026-11-03 续接：用户选择只做静态检查和修复。本轮已修复新版 SDK 吞工具异常/继续模型循环、SDK 超时分类、分类输入转义膨胀回退及命名字段多模态拒绝，新增回归测试代码；仍未运行编译或测试。详见后文“静态续接修复”，下次先检查这些新增工作区文件。

1. 先阅读本记录并核对工作区，避免把新增未跟踪Java文件或测试漏掉。新核心文件：`AiModelRoutingProperties`、`InvocationChatModelListener`、`InvocationFailureClassifier`、`TaskComplexityModelRouter`、`ChatBackedDecisionModel`、`FrameworkDecisionModelRouter`，以及四个对应测试。
2. **先验证本轮 SDK 迁移，再叠加功能。** 用户下次明确授权后使用JDK17.0.19和Maven3.8.1，先编译 AI及依赖，再执行 focused 假模型测试；优先检查新API签名、工具执行异常传播、结构化POJO解析、ChatMemory、大ID、并发拒绝、路由续轮、拒答处理和监控单次计量。尤其审查新版 DefaultToolExecutor 的异常包装/返回行为与现有 AgentToolFlowHaltException 是否仍能正确中止，不能只看编译。
3. 根Jackson BOM升级影响全项目；Boot2.7仍管理旧SLF4J/Logback，SDK发布POM请求SLF4J2，需核验实际依赖及启动兼容，不能据源码部分搜索宣称兼容。
4. 用户打开依赖服务器后，再在隔离端口/只读主库/关闭后台执行及组件启动恢复的条件下启动验证；不擅自换库、启动生产worker或调用真实模型。正常运行与端到端验收另行授权。
5. 明确剩余空间上下文缺口：普通Skill/本体页面可能缺可信空间，当前仍未修复；不能从共享Skill随意选第一个空间。后续先设计可信空间传递契约再补齐。
6. 再按收益顺序增强：**AI Service生命周期事件 + 输出Guardrails → Tool Search按需加载 → 有限只读并行/原生流式**。这些仅讨论、未实施，用户尚未选定下一项。先权限allowlist再检索工具；护栏不代替领域校验/确认；并行前检查线程上下文安全。
7. 价格/费用估算、实际扣费、模型质量评测仍未实现；Token统计不是账单，方案评估不是LLM质量评测；DB-GPT只统计外层调用。

### 验证命令参考（本轮未执行）

Windows Git Bash显式选择本机已安装工具，不修改系统默认Java/Maven：

```bash
export JAVA_HOME='C:/Users/Administrator/.jdks/ms-17.0.19'
export PATH="$JAVA_HOME/bin:$PATH"
MVN='/d/soft/IntelliJ IDEA Community Edition 2021.3.2/plugins/maven/lib/maven3/bin/mvn'
"$MVN" -pl datamaster-ai -am compile -DskipTests
"$MVN" -pl datamaster-ai -am test \
  -Dtest=ChatBackedDecisionModelTest,FrameworkDecisionModelRouterTest,InvocationChatModelListenerTest,TaskComplexityModelRouterTest,StructuredModelOutputTest,DecisionPlanningOutputTest,AgentToolIdContractTest,AgentToolExecutionHaltTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

默认 Maven3.6.0 不满足插件要求（至少3.6.3），不要通过降级插件或跳过版本检查掩盖问题。按仓库约定，仅在用户授权验证后执行上述命令；不因这份交接记录自动运行。


## 静态续接修复（2026-11-03）

用户本轮选择**只做静态检查和修复**，不授权编译、构建或测试；本节不代表运行验证通过。通过本机缓存的 SDK 1.23.0 jar 的 `javap` 接口/字节码只读检查，确认并修复：

1. **工具执行异常不能成为正常工具文本。** `new DefaultToolExecutor(bean, method)` 默认 `propagateToolExecutionExceptions=false`，反射调用抛出的业务异常会变成错误文本；现有字符串适配还会丢失框架 `isError`。新增 `AgentToolExecutors`，显式使用 builder 开启异常传播；SDK 包装的 `AgentToolFlowHaltException` 恢复为平台原异常，保留待补充/待确认状态。目录工具、人工调用及决策工具边界统一复用。
2. **AiServices 工具错误必须立即终止。** 1.23.0 同步工具循环默认将异常文本发回模型继续执行，因此仅让执行器抛异常还不够。Data Agent（含保存重试分支）、指标 Agent、决策主 Agent 和规划子 Agent 均显式配置 `ToolExecutionErrorHandler.failInvocation()`，避免待确认、待补充、失败、取消或预算异常被转换为下一轮模型输入。未启用并行工具执行。
3. **超时分类补齐 SDK 自身异常。** `dev.langchain4j.exception.TimeoutException` 可以只有消息而无 Java 超时 cause；统一分类器现在按该类型记 `TIMEOUT`，不读取或保存异常消息，不猜测 HTTP 状态。
4. 新增未执行的 `AgentToolExecutionHaltTest`，覆盖执行器保留原终止结果、AiServices 不再请求下一轮模型、同批后续工具不执行、正常成功工具仍可续轮、预算拒绝、目录的三个终态、人工调用保留待补充状态、反射异常不作为成功文本、决策边界记录失败；在 `InvocationChatModelListenerTest` 补 SDK 原生超时单次落库与 permit 释放用例。新增测试不依赖真实模型/数据库，也未额外引入测试库。

5. **分类 JSON 转义膨胀保守回退。** 短于规则阈值的文本（例如 11999 个反斜杠）在 JSON 转义后仍可能超过适配器 24000 字符上限。新增仅表示本地大小限制的 `InputTooLargeException`，语义路由只对该异常回默认 advanced，不笼统吞掉 `IllegalArgumentException` 或供应商/容量错误；补充未执行的回归用例。
6. **拒绝命名字段内嵌多模态内容。** SDK `FieldsInput` 可包含 `Content` 或其列表，不能将其 JSON 文本化后假装已理解图片。适配器序列化前明确拒绝这类字段，抛 `UnsupportedFeatureException`；普通命名字段仍支持。补充图片、图片列表拒绝且不调用模型，以及普通字段继续工作的测试代码。

前文 focused 测试列表已加入 `AgentToolExecutionHaltTest`，仅供后续获得授权时执行。仍需编译与实际测试确认；未启动服务、连接业务数据库、调用真实模型或切换路由开关。

## 已修改

1. 现有 Agent、指标分析、决策预算包装和结构化 AiServices 从 ChatLanguageModel/chatLanguageModel 迁移至 ChatModel/chatModel；工具目录从 ToolParameters 迁移到新版 ToolSpecification 的公开 JSON Schema 导出，保留工具字符串 ID 契约。
2. Data Agent 移除重复 OpenAiChatModel 构造，统一使用 GovernedChatModelFactory，避免只有部分入口获得新监听/路由功能。
3. OpenAiChatModel 注册 ChatModelListener，响应和错误事件更新原 AiInvocationGateway.Invocation。准入在外层完成：框架会忽略监听器异常，不能把 Semaphore 准入放到 onRequest。
4. 每次框架 chat 仍只有一个逻辑 lease，包含 SDK 内部重试。监听器不新增记录、不重复释放，外层 finally close 持久化。仍不代表每次 HTTP 尝试、供应商完整账单或结构化业务校验成功。
5. 根据异常类型识别 HTTP 状态、超时、网络、取消，禁止持久化原始异常文本、提示词、响应正文、密钥或端点。保留用量未知为 NULL，不估算 Token。
6. 使用框架 RoutingChatModel / ChatModelRouter；每个实际候选模型都独立包原准入/监控网关，记录实际选择的供应商/模型。路由工具续轮由框架的 route attribute 保持一致。
7. 新增默认关闭的 ai.model-routing 配置，只接受运维配置的空间内模型 profile ID。profiles.resolve(spaceId, profileId) 校验同一空间、启用状态和凭据；不从用户文本或模型输出选 URL/密钥。错误不静默切换供应商。
8. SDK 1.23.0 的发布 POM 依赖 Jackson 2.22.4，根 POM 显式导入一致的 Jackson BOM，避免 Boot2.7 管理版本把它降到旧版。此项影响全平台 Jackson，需完整回归，不宣称仅影响 AI 模块。保留 Boot 现有 SLF4J/Logback 管理，不为本轮强行切换日志栈；新 SDK/其传递依赖与旧日志绑定仍需启动验证。

## 路由启用方式

先在已有“模型调用监控与配置”页面，为**同一个空间**新增并启用经济、高级两个模型配置；它们不必都设为默认。模型配置表和加密密钥需按此前流程部署，不在本轮自动创建或生成。

以下仅示例，101/102 必须替换为该空间真实 profile ID，1 替换为真实 space ID：

```yaml
ai:
  model-routing:
    enabled: true
    complex-input-characters: 12000
    spaces:
      1:
        economy-profile-id: 101
        advanced-profile-id: 102
        strict-json-schema: false
```

- 不启用、当前可信空间未配置路由、无空间上下文：保留现有空间默认模型/部署默认模型逻辑，不跨空间找其他候选。
- 短文本、无工具、非高风险场景优先 economy；长输入、多模态、工具调用、原生 Schema 请求，以及决策/动作/本体生成/ETL诊断场景优先 advanced。
- 默认仅做明确的场景/输入规模启发式；可按下节显式启用框架语义决策分类，增加一次被计量的分类请求。不保证费用一定降低或答案质量。
- 指标和管理 Agent 常带工具，因此通常使用 advanced；不能宣称所有简单指标问题均会降到便宜模型。
- 不做跨模型故障重试/降级，不把一个厂商失败后的业务上下文自动发送给另一个厂商。
- 框架路由 API 在当前版本标记 Experimental，已将依赖固定在 1.23.0，不自动追随最新版。

## 框架 DecisionServices 语义决策（后续改造）

用户确认使用框架决策抽象而不更换供应商。本轮新增 ChatBackedDecisionModel，实现框架 DecisionModel，通过已有 ChatModel + AiServices 适配有限枚举选择；使用原聊天协议，不要求厂商提供 /decisions。FrameworkDecisionModelRouter 真正创建 DecisionServices，使用 @Decide 声明问题和枚举描述，不是把普通 service 命名为框架决策服务。

```yaml
ai:
  model-routing:
    enabled: true
    spaces:
      1:
        economy-profile-id: 101
        advanced-profile-id: 102
        semantic-decision-enabled: true
        decision-profile-id: 101
```

- semantic-decision-enabled 默认 false，不会因部署代码自动增加模型消耗；decision-profile-id 可省略，默认当前空间 economy profile，若显式填写则仍校验同空间及启用凭据。
- 分类器由工厂直接创建**不带路由**的受治理聊天客户端，避免分类器递归分类自身。分类调用场景为 MODEL_ROUTE_DECISION，真实目标模型另记原业务 scene；监控会正确体现两次模型调用及各自用量，而非重复记同一调用。
- 场景/工具/输入上限等硬边界仍优先，高风险和工具请求不能被语义分类降到 economy；其余请求由 DecisionServices 返回 ECONOMY / ADVANCED / NEED_INPUT。此处是模型等级分诊，不改变正式指标/探索/管理业务入口，也不生成执行许可。
- 拒答、无法判断、无效选项/格式回默认 advanced；容量、网络等供应商错误传播，不吞错发起其他厂商请求。不会把 NEED_INPUT 当成已判定简单；路由的 NEED_INPUT 仅表示分类不确定，不自动代替业务澄清流程。
- 适配器只支持 ChoiceQuestion，返回值严格限制在框架声明的选项内；问题遗漏、重复、非法选项拒绝。未报告的 probabilities/confidence 保持为空，不编造 0/1，不使用缺乏校准的自报概率触发阈值。
- 布尔概率阈值、Scale 连续评分、多模态和专用原生 DecisionModel 协议不在该适配器支持范围。不把聊天选择包装成概率决策能力。
- DecisionServices、DecisionModel 与路由 API 均为 Experimental，固定版本；本次仍未执行任何编译、测试或真实模型验证。
- 新增 ChatBackedDecisionModelTest 和 FrameworkDecisionModelRouterTest（未执行），覆盖框架枚举服务、拒答、非法/重复选项、语义升级、默认保守回退、硬边界和容量错误传播。

## 原生严格结构化输出

路由配置可显式设置 strict-json-schema=true，工厂才声明 RESPONSE_FORMAT_JSON_SCHEMA 并配置 strictJsonSchema(true)，供 AiServices 生成原生 response_format。默认 false，保留现有 POJO 提示与解析方式。

开启前必须确认**该空间两个路由供应商/模型和实际输出 DTO 都支持所需 Schema**。动态 Map/开放字段可能受供应商 strict Schema 限制；不支持时应保持关闭，不把配置存在视为能力认证。领域校验、候选白名单、人工确认和权限不因 Schema 开启而撤除。

## 保留的限制与未做事项

- 普通 Skill/本体页面的空间上下文缺口**未在本轮修复**。调查发现部分 Skill 本身无唯一空间归属，不能随意从共享数据源挑一个空间。后续需在已授权入口明确空间契约，再统一传给模型网关；本轮不会从未校验 HTTP 参数或线程状态猜空间。缺少可信空间的入口仍用部署配置，统计仍可能为空空间。
- DB-GPT 保持既有 AOP 外层调用监控，内部模型、向量化和计费不受本平台 RoutingChatModel 控制。
- 旧 complete 文本兼容入口仍使用原 HTTP 协议和监控，不在本轮迁移到新框架路由；当前六条结构化业务链及 Agent 走新工厂。
- 未新增价格、费用估算、扣费、预算冻结、分布式配额或模型质量评测；没有把 Token 统计改称实际账单。
- 决策主/子 Agent 仍使用既有服务端组合，未引入额外 agentic 模块，不替换确定性方案测算及原动作/Workflow 引擎。
- 没有前端新路由管理页面，本轮路由策略由受控部署配置启用。

## 新增/适配的未执行测试

- InvocationChatModelListenerTest：框架回调与外层网关只落一条记录、容量超限不调用模型、HTTP错误分类与未知用量。
- TaskComplexityModelRouterTest：短/长文本、决策/工具请求路由及工具续轮保持框架 route。
- StructuredModelOutputTest / DecisionPlanningOutputTest：适配新版 ChatModel 假模型。
- AgentToolIdContractTest：适配新版工具 Schema JSON，继续检查大 ID 字符串契约。

建议用户授权后先执行 AI 及依赖的编译和 focused 测试，再完整打包/启动；重点回归 Spring MVC/Jackson、Redis JSON、工具 Schema/执行器、ChatMemory、大 ID、结构化解析和并发准入，最后在授权测试空间验证真实模型路由及监控入库。

## 接口依据

- https://repo.maven.apache.org/maven2/dev/langchain4j/langchain4j-core/maven-metadata.xml
- https://repo.maven.apache.org/maven2/dev/langchain4j/langchain4j-core/1.23.0/langchain4j-core-1.23.0-sources.jar
- https://repo.maven.apache.org/maven2/dev/langchain4j/langchain4j/1.23.0/langchain4j-1.23.0-sources.jar
- https://repo.maven.apache.org/maven2/dev/langchain4j/langchain4j-open-ai/1.23.0/langchain4j-open-ai-1.23.0-sources.jar
