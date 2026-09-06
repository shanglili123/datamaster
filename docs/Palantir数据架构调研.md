# Palantir Foundry 数据架构调研

> 本文汇总对 Palantir Foundry 的调研结论,覆盖:数据绑定方式、修改如何落地、冲突如何处理、决策/审批如何实现、留痕存在哪、以及是否使用图数据库。
> 所有结论均逐页核对 Palantir 官方文档(简体机器翻译版),原始抓取文本存于 `docs/datasource/py/palantir_docs/*.txt`,文中以 `[来源: 文件名]` 标注出处。

---

## 〇、一句话结论

Palantir Foundry 是一个**以 Ontology(本体)为核心、把"数据接入→建模→操作→审批→应用"缝成闭环的企业数据运营平台**。它底层**不使用图数据库**,而是用"元数据服务 + 搜索索引型对象数据库 + 列存数据集(Parquet)+ Spark 查询层"实现所谓的"知识图谱/数字孪生"。图只是**逻辑语义模型**,物理落地是列存 + 索引 + 按主键合并。

---

## 一、数据绑定:两种方式

Ontology 把物理数据"绑定"成业务对象有两条路径:

1. **同步为 Foundry 数据集**:通过 Data Connection 把外部系统(SAP、Salesforce、数据库、S3 等)的数据搬进 Foundry,落成 Foundry 自己的数据集(Parquet 列存)。这是 Foundry 自己的一份拷贝。
2. **虚拟表(Virtual Tables)**:不搬数据,只在 Foundry 里建一个**指向源系统表的指针**,查询时直接下推到源系统。
   - GA 支持的源:Amazon S3、Azure ADLS Gen2、BigQuery、Google Cloud Storage、Snowflake。
   - 格式:Avro / Delta / Iceberg / Parquet(对象存储类),或 Table / View / Materialized View(数仓类)。
   - 限制:必须**直连**,不支持通过代理连接;并非所有 Foundry 应用都支持虚拟表作为输入。

`[来源: virtual-tables.txt]`

> 二者可组合:虚拟表适合超大表(省重复存储、可下推计算)、不想搬数据的场景;同步数据集适合要高交互性能、要版本控制/分支的场景。

---

## 二、修改如何落地:写"编辑层",不动原表

**核心结论:Actions(操作)不会改原始数据源,也不会改虚拟表指向的源表。它写的是一个独立的"用户编辑层"。**

- 用户通过 Action 触发编辑 → 操作服务把修改指令发给 Funnel → 指令进入 Funnel 管理的**带偏移量的队列** → 应用到对象数据库的实时索引上。
- 读取时:对象数据库把 **数据源数据 ∪ 用户编辑** 按主键合并出对象的最新状态。
- 原表(数据源)**始终不动**;虚拟表指向的源系统更**不会**被 Foundry 回写(除非你显式配置 Webhook 回写)。

`[来源: edits-how-applied.txt]`

### 物化(Materialization)

如果下游管道或批量下载需要"每个对象实例的最新状态(含用户编辑)",可以创建**物化数据集**:

- 物化 = 把"源 ∪ 编辑"合并后的最新状态,输出成一个普通 Foundry 数据集或受限视图。
- OSv2 里物化是**非必填**的(OSv1 里叫"数据输出数据集",是必需的)。
- 物化数据集里带 `__is_deleted`、`__patch_offset` 等 `__` 前缀元数据列,用于 Foundry **去重**,不代表对象业务状态。
- OSv2 物化数据集**不可自定义保留策略**,历史事务被持续删除,只保证最新快照。

`[来源: edits-materializations.txt, edits-materializations2.txt]`

---

## 三、冲突如何处理:分层优先级 + 主键/字段级对齐

当同一个对象实例(同一主键)同时收到"数据源更新"和"用户编辑"时,靠**冲突解决策略**透明处理。策略在**对象类型的每个数据源级别**配置,仅 OSv2 支持:

### 策略 1:应用用户编辑(默认)

对象最终状态**始终由用户编辑决定**,无论数据源后续怎么更新。
- 只对**被编辑过的字段**生效;未编辑字段仍接受数据源更新(字段级合并)。
- 即使源行消失过再回来,之前的用户编辑仍然保留、仍然覆盖。

### 策略 2:应用最新值

用户编辑**有条件应用** —— 只有当用户编辑的时间戳比数据源该行的时间戳更新时才应用。
- 要求数据源有**时间戳类型**属性(date 类型无效),且必须是 UTC。
- 比较只发生在"输入数据源的时间戳" vs "用户编辑应用时间"之间;即使编辑改了时间戳字段本身,也只看输入源时间戳。
- **仅编辑属性(edit-only)**:无论时间戳如何,用户编辑**总是**应用。

