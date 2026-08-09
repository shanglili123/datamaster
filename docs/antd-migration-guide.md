# DataMaster 前端 Element Plus → Ant Design Vue 模板迁移指南

> 目标：移除 element-plus，全量迁移到 ant-design-vue（^4.2.6）。
> JS API 层（ElMessage/ElMessageBox/ElNotification/ElLoading → message/Modal/notification）已完成迁移。
> 本指南用于 **模板层 el-* 组件** 的迁移。

## 总原则

1. **只改模板（template）**，不改 JS 业务逻辑（函数体、API 调用、数据结构、ref 声明）。
2. 每个 el-* 标签都映射为对应的 antd 组件（见映射表），保持语义等价。
3. **v-model 绑定差异**：el 组件是 `v-model`，antd 大部分是 `v-model:value`（switch 是 `v-model:checked`，modal/drawer 是 `v-model:open`，tabs 是 `v-model:activeKey`）。
4. **el- 相关样式类**：模板中的 `el-form-input-width`、`el-input__inner` 等 element 内部类在 antd 下失效，若仅是自定义宽度类保留，若是 element 内部结构类删除或替换。
5. **注释里的 el- 标签不要动**（保持注释原样）。
6. antd 组件全局注册（main.js `app.use(Antd)`），模板直接用 `a-xxx`，**不需要 import 组件**；图标除外（见图标节）。
7. 迁移完成后，运行 `Get-ChildItem src -Recurse -Filter *.vue | Select-String '<el-'` 确认无真实 el- 标签残留（注释除外）。

## 样板文件（先读再动手）

- `src/views/tax/cat/assetCat/index.vue` — a-form / a-form-item / a-table+columns / a-modal / a-switch / a-radio-group 完整样板
- `src/views/sys/tool/gen/index.vue` — a-form inline / a-table / a-button 图标用法
- `src/layout/components/Navbar.vue` — a-select / a-dropdown / a-menu 样板
- `src/views/sys/login.vue` — a-form 布局样板

## 组件映射表

### 布局类
| el | antd | 要点 |
|---|---|---|
| `el-row` | `a-row` | `:gutter` 兼容 |
| `el-col` | `a-col` | `:span` / `:offset` 兼容 |
| `el-container` | `a-layout` | |
| `el-header` | `a-layout-header` | |
| `el-aside` | `a-layout-sider` | |
| `el-main` | `a-layout-content` | |
| `el-footer` | `a-layout-footer` | |
| `el-divider` | `a-divider` | |

### 表单类
| el | antd | 要点 |
|---|---|---|
| `el-form` | `a-form` | `label-width="80px"` → `:label-col="{ style: { width: '80px' } }"`；`inline` → `layout="inline"` |
| `el-form-item` | `a-form-item` | **`prop="x"` → `name="x"`**；`label` 兼容 |
| `el-input` | `a-input` | `clearable` → `allow-clear`；`show-password` → `type="password"`；`prefix-icon` → `#prefix` 插槽；`suffix-icon` → `#suffix`；`maxlength`/`show-word-limit` 兼容 |
| `el-input-number` | `a-input-number` | `:min`/`:max`/`:step` 兼容；`v-model` → `v-model:value` |
| `el-select` | `a-select` | **`v-model` → `v-model:value`**；`clearable` → `allow-clear`；`multiple` → `mode="multiple"`；`filterable` → `show-search`；`collapse-tags` → `max-tag-count` |
| `el-option` | `a-select-option` | `:label`/`:value` 兼容；`v-if`/`v-for` 保留 |
| `el-radio` | `a-radio` | **`el-radio` 的值用 `label`（新版本也可 `value`）→ antd 用 `:value`** |
| `el-radio-group` | `a-radio-group` | `v-model` → `v-model:value` |
| `el-checkbox` | `a-checkbox` | |
| `el-checkbox-group` | `a-checkbox-group` | `v-model` → `v-model:value` |
| `el-switch` | `a-switch` | **`v-model` → `v-model:checked`**；`active-text`/`inactive-text` → `checked-children`/`un-checked-children`；`active-value` → `checked-value`；`inactive-value` → `un-checked-value` |
| `el-date-picker` | `a-date-picker` | `v-model` → `v-model:value`；`type="datetime"` → 加 `show-time`；`value-format="YYYY-MM-DD"` → `value-format="YYYY-MM-DD"`（antd v4 支持）；范围选择 → 拆成 `a-range-picker` |
| `el-slider` | `a-slider` | |
| `el-rate` | `a-rate` | |
| `el-cascader` | `a-cascader` | `:options` → `:options`；`v-model` → `v-model:value` |
| `el-transfer` | `a-transfer` | `v-model` → `v-model:target-keys` |
| `el-autocomplete` | `a-auto-complete` | `:fetch-suggestions` → `:options`（需改写为 options 数组，注意保持逻辑最小改动） |

