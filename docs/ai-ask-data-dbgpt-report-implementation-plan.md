# AI 问数与 DBGPT 报告模式实现方案

## 背景

当前 DataMaster 的 AI 问数页面以选择数据库为入口，前端选择数据源后，将用户问题和数据源标识传给后端，后端再调用 DBGPT。DataMaster 中已有的 AI Skill 会同步到 DBGPT 知识库，但这些 Skill 当前主要来自元数据，并且以表级 Skill 为主。

后续目标不是让 DataMaster 先要求 DBGPT 生成 SQL，再由 DataMaster 自己执行 SQL 查询。这条路径属于早期实现思路，会造成 DBGPT 问数能力重复建设。正确方向是：普通问数由 DBGPT 基于数据库和知识库直接完成；SQL 只作为可选的解释、审计、校验信息返回。

报告模式需要在普通问数之上增加模板编排能力。DBGPT 负责按模板要求返回结构化数据，前端负责用报告模板和结构化数据渲染 HTML 报告。

## 总体目标

1. 支持普通数据问答：选择数据库和知识库后，直接调用 DBGPT 返回答案、数据、解释。
2. 支持 SQL 可选返回：仅当用户或页面选项要求校验 SQL 时，提示 DBGPT 附带 SQL。
3. 支持 Skill 范围扩展：从表级 Skill 扩展到整库 Skill、单表 Skill、多表 Skill、报告模板 Skill。
4. 支持报告模板管理：模板既包含数据字段要求，也包含展示结构和样式配置。
5. 支持报告模式：前端加载模板，后端调用 DBGPT 获取模板所需结构化数据，前端渲染报告 HTML。

## 核心分工

### 数据库

数据库提供 DBGPT 的数据查询基础，包括 schema、表、字段、数据查询和统计能力。

普通问数和报告模式都必须选择数据库。数据库选择决定 DBGPT 面向哪个数据源回答问题。

### 知识库

知识库承载 DataMaster 生成或维护的业务知识。DataMaster 的 Skill 同步到 DBGPT 后，本质上变成 DBGPT 的知识库文档。

知识库主要作用：

1. 补字段语义：字段中文名、业务含义、单位、枚举值、时间字段。
2. 补指标口径：指标定义、统计范围、过滤条件、聚合方式。
3. 补表关系：主外键关系、业务关联关系、推荐 join 路径。
4. 补质量风险：空值、重复、口径变化、数据延迟、不可用字段。
5. 补报告规则：报告章节、分析维度、指标解释、风险提示规则。

知识库不是页面模板引擎，它负责让 DBGPT 理解业务和数据，不负责稳定渲染页面。

### DBGPT

DBGPT 负责基于数据库和知识库完成问数、分析和结构化数据生成。

普通问数模式下，DBGPT 直接返回答案、数据、解释。只有开启 SQL 返回选项时，才附带 SQL。

报告模式下，DBGPT 不直接返回完整 HTML，而是按照模板定义返回结构化报告数据。

### 报告模板

报告模板负责定义报告页面需要哪些数据槽位、展示区域、图表、表格、样式和布局。

模板应为结构化配置，可以上传、编辑、版本化、绑定业务场景或知识库。模板不是单纯自然语言描述，应包含机器可读的字段定义和样式定义。

### 前端

前端负责模式选择、数据库选择、知识库选择、模板选择、报告预览和 HTML 渲染。

普通问数中，前端展示 DBGPT 返回的答案、表格、图表或 Markdown。

报告模式中，前端加载模板配置，将 DBGPT 返回的结构化数据填充到模板，渲染为 HTML 报告。

## Skill 类型设计

### 整库 Skill

整库 Skill 描述一个数据库的整体业务背景和数据域。

建议内容：

1. 数据库主题和业务域。
2. 核心表清单。
3. 表之间的主题关系。
4. 公共时间字段、区域字段、组织字段。
5. 通用指标口径。
6. 数据更新频率和质量说明。

