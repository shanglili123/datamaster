# AI 问数 Skill 化实现计划

## 1. 背景与目标

当前 DataMaster 已具备元数据采集、资产管理、质量核检和 AI 对话能力。问数模块的目标不是让大模型直接根据自然语言猜测 SQL，而是先把平台已有的数据治理能力沉淀为可检索、可编辑、可复用的 Skill，再让 AI 问数过程基于这些 Skill 理解表、字段、指标口径和质量风险。

本方案分三层建设：

1. 基础能力 Skill：将元数据采集能力和质量核检能力整理为平台级 Skill。
2. 表级 Skill 生成：问数模块基于元数据 Skill 和质量 Skill，生成具体数据表的问数 Skill。
3. AI 问数引用：用户发起自然语言问数时，系统检索相关 Skill，生成 SQL、解释结果，并提示数据质量风险。

最终效果：

- AI 能知道某张表是什么、字段代表什么、常用时间字段和指标字段是什么。
- AI 能知道这张表最近是否有质量核检异常，回答时能提示可信度和风险。
- 表级 Skill 可人工修改，沉淀业务口径，后续问数持续复用。
- Skill 本身成为 DataMaster 的数据知识资产，而不是一次性的模型上下文。

## 2. 范围说明

### 2.1 本期范围

- 梳理元数据采集模块的功能描述、关键接口、入参出参和调用流程。
- 梳理质量核检模块的功能描述、关键接口、入参出参和质量报告解释规则。
- 新增 Skill 管理能力，用于保存、查询、编辑、发布和归档 Skill。
- 新增表级 Skill 生成能力，根据表资产、字段、血缘、质量报告生成 Markdown Skill。
- 改造 AI 问数流程，使其在生成 SQL 前检索并引用相关 Skill。
- 在前端提供 Skill 管理、表级 Skill 预览编辑、问数引用来源展示。

### 2.2 暂不纳入本期

- 自动执行高风险写 SQL。
- 跨系统实时联邦查询优化。
- 完整指标平台能力。
- 全自动业务口径确认。
- 复杂权限外推。问数必须继承现有用户、菜单、数据源和资产权限。

## 3. 现有模块关联

建议优先复用以下模块：

- `datamaster-collector`：元数据采集任务、采集记录、数据源表结构发现。
- `datamaster-assets`：资产、表、字段、血缘、预览、资产质量信息。
- `datamaster-quality`：质量任务、质量规则、质量执行日志、问题数据和质量报告。
- `datamaster-assets` 的 `skill`、`dbgpt` 子域：AI Skill、问数会话、DBGPT 数据源和知识库同步能力。
- `datamaster-ui/src/views/ai/chat`：现有 AI 对话界面。
- `datamaster-ui/src/api/ai`：现有 AI 前端接口封装。
- `datamaster-ui/src/api/ast/quality`：质量任务和质量日志接口封装。
- `datamaster-ui/src/api/ast/asset`：资产和字段接口封装。
- `datamaster-ui/src/views/meta`、`datamaster-ui/src/views/col/asset`：元数据和采集资产页面。

历史空模块 `datamaster-ai` 和 `datamaster-mdule-ai` 已移除，AI 问数相关后端能力统一收敛到 `datamaster-assets`。

## 4. 总体架构

```text
用户自然语言问题
        |
        v
AI 问数入口
        |
        v
意图识别与实体抽取
        |
        v
Skill 检索
  |             |
  v             v
平台级 Skill    表级 Skill
元数据采集      表结构、字段、指标、质量结论
质量核检
        |
        v
候选表和字段确认
        |
        v
生成校验 SQL 或直接问数
        |
        v
DBGPT 基于数据库与知识库完成查询分析
        |
        v
结果解释、引用来源、质量风险提示
```

## 5. Skill 类型设计

### 5.1 平台级元数据 Skill

建议 Skill 名称：`datamaster-metadata`

用途：

- 告诉 AI 如何在 DataMaster 中查找数据源、库、表、字段、注释、类型、分区、主键、索引、血缘和资产归属。
- 告诉 AI 调用哪些内部接口获取元数据。
- 告诉 AI 如何根据用户问题定位候选表和候选字段。

内容结构：