### 弹层类
| el | antd | 要点 |
|---|---|---|
| `el-dialog` | `a-modal` | **`v-model` → `v-model:open`**；`width` 兼容；`append-to-body` 删除（antd 默认挂 body）；`align-center` → `centered`；`destroy-on-close` 兼容；`close-on-click-modal` → `:mask-closable`；`close-on-press-escape` → `:keyboard`；`show-close` 兼容；`#footer` 插槽兼容 |
| `el-drawer` | `a-drawer` | `v-model` → `v-model:open`；`direction="rtl"` 默认；`size` 兼容 |
| `el-popover` | `a-popover` | `content` → `content`（或用 `#content`）；`trigger` 兼容；`placement` 兼容 |
| `el-tooltip` | `a-tooltip` | **`content="xxx"` → `title="xxx"`**；`placement` 兼容 |
| `el-dropdown` | `a-dropdown` | `@command="fn"` → `@click="fn"`（参数从 command 变为 `{ key }`，见下）；`trigger="click"` 兼容；**`#dropdown` → `#overlay`** |
| `el-dropdown-menu` | `a-menu` | 作为 `#overlay` 内容 |
| `el-dropdown-item` | `a-menu-item` | `command="x"` → `key="x"`；`divided` → 拆出 `a-menu-divider` |
| `el-popconfirm` | `a-popconfirm` | `title` 兼容；`@confirm` 兼容 |

### 数据展示类
| el | antd | 要点 |
|---|---|---|
| `el-table` | `a-table` | **大改**：`:data="rows"` → `:data-source="rows"`；`:loading` 兼容；`stripe` → 删除；`border` → `bordered`；`show-overflow-tooltip` 移到列定义 `ellipsis: true`；`:row-key` 兼容；`@selection-change` → `@change`（需按 antd 语义调整，保持逻辑最小改动）；列定义改为 `:columns="columns"` 数组 + `#bodyCell` 插槽（见下表） |
| `el-table-column` | columns 数组 | 转为 `{ title, dataIndex, key, width, align, ellipsis, fixed, customRender }`；有自定义内容的列（按钮/switch/图片）在 `#bodyCell="{ column, record }"` 插槽里按 `column.key` 分支渲染 |
| `el-tag` | `a-tag` | `type="primary"` → 默认（或 `color="blue"`）；`type="success"` → `color="green"`；`type="info"` → 默认；`type="warning"` → `color="orange"`；`type="danger"` → `color="red"`；`closable` 兼容 |
| `el-badge` | `a-badge` | `:value`/`:max` 兼容；`is-dot` → `dot` |
| `el-card` | `a-card` | |
| `el-descriptions` | `a-descriptions` | `:column` 兼容 |
| `el-descriptions-item` | `a-descriptions-item` | `label` 兼容 |
| `el-empty` | `a-empty` | `description` 兼容 |
| `el-image` | `a-image` | `:src`/`:preview-src-list` 兼容；`fit` 兼容 |
| `el-avatar` | `a-avatar` | `:size`/`:src` 兼容；`shape="circle"` 默认 |
| `el-progress` | `a-progress` | `:percentage` 兼容；`type="circle"` → `type="circle"` |
| `el-skeleton` | `a-skeleton` | `:loading`/`animated` 兼容 |
| `el-alert` | `a-alert` | `:title`/`type` 兼容 |
| `el-text` | `span` | 直接替换为 `<span>`，class 保留 |
| `el-link` | `a-typography-link` | 或 `<a>`；保持原有 @click |
| `el-check-tag` | `a-tag` | 自定义样式模拟选中态 |
| `el-timeline` | `a-timeline` | |
| `el-timeline-item` | `a-timeline-item` | `:timestamp` 兼容 |
| `el-calendar` | （无） | 用 a-calendar 或保留布局 |