### 单表 Skill

单表 Skill 描述一张表的字段和业务使用规则。

建议内容：

1. 表用途。
2. 字段含义。
3. 字段单位。
4. 枚举值说明。
5. 常用过滤条件。
6. 常用统计口径。
7. 字段质量风险。

### 多表 Skill

多表 Skill 描述一组表的业务主题、关联关系和跨表指标。

建议内容：

1. 主题域说明。
2. 涉及表清单。
3. 推荐关联关系。
4. 维表和事实表划分。
5. 跨表指标口径。
6. 查询注意事项。

### 报告模板 Skill

报告模板 Skill 描述某类报告的数据要求和业务分析规则。

建议内容：

1. 报告适用场景。
2. 报告章节。
3. 指标口径。
4. 图表说明。
5. 风险判断规则。
6. 结论生成规则。

报告模板应关联到 Skill 下管理。Skill 负责承载业务知识、元数据说明和报告模板资源；报告模板可以随 Skill 被查看、上传、编辑和发布。报告模板中的业务说明可以同步到 DBGPT 知识库，帮助 DBGPT 理解报告业务要求；实际渲染模板仍然以结构化 JSON 配置为准。

## 普通问数模式

### 业务流程

```text
用户选择数据库
用户选择或默认绑定知识库
用户输入问题
前端调用后端问数接口
后端组装 DBGPT 请求
DBGPT 基于数据库和知识库直接问数
后端流式返回 DBGPT 响应
前端展示答案、表格、图表或 Markdown
```

### SQL 返回策略

普通问数默认不要求 DBGPT 返回 SQL。

页面可以增加一个选项：

```text
是否返回 SQL 用于校验
```

当选项关闭：

```text
请直接返回查询结果、分析结论和必要说明，不要只返回待执行 SQL。
```

当选项开启：

```text
请在返回查询结果和分析结论后，附带实际使用或建议校验的 SELECT SQL。
```

SQL 只用于人工校验或调试，不作为主查询链路。

### 需要清理的旧流程

早期实现中存在“DBGPT 生成 SQL，DataMaster 再执行 SQL”的流程。这不应作为普通问数主链路。

处理原则：

1. 前端不应在 DBGPT 回复后自动抽取 SQL 并调用 DataMaster 本地执行接口。
2. 后端不再保留 `/ai/ask-data/execute`；`/sql` 仅用于生成或展示校验 SQL。
3. 普通问数请求中不再默认要求 DBGPT 必须输出 SQL。
4. 如需返回 SQL，应由显式参数控制，例如 `returnSql=true`。

## 报告模式

### 业务流程

```text
用户选择数据库
用户选择知识库
用户选择报告模板
前端加载报告模板配置
前端发起报告生成请求
后端把数据库、知识库、模板数据槽位、返回字段要求传给 DBGPT
DBGPT 返回结构化报告数据
前端用模板和数据渲染 HTML 报告
用户预览、导出或保存报告
```

### 报告模式请求重点

报告模式下请求 DBGPT 时，需要明确告诉 DBGPT：

1. 当前数据库是什么。
2. 当前知识库是什么。
3. 当前报告模板是什么。
4. 需要返回哪些字段。
5. 每个字段的数据类型是什么。
6. 哪些字段是必填。
7. 是否需要返回 SQL 用于校验。
8. 必须返回 JSON，不返回完整 HTML。

### DBGPT 返回原则

DBGPT 应返回结构化 JSON，例如：

