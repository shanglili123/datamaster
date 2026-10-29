# 数据分析 Agent：意图路由与指标工具

## 产品方向

不建设独立传统 BI、DataEase 嵌入或拖拽看板。复用当前 LangChain4j 0.35.0，将指标定义、维度、发布口径和受控查询沉淀为 AI 可调用的语义资产。

现有能力分工：

- 本体模块维护指标、业务对象和不可变版本。
- MetricAnalysisService 执行参数化、限量且经授权的 Doris ADS 查询。
- LangChain4j 负责理解目标和调用工具，不直接计算数据库 SQL。
- DB-GPT 保留探索性问数；不会在正式指标工具失败后自动作为降级路径。
- 指标分析表格和个人保存条件页面仅作人工验收、复用和排障，不发展为 BI 编辑器。

## 路由

入口已改到现有AI问数页面，统一 `/ai/ask-data/chat/stream`。正式指标由独立AskMetricAgentService执行，探索问数直接复用DB-GPT。LangChain4jAgentService仅用于Data Agent流程建设与指标草稿生成，不再承接这条分析路径。详见[职责分离实施记录](AI问数与DataAgent职责分离实施记录.md)。规则在模型调用前执行，不新增分类模型请求。

| 意图 | 例子 | 可用工具 |
| --- | --- | --- |
| MANAGEMENT | 创建数据源、生成本体、生成指标草稿 | 问数页提示去Data Agent；Data Agent仅管理工具及preview_metrics/save_metric_draft |
| METRIC | 本月支付金额、订单数趋势、指标同比报告、保存分析 | search_metrics、get_metric_definition、execute_metric_analysis、compare_periods、analyze_distribution、analyze_trend、build_chart_spec、save_analysis |
| EXPLORATORY | 查询订单明细、查看表字段、临时查数 | 问数入口直接委托既有DB-GPT问答/报告服务，不将管理工具交给模型 |
| CLARIFY | 生成本体后分析销售额、指标加自由SQL、无法判断 | 不调用模型，返回 NEED_INPUT |

指标意图不会因为含“报告”被强制改为探索；指标创建单独引导到Data Agent。原管理快捷分支只在流程建设运行时存在，问数独立运行时不经过这些分支。

Data Agent继续使用既有平台流程续跑；指标问数每个新问题建立独立ASK_METRIC操作，普通问数会话保持记忆。保存确认只恢复原操作的服务端参数，不重新调用模型。

规则分类是有限的中文词汇覆盖，不等于任意自然语言理解。未覆盖表达先澄清；尚未完成用户语料评测，不宣称零误判。路由仅决定工具范围，绝不作为权限证明。原有独立 DB-GPT 接口权限边界未被此改动替换。

## 指标工具

### search_metrics

- 只查询当前空间及当前本体下的 PUBLISHED 指标；页面本体优先，其次同用户同空间的操作上下文，必要时由用户提供真实本体ID。
- 权限：ont:metric:list 和 ont:metric:query；同时校验登录用户与Agent上下文相同、空间及本体可见性。
- 最大首屏50条，不无限遍历。code在首屏本地精确过滤；searchComplete=false时不能据此断言编码不存在或全局唯一。
- 返回ID字符串、指标编码、发布时名称/业务口径、单位、粒度、聚合、版本、允许维度及类型、时间属性及范围要求。
- 不返回可变草稿口径、物理字段、连接配置或sourceSnapshot。
- 不含当前业务时区和运行时“今天”的可信解析能力；相对时间有歧义时必须明确日期，不让模型猜统计时点。

### execute_metric_analysis

- 必须使用本轮成功search_metrics返回的指标ID及发布版本，不能复制历史会话ID后直接执行。
- 权限：ont:metric:query 和 ont:metric:execute；服务端再次校验全部来源与数据权限。
- 输入是具体DTO，所有业务ID用字符串，过滤value为字符串，IN用valueList字符串数组，兼容现有LangChain4j Schema生成器。
- 不接收SQL、物理表名、自由JOIN或计算表达式。仅调用 IMetricAnalysisService.query。
- 维度最多5个，过滤最多20条，IN最多100个，后端还限制固定+分析条件合计20条。
- 查询limit最多100，给模型的结果最多20行；超过任一限制标记truncated，模型不能据此求全量总和。
- 返回版本、单位、列、结果、时间范围、执行时间及耗时；numeric结果保持十进制字符串，不经过浮点转换。
- 执行时间不代表源数据刷新时间，旧版本也不代表旧数据快照。
- 错误使用固定业务文案，不向模型回传驱动堆栈、SQL或凭据。

