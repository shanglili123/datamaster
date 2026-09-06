# datamaster-ui: el-* → a-* 迁移规范

目标：移除 element-plus，全部改用 ant-design-vue（v4.2.6，`app.use(Antd)` 全局注册 `a-` 前缀组件）。
所有迁移 agent 必须严格遵循本文档。迁移完成后 `el-` 标签与 `v-loading` 必须清零。

## 1. 组件标签映射表

| element-plus | ant-design-vue | 备注 |
|---|---|---|
| `el-button` | `a-button` | type/size 见 §3.1 |
| `el-input` | `a-input` | v-model → v-model:value；textarea 用 `a-textarea` |
| `el-input-number` | `a-input-number` | v-model → v-model:value |
| `el-select` | `a-select` | v-model → v-model:value |
| `el-option` | `a-option` | label/value 语义相同 |
| `el-radio` | `a-radio` | **范式差异**：`:label` 是值 → `:value`；文本移入插槽，见 §4.2 |
| `el-radio-group` | `a-radio-group` | v-model → v-model:value |
| `el-checkbox` | `a-checkbox` | v-model → v-model:checked |
| `el-checkbox-group` | `a-checkbox-group` | v-model → v-model:value |
| `el-switch` | `a-switch` | v-model → v-model:checked；active-color/inactive-color 删除 |
| `el-date-picker` | `a-date-picker` | v-model → v-model:value；value-format → valueFormat；type=daterange → `a-range-picker` |
| `el-time-picker` | `a-time-picker` | v-model → v-model:value |
| `el-form` | `a-form` | 见 §4.1 |
| `el-form-item` | `a-form-item` | prop → name |
| `el-row` | `a-row` | gutter 见 §3.2 |
| `el-col` | `a-col` | 相同 |
| `el-table` | `a-table` | **范式差异**：:data → :data-source，列定义见 §4.3 |
| `el-table-column` | —（columns 数组） | 见 §4.3 |
| `el-dialog` | `a-modal` | v-model → v-model:open；footer 插槽相同；append-to-body 删除 |
| `el-drawer` | `a-drawer` | v-model → v-model:open |
| `el-tabs` | `a-tabs` | v-model → v-model:activeKey |
| `el-tab-pane` | `a-tab-pane` | label → :tab；name → :key |
| `el-tooltip` | `a-tooltip` | content → :title |
| `el-popover` | `a-popover` | 相同 |
| `el-tag` | `a-tag` | type: success/info/warning/danger → success/default/warning/error |
| `el-dropdown` | `a-dropdown` | **结构差异**：子项用 a-menu/a-menu-item，见 §4.4 |
| `el-dropdown-item` | `a-menu-item` | 见 §4.4 |
| `el-dropdown-menu` | `a-menu` | 见 §4.4 |
| `el-tree` | `a-tree` | :data → :tree-data；:props → :field-names |
| `el-tree-select` | `a-tree-select` | :data → :tree-data；:props → :field-names；value-key 删除 |
| `el-cascader` | `a-cascader` | 相同 |
| `el-transfer` | `a-transfer` | :data → :data-source |
| `el-autocomplete` | `a-auto-complete` | v-model → v-model:value |
| `el-upload` | `a-upload` | **API 差异大**，见 §4.5 |
| `el-pagination` | `a-pagination` | 项目已有封装组件 `<pagination>`（已迁移），页面直接替换为 `<pagination>` 即可 |
| `el-dialog` 内 `el-icon` | 图标组件 | 见 §3.3 |
| `el-icon` | — | 包裹层删除，图标组件直接使用，见 §3.3 |
| `el-card` | `a-card` | 相同 |
| `el-divider` | `a-divider` | 相同 |
| `el-empty` | `a-empty` | 相同 |
| `el-alert` | `a-alert` | 相同 |
| `el-avatar` | `a-avatar` | 相同 |
| `el-skeleton` | `a-skeleton` | 相同 |
| `el-image` | `a-image` | :src → :src；preview-src-list → :preview 数组 |
| `el-link` | `a-link` | 相同 |
| `el-badge` | `a-badge` | 相同 |
| `el-descriptions` / `el-descriptions-item` | `a-descriptions` / `a-descriptions-item` | 相同 |
| `el-container`/`el-aside`/`el-header`/`el-main`/`el-footer` | `a-layout`/`a-layout-sider`/`a-layout-header`/`a-layout-content`/`a-layout-footer` | 低频 |
| `el-menu`/`el-sub-menu`/`el-menu-item` | `a-menu`/`a-sub-menu`/`a-menu-item` | 低频 |
| `el-scrollbar` | — | antd 无对应，保留 div + CSS overflow |
| `el-collapse`/`el-collapse-transition` | `a-collapse`/— | 低频 |
| `el-segmented` | `a-segmented` | 低频 |
| `el-check-tag` | `a-checkable-tag` | 低频 |
| `el-calendar`/`el-carousel`/`el-steps`/`el-progress`/`el-rate`/`el-slider`/`el-color-picker` | `a-calendar`/`a-carousel`/`a-steps`/`a-progress`/`a-rate`/`a-slider`/`a-color-picker` | 低频，v-model → v-model:value |
| `el-timeline` | `a-timeline` | 低频 |
| `el-result` | `a-result` | 低频 |
| `el-page-header` | `a-page-header` | 低频 |
| `el-backtop` | `a-backtop` | 低频 |
| `el-affix` | `a-affix` | 低频 |
| `el-breadcrumb` | `a-breadcrumb` | 低频 |
| `el-popconfirm` | `a-popconfirm` | 低频 |

