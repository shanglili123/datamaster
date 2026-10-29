# 决策主 Agent 实施记录

## 已实现链路

现有AI问数页选择“决策分析”后，经`/ai/ask-data/chat/stream`的公共用户、空间、会话、本体、数据源校验，进入独立`DecisionAgentService`，不进入Data Agent平台管理流程。

1. LangChain4j主Agent调用`decision_analyze`，复用只读指标子Agent或受限DB-GPT探索接口。
2. `decision_plan`调用专业规划Agent，按需读取本体已有动作和已发布Workflow，生成最多3个处置候选与不干预基线。
3. `DecisionEvaluationService`清除模型伪造的量化收益与约束PASS。新增`DecisionCalculationService`、`StandardCostCapacityEvaluator`、`DecisionComparisonService`实现确认输入后的成本、供给、需求缺口、预算与确定性排名；尚未测算的候选保留UNKNOWN。**不是制造排产求解器，不声称全局最优。**
4. 用户选候选并准备预览，服务端读取定义、检查对象访问、必填参数与单动作前置条件，冻结新版本、参数及摘要；不调用submit、不试运行函数/Webhook。
5. 明确确认后，服务端行锁和唯一dispatch记录消费准确版本，使用稳定幂等键提交既有动作/Workflow，保留原审批及Worker。确认不调用模型，不接受客户端替换参数。
6. 页面可查询执行状态，并在成功后重放服务端保存的正式单指标查询，记录只读复查结论。复查沿用原日期/过滤，不自动改变窗口；不做因果归因，不自动声称改善。只有两期比较或探索文字证据时返回UNKNOWN。

## 主要文件

- `datamaster-ai/.../agent/runtime/DecisionAgentService.java`：协调及专业规划工具循环，工具对象仅本轮创建，不自动注册进管理Agent目录。
- `datamaster-ai/.../agent/decision/DecisionModels.java`：结构化证据、候选、评估与执行绑定。
- `DecisionBudget.java` / `DecisionChatModel.java`：共享调用预算、取消与时限检查；不能强制中断已经在途的模型HTTP或业务执行。
- `DecisionProposalService.java`：不可变版本、归属、确认、幂等提交与结果未知对账。
- `DecisionBindingResolver.java`：只读定义/对象校验及摘要；单动作前置条件校验，Workflow完整schema与步骤条件仍由原运行时最终检查。
- `DecisionProposalController.java` / `DecisionReviewController.java`：提案读取、准备、确认、状态及复查API。
- `AskMetricAgentService.java`：只读子任务入口，排除save_analysis，复用现有模型调用治理；原问数保存路径保留。
- `AgentToolContextHolder.java`：嵌套作用域恢复CONTEXT/RESULTS/CALLS，避免子Agent清理主Agent状态。
- `datamaster-ui/src/views/ai/chat/index/components/DecisionProposal.vue`：候选、证据、预览确认、状态及复查交互。

## 数据库前置（未执行）

人工审阅并在平台元数据库执行：

`sql/update_20261028_ai_decision_proposal.sql`

新增`ai_decision_proposal`与`ai_decision_dispatch`。脚本采用PostgreSQL兼容DDL（含COMMENT ON、CREATE INDEX IF NOT EXISTS）；其他元数据库应先适配。**本次没有运行迁移脚本，没有运行时自动创建决策表**。未迁移前决策模式不能落库，普通问答不依赖新表。

需要现有`AI_AGENT_OPERATION`和指标相关表已按原功能准备；不要把多个历史迁移脚本直接批量运行。

## 权限与副作用

- 查询要求`ai:ask-data:query`，准备/确认另要求`ont:action:edit`，且逐次校验真实所有者、空间成员、本体及会话。
- 正式指标沿用原查询权限、发布版本与数据治理；能力发现要求动作查看权限。复查要求指标查询及执行权限。
- 请求不提供执行权限的通用confirmed开关。模型工具中没有submit或execute，管理Agent也不会自动获得决策工具。
- `submitExecution`本身可能进入自动执行队列，不能用作预览。用户确认后即可能产生真实业务副作用；提交成功不是执行成功。
- 结果未知时按相同幂等键查回执行记录，不自动重提，不盲目重放外部副作用。没有查到可靠记录时进入`RECONCILIATION_REQUIRED`。
- 提交DTO新增可选`expectedVersion`，动作/Workflow在实际读取定义时复核；调用方不传时维持原行为。预览/确认还比较定义与对象行摘要。
- 当前未提供跨数据库或跨外部系统exactly-once保证；在最后状态读取和业务写入间仍依赖既有动作前置条件/锁。Workflow当前实现也不是整条图的不可变快照执行器，运行中管理员修改定义的处理沿用原运行时，不能宣称本次已解决所有定义漂移竞态。

## 已知边界