```markdown
---
name: datamaster-metadata
description: DataMaster 元数据采集和资产检索能力说明。用于 AI 问数、表结构查找、字段含义理解、数据源定位、血缘和资产上下文检索。
---

# DataMaster 元数据能力

## 能力边界

## 核心接口

## 表检索流程

## 字段理解流程

## 血缘和影响分析流程

## 返回字段解释

## 注意事项
```

### 5.2 平台级质量 Skill

建议 Skill 名称：`datamaster-quality`

用途：

- 告诉 AI 如何查找质量任务、质量规则、执行日志、异常数据和质量报告。
- 告诉 AI 如何解释质量维度、规则类型、执行状态、通过率、异常数量。
- 告诉 AI 在问数回答中如何表达质量风险。

内容结构：

```markdown
---
name: datamaster-quality
description: DataMaster 数据质量核检能力说明。用于 AI 问数前检查表和字段质量状态、解释质量报告、识别异常数据风险、生成质量可信度提示。
---

# DataMaster 质量核检能力

## 能力边界

## 核心接口

## 质量报告检索流程

## 质量风险解释规则

## 问数回答中的质量提示

## 注意事项
```

### 5.3 表级问数 Skill

建议 Skill 命名规则：

```text
table-{datasource-code}-{schema-name}-{table-name}
```

示例：

```text
table-ods-mysql-public-order-info
```

用途：

- 描述某张具体表的业务含义。
- 描述字段、关联表、常用过滤条件、时间字段、指标口径。
- 汇总最近质量核检结论。
- 提供常见问数示例和 SQL 生成约束。
- 允许人工编辑补充业务口径。

内容结构：

```markdown
---
name: table-ods-mysql-public-order-info
description: 订单明细表问数 Skill。用于订单金额、订单数量、客户订单、时间趋势、质量风险提示等 AI 问数场景。
---

# 订单明细表

## 表身份

## 业务说明

## 字段说明

## 常用指标口径

## 常用时间字段

## 关联表

## 质量核检结论

## SQL 生成规则

## 示例问题

## 人工维护备注
```

## 6. Skill 存储设计

建议将 Skill 作为数据库记录管理，同时保留 Markdown 正文，方便人工编辑和模型引用。

### 6.1 Skill 主表

建议表名：`AI_SKILL`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| ID | bigint | 主键 |
| SKILL_CODE | varchar | Skill 唯一编码 |
| SKILL_NAME | varchar | Skill 名称 |
| SKILL_TYPE | varchar | `PLATFORM_METADATA`、`PLATFORM_QUALITY`、`TABLE`、`METRIC` |
| STATUS | varchar | `DRAFT`、`PUBLISHED`、`ARCHIVED` |
| SOURCE_TYPE | varchar | `MANUAL`、`GENERATED`、`GENERATED_EDITED` |
| BIZ_OBJECT_TYPE | varchar | 关联对象类型，如 `TABLE`、`DATA_SOURCE`、`QUALITY_TASK` |
| BIZ_OBJECT_ID | bigint | 关联对象 ID |
| CONTENT | clob/text | Skill Markdown 正文 |
| CONTENT_HASH | varchar | 内容哈希，用于判断是否变化 |
| VERSION | int | 版本号 |
| CREATE_BY | varchar | 创建人 |
| CREATE_TIME | datetime | 创建时间 |
| UPDATE_BY | varchar | 更新人 |
| UPDATE_TIME | datetime | 更新时间 |
| DEL_FLAG | char | 删除标记 |

### 6.2 Skill 版本表

建议表名：`AI_SKILL_VERSION`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| ID | bigint | 主键 |
| SKILL_ID | bigint | Skill ID |
| VERSION | int | 版本号 |
| CONTENT | clob/text | 版本内容 |
| CHANGE_TYPE | varchar | `GENERATE`、`EDIT`、`PUBLISH`、`ROLLBACK` |
| CHANGE_REMARK | varchar | 变更说明 |
| CREATE_BY | varchar | 创建人 |
| CREATE_TIME | datetime | 创建时间 |

### 6.3 Skill 引用表