### 导航类
| el | antd | 要点 |
|---|---|---|
| `el-menu` | `a-menu` | `:default-active` → `v-model:selectedKeys`（数组）或 `:selected-keys`；`@select` → `@click`（参数 `{ key }`）；`collapse` → `:inline-collapsed`；`mode="vertical"` 默认 |
| `el-menu-item` | `a-menu-item` | `index="x"` → `key="x"` |
| `el-sub-menu` | `a-sub-menu` | `index` → `key`；`#title` 插槽兼容 |
| `el-tabs` | `a-tabs` | **`v-model` → `v-model:activeKey`**；`@tab-click` → `@change`（参数不同，按最小改动处理）；`type="border-card"` → `type="card"` |
| `el-tab-pane` | `a-tab-pane` | **`label="x"` → `tab="x"`**；`name="x"` → `key="x"` |
| `el-breadcrumb` | `a-breadcrumb` | |
| `el-breadcrumb-item` | `a-breadcrumb-item` | |
| `el-tree` | `a-tree` | `:data` → `:tree-data`；`:props="{ label, children }"` → `:field-names="{ title, children }"`（label → title）；`node-key` → `:field-names="{ value }"` 或 row-key；`show-checkbox` → `checkable`；`@node-click` → `@select`（参数不同，注意处理）；`default-expand-all` 兼容 |
| `el-scrollbar` | （无） | 替换为 `<div style="overflow:auto">`，保留 height/width 样式 |

### 其他
| el | antd | 要点 |
|---|---|---|
| `el-button` | `a-button` | `type="primary"` 兼容；`type="danger"` → `danger`（属性）；`type="text"` → `type="text"`；`type="link"` → `type="link"`；`type="info"/"warning"/"success"` → 保留 `type` 或改默认样式；`plain` → 删除（antd 无）；`round` → `shape="round"`；`circle` → `shape="circle"`；`size="medium"` → `size="middle"`；`size="mini"` → `size="small"`；`icon="Edit"` → `:icon="h(EditOutlined)"`（需 import）；`v-loading` → `:loading` |
| `el-icon` | antd 图标 | 见图标节 |
| `el-upload` | `a-upload` | `:action` 兼容；`:before-upload` 兼容；`:on-success` → 在 `@change` 或 `:custom-request` 中处理（最小改动原则）；`list-type="picture-card"` 兼容；`drag` → 用 `<a-upload-dragger>` |
| `el-pagination` | 项目封装 `<pagination>` | RuoYi 封装组件 `v-model:page` + `v-model:limit` + `@pagination`，参考已迁移文件用法；或 `a-pagination` |
| `el-skeleton` | `a-skeleton` | |
| `el-segmented` | `a-segmented` | `:options` 兼容 |

### 指令
| el 指令 | antd 处理 |
|---|---|
| `v-loading="x"` | 表格 → `:loading="x"`；普通元素 → 用 `<a-spin :spinning="x">` 包裹 |
| `el-` 类样式 | 模板中的自定义类（如 `el-form-input-width`）保留；element 内部结构类（`el-input__inner` 等）删除或改用 antd 类 |

## 图标迁移

element-plus 图标（`<el-icon><Edit /></el-icon>` 或 `icon="Edit"`）→ antd 图标：

```vue
<!-- el-icon 包裹 -->
<el-icon><Edit /></el-icon>
<!-- 改为（需在 script 里 import） -->
<script setup>
import { EditOutlined } from '@ant-design/icons-vue'
</script>
<template>
<EditOutlined />
</template>
```