### get_metric_definition

- 必须先在本轮检索到对应指标版本，要求 ont:metric:query，并重新检查当前发布状态及本体归属。
- 返回发布业务定义、负责人、单位、粒度、聚合、度量/时间属性、允许维度与固定过滤；只返回属性语义，不返回物理列或来源连接。
- 不查询数据源、不修改指标；业务定义文本是数据，不是给模型的可执行指令。

### compare_periods

- 要求 ont:metric:query + ont:metric:execute，只能使用本轮检索到的指标版本。
- 明确提供currentStartTime/currentEndTime、baselineStartTime/baselineEndTime，半开区间，不自动猜测同比/环比日期；不允许重叠。
- 同一指标版本、同一套非时间维度过滤、无分组，分别执行两次受控总量查询；两个计划先通过结构校验，执行时每期仍独立授权。
- 仅数值指标；用BigDecimal计算difference及growthRatePercent=(current-baseline)/baseline*100，百分数保留6位HALF_UP。
- null不当作0；基期0或负数时不返回增长率，分别标NO_DATA/ZERO_BASELINE/NEGATIVE_BASELINE。
- 明确标记equalDuration；不同长度区间不自动按天归一化，不自称标准同比环比。两个查询不保证同一数据快照，不给因果结论。
- 不支持分组同比、排名、趋势分桶、自动基期选择或落库。不增加SQL/权限迁移或部署开关。

### analyze_distribution / analyze_trend

- 仅读取本轮服务器记录的execute_metric_analysis结果，输入只有指标ID和版本，不接受模型提供数据行。
- 查询需完整、未截断、单维数值结果且最多20组；拒绝在字典序截断结果上伪造全局TopN。
- 分布提供精确数值竞赛排名、最小最大值和NULL计数。当前没有可加性契约，因此不计算占比、集中度和分组总和，尤其不能把分组去重之和当总去重。
- 趋势要求已发布date维度，日期排序，仅相邻自然日计算变化，缺失日期不补0或插值；不自动把timestamp分桶、不做统计异常检测、预测或因果分析。
- 继续检查当前用户/空间/发布状态，但不执行新SQL。

### build_chart_spec

- 只从本轮真实查询结果生成声明式table/bar/line配置，不接受JS/HTML/任意数据行，不新增渲染器。
- AUTO在连续完整日期且无NULL时选择line，最多7个完整普通类别选择bar，单值/多维/截断等回退table；不生成颜色和可执行库配置。
- 标记rendererStatus=SPEC_ONLY，并要求后续渲染保持单数轴、表格替代、精确数字和缺失日期处理。不能宣称前端已绘图。

### save_analysis

- 权限ont:analysis:add + ont:metric:query；必须传入本轮实际执行成功的相同query和名称，不存数据结果。
- 首次返回NEED_CONFIRMATION；后台持久化完整确认参数hash，用户确认后必须原样重放名称/说明/版本/维度/过滤/日期/limit。
- 本工具使用专用参数确认而不是通用confirmed写入放行。校验同用户同空间的待确认操作，行锁下确认一次性消费和创建分析处于同事务，防重复确认生成多份记录。
- 确认后不重新查询Doris；仍检查当前指标状态、空间和个人保存权限，保存明确版本，不发布指标。运行时在创建模型客户端之前恢复服务端最后待确认参数并直接调用保存工具，不要求模型重放；失败或参数不匹配不得报告已入库。此流程尚需联调验证。

## 入库位置与持久化边界