- DB-GPT同步响应无法证明远端实际查询，标记`UNVERIFIED_EXPLORATORY`，不能当作正式指标验真。
- 方案影响默认清空模型数字并标unverified；未配置领域计算器时，界面明确UNKNOWN，须人工核实。当前不从Markdown解析可执行指令。
- 首版绑定一个已有单动作/复合动作或已发布Workflow，不临时创建发布新动作，不执行模型生成的自由步骤列表。
- 自动选中的参数仍须预览确认，必填/完整schema最终由原执行端复核；缺参会停止，不自动猜对象主键。
- 提案首版绑定本体，未选本体时需补选；历史提案按服务端原本体归属校验，不会因页面选项改变而改绑。
- 结构化证据和有限查询结果会进入元数据库提案/操作与现有审计，会话也有展示快照；需沿用业务数据保留和访问策略，脱敏键过滤不是全量业务脱敏。
- 额度/超时失败可以留下ANALYZING操作/初始提案，过期后不可执行；本次未增加后台自动重跑决策。

## 类型化结构化输出

`DecisionAgentService.PlanningAssistant.plan`现在直接返回`DecisionPlanningOutput.Result`，通过LangChain4j AI Services解析POJO，移除手工搜索方括号、截取字符串及候选JSON数组解析。输出采用专用草稿DTO，只含方案名称、理由、证据引用、风险、约束、影响名称/单位和建议绑定，不包含提案ID、计算结果、确认人、审批状态、定义版本。通过显式映射生成业务Candidate，既有候选白名单、数量限制、证据与参数校验继续执行。

约束状态用UNKNOWN/FAIL枚举，动作绑定类型用ACTION/WORKFLOW枚举；信息不足通过missingInformation返回。框架解析失败沿用NEED_INPUT/失败关闭，不尝试从任意文本拼凑数组，不提交业务执行。

本次保持LangChain4j0.35.0和模型配置，不启用未经供应方确认支持的`json_schema/strictJsonSchema`；当前是框架类型化输出+解析，不能宣称已经启用模型供应方原生严格Schema保证。主Agent的人类可读解释仍为String，业务计算和执行状态不由模型输出。没有修改单动作选择器的跨模块模型网关，避免将本体模块耦合到AI运行时。

新增`DecisionPlanningOutputTest`使用本地假模型覆盖POJO解析、字符串ID精度、缺失信息、服务端字段隔离、无效输出拒绝；未运行。

## 标准测算使用与样例

候选生成后展开“标准测算”：填写共同对象范围、明确统计窗口、币种/产出单位、需求、已有供给、预算（或明确不设预算限制）；每个候选填一次固定费和1至10条投入的数量/单价/单位产出。数值可来自服务器保存的未截断正式单指标单元格，也可为用户明确确认的假设。正式指标单位必须与字段单位完全相同；不从摘要取数字，不允许探索文字或维度列冒充正式数值。

确认输入后调用`POST /ai/decision/calculate`，使用BigDecimal计算，生成新提案revision，清空旧执行预览。不新增数据库表，新增测算字段保存在现有提案JSON。输入快照、来源、确认人/时间与摘要一起持久化，结果不是实时保证。

可复现例：共同需求100件，已有供给40件，增量预算500 CNY。

| 方案 | 数量 | 单价 | 每单位产出 | 一次固定费 | 增量成本 | 剩余缺口 |
|---|---:|---:|---:|---:|---:|---:|
| A | 60小时 | 5 CNY/小时 | 1件/小时 | 20 CNY | 320 CNY | 0件 |
| B | 10小时 | 25 CNY/小时 | 6件/小时 | 0 CNY | 250 CNY | 0件 |
| 不干预 | 0 | — | — | 0 | 0 CNY | 60件 |

“预算内满足需求，增量成本最低”策略按已测算候选推荐B；如果同成本则并列；不干预不满足需求而不参与此策略排名。另一策略先最小缺口再最小成本。未知/超预算/硬约束FAIL不参与推荐；只算部分候选时标PARTIAL。结果为给定产出系数等假设下的条件性推荐，不证明交期、设备冲突或因果收益。

执行时每条投入quantity需映射到已有动作/Workflow的唯一顶层参数，例如quantity。服务端在prepare/confirm/dispatch精确复核数量；还绑定测算时已有的目标、对象和整份执行参数摘要，参数变化或未绑定只能比较，不能按已测算方案静默提交。人工测算表单不接受任意动作写参数。可明确勾选“同步执行数量”，只更新映射到已有数值参数的数量，不新增字段、不改其他参数；随后冻结整份目标/参数摘要并重新预览确认。未勾选时数量不一致会阻止提交，需调整后重新测算。

新增未执行测试`DecisionCalculationServiceTest`涵盖成本/排名样例、精确预算边界和并列、缺参不当零、截断证据拒绝、执行数量变化拒绝。

## 验证状态

仅完成静态阅读与增量检查；**未编译、未运行测试、未lint、未构建前端、未执行迁移、未调用真实模型或业务执行接口**。

新增未执行单元测试：`DecisionEvaluationServiceTest`、`AgentToolContextScopeTest`、`DecisionToolSchemaTest`、`DecisionBindingResolverTest`。

用户本地重点验证：Java8与LangChain4j0.35工具schema、普通qa/report及指标保存回归、越权与跨空间、候选选择与新版本、重复确认不重复提交、参数或定义变化停止、审批等待与执行成功区分、提交响应丢失后按键对账、原查询复查与未知效果标记。仅在授权测试环境使用无破坏性动作联调。