```json
{
  "type": "report_data",
  "templateCode": "water_month_report",
  "title": "月度取水许可分析报告",
  "period": "2026-07",
  "summary": "本月取水许可整体平稳，重点区域取水量占比较高。",
  "metrics": {
    "validPermitCount": 128,
    "newPermitCount": 6,
    "totalWaterAmount": 384200.5
  },
  "charts": {
    "permitTrend": [
      { "month": "2026-01", "count": 112 },
      { "month": "2026-02", "count": 118 }
    ],
    "areaWaterRanking": [
      { "area": "一区", "amount": 86000.0 },
      { "area": "二区", "amount": 73200.0 }
    ]
  },
  "tables": {
    "riskPermits": [
      {
        "permitNo": "P-001",
        "enterpriseName": "示例企业",
        "riskReason": "接近许可上限"
      }
    ]
  },
  "insights": [
    "重点区域取水量占比高，需要持续跟踪。",
    "部分许可证即将到期，建议提前核查。"
  ],
  "sql": null
}
```

## 报告模板结构

报告模板应包含数据结构和样式结构。模板关联到 Skill 下，可以在 Skill 模板管理中上传、查看和编辑。

为了避免使用人员随意生成模板，AI 问数页的报告模式顶部应提供“模板格式”按钮。该按钮展示系统支持的标准 JSON 模板格式，并提供复制能力。使用人员复制后，只允许在固定结构下修改模板编码、字段、布局、样式和提示语，再到 Skill 模板管理中上传或编辑。

示例：

```json
{
  "templateCode": "water_month_report",
  "templateName": "月度取水许可分析报告",
  "version": 1,
  "description": "用于生成水资源取水许可月度分析报告",
  "skillBinding": {
    "skillId": null,
    "skillCode": "",
    "knowledgeSpace": "datamaster-skills"
  },
  "dataSchema": {
    "required": [
      "summary",
      "metrics.validPermitCount",
      "metrics.totalWaterAmount",
      "charts.permitTrend",
      "charts.areaWaterRanking",
      "tables.riskPermits",
      "insights"
    ],
    "fields": [
      {
        "key": "summary",
        "label": "报告摘要",
        "type": "string",
        "required": true
      },
      {
        "key": "metrics.validPermitCount",
        "label": "有效许可证数量",
        "type": "number",
        "required": true
      },
      {
        "key": "metrics.totalWaterAmount",
        "label": "总取水量",
        "type": "number",
        "unit": "万立方米",
        "required": true
      },
      {
        "key": "charts.permitTrend",
        "label": "许可数量趋势",
        "type": "array",
        "itemSchema": {
          "month": "string",
          "count": "number"
        },
        "required": true
      },
      {
        "key": "charts.areaWaterRanking",
        "label": "区域取水量排名",
        "type": "array",
        "itemSchema": {
          "area": "string",
          "amount": "number"
        },
        "required": true
      },
      {
        "key": "tables.riskPermits",
        "label": "风险许可证明细",
        "type": "array",
        "itemSchema": {
          "permitNo": "string",
          "enterpriseName": "string",
          "riskReason": "string"
        },
        "required": false
      }
    ]
  },
  "layout": {
    "type": "report",
    "page": {
      "width": "A4",
      "padding": "24px",
      "background": "#f7f8fa"
    },
    "sections": [
      {
        "key": "header",
        "type": "title",
        "bind": "title",
        "style": {
          "fontSize": "24px",
          "fontWeight": "600",
          "color": "#1d2129",
          "align": "center"
        }
      },
      {
        "key": "summary",
        "type": "paragraph",
        "bind": "summary",
        "style": {
          "fontSize": "14px",
          "lineHeight": "1.8",
          "color": "#4e5969"
        }
      },
      {
        "key": "coreMetrics",
        "type": "metricGrid",
        "columns": 3,
        "items": [
          {
            "label": "有效许可证数量",
            "bind": "metrics.validPermitCount",
            "format": "integer"
          },
          {
            "label": "本月新增",
            "bind": "metrics.newPermitCount",
            "format": "integer"
          },
          {
            "label": "总取水量",
            "bind": "metrics.totalWaterAmount",
            "format": "decimal",
            "unit": "万立方米"
          }
        ],
        "style": {
          "cardBackground": "#ffffff",
          "cardBorder": "1px solid #e5e6eb",
          "cardRadius": "6px"
        }
      },
      {
        "key": "permitTrend",
        "type": "lineChart",
        "title": "许可数量趋势",
        "bind": "charts.permitTrend",
        "xField": "month",
        "yField": "count",
        "style": {
          "height": "260px",
          "color": "#165dff"
        }
      },
      {
        "key": "areaWaterRanking",
        "type": "barChart",
        "title": "区域取水量排名",
        "bind": "charts.areaWaterRanking",
        "xField": "area",
        "yField": "amount",
        "style": {
          "height": "260px",
          "color": "#00b42a"
        }
      },
      {
        "key": "riskPermits",
        "type": "table",
        "title": "风险许可证明细",
        "bind": "tables.riskPermits",
        "columns": [
          { "label": "许可证号", "prop": "permitNo" },
          { "label": "企业名称", "prop": "enterpriseName" },
          { "label": "风险原因", "prop": "riskReason" }
        ],
        "style": {
          "size": "small",
          "border": true
        }
      },
      {
        "key": "insights",
        "type": "insightList",
        "title": "分析结论",
        "bind": "insights",
        "style": {
          "markerColor": "#f53f3f"
        }
      }
    ]
  }
}
```