## 2. 通用 v-model 映射

| element-plus | ant-design-vue |
|---|---|
| `v-model="x"`（表单控件） | `v-model:value="x"` |
| `v-model`（switch/checkbox） | `v-model:checked="x"` |
| `v-model`（dialog/drawer） | `v-model:open="x"` |
| `v-model`（tabs activeName） | `v-model:activeKey="x"` |
| `v-model`（radio-group/select/date-picker/tree-select/input-number/cascader） | `v-model:value="x"` |

## 3. 高频属性映射

### 3.1 a-button
- `type`: primary/success/warning/danger/info → `primary`/`success`/`warning`/`danger`/`default`（info → default）
- `size`: default/small/large → 省略（middle）/`small`/`large`
- `plain` → 删除（antd 无对应）
- `round` → `shape="round"`；`circle` → `shape="circle"`；`text` → `type="text"`；`link` → `type="link"`
- `icon="Edit"` → 需要图标：`import { EditOutlined } from '@ant-design/icons-vue'`，`:icon="h(EditOutlined)"`，文本仍为插槽内容。**若无法快速确定图标，直接删除 icon 属性**（保证功能正确优先）
- `@click`/`:disabled`/`:loading` 相同

### 3.2 a-row / a-col
- `:gutter="20"` → `:gutter="20"`（数字兼容）；`:gutter="[16, 16]"` → 相同
- `:span`/`:offset`/`:xs`/`:sm`/`:md`/`:lg` 相同

### 3.3 图标（el-icon）
- `<el-icon><Edit /></el-icon>` → 直接 `<EditOutlined />`（需 import 对应图标）
- 常见映射（element-plus icons → antd icons）：
  - Plus→PlusOutlined, Edit→EditOutlined, Delete→DeleteOutlined, Search→SearchOutlined, Refresh→ReloadOutlined, Download→DownloadOutlined, Upload→UploadOutlined, Setting→SettingOutlined, User→UserOutlined, Lock→LockOutlined, Close→CloseOutlined, Check→CheckOutlined, ArrowDown→DownOutlined, ArrowUp→UpOutlined, ArrowLeft→LeftOutlined, ArrowRight→RightOutlined, Fold→MenuFoldOutlined, Expand→MenuUnfoldOutlined, Document→FileTextOutlined, Link→LinkOutlined, Copy→CopyOutlined, Warning→WarningOutlined, Info→InfoCircleOutlined, Loading→LoadingOutlined, ChatDotRound→MessageOutlined, View→EyeOutlined, Hide→EyeInvisibleOutlined, CirclePlus→PlusCircleOutlined, FolderOpened→FolderOpenOutlined, Tickets→ProfileOutlined, Collection→FolderOutlined, Histogram→BarChartOutlined, Opportunity→FundOutlined, DataAnalysis→LineChartOutlined, Coin→DollarOutlined, Promotion→SendOutlined, Cpu→CpuOutlined, RefreshRight→ReloadOutlined, Tickets→ProfileOutlined, Search→SearchOutlined, Download→DownloadOutlined, Delete→DeleteOutlined, Document→FileTextOutlined, FolderOpened→FolderOpenOutlined, Folder→FolderOutlined, Tickets→ProfileOutlined
  - 图标统一从 `@ant-design/icons-vue` 导入（已在 package.json，^7.0.1）