建议表名：`AI_SKILL_REF`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| ID | bigint | 主键 |
| SKILL_ID | bigint | Skill ID |
| REF_TYPE | varchar | `ASSET`、`COLUMN`、`QUALITY_TASK`、`QUALITY_LOG`、`DATA_SOURCE` |
| REF_ID | bigint | 引用对象 ID |
| REF_CODE | varchar | 引用对象编码 |
| REF_NAME | varchar | 引用对象名称 |
| CREATE_TIME | datetime | 创建时间 |

## 7. 后端接口设计

建议在 AI 模块中新增 Skill 管理接口，路径统一放在：

```text
/ai/skill
```

### 7.1 Skill 管理接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/ai/skill/page` | 分页查询 Skill |
| GET | `/ai/skill/{id}` | 获取 Skill 详情 |
| POST | `/ai/skill` | 新增 Skill |
| PUT | `/ai/skill` | 修改 Skill |
| DELETE | `/ai/skill/{id}` | 删除或归档 Skill |
| POST | `/ai/skill/{id}/publish` | 发布 Skill |
| POST | `/ai/skill/{id}/rollback/{version}` | 回滚到指定版本 |
| GET | `/ai/skill/{id}/versions` | 查看 Skill 版本 |

### 7.2 平台级 Skill 生成接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/ai/skill/generate/metadata` | 生成或刷新元数据平台 Skill |
| POST | `/ai/skill/generate/quality` | 生成或刷新质量平台 Skill |
| GET | `/ai/skill/platform` | 查询平台级 Skill |

平台级 Skill 可以先由代码模板生成，接口清单由人工维护；后续可扩展为从 Controller 注解或 OpenAPI 文档中自动抽取。

### 7.3 表级 Skill 生成接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/ai/skill/generate/table/{assetId}` | 根据资产表生成表级 Skill |
| POST | `/ai/skill/generate/table/batch` | 批量生成表级 Skill |
| POST | `/ai/skill/generate/table/{assetId}/refresh` | 刷新表级 Skill |
| GET | `/ai/skill/table/{assetId}` | 查询表关联 Skill |

刷新规则：

- 如果 Skill 未人工编辑，允许直接覆盖生成内容。
- 如果 Skill 已人工编辑，只更新自动生成区块，并保留人工维护区块。
- 每次刷新都写入版本表。

### 7.4 问数接口

建议在现有 AI 对话接口基础上增加问数专用上下文构建：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/ai/ask-data/prepare` | 根据问题检索候选 Skill、表和字段 |
| POST | `/ai/ask-data/sql` | 生成 SQL 和解释 |
| POST | `/ai/ask-data/chat` | 一站式问数对话，不在 DataMaster 本地执行 SQL |

## 8. Skill 生成流程

### 8.1 生成平台级元数据 Skill

1. 扫描或人工配置元数据相关接口。
2. 汇总能力说明：数据源、库、表、字段、血缘、资产归属。
3. 生成 `datamaster-metadata` Markdown。
4. 写入 `AI_SKILL`，类型为 `PLATFORM_METADATA`。
5. 发布后供问数模块引用。

### 8.2 生成平台级质量 Skill

1. 扫描或人工配置质量任务、规则、日志、问题数据接口。
2. 汇总质量维度、规则类型、执行状态、风险解释规则。
3. 生成 `datamaster-quality` Markdown。
4. 写入 `AI_SKILL`，类型为 `PLATFORM_QUALITY`。
5. 发布后供问数模块引用。

### 8.3 生成表级 Skill

输入：

- 表资产 ID。
- 表元数据。
- 字段元数据。
- 表关联血缘。
- 最近质量任务和质量执行日志。
- 人工维护内容，若存在。

处理：

1. 查询表基础信息。
2. 查询字段列表，按业务字段、时间字段、金额/数量字段、枚举字段进行初步归类。
3. 查询质量任务和最近执行结果。
4. 查询血缘和关联资产。
5. 生成 Markdown。
6. 合并人工维护区块。
7. 保存 Skill 和引用关系。

输出：

- 表级 Skill 正文。
- Skill 引用来源。
- 生成摘要和待人工确认项。

## 9. 表级 Skill Markdown 模板

```markdown
---
name: {{skillCode}}
description: {{tableComment}} 问数 Skill。用于 {{businessKeywords}} 等 AI 问数场景。
---

# {{tableComment}}

## 表身份