### 关键机制:不是全表对比

- 靠**主键**对齐,判断"是不是同一条数据的冲突"。
- 靠 **transaction 事件 + 偏移量队列**感知"源数据来了新版本",Funnel 自动计算 **change record(数据差异)**,只对变化的行做增量索引 —— **不是每次全表扫描对比**。
- OSv2 禁止用某些类型做主键(Geohash、数组、时间序列、实数),且强制数据源主键唯一。**没主键的表无法直接作为对象类型索引**。

`[来源: edits-how-applied.txt, object-storage-v2.txt, funnel-batch.txt]`

---

## 四、决策/审批如何实现:没有 "Decision" 原语

**重要澄清:Palantir 官方没有叫 "Decision"(决策)的原语。** (核实方式:抓取官方 sitemap 5000+ URL,"decision" 仅出现在 1 篇用例文章,无独立原语页。)

Palantir 靠三套官方机制组合来做"判断 / 审批 / 回执":

### 1. Actions(操作类型)+ Rules(规则)

Action 是修改对象的受控入口,可作用于**单个对象 / 链接实例 / 对象集**(即可单条可批量)。其内部由 Rules 定义:
- **Ontology 规则**:创建/修改/删除 对象或链接。
- **Function 规则**:用 TypeScript 写任意复杂逻辑。
- **副作用(Side effects)**:通知 + Webhook。

`[来源: action-rules.txt, function-actions.txt]`

### 2. Submission Criteria(提交标准)

对 Action 提交做**条件校验**:用 condition/operator 表达式,支持"当前用户"模板、参数模板,不满足就拦截提交。

`[来源: submission-criteria.txt]`

### 3. Approvals(审批)

人工决策的留痕机制,Request/Task 模型:
- 多审核人、状态机(等待审批 / 关闭 / 被拒绝并关闭 / 请求更改 / 需要操作 / 已完成;task 层:审核 / 已批准 / 已拒绝)。
- **Checkpoint**:强制填写理由。
- **请求永久保留** = 审批审计日志。

`[来源: approvals-overview.txt, approvals-review.txt]`

### Webhooks:数据输出 vs 副作用

| 类型 | 应用时机 | 失败是否给用户看 | 数量 |
|---|---|---|---|
| 数据输出(data output) | 对象更改**之前** | 是(用户看到成功/失败前) | 只能 1 个 |
| 副作用(side effect) | 对象更改**之后** | 否(可能在成功消息之后) | 可多个,无序 |

只提供"部分事务性":数据输出 Webhook 失败则 Ontology 不改;但仍可能出现"外部成功、Ontology 改失败"。

`[来源: action-webhooks.txt]`

---

## 五、留痕存在哪 & 是否用图库(核心结论)

### 5.1 Palantir 不用图数据库 —— 逐页证据

官方《Ontology 后端架构概述》明确:Ontology 后端是一组**微服务**,没有任何一个是图引擎。

| 服务 | 职责 |
|---|---|
| **OMS**(Ontology 元数据服务) | 存对象类型/链接类型/操作类型的**定义**(元数据) |
| **对象数据库** | 存**索引后的对象数据**,供快速查询 |
| **OSS**(对象集服务) | 读:搜索、筛选、聚合、加载 |
| **操作(Actions)** | 把用户编辑写进对象数据库 |
| **对象数据漏斗(Funnel)** | 协调:把数据源 + 用户编辑索引进对象数据库 |

`[来源: object-backend-overview.txt]`

**"对象数据库"到底是什么引擎(不是图库):**

Funnel 批处理管道四步 —— **更改记录 → 合并更改 → 索引 → 数据灌注**:
- **合并更改**:"通过对象类型的**主键**合并所有更改" → 按主键 join 的行合并,不是图的点边遍历。
- **索引**:把合并后的行转换成**索引文件**,存进单独索引数据集。
- **数据灌注**:把索引文件"下载到 **OSv2 数据库搜索节点的磁盘上**"。
- **查询层**:"通过基于 **Spark** 的查询执行层"做搜索和聚合。

→ 形态是**搜索/索引型数据库(近似 Elasticsearch 那类)+ 底层 Parquet 列存**,不是图存储。

`[来源: funnel-batch.txt, object-storage-v2.txt]`