- **el-icon 仅用于图标包裹时删除**；若 el-icon 带动态内容保留子元素

### 3.4 v-loading（183 处）
- **a-table 上**：`:loading="x"`（v-loading 删除）
- **其他元素**：`<a-spin :spinning="x">...</a-spin>` 包裹原元素，v-loading 删除。若包裹会破坏布局（如绝对定位容器），可在最外层包一层 `<a-spin>` 并给原容器设 `min-height`
- v-loading 带 `element-loading-text` → a-spin 的 `tip` 属性

## 4. 范式转换（重点）

### 4.1 el-form → a-form（校验体系）
**element-plus：**
```vue
<el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
  <el-form-item label="名称" prop="name">
    <el-input v-model="form.name" />
  </el-form-item>
</el-form>
```
```js
formRef.value.validate((valid) => { if (valid) { ... } });  // 回调风格
proxy.resetForm('formRef');  // 重置
```
**ant-design-vue：**
```vue
<a-form ref="formRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
  <a-form-item label="名称" name="name">
    <a-input v-model:value="form.name" />
  </a-form-item>
</a-form>
```
```js
formRef.value.validate().then(() => { ... }).catch(() => {});  // promise 风格
formRef.value.resetFields();  // 重置（label-width 用 label-col 替代）
```
- `label-width="80px"` → `:label-col="{ style: { width: '80px' } }"`
- `label-position="top"` → `layout="vertical"`；`inline` → `:layout="inline"`
- `:rules` 中 `trigger` 值：el 的 `blur`/`change` 在 antd 同样支持，无需改动
- 校验失败的 catch 必须存在（`.catch(() => {})`），避免未处理 rejection
- `clearValidate()` 相同

### 4.2 el-radio → a-radio（值语义）
**element-plus：**
```vue
<el-radio v-model="form.validFlag" :label="true">启用</el-radio>
<el-radio v-model="form.validFlag" :label="false">禁用</el-radio>
```
**ant-design-vue：**（文本移入插槽，值用 :value）
```vue
<a-radio-group v-model:value="form.validFlag">
  <a-radio :value="true">启用</a-radio>
  <a-radio :value="false">禁用</a-radio>
</a-radio-group>
```
- el-radio 单独使用（非 group 内）时：`v-model` → `v-model:checked`，`:label` → `:value`
- el-radio 的 `disabled`/`border` 相同

