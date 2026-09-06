# Palantir 差距概念对齐：FQL · 对象浏览器 · 决策能力（非本体原语）

> 本文聚焦 FQL、对象浏览器（Object Explorer）以及由 Action / Function / 应用工作流组合出的决策能力。必须先澄清：**Decision 不是 Palantir Ontology 公开模型中与 Object Type、Link Type、Action Type 并列的通用原语**。
>
> 现状结论以代码核对为准（引用到文件）。配套背景见 `docs/PalantirArchitecture与流程详解.md`（讲产品/分层/本体三要素）。

---

## 1. FQL（Foundry Query Language）

### 1.1 Palantir 是什么

FQL 是 Palantir Foundry 为**本体（Ontology）语义层**设计的对象查询能力——它不是对着数据库表写裸 SQL，而是对着**对象集（Object Set）**（已被抽象成本体的业务对象，如 `Customer`/`Order`）做筛选、关联、聚合。

```typescript
// FQL / TypeScript SDK 风格示例
const sets = Client.objects.Customer
  .where(c => c.status.eq('ACTIVE').and(c.creditLimit.gt(10000)))
  .groupBy(c => c.city)
  .aggregate(c => c.count())
```

核心特征：
- **对象集 / 属性 / 关系是一等公民**（语义抽象，屏蔽背后物理表和方言）；
- **声明式组合**过滤 / 排序 / 分组 / 聚合（不像拼 SQL 字符串）；
- 底层翻译成对物理存储的查询，但对调用方**屏蔽物理表/方言差异**；
- **FQL 最终当然也会转换成 SQL/存储查询**——纠结"它是不是真 SQL"没有意义；
- **关键**：Palantir 里真正"写 FQL"的是**工程师/数据科学家**（用 SDK）；**业务/普通用户根本不学 FQL**，他们在 **Workshop / Object Explorer** 这类**可视化界面**点点选选做筛选，FQL 只是底层引擎。

### 1.2 我们仓库现在在哪

我们**没有**独立的 FQL 语言，但**对象集（对象实例）抽象已经存在**：

- 后端 `ObjectInstanceController`（`datamaster-ontology-core/.../objectinstance/ObjectInstanceController.java`）提供三个接口：
  - `GET /ont/object-instance/object-sets` —— 对象集（概念 + 物理表绑定）
  - `GET /ont/object-instance/objects` —— 分页查询对象实例
  - `GET /ont/object-instance/lineage/{conceptId}` —— 四维血缘
- `ObjectInstanceQueryServiceImpl.queryObjects` 负责 概念 → 表绑定 → 属性语义 → 表权限校验（`checkTableAccess`，entrance=`ONTOLOGY_OBJECT_QUERY`）→ `DbQuery` 执行。
- **当前能力短板**（都以代码核对为准）：
  - `ObjectInstanceQueryServiceImpl.doQuery`：**只支持等值 AND 过滤**（`column = :param`），`SELECT *`，无排序 / 投影 / 聚合 / 跨表关联 / 全文搜索；
  - `ConceptTableServiceImpl.previewData`（可视化预览用）：同样是等值 AND + `SELECT *`；
  - `ObjectPanel.vue` / `object/index.vue` 的 `parseFilter`：用 `lastIndexOf('=')` 解析**单个等值过滤**。
- **方向判断**：普通用户面对**表单化对象浏览器 UI**（不学语言）；API / AI 面对**结构化查询**。因此——**不造独立 FQL 语言**，而是把"对象集 + 类型化结构化查询（JSON 过滤/排序/投影/模糊）"作为 FQL 概念的落地骨架，`DbQuery` 作为执行后端，新增权限仍走统一 `checkTableAccess`。

### 1.3 落地方向

> 差距项：统一对象查询层 + 对象浏览器增强（合并处理）
> - 后端：查询从"等值 AND"升级为**类型化结构化查询**——`filters`（`[{field, op(eq/ne/gt/ge/lt/le/like/in/between), value}]` + 逻辑组 `AND/OR/NOT`）+ `orderBy` + 列投影 + 关键字模糊搜索；列白名单校验 + NamedParameter 防注入；`ObjectInstanceQueryServiceImpl` 与 `ConceptTableServiceImpl.previewData` 共用同一套构建器。
> - 前端：在**现有入口**（可视化图谱预览 `GraphPanel.vue` + 对象实例 `ObjectPanel.vue` / `object/index.vue`）补**多条件筛选器（字段/运算符/值 + 与或非逻辑组 + 排序 + 列投影 + 模糊搜索）**——这其实是恢复并补全用户此前做过、后被移除的多条件筛选。
> - **不建独立 FQL/DSL 语言**；如需对外工程师级 API，留到 SDK 阶段再在其上叠声明式 DSL。