## Skill 下的模板上传与管理

报告模板建议作为 Skill 下的子资源管理，不直接混在 Skill 的 Markdown 内容中。这样既能保持 Skill 知识内容清晰，也能让报告模式快速根据当前 Skill 预加载可用模板。

Skill 列表操作栏建议保留模板管理入口：

1. `上传报告模板`：上传或粘贴结构化模板 JSON。
2. `查看模板`：查看当前 Skill 已绑定的模板列表。
3. `编辑模板`：在查看模板时切换编辑模式。
4. `设为默认模板`：报告模式下优先加载默认模板。
5. `启用/停用模板`：控制模板是否可用于报告生成。

标准模板格式入口不放在 Skill 操作栏，放在 AI 问数页报告模式顶部，便于生成报告前查看和复制。

建议字段：

1. Skill ID。
2. 模板编码。
3. 模板名称。
4. 模板版本。
5. 适用业务域。
6. 默认知识库。
7. 数据 Schema。
8. 布局配置。
9. 样式配置。
10. 是否默认模板。
11. 创建人和发布时间。
12. 状态：草稿、发布、停用。

上传模板时需要校验：

1. JSON 格式合法。
2. `dataSchema.fields` 中字段唯一。
3. `layout.sections` 中绑定字段存在。
4. 图表字段包含 `xField`、`yField`。
5. 表格字段包含 `columns`。
6. 样式字段只允许白名单 CSS 属性，避免任意脚本和危险样式。
7. 模板必须符合报告模式“模板格式”中展示的标准结构。

## 后端接口建议

### Skill 报告模板管理接口

模板作为 Skill 下的子资源管理。基础接口：

```text
GET    /ai/skill/{skillId}/report-templates
GET    /ai/skill/{skillId}/report-templates/{templateId}
POST   /ai/skill/{skillId}/report-templates
PUT    /ai/skill/{skillId}/report-templates/{templateId}
DELETE /ai/skill/{skillId}/report-templates/{templateId}
POST   /ai/skill/{skillId}/report-templates/{templateId}/default
```

模板保存请求：

```json
{
  "templateCode": "water_month_report",
  "templateName": "月度取水许可分析报告",
  "templateContent": "{...标准模板JSON...}",
  "status": "DRAFT",
  "defaultFlag": true,
  "remark": "适用于水资源月报"
}
```

后端保存时至少校验：

1. `templateContent` 是合法 JSON。
2. 存在 `dataSchema`。
3. 存在 `layout`。
4. 存在 `style`。
5. 同一 Skill 下 `templateCode` 不重复。

### 普通问数接口

请求：