### 4.3 el-table → a-table（列定义迁移到 script）
**element-plus（模板子组件定义列）：**
```vue
<el-table :data="list" v-loading="loading" @selection-change="handleSelectionChange">
  <el-table-column type="selection" width="55" />
  <el-table-column label="名称" prop="name" align="center" width="120" :show-overflow-tooltip="true">
    <template #default="scope">{{ scope.row.name || '-' }}</template>
  </el-table-column>
  <el-table-column label="操作" align="center" fixed="right" width="200">
    <template #default="scope">
      <a-button type="link" @click="handleUpdate(scope.row)">修改</a-button>
    </template>
  </el-table-column>
</el-table>
```
**ant-design-vue（columns 数组 + #bodyCell 插槽）：**
```vue
<a-table :data-source="list" :loading="loading" :row-key="'id'" :columns="columns"
  :row-selection="rowSelection" @change="handleTableChange">
  <template #bodyCell="{ column, record }">
    <template v-if="column.key === 'action'">
      <a-button type="link" @click="handleUpdate(record)">修改</a-button>
    </template>
    <template v-else-if="column.key === 'name'">{{ record.name || '-' }}</template>
  </template>
</a-table>
```
```js
const columns = [
  { title: '名称', dataIndex: 'name', key: 'name', align: 'center', width: 120, ellipsis: true },
  { title: '操作', key: 'action', align: 'center', fixed: 'right', width: 200 },
];
const rowSelection = { onChange: (selectedRowKeys, selectedRows) => { handleSelectionChange(selectedRows); } };
```
**映射规则：**
- 每列：`label` → `title`；`prop` → `dataIndex`；`key` 保留；`width`/`align`/`fixed`/`class-name` 相同；`:show-overflow-tooltip` → `ellipsis: true`（同时加 `tooltip: true` 可选）
- `type="selection"` → 移到 `:row-selection="{ selectedRowKeys, onChange }"`（selectedRowKeys 需要 ref，若原代码无维护则用 `onChange` 直接处理行即可）
- `type="index"` → 列定义 `{ title: '#', dataIndex: 'index', key: 'index', customRender: ({ index }) => index + 1 }`（注意：antd customRender 无 index 参数，改用 `#bodyCell` 中 `record` 无法取 index → 用 `customRender` 不行时改为列数据里无序号，或直接删除 index 列）
- **带 `#default="scope"` 插槽的列**：整列内容搬入 `#bodyCell`，条件为 `column.key === 'xxx'`（列必须定义 `key`）。`scope.row` → `record`
- **无插槽的简单列**（`{{ scope.row.xxx }}` 形式）也可放 `customRender: ({ text }) => text || '-'`，或统一走 bodyCell
- `:data` → `:data-source`；`row-key="id"` → `:row-key="'id'"` 或 `:row-key="row => row.id"`
- `v-loading` → `:loading`
- `@selection-change` → rowSelection.onChange（参数是 (keys, rows)）
- `@row-click` → `@row-click`（antd 事件名 `rowClick` → 模板中 `@row-click` 相同）；`@row-dblclick` 相同
- 树形：`:tree-props="{ children: 'children' }"` → 删除（antd 默认 children 字段）；`:default-expand-all` → `:default-expand-all-rows`；`:expand-row-keys` → 相同
- `:show-header` → `:show-header`；`empty-text` → `:locale="{ emptyText: '暂无数据' }"`
- `height`/`max-height` 相同；`:header-cell-style`/`:cell-style` 相同
- 分页：若 el-table 内嵌分页（`:pagination` 不存在于 el-table，忽略）
- **el-table 上 el-table-column 的 `:formatter`** → `customRender: ({ text }) => ...`

### 4.4 el-dropdown → a-dropdown（子菜单结构）
**element-plus：**
```vue
<el-dropdown @command="handleCommand">
  <span>更多</span>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item command="a">选项A</el-dropdown-item>
      <el-dropdown-item command="b" divided>选项B</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
```
**ant-design-vue：**
```vue
<a-dropdown>
  <span>更多</span>
  <template #overlay>
    <a-menu @click="handleMenuClick">
      <a-menu-item key="a">选项A</a-menu-item>
      <a-menu-item key="b">选项B</a-menu-item>
    </a-menu>
  </template>
</a-dropdown>
```
```js
const handleMenuClick = ({ key }) => { handleCommand(key); };
```
- `@command` → `@click` 在 a-menu 上，参数从 `command` 变成 `{ key }`
- `divided` → 删除或加 `<a-menu-divider />`
- `disabled` → `:disabled` 相同

### 4.5 el-upload → a-upload
**element-plus：**
```vue
<el-upload :action="url" :headers="headers" :file-list="fileList" :on-success="handleSuccess"
  :on-remove="handleRemove" list-type="picture-card" :limit="1">
  <el-button>上传</el-button>
</el-upload>
```
**ant-design-vue：**
```vue
<a-upload :action="url" :headers="headers" v-model:file-list="fileList" :on-success="handleSuccess"
  :on-remove="handleRemove" list-type="picture-card" :max-count="1">
  <a-button>上传</a-button>
</a-upload>
```
- `:file-list` → `v-model:file-list`（若原来是只读展示则保留 `:file-list`）
- `:limit` → `:max-count`
- `:on-success` → `:on-success`（函数签名 `(response, file, fileList)` 相同）
- `list-type="text"` → 删除（默认 text）；`"picture-card"` → 相同
- `:before-upload` → `:before-upload`（返回 false 阻止上传相同）
- `:on-preview`/`:on-remove`/`:on-change` 相同
- `:accept` 相同；`:drag` → `:dragger`（或 `a-upload-dragger`）