**"链接/关系"看着像图,存的不是图:** 对象类型、链接类型只是 OMS 里的**元数据定义**;多对多链接实例通过**连接表/合并表**按主键维护 —— 是关系表 join,不是图库的边。所谓"数字孪生/知识图谱"的图是**逻辑语义模型**。

`[来源: edits-how-applied.txt, object-backend-overview.txt]`

### 5.2 三种留痕的存放位置

| 层面 | 存在哪 | 用什么 |
|---|---|---|
| **本体定义**(对象/链接/操作类型) | OMS(元数据) | 配置/schema,非图库 |
| **对象/链接实例数据** | 对象数据库 + Foundry 数据集(Parquet) + Funnel 编辑层 | 列存 + 搜索索引 |
| **数据血缘(Data Lineage)** | 不是"存"出来的,是**从数据集/管道依赖 + 构建事务推导可视化**出来的 | 交互式工具,非图库 |
| **决策/审批留痕** | Approvals 的 Request/Task**永久保留** + Action execution 历史 | 普通数据集/元数据,非图库 |

`[来源: data-lineage-overview.txt, approvals-overview.txt, ontology-overview.txt]`

---

## 六、与本项目 / datamaster 系统的对照

| 维度 | Palantir Foundry | 本仓(sta-explorer-web) | datamaster-ontology-core |
|---|---|---|---|
| 数据落地 | 列存数据集 + **编辑层**(不动源) | 直接改**原始表**,单一写入源 | — |
| 修改留痕 | 编辑层 + Approvals 永久保留 | 操作日志发**远程日志服务**(无前后值) | Neo4j 行级前后值 |
| 决策 | Approvals + Actions（**无通用 Decision 原语**） | — | V1 采用 Action + Criteria + Approval + Worker；领域需要时再建 DecisionRecord 对象 |
| 本体/血缘存哪 | Parquet + 元数据 + 推导(**无图库**) | — | **Neo4j 图库** |

**关键结论:**
1. Palantir **刻意不用图库**,用元数据服务 + 搜索索引型对象数据库 + 列存 + Spark 查询,以便解耦、横向扩展。
2. datamaster 用 Neo4j 存本体/血缘/决策,是**自己的技术选择,不是 Palantir 的路子**。
3. 本仓是"直接改原始表 + 执行记录审计"的单一写入源模式,**上图库存血缘的必要性很低**;审计诉求(1~2 跳、按主键/时间过滤)用关系表即可满足。真要对齐 Palantir,方向反而是"关系表/列存 + 主键合并 + 索引",而非图库。

---

## 七、图库有意义的判据(给 datamaster 决策参考)

区分两种东西 —— 混在一起就会误判:

| | 是什么 | 形态 | 需要图库吗 |
|---|---|---|---|
| **审计/留痕** | 某条记录被谁何时改成什么(前后值 + SQL) | 扁平流水,按主键/时间过滤 | ❌ 一张表够 |
| **血缘** | 数据从哪来、经哪些加工、影响哪些下游 | 变长多跳有向图 | ✅ 有时需要 |

**图库只在满足以下任一条件时才值:**
1. 血缘是**多级管道**(3 跳以上)且要做**跨级影响分析**。
2. 要做**交互式溯源界面**,展开任意深度上下游。
3. 关系是**网状**的,join 写到爆。

在此之前,`ActionExecution -[DECISION_ACTION]-> Object` 那套图存,对单一写入源、浅血缘的场景是**过度设计**。

---

## 附:原始抓取文档索引

存于 `docs/datasource/py/palantir_docs/`:

- `virtual-tables.txt` — 虚拟表(联邦/指针、GA 源、限制)
- `edits-how-applied.txt` — 用户编辑如何应用、冲突解决两策略
- `edits-materializations.txt` / `edits-materializations2.txt` — 物化数据集
- `edits-overview.txt` — 对象编辑概述
- `object-backend-overview.txt` — **Ontology 后端架构(证明无图库)**
- `object-storage-v2.txt` — OSv1/OSv2 重大变更、主键约束
- `object-indexing-overview.txt` — 索引概述(Funnel)
- `funnel-batch.txt` — **Funnel 批处理四步管道(主键合并/索引/数据灌注/Spark)**
- `ontology-overview.txt` — Ontology 搭建概述
- `action-overview.txt` / `action-rules.txt` / `function-actions.txt` — 操作类型与规则
- `action-webhooks.txt` — Webhook 数据输出 vs 副作用
- `submission-criteria.txt` — 提交标准校验
- `approvals-overview.txt` / `approvals-review.txt` — 审批 Request/Task 模型
- `data-lineage-overview.txt` — 数据沿袭(交互工具,非图库)