```json
{
  "question": "统计本月各区县取水量排名",
  "datasourceId": 1,
  "knowledgeSpace": "datamaster-skills",
  "mode": "qa",
  "returnSql": false
}
```

说明：

1. `datasourceId` 指 DataMaster 数据源。
2. `knowledgeSpace` 指 DBGPT 知识库，可以默认使用 `datamaster-skills`。
3. `returnSql` 控制是否要求 DBGPT 附带 SQL。

### 报告生成接口

请求：

```json
{
  "question": "生成本月取水许可分析报告",
  "datasourceId": 1,
  "knowledgeSpace": "datamaster-skills",
  "templateCode": "water_month_report",
  "returnSql": false,
  "params": {
    "period": "2026-07"
  }
}
```

后端处理：

1. 读取模板。
2. 根据模板生成 DBGPT 结构化输出要求。
3. 带上数据库、知识库、模板字段要求调用 DBGPT。
4. 校验 DBGPT 返回 JSON 是否满足模板 Schema。
5. 返回报告数据给前端。

基础接口：

```text
POST /ai/ask-data/dbgpt/report
```

请求体：

```json
{
  "question": "生成本月取水许可分析报告",
  "datasourceId": 1,
  "skillId": 10,
  "templateId": 20,
  "returnSql": false,
  "params": {
    "period": "2026-07"
  }
}
```

响应体：

```json
{
  "question": "生成本月取水许可分析报告",
  "skillId": 10,
  "templateId": 20,
  "templateCode": "water_month_report",
  "templateName": "月度取水许可分析报告",
  "templateContent": "{...模板JSON...}",
  "reportData": {
    "title": "月度取水许可分析报告",
    "summary": "...",
    "metrics": {},
    "charts": {},
    "tables": {},
    "insights": []
  },
  "rawReply": "...",
  "qualityWarning": null
}
```

## DBGPT 请求组装建议

普通问数提示重点：

```text
请基于已选择的数据库和知识库回答用户问题。
由 DB-GPT 完成数据查询、统计和分析，不要只返回待执行 SQL。
如果 returnSql=false，不要默认输出 SQL。
如果 returnSql=true，请在答案末尾附带实际使用或建议校验的 SELECT SQL。
```

报告模式提示重点：

```text
请基于已选择的数据库和知识库，为指定报告模板生成结构化报告数据。
必须只返回 JSON，不返回 Markdown，不返回 HTML。
必须包含模板要求的 required 字段。
字段名必须与模板 dataSchema 中的 key 保持一致。
图表数据返回数组。
表格数据返回数组。
结论返回字符串数组。
如果 returnSql=true，可在 sql 字段返回用于校验的 SELECT SQL；否则 sql 返回 null。
```

同时需要确认 DBGPT 原始页面选择数据库和知识库时的请求体字段。DataMaster 应按 DBGPT 原始接口支持的字段传递知识库，例如可能是：

```text
space_name
space_id
knowledge_space
ext_info.space_name
resource
```

最终字段名应以 DBGPT 实际请求为准。

## 前端页面建议

### 普通问数页面

控件：

1. 数据库选择。
2. 知识库选择或默认知识库展示。
3. 是否返回 SQL 开关。
4. 问题输入框。
5. 流式回答区域。

展示：

1. 文本答案。
2. 结构化表格。
3. 可选 SQL 校验块。
4. DBGPT 思考过程折叠区。

### 报告生成页面

控件：

1. 数据库选择。
2. 知识库选择。
3. Skill 选择或根据知识库自动匹配 Skill。
4. 报告模板选择，默认加载当前 Skill 下的默认模板。
5. 参数输入，例如日期范围、区域、组织。
6. 是否返回 SQL 开关。
7. 生成按钮。

展示：

1. 报告预览。
2. 结构化数据查看。
3. 可选 SQL 查看。
4. 导出 HTML/PDF。
5. 保存报告快照。