### 4.6 el-date-picker
- 单日期：`value-format="YYYY-MM-DD"` → `valueFormat="YYYY-MM-DD"`；`:picker-options` → `:disabled-date`（函数不同，若复杂可保留 picker-options 并加注释 TODO）
- daterange：`<el-date-picker type="daterange" ...>` → `<a-range-picker valueFormat="YYYY-MM-DD" ...>`，其余属性迁移到 a-range-picker
- `@change` 回调参数相同（value）；antd 的 `@change` 参数为 `(dates, dateStrings)`——dateStrings 才是格式化字符串，注意适配

## 5. 迁移步骤（每个文件）

1. **读整个文件**，列出所有 el-* 标签与相关 script 逻辑
2. 按 §1 映射标签，按 §2/§3 映射属性，按 §4 处理范式转换
3. **补充 import**：antd 组件全局注册无需 import；但**图标**需要 `import { XxxOutlined } from '@ant-design/icons-vue'`；若 script 中直接用 `message`/`Modal` 需 `import { message, Modal } from 'ant-design-vue'`
4. 删除 element-plus 相关：v-loading 移除、el-icon 包裹移除
5. **不要改动**：业务逻辑、API 调用、proxy.$modal（已是 antd 封装）、v-hasPermi、pagination 组件（已迁移）
6. 自查：grep 无 `el-`、无 `v-loading`、无 `element-plus` import

## 6. 验收标准

- 文件内无 `<el-` 标签（除注释/字符串中的合法引用）
- 无 `v-loading`
- 无 `from 'element-plus'`
- `npm run eslint:lint` 通过（或仅剩预先存在的风格问题）
- 保持原有业务逻辑与交互语义不变

## 7. 常见坑

- `a-form` 的 `name` 必须与 rules 的 key 对应，否则校验失效
- `a-form` 校验失败 promise reject，必须 `.catch(() => {})`
- `a-table` 列未定义 `key` 时 bodyCell 无法匹配——每列都定义 key
- `a-modal` 的 `@ok` 返回 promise 会显示 loading；原 `@confirm` → `@ok`
- 单选 radio 组必须用 `a-radio-group` 包裹（antd 无 el 式独立 v-model 语义）
- 表格内 `scope.row.xxx` 全部改 `record.xxx`
- el- 组件的 `size="mini"` → antd `size="small"`；`size="medium"`/`size="default"` → 省略
- 不要迁移 `views/sys/tool/gen/`（代码生成器模板字符串含 element-plus 字样，属模板配置）
- `el-form` 的 `:inline` 若带 label-width 需同时处理 label-col

## 8. ai/chat 批量迁移经验（2026-08）

### 8.1 a-select 的 `prefix` 插槽

antd `a-select` **没有** el-select 的 `#prefix` 插槽。处理方式：用外层 div 包住图标 + a-select，把 border/padding/width 样式移到外层 div，select 内部做成透明无边框：

```vue
<div class="model-select">
  <img :src="selectedModelIcon" class="model-icon" />
  <a-select v-model:value="selectedModelId" ... />
</div>
```

```scss
.model-select {
  display: flex; align-items: center; gap: 4px;
  border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 10px;
  :deep(.ant-select-selector) {
    background: transparent; box-shadow: none !important;
    border: none !important; padding: 0; height: 30px;
  }
  :deep(.ant-select-selection-item) { line-height: 30px; }
}
```

- `popper-class="x"` → `popup-class-name="x"`（`popupClassName`）
- `:global(.ai-x-popper .el-select-dropdown__item)` → `:global(.ai-x-popper .ant-select-item-option)`
- 下拉项内图标 + 文字结构保留在 a-select-option 插槽里即可

### 8.2 a-textarea

- `el-input type="textarea"` → `a-textarea`；`:autosize` → `:auto-size`
- `@keydown.enter.native` → `@keydown.enter`（Vue 3 移除 `.native`，a-textarea 原生事件直接透传）
- 样式：`:deep(.el-textarea__inner)` → `:deep(.ant-input)`
- 图标：`import { Plus } from "@element-plus/icons-vue"` → `import { PlusOutlined } from "@ant-design/icons-vue"`
- `el-icon` 包裹层删除，图标组件直接放；`:is="动态组件"` 的图标来自其它常量文件（element-plus 图标）时保留 `:is` 引用即可，无需改常量文件

### 8.3 el-skeleton

- `el-skeleton animated` → `a-skeleton active`（antd 用 `active` 开启动画）