| 内容 | 平台元数据库表 | 写入位置 |
| --- | --- | --- |
| 当前指标定义 | ont_metric | MetricServiceImpl.create/update |
| 不可变发布口径与映射 | ont_metric_version | MetricServiceImpl.publish |
| 本人的分析条件 | ont_metric_analysis | SaveMetricAnalysisTool → IMetricAnalysisService.create → analysisMapper.insert |
| Agent流程、状态与工具结果 | ai_agent_operation | LangChain4jAgentService.persistOperation |
| 工具审计参数和结果 | ai_agent_tool_call | AgentToolAuditService.record |
| 模型对话与工具消息记忆 | ai_agent_chat_memory | PersistentAgentChatMemoryStore.updateMessages（加密messages_json） |

前三张表是本次提供迁移的业务元数据，保存在平台应用数据源，不写入Doris。Doris是SELECT-only分析来源；本实现不创建ADS表、不回写业务数据。

**“分析条件不保存结果”仅指ont_metric_analysis.query_json。不能理解为整个平台不存查询结果。** Agent操作result_json会序列化工具返回数据，审计result_json会保存经过现有敏感键过滤及长度限制的结果，会话记忆也包含工具消息。因此查询结果、排名、比较或展示配置仍可能进入平台元数据库。敏感键过滤不是任意业务数据脱敏，记忆加密也不覆盖流程/审计表。生产需对这些表和统一AI会话实施授权、保留期限及清理策略；本次未添加结果归档表、全量加密或自动清理，也未擅自删除历史数据。

确认保存时使用已有操作表的行锁与参数hash。确认消费标记和分析创建同事务；历史pending若再次合并，也会因同hash已消费而拒绝。新增MetricSaveConfirmationTest覆盖最后待确认工具判定、参数恢复和旧确认防重放；未运行。


## 当前支持与限制

本批支持完整的小规模单维分布和日期序列描述分析、两期总量比较、个人条件保存以及声明式展示配置。不提供自动时间分桶、跨表、日历同比基期推导、预测、显著性或因果归因。compare_periods不等于完整同比引擎，仍需确认日期和业务可比性。

没有生成图像或实现图表渲染器，没有切换模型提供方。工具结果和Agent会话仍使用现有运行时和审计机制；生产使用须确认所配置模型服务获准处理这些数据。

## 上线前置

1. 人工审阅并依次执行 sql/update_20261027_ontology_metric.sql、sql/update_20261027_metric_analysis.sql；本次未执行。
2. 配置指标目录、版本查询和执行权限，同时保留现有 ai:ask-data:query 入口权限。
3. 按 docs/metric-analysis-backend.md 完成专用只读账号、ADS安全视图、strict资产治理、绑定与用户名白名单。
4. 查询执行默认关闭；不开启配置时检索和定义可用，但执行应失败关闭。
5. 在本体上下文选一个主题验证，再扩大开放范围。

## 验收清单（未执行）

- 编译Java8，检查八个工具Schema能由当前LangChain4j生成并被Catalog注册。
- 管理请求不能调用指标/DB-GPT；指标和探索请求不会进入旧管理快捷分支。
- 短续接保留原工具范围；实质新问题重新路由；混合问题返回澄清。
- 指标检索返回发布口径而非草稿，时间要求和维度类型齐全；不暴露物理来源。
- 多候选需澄清，检索截断不宣称全局唯一；本轮未检索不得执行。
- 无权限、错误登录上下文、跨空间/本体、停用指标、非法来源或敏感字段均拒绝。
- 超20行输出标记截断，大整数和小数原样保留；不声称自动分桶或自由JOIN。
- 聚合基准SQL对账、驱动只读与超时、Doris资源限制仍需集成环境验证。

新增 IntentRouterTest 覆盖基础路由规则，MetricToolSchemaTest 检查具体DTO与LangChain4j工具Schema，MetricPeriodComparisonTest覆盖高精度差值/增长率、无数据、基期零/负数、非重叠时间范围及不同区间长度。另新增MetricResultAnalysisTest覆盖精确排名、NULL、缺失日期、截断拒绝和声明式配置回退；保存参数确认与并发消费尚需集成验证。未运行构建、测试、lint、迁移或模型请求。