常用图标名映射（element → antd）：`Edit` → `EditOutlined`，`Delete` → `DeleteOutlined`，`Plus` → `PlusOutlined`，`Search` → `SearchOutlined`，`Refresh` → `ReloadOutlined`，`Download` → `DownloadOutlined`，`Upload` → `UploadOutlined`，`Close` → `CloseOutlined`，`Check` → `CheckOutlined`，`Warning` → `WarningOutlined`，`Info` → `InfoCircleOutlined`，`ArrowDown` → `DownOutlined`，`ArrowUp` → `UpOutlined`，`ArrowLeft` → `LeftOutlined`，`ArrowRight` → `RightOutlined`，`Setting` → `SettingOutlined`，`User` → `UserOutlined`，`Lock` → `LockOutlined`，`View` → `EyeOutlined`，`Hide` → `EyeInvisibleOutlined`，`Calendar` → `CalendarOutlined`，`Clock` → `ClockCircleOutlined`，`Document` → `FileOutlined`，`Folder` → `FolderOutlined`，`Menu` → `MenuOutlined`，`More` → `MoreOutlined`，`Star` → `StarOutlined`，`Bell` → `BellOutlined`，`Message` → `MessageOutlined`，`Phone` → `PhoneOutlined`，`Printer` → `PrinterOutlined`，`Link` → `LinkOutlined`，`Filter` → `FilterOutlined`，`Sort` → `SortAscendingOutlined`，`Grid` → `AppstoreOutlined`，`List` → `UnorderedListOutlined`，`Data` → `DatabaseOutlined`，`Connection` → `ApiOutlined`，`Top` → `VerticalAlignTopOutlined`，`Bottom` → `VerticalAlignBottomOutlined`，`Caret-bottom` → `CaretDownOutlined`，`Caret-top` → `CaretUpOutlined`，`FullScreen` → `FullscreenOutlined`，`Aim` → `AimOutlined`，`SwitchButton` → `PoweroffOutlined`，`Question` → `QuestionCircleOutlined`，`CirclePlus` → `PlusCircleOutlined`，`CircleCheck` → `CheckCircleOutlined`，`CircleClose` → `CloseCircleOutlined`，`Memo` → `FileTextOutlined`，`ChatDotRound` → `MessageOutlined`，`Key` → `KeyOutlined`，`Monitor` → `MonitorOutlined`，`Operation` → `ControlOutlined`，`TrendCharts` → `LineChartOutlined`，`Odometer` → `DashboardOutlined`，`Timer` → `FieldTimeOutlined`，`OfficeBuilding` → `BankOutlined`，`UserFilled` → `UserOutlined`，`Histogram` → `BarChartOutlined`，`PieChart` → `PieChartOutlined`，`Search` → `SearchOutlined`，`ZoomIn` → `ZoomInOutlined`，`ZoomOut` → `ZoomOutOutlined`，`Position` → `EnvironmentOutlined`，`Location` → `EnvironmentOutlined`，`Share` → `ShareAltOutlined`，`Collection` → `FolderOpenOutlined`，`Files` → `CopyOutlined`，`CopyDocument` → `CopyOutlined`，`Tickets` → `FileTextOutlined`，`Notebook` → `BookOutlined`，`Reading` → `ReadOutlined`，`MagicStick` → `ThunderboltOutlined`，`Cpu` → `CpuOutlined`，`Platform` → `GlobalOutlined`，`Box` → `InboxOutlined`，`Commodity` → `ShoppingOutlined`，`Goods` → `ShoppingCartOutlined`，`SetUp` → `ToolOutlined`，`Tools` → `ToolOutlined`，`Unlock` → `UnlockOutlined`，`Lock` → `LockOutlined`，`Help` → `QuestionCircleOutlined`，`CirclePlusFilled` → `PlusCircleFilled`，`PlusFilled` → `PlusCircleFilled`，`InfoFilled` → `InfoCircleFilled`，`WarningFilled` → `ExclamationCircleFilled`，`SuccessFilled` → `CheckCircleFilled`，`CircleCloseFilled` → `CloseCircleFilled`。

若遇到映射表外的图标：在 `@ant-design/icons-vue` 中查找语义相近的图标；antd 图标组件名后缀统一为 `Outlined`/`Filled`/`TwoTone`。

## 验收清单（每个文件）

- [ ] 无真实 el-* 标签残留（注释里的允许保留）
- [ ] `v-model` 绑定已按 antd 语义转换（value/checked/open/activeKey）
- [ ] 无 `el-` 指令（v-loading 等）残留
- [ ] JS 业务逻辑未被改动（对比迁移前后 diff）
- [ ] 使用的 antd 图标已在 script 中 import
- [ ] 模板结构、class、事件绑定保持原意

## 禁止事项

- ❌ 不修改 JS 业务逻辑、API 调用、数据结构、ref/reactive 声明
- ❌ 不修改 router、store、api 文件
- ❌ 不引入新的 npm 依赖
- ❌ 不删除功能（包括看起来"没用"的代码）
- ❌ 不重排模板结构（保持 DOM 层级）
- ❌ 注释里的 el- 内容不要动