## 渲染器建议

前端报告渲染器应支持以下组件类型：

1. `title`：标题。
2. `paragraph`：段落。
3. `metricGrid`：指标卡。
4. `lineChart`：折线图。
5. `barChart`：柱状图。
6. `pieChart`：饼图。
7. `table`：表格。
8. `insightList`：结论列表。
9. `warningList`：风险提示。
10. `section`：章节容器。

渲染器只执行受控组件渲染，不直接执行模板中的任意 JavaScript。

第一版前端渲染器支持：

1. `title`
2. `paragraph`
3. `metricGrid`
4. `lineChart`
5. `barChart`
6. `pieChart`
7. `table`
8. `insightList`
9. `warningList`

## 安全与稳定性

1. 不让 DBGPT 直接输出完整 HTML 作为最终页面。
2. 模板样式使用白名单属性。
3. DBGPT 返回 JSON 必须经过 Schema 校验。
4. SQL 默认不返回，只有显式开启才要求返回。
5. SQL 即使返回，也只作为校验说明，不作为主链路自动执行。
6. 报告快照应保存模板版本和生成数据，避免模板升级后历史报告失真。

## 分阶段实施

### 第一阶段：修正问数主链路

1. 普通问数不再默认要求 DBGPT 返回 SQL。
2. 前端不再自动抽 SQL 调 DataMaster `/execute`。
3. 增加 `returnSql` 参数。
4. 确认并接入 DBGPT 知识库选择参数。

### 第二阶段：扩展 Skill 生成

1. 支持整库 Skill。
2. 支持多表 Skill。
3. 支持报告模板 Skill。
4. Skill 同步到 DBGPT 知识库时保留类型和业务域信息。

当前实现说明：

1. 表级 Skill：选择数据源和单表生成。优先使用 DataMaster 资产与字段元数据；资产字段为空时优先读取元数据采集表 `AST_DISCOVERY_TABLE`、`AST_DISCOVERY_COLUMN` 并回写资产字段；采集字段也不存在时，再从数据源实时探查表和字段。
2. 整库 Skill：选择数据源生成。优先复用已有资产；实时表清单用于补充缺失资产，实时连接失败时仍可基于已有资产和采集字段生成。
3. 多表 Skill：选择数据源后多选表生成。逐表复用资产字段或采集字段，生成同名字段、疑似关联键和跨表问数规则。
4. 问数 Skill 检索范围已从表级扩展到 `TABLE`、`DATABASE`、`MULTI_TABLE`、`REPORT_TEMPLATE`。
5. 同步全部 Skill 到 DBGPT 时同步所有已发布 Skill，不再只同步表级 Skill。
6. 元数据采集详情字段列表在表状态或变更态参数变化后会重算查询参数并刷新，避免从“版本与变更”返回后字段列表沿用旧过滤条件。

### 第三阶段：模板管理

1. 新增报告模板表。
2. 支持模板上传、校验、发布。
3. 支持模板绑定知识库和业务域。
4. 支持模板版本管理。

### 第四阶段：报告生成

1. 新增报告生成接口。
2. 后端按模板 Schema 组装 DBGPT 请求。
3. 校验 DBGPT 返回数据。
4. 前端渲染报告模板。

### 第五阶段：导出与沉淀

1. 支持报告保存。
2. 支持 HTML/PDF 导出。
3. 支持报告快照。
4. 支持历史报告查看和复用。

## 需要确认的问题

1. DBGPT 原始页面同时选择数据库和知识库时，实际请求字段是什么。
2. 知识库是否按业务域拆分，还是统一使用 `datamaster-skills`。
3. 报告模板是全局模板、业务域模板，还是绑定某个数据库。
4. 报告模板样式允许到什么程度。
5. 报告生成是否需要保存每次 DBGPT 返回的原始 JSON。
6. SQL 返回是全局开关，还是每次问答/报告单独控制。