- 数据源：{{datasourceName}}
- 库/Schema：{{schemaName}}
- 表名：{{tableName}}
- 资产 ID：{{assetId}}
- 负责人：{{ownerName}}
- 最近采集时间：{{lastCollectTime}}

## 业务说明

{{businessDescription}}

## 字段说明

| 字段 | 类型 | 说明 | 推荐用途 | 注意事项 |
| --- | --- | --- | --- | --- |
{{columnRows}}

## 常用指标口径

{{metricDefinitions}}

## 常用时间字段

{{timeFieldRules}}

## 关联表

{{relationTables}}

## 质量核检结论

- 最近核检时间：{{lastQualityCheckTime}}
- 最近核检状态：{{lastQualityStatus}}
- 通过率：{{passRate}}
- 主要异常：{{qualityProblems}}

## SQL 生成规则

- 优先使用已发布字段和有中文注释的字段。
- 涉及时间范围时优先使用本 Skill 推荐的时间字段。
- 涉及金额、数量、状态枚举时必须参考字段说明和指标口径。
- 如果最近质量核检失败，回答中必须提示数据风险。
- 未确认口径时生成待确认 SQL，不直接给出确定性业务结论。

## 示例问题

{{exampleQuestions}}

## 人工维护备注

{{manualNotes}}
```

## 10. AI 问数执行流程

### 10.1 上下文构建

1. 从用户问题中抽取业务主题、时间范围、指标词、维度词和过滤条件。
2. 检索平台级 Skill，确定可用的元数据和质量能力。
3. 检索表级 Skill，获取候选表。
4. 根据表名、字段注释、业务说明、示例问题和历史问答进行排序。
5. 如果候选表不唯一，向用户提出澄清问题。

### 10.2 SQL 生成

1. 选择表和字段。
2. 根据 Skill 中的时间字段和指标口径生成 SQL。
3. 做 SQL 安全检查，只允许只读查询。
4. 检查当前用户是否有表和数据源权限。
5. 返回 SQL、解释、引用 Skill 和风险提示。

### 10.3 结果解释

回答必须包含：

- 查询结论。
- 统计口径。
- 使用的数据表和字段。
- 时间范围。
- 数据质量状态。
- 引用的 Skill。

如果质量核检失败或最近无质量报告，回答必须提示：

- 数据可能不完整。
- 哪些规则异常。
- 最近一次质量核检时间。
- 是否建议重新核检或联系负责人确认。

## 11. 前端页面设计

### 11.1 Skill 管理页面

建议菜单：

```text
智能问数 / Skill 管理
```

功能：

- Skill 列表。
- 按类型、状态、关联对象搜索。
- 新增、编辑、发布、归档。
- 查看版本。
- 回滚版本。
- 查看引用对象。

### 11.2 表级 Skill 生成页面

入口可以放在：

- 资产详情页。
- AI 问数配置页。
- Skill 管理页。

功能：

- 选择资产表。
- 一键生成 Skill。
- 预览 Markdown。
- 显示自动生成区块和人工维护区块。
- 保存草稿。
- 发布。
- 刷新元数据和质量报告。

### 11.3 问数对话页面增强

在现有 AI 对话界面增加：

- 数据范围选择。
- 引用 Skill 展示。
- 候选表确认。
- SQL 预览。
- 质量风险提示卡片。
- 结果解释卡片。

## 12. 权限与安全

- Skill 管理权限应单独配置，例如 `ai:skill:list`、`ai:skill:edit`、`ai:skill:publish`。
- 表级 Skill 的可见性必须受资产权限约束。
- 问数 SQL 只能生成和执行只读语句。
- SQL 执行前必须进行语法和关键字检查，禁止 `insert`、`update`、`delete`、`drop`、`alter`、`truncate`、`call` 等操作。
- SQL 执行必须限制超时时间和最大返回行数。
- AI 回答不得泄露用户无权访问的表、字段或质量报告。
- Skill 中如果包含敏感字段，应在生成时标记，并在问数时脱敏或禁止使用。

## 13. 分阶段实施计划

### 阶段一：基础调研与接口盘点

目标：

- 盘点元数据采集、资产、质量核检现有 Controller、Service、VO、DTO。
- 明确问数模块应复用的接口和需要补齐的接口。
- 确认 `datamaster-assets` 中 AI Skill、DBGPT 同步和问数会话的职责边界。

产出：

- 元数据接口清单。
- 质量接口清单。
- 表级 Skill 需要的数据字段清单。

### 阶段二：Skill 存储和管理能力

目标：

- 新增 Skill 表和版本表。
- 新增 Skill 管理 Controller、Service、Mapper、DO、VO。
- 前端新增 Skill 管理基础页面。

产出：

- Skill 可新增、编辑、查询、发布、回滚。
- 人工可维护 Markdown 正文。

### 阶段三：平台级 Skill 生成

目标：

- 生成 `datamaster-metadata` Skill。
- 生成 `datamaster-quality` Skill。
- 建立平台级 Skill 的刷新和发布流程。

产出：

- 元数据采集能力 Skill。
- 质量核检能力 Skill。
- 问数模块可读取平台级 Skill。

### 阶段四：表级 Skill 生成

目标：

- 根据资产表生成表级 Skill。
- 汇总字段、血缘、质量报告和人工备注。
- 支持生成后人工修改。

产出：

- 单表 Skill 生成接口。
- 批量生成接口。
- 表级 Skill 编辑和发布页面。

### 阶段五：AI 问数集成

目标：

- AI 问数前检索平台级 Skill 和表级 Skill。
- 根据 Skill 生成 SQL。
- 返回引用来源和质量风险。

产出：

- 问数上下文构建服务。
- SQL 生成和安全校验服务。
- 问数结果解释和质量风险提示。

### 阶段六：优化与运营

目标：

- 增加 Skill 命中率统计。
- 增加问数反馈。
- 增加低质量 Skill 识别。
- 支持根据问数历史反向优化表级 Skill。

产出：

- Skill 使用统计。
- 问数反馈闭环。
- Skill 迭代机制。

## 14. 推荐后端类命名

以 AI 模块承载为例：

```text
AiSkillController
AiSkillService
AiSkillServiceImpl
AiSkillMapper
AiSkillDO
AiSkillVersionDO
AiSkillRefDO
AiSkillGenerateService
AiTableSkillGenerateService
AiAskDataService
AiAskDataContextService
AiSqlSafetyService
```

请求和响应对象：

```text
AiSkillPageReqVO
AiSkillSaveReqVO
AiSkillRespVO
AiSkillGenerateReqVO
AiTableSkillGenerateReqVO
AiAskDataPrepareReqVO
AiAskDataPrepareRespVO
AiAskDataSqlReqVO
AiAskDataSqlRespVO
```

## 15. 推荐前端目录

```text
datamaster-ui/src/api/ai/skill.js
datamaster-ui/src/api/ai/askData.js