---

## 2. 对象浏览器（Object Explorer）

### 2.1 Palantir 是什么

对象浏览器是面向**业务/治理用户**的、以**业务对象为单位**的可视化数据浏览界面（对标 Foundry 的 **Object Explorer**）。与"看物理表"的区别：

| 传统看物理表 | 对象浏览器 |
|---|---|
| 看 `customer_table` 和原始字段名 | 看业务对象"客户"和语义属性（城市/信用额度/状态） |
| 自己写/看 SQL 过滤 | **表单化筛选**：选对象集 → 点字段 → 选条件（等于/大于/包含）→ 出结果 |
| 无跨表概念 | 对象可通过**关系**跳转关联对象（客户→订单→商品） |
| 难理解血缘 | 从对象看数据/决策/版本/权限四维血缘 |

**普通用户的入口是表单化浏览器，不是查询语言**——这一点也可能是 FQL 问题的关键：业务用户永远不会直接面对 FQL，他们在浏览器里点选，FQL 只在底层。

### 2.2 我们仓库现在在哪

**对象浏览器的基础已经存在（不是从零建）**：

- 后端：`ObjectInstanceController`（对象集 + 分页实例 + 四维血缘）；
- 前端：
  - `views/ont/workspace/` **本体工作台**的「可视化图谱」Tab（`GraphPanel.vue`）——**点击概念节点，可视化下方就地预览该概念的对象数据**（表头=属性名，最多 5 行，支持按属性值过滤）；这也是用户所指"点概念节点下面出现的对象数据"。
  - 本体工作台「对象实例」Tab（`ObjectPanel.vue`）与 `views/ont/object/index.vue`——选对象集 → 分页出实例 → 开对象血缘/行级数据变化图。
  - `ObjectPanel.vue` 带「行级数据变化」（AntV X6 链式图，按执行时间展示某行 beforeData/afterData）和「对象血缘」（数据/决策/版本/权限四维）弹窗。

**差距（增强而非重建）**：从"能看列表"升级到"能像 Object Explorer 那样不写 SQL 做业务查询"：
1. **条件筛选**：现在只支持等值 AND / 单等值框，没有大于/小于/包含/区间、没有 OR / NOT / 多条件组合；
2. **排序**：无 ORDER BY；
3. **列投影**：固定 `SELECT *`，不能只挑列；
4. **跨对象关系跳转**（客户→订单→商品）：当前单张物理表，无通过关系跳转；
5. **全文/模糊搜索**：无。
（1/2/3 的根源在后端只认等值，故与第 1 节查询层是同一件事的底层与前端。）

### 2.3 落地方向

> - 档位 A（多条件筛选 + 运算符 + 排序 + 列投影 + 模糊搜索）在现有 `GraphPanel` 预览与 `ObjectPanel`/`object` 两个入口落地；
> - 档位 B = 档位 A + **跨对象关系跳转**：通过关系绑定（`RelationColumnDO`：`sourceConceptTableId+sourceColumn → targetConceptTableId+targetColumn`）与 `RelationDO`（`sourceConceptId/targetConceptId/relationType`）把客户行跳到订单对象实例（用源行关联值预置目标过滤），目标同样走结构化查询 + 每张被触达物理表单独 `checkTableAccess`；
> - 交互形态（待定）：**选项 1**= 行级"关联对象"操作/属性链接 → 跳到目标对象实例视图并预置过滤（推荐，复用 ObjectPanel）；**选项 2**= 同页内嵌展开关联对象（更沉浸、前端改动更大）。

---

## 3. 决策能力（Decision Capability，不是独立原语）

### 3.1 Palantir 的准确口径