datamaster-ui/src/views/ai/skill/index.vue
datamaster-ui/src/views/ai/skill/detail.vue
datamaster-ui/src/views/ai/skill/components/SkillEditor.vue
datamaster-ui/src/views/ai/skill/components/SkillVersionDrawer.vue
datamaster-ui/src/views/ai/skill/components/TableSkillGenerateDialog.vue

datamaster-ui/src/views/ai/chat/index/components/SkillReferencePanel.vue
datamaster-ui/src/views/ai/chat/index/components/QualityRiskCard.vue
```

## 16. 风险与处理

| 风险 | 说明 | 处理方式 |
| --- | --- | --- |
| 元数据不完整 | 表或字段缺少注释 | 生成 Skill 时标记待补充项 |
| 质量报告缺失 | 没有最近核检记录 | 回答中提示未核检，不给确定可信结论 |
| 表选择错误 | 用户问题对应多张相似表 | 先返回候选表让用户确认 |
| 业务口径不明确 | 金额、数量、时间口径不一致 | Skill 中维护人工口径，未维护时要求确认 |
| 权限泄露 | Skill 包含无权字段 | Skill 检索和回答都按用户权限过滤 |
| 人工修改被覆盖 | 刷新 Skill 覆盖业务备注 | 区分自动生成区块和人工维护区块 |

## 17. 下一步建议

优先执行阶段一，先产出两份接口清单：

1. 元数据采集和资产接口清单。
2. 质量核检和质量报告接口清单。

接口清单确认后，再开始建表和后端接口实现。这样可以避免 Skill 模型先定死，后面发现现有数据无法支撑而返工。