Palantir 公开 Ontology 模型里，直接与业务执行相关的核心类型是 **Action Type**。Action Type 描述用户或应用可以对对象执行什么受治理的变更，并可由规则、函数和应用交互触发。AIP Logic、Automations、Workshop 等能力可以读取对象、运行函数、调用 Action，并加入人工确认，从而形成“发现问题 → 提出建议 → 审批 → 执行”的决策闭环。

因此：

- “决策”是一个业务过程或应用能力，不是必须单独落成 `Decision` 核心类型；
- “提案（Proposal）”可以是应用里的待确认方案或我们自己的运行时模型，不应描述为 Palantir Ontology 的固定原语；
- 审批、条件、并行、动作链属于应用逻辑/自动化/工作流编排问题，不是 Action Type 本身天然包含一张通用流程图；
- 需要记录“谁依据什么作出什么结论”时，可以把决策记录建模为普通对象类型，或由 Action 写回业务对象并保留 Action 审计。

可采用的组合是：

```text
Objects / Links
    → Rules / Functions / AIP Logic
    → Action proposal or user confirmation
    → Action execution
    → Object state + audit feedback
```

### 3.2 我们仓库现在在哪

我们**没有独立的 Decision 原语，这本身并不是相对 Palantir 的缺口**。目前“决策能力”由 Action 执行、提交条件、审批和审计组合：

- **审批门（挂在动作/函数执行上）**：`ActionExecutionServiceImpl.submitExecution / approveExecution / rejectExecution / executeExecution / rollbackExecution`，状态机 `DRAFT / PENDING_APPROVAL / APPROVED / REJECTED / EXECUTED / FAILED / ROLLED_BACK`；审批记录字段 `status / approvalReason / approverId / approveTime`；`FunctionDO.needsApproval` 也可门控函数执行。
- **决策血缘**：`LineageDataService.saveActionExecution(ActionExecutionNode)` 落 Neo4j `ActionExecution -[DECISION_ACTION]-> Object`（README §5.12.2 对象血缘"决策"维度）。
- 前端：`views/ont/object/components/ObjectLineageDialog.vue` 的"决策"Tab、`ObjectRowChangesDialog.vue` 行级数据变化图。

当前真实差距是运行时能力，而非缺少某个同名原语：

1. 数据触发提交后没有可靠的自动执行 Worker；
2. 没有通用 Workflow Run / Step，不能表达动作串行、条件、并行和汇聚；
3. 外部副作用缺少 Outbox、补偿和对账；
4. 如业务确需“只记录判定、不修改原对象”，可新增领域对象 `DecisionRecord`，仍按普通 Object Type 管理。

### 3.3 DataMaster 落地方向

V1 不创建独立 `Decision` 平台原语，采用：

```text
Action
  + Submission Criteria
  + Approval Runtime
  + Reliable Action Worker
  + Audit / Lineage
```

V2 再增加 Workflow Definition / Run / Step，承载串行、条件、并行、等待和补偿。若某个领域确实需要独立判定记录，再把 `DecisionRecord` 作为该领域的业务对象类型，而不是写成对 Palantir 官方原语的复刻。

---

## 4. 今日对齐结论小结

| 主题 | 结论 |
|---|---|
| FQL | 不做独立语言；落地为"对象集 + 类型化结构化查询（JSON 过滤/排序/投影/模糊）"，`DbQuery` 执行，权限走统一入口 |
| 对象浏览器 | 已在（工作台点概念预览 + 对象实例页）；增强 = 恢复并补全多条件筛选（与或非）+ 运算符 + 排序 + 投影 + 模糊 + 关系跳转 |
| 关系跳转（档位B） | 通过 `RelationColumnDO` 关联字段映射 + `RelationDO` 目标概念实现；UX 选项 1/2 待定 |
| 决策 | Decision 不是 Palantir 通用本体原语；V1 用 Action + Criteria + Approval + Worker + Audit 组成闭环 |
| 优先级 | 可靠单动作运行时 → 对象查询增强 → Workflow 编排 → 外部副作用对账/补偿 → AI Proposal |

## 5. 相关

- 现有体系说明：`README.md`、`docs/整体规范化改造目标与方案.md`
- Palantir 整体架构：`docs/PalantirArchitecture与流程详解.md`
- 动作执行引擎设计：`docs/Action动作执行引擎设计.md`
- 计划草稿（决策完备、含实施 todos）：`.omo/drafts/palantir-ontology-gap.md`
