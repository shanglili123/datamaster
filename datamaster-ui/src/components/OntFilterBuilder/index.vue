<template>
  <div class="ont-filter-builder">
    <!-- 头部：筛选标签 + 命中数 + 重置 + 折叠切换 -->
    <div class="ofb-header">
      <span class="ofb-title">
        <FilterOutlined class="ofb-title-icon" />
        筛选条件
        <span v-if="activeCount" class="ofb-count">{{ activeCount }}</span>
      </span>
      <span class="ofb-header-actions">
        <a class="ofb-reset" @click="reset">重置</a>
        <a-button type="text" size="small" class="ofb-toggle" @click="collapsed = !collapsed">
          <DownOutlined v-if="collapsed" />
          <UpOutlined v-else />
          {{ collapsed ? '展开' : '收起' }}
        </a-button>
      </span>
    </div>

    <div v-show="!collapsed" class="ofb-body">
      <!-- 工具栏（整条同一行）：关键字 + 展示列 + 添加条件组 + 添加排序 + 查询 -->
      <div class="ofb-toolbar">
        <a-input
          v-model:value="localKeyword"
          placeholder="关键字（跨文本列模糊搜索）"
          allow-clear
          class="ofb-keyword"
          @change="emitChange"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>

        <span class="ofb-lab">展示列</span>
        <a-select
          :value="localColumns"
          mode="multiple"
          placeholder="全部列"
          size="small"
          class="ofb-columns"
          show-search
          option-filter-prop="label"
          @change="onColumnsChange"
        >
          <a-select-option v-for="opt in fieldOptions" :key="opt.value" :value="opt.value" :label="opt.label">
            {{ opt.label }}
          </a-select-option>
        </a-select>

        <a-button size="small" type="link" class="ofb-add-group-btn" @click="addGroup">
          + 添加条件组（括号）
        </a-button>

        <a-button size="small" type="link" class="ofb-add-sort-btn" @click="addSort">+ 排序</a-button>

        <a-button v-if="showQuery" type="primary" size="small" class="ofb-query-btn" @click="emit('query')">
          查询
        </a-button>
      </div>

      <!-- 条件组（括号）：组内扁平条件，每条自带 且/或/非 连接符 -->
      <div v-for="(group, gi) in localGroups" :key="gi" class="ofb-group">
        <div class="ofb-group-head">
          <template v-if="gi === 0">
            <span class="ofb-group-label">条件组</span>
          </template>
          <template v-else>
            <span class="ofb-group-link">连接</span>
            <a-select
              :value="group.connector"
              size="small"
              class="ofb-group-connector"
              @change="v => onGroupConnectorChange(gi, v)"
            >
              <a-select-option value="AND">且</a-select-option>
              <a-select-option value="OR">或</a-select-option>
            </a-select>
            <span class="ofb-group-hint">（组 = 括号）</span>
          </template>
          <a class="ofb-del-group" @click="removeGroup(gi)">删除组</a>
        </div>

        <div
          v-for="(f, fi) in group.filters"
          :key="fi"
          class="ofb-cond"
          :class="{ 'ofb-cond-first': fi === 0 }"
        >
          <!-- 连接符：组内首条不展示，其后 且/或/非 -->
          <a-select
            v-if="fi > 0"
            :value="f.connector || 'AND'"
            size="small"
            class="ofb-conn"
            @change="v => onConnectorChange(gi, fi, v)"
          >
            <a-select-option value="AND">且</a-select-option>
            <a-select-option value="OR">或</a-select-option>
            <a-select-option value="NOT">非</a-select-option>
          </a-select>
          <span v-else class="ofb-conn-first">且</span>

          <!-- 属性选择 -->
          <a-select
            :value="f.field"
            placeholder="选择属性"
            size="small"
            class="ofb-field"
            show-search
            option-filter-prop="label"
            @change="v => onFieldChange(gi, fi, v)"
          >
            <a-select-option v-for="opt in fieldOptions" :key="opt.value" :value="opt.value" :label="opt.label">
              {{ opt.label }}
            </a-select-option>
          </a-select>

          <!-- 运算符选择 -->
          <a-select
            :value="f.op || 'eq'"
            size="small"
            class="ofb-op"
            @change="v => onOpChange(gi, fi, v)"
          >
            <a-select-option v-for="op in operatorOptions" :key="op.value" :value="op.value">{{ op.label }}</a-select-option>
          </a-select>

          <!-- 值输入（按运算符区分） -->
          <template v-if="noValueOp(f.op)">
            <!-- 不为空 / 为空：无需输入 -->
          </template>
          <template v-else-if="isValuesOperator(f.op)">
            <a-input
              v-if="f.op === 'between'"
              :value="(f.values || [])[0]"
              placeholder="最小值"
              size="small"
              class="ofb-val"
              @change="e => onValuesChange(gi, fi, 0, e.target.value)"
            />
            <a-input
              v-if="f.op === 'between'"
              :value="(f.values || [])[1]"
              placeholder="最大值"
              size="small"
              class="ofb-val"
              @change="e => onValuesChange(gi, fi, 1, e.target.value)"
            />
            <a-select
              v-else-if="f.op === 'in'"
              :value="(f.values || [])"
              mode="tags"
              placeholder="多个值回车"
              size="small"
              class="ofb-val-in"
              :open="false"
              @change="v => onValuesChange(gi, fi, -1, v)"
            />
          </template>
          <template v-else>
            <a-input
              :value="f.value"
              placeholder="值"
              size="small"
              class="ofb-val"
              @change="e => onValueChange(gi, fi, e.target.value)"
            />
          </template>

          <a class="ofb-del" @click="removeFilter(gi, fi)">移除</a>
        </div>

        <div class="ofb-add-cond">
          <a-button size="small" type="dashed" @click="addFilter(gi)">+ 添加条件</a-button>
        </div>
      </div>

      <!-- 排序条件（已设置时展示；「+排序」在工具栏第一行） -->
      <div v-if="localOrderBy.length" class="ofb-section">
        <span class="ofb-lab">排序</span>
        <template v-for="(s, si) in localOrderBy" :key="si">
          <a-select
            :value="s.field"
            size="small"
            class="ofb-sort-field"
            show-search
            option-filter-prop="label"
            @change="v => onSortFieldChange(si, v)"
          >
            <a-select-option v-for="opt in fieldOptions" :key="opt.value" :value="opt.value" :label="opt.label">
              {{ opt.label }}
            </a-select-option>
          </a-select>
          <a-select
            :value="s.dir"
            size="small"
            class="ofb-sort-dir"
            @change="v => onSortDirChange(si, v)"
          >
            <a-select-option value="asc">升序</a-select-option>
            <a-select-option value="desc">降序</a-select-option>
          </a-select>
          <a class="ofb-del" @click="removeSort(si)">移除</a>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { FilterOutlined, DownOutlined, UpOutlined, SearchOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  // 可选字段列表：[{ label, value: 物理列名, type? }]
  fields: {
    type: Array,
    default: () => []
  },
  // 类型化查询 spec 对象（v-model）
  modelValue: {
    type: Object,
    default: null
  },
  // 是否在工具栏内展示「查询」按钮（对象浏览器页面开启；其它复用面板可按需关闭）
  showQuery: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['update:modelValue', 'query'])

const operatorOptions = [
  { value: 'eq', label: '=' },
  { value: 'ne', label: '≠' },
  { value: 'gt', label: '>' },
  { value: 'ge', label: '≥' },
  { value: 'lt', label: '<' },
  { value: 'le', label: '≤' },
  { value: 'like', label: '包含' },
  { value: 'in', label: '在集合' },
  { value: 'between', label: '区间' },
  { value: 'isnotnull', label: '不为空' },
  { value: 'isnull', label: '为空' }
]

const fieldOptions = computed(() =>
  (props.fields || []).map(f => ({ label: f.label || f.value, value: f.value }))
)

const localKeyword = ref('')
const localGroups = ref([emptyGroup()])
const localOrderBy = ref([])
const localColumns = ref([])
const collapsed = ref(false)

function emptyGroup() {
  return { connector: 'AND', filters: [] }
}

function emptyFilter() {
  return { connector: 'AND', field: undefined, op: 'eq', value: '', values: [] }
}

// 已生效条件数（用于头部计数角标）
const activeCount = computed(() => {
  let n = 0
  for (const g of localGroups.value) {
    for (const f of (g.filters || [])) {
      if (f && f.field) n += 1
    }
  }
  return n
})

function syncFromModel() {
  const s = props.modelValue || {}
  localKeyword.value = s.keyword || ''
  localGroups.value = (s.groups && s.groups.length ? s.groups : [emptyGroup()])
    .map(g => ({
      connector: g.connector === 'OR' ? 'OR' : 'AND',
      filters: (g.filters || []).map(f => ({
        connector: f.connector === 'OR' ? 'OR' : f.connector === 'NOT' ? 'NOT' : 'AND',
        field: f.field,
        op: f.op || 'eq',
        value: f.value,
        values: f.values || []
      }))
    }))
  localOrderBy.value = (s.orderBy || []).map(o => ({ field: o.field, dir: o.dir === 'desc' ? 'desc' : 'asc' }))
  const validCols = new Set(fieldOptions.value.map(f => f.value))
  localColumns.value = (s.columns || []).filter(c => validCols.has(c))
}

// 外部 model 变化时同步本地。本组件 emit 造成的回环（父组件原样写回 buildSpec()）视为等价，
// 跳过重同步，避免覆盖用户正在编辑的本地行（否则「添加条件/添加组」的新行会消失）。
watch(
  () => props.modelValue,
  (val) => {
    const incoming = JSON.stringify(val || {})
    const current = JSON.stringify(buildSpec())
    if (incoming === current) {
      return
    }
    syncFromModel()
  },
  { immediate: true }
)

// 组装 spec 并对外 emit
function noValueOp(op) {
  return op === 'isnull' || op === 'isnotnull'
}

function isValuesOperator(op) {
  return op === 'in' || op === 'between'
}

function buildSpec() {
  // 忠实反映本地编辑状态（含未填字段的空条件行、含空条件组），
  // 使 v-model 回环（本组件 emit -> 父组件写回 -> 本组件 watch 再同步）能原样还原，
  // 避免「添加条件/添加组」的新行被同步拥抱掉。空条件行向后端传递会被白名单忽略，无副作用。
  const groups = (localGroups.value || [])
    .map(g => ({
      connector: g.connector === 'OR' ? 'OR' : 'AND',
      filters: (g.filters || [])
        .map(f => {
          const normal = {
            connector: f.connector === 'NOT' ? 'NOT' : (f.connector === 'OR' ? 'OR' : 'AND'),
            field: f.field,
            op: f.op || 'eq'
          }
          if (isValuesOperator(f.op)) {
            normal.values = (f.values || []).filter(v => v !== '' && v !== null && v !== undefined)
          } else if (!noValueOp(f.op)) {
            normal.value = f.value
          }
          return normal
        })
    }))
  const orderBy = localOrderBy.value.filter(o => o.field)
  const keyword = localKeyword.value ? String(localKeyword.value).trim() : ''
  return {
    groups: groups.length ? groups : [emptyGroup()],
    orderBy,
    columns: localColumns.value,
    keyword
  }
}

function emitChange() {
  emit('update:modelValue', buildSpec())
}

// ===== 条件组（括号）操作 =====
function addGroup() {
  localGroups.value.push(emptyGroup())
  emitChange()
}

function removeGroup(gi) {
  // 至少保留一个（空）组，避免 UI 无组可操作
  if (localGroups.value.length <= 1) {
    localGroups.value = [emptyGroup()]
  } else {
    localGroups.value.splice(gi, 1)
  }
  emitChange()
}

function onGroupConnectorChange(gi, v) {
  localGroups.value[gi].connector = v
  emitChange()
}

// ===== 组内条件操作 =====
function addFilter(gi) {
  localGroups.value[gi].filters.push(emptyFilter())
  emitChange()
}

function removeFilter(gi, fi) {
  localGroups.value[gi].filters.splice(fi, 1)
  emitChange()
}

function onConnectorChange(gi, fi, v) {
  localGroups.value[gi].filters[fi].connector = v
  emitChange()
}

function onFieldChange(gi, fi, v) {
  localGroups.value[gi].filters[fi].field = v
  emitChange()
}

function onOpChange(gi, fi, v) {
  const f = localGroups.value[gi].filters[fi]
  f.op = v
  f.value = ''
  f.values = []
  emitChange()
}

function onValueChange(gi, fi, val) {
  localGroups.value[gi].filters[fi].value = val
  emitChange()
}

function onValuesChange(gi, fi, idx, val) {
  const f = localGroups.value[gi].filters[fi]
  if (idx === -1) {
    f.values = val || []
  } else {
    const arr = f.values || []
    arr[idx] = val
    f.values = arr
  }
  emitChange()
}

// ===== 排序 =====
function addSort() {
  localOrderBy.value.push({ field: undefined, dir: 'asc' })
  emitChange()
}

function removeSort(si) {
  localOrderBy.value.splice(si, 1)
  emitChange()
}

function onSortFieldChange(si, v) {
  localOrderBy.value[si].field = v
  emitChange()
}

function onSortDirChange(si, v) {
  localOrderBy.value[si].dir = v
  emitChange()
}

// ===== 投影 =====
function onColumnsChange(val) {
  localColumns.value = val || []
  emitChange()
}

// ===== 重置 =====
function reset() {
  localKeyword.value = ''
  localGroups.value = [emptyGroup()]
  localOrderBy.value = []
  localColumns.value = []
  emitChange()
}

defineExpose({ reset })
</script>

<style lang="scss" scoped>
.ont-filter-builder {
  width: 100%;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 4px;
  font-size: 13px;

  // 头部
  .ofb-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px 10px;

    .ofb-title {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-weight: 600;
      color: #333;

      .ofb-title-icon {
        color: #1677ff;
      }

      .ofb-count {
        min-width: 18px;
        height: 18px;
        line-height: 18px;
        padding: 0 6px;
        background: #1677ff;
        color: #fff;
        border-radius: 9px;
        font-size: 12px;
        text-align: center;
      }
    }

    .ofb-header-actions {
      display: inline-flex;
      align-items: center;
      gap: 4px;

      .ofb-reset {
        color: #ff4d4f;
        font-size: 12px;
      }

      .ofb-toggle {
        color: #666;
        font-size: 12px;
      }
    }
  }

  .ofb-body {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 4px 10px 10px;
  }

  // 工具栏（关键字 + 添加条件组 + 查询 同一行）
  .ofb-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .ofb-keyword {
      flex-shrink: 0;
      width: 180px;
    }

    .ofb-lab {
      color: #888;
      font-size: 13px;
      flex-shrink: 0;
    }

    .ofb-columns {
      width: 220px;
      flex-shrink: 0;
    }

    .ofb-add-group-btn {
      color: #1677ff;
      flex-shrink: 0;
    }

    .ofb-add-sort-btn {
      color: #1677ff;
      flex-shrink: 0;
    }

    .ofb-query-btn {
      flex-shrink: 0;
    }
  }

  // 条件组
  .ofb-group {
    border: 1px solid #edeef1;
    background: #fafbfc;
    border-radius: 6px;
    padding: 8px;

    .ofb-group-head {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 8px;

      .ofb-group-label {
        font-weight: 600;
        color: #444;
      }

      .ofb-group-link {
        color: #888;
      }

      .ofb-group-connector {
        width: 84px;
      }

      .ofb-group-hint {
        color: #bbb;
        font-size: 12px;
      }

      .ofb-del-group {
        margin-left: auto;
        color: #f5222d;
        font-size: 12px;
      }
    }

    .ofb-cond {
      display: flex;
      align-items: center;
      gap: 8px;
      // 条件行保持单行，空间不足时行内横向滚动，避免控件换行参差
      flex-wrap: nowrap;
      overflow-x: auto;
      white-space: nowrap;
      padding: 4px 6px;
      border-radius: 4px;
      margin-bottom: 4px;
      transition: background 0.15s;
      scrollbar-width: thin;
      scrollbar-color: #d9d9d9 transparent;

      &:hover {
        background: #f2f5fa;
      }

      &.ofb-cond-first {
        margin-top: 0;
      }

      .ofb-conn {
        width: 70px;
        flex-shrink: 0;
      }

      .ofb-conn-first {
        width: 70px;
        flex-shrink: 0;
        text-align: center;
        color: #b7bcc4;
      }

      .ofb-field {
        width: 150px;
        flex-shrink: 0;
      }

      .ofb-op {
        width: 100px;
        flex-shrink: 0;
      }

      .ofb-val {
        flex: 1 1 0;
        min-width: 120px;
        max-width: 240px;
      }

      .ofb-val-in {
        flex: 1 1 0;
        min-width: 180px;
        max-width: 280px;
      }

      .ofb-del {
        flex-shrink: 0;
        color: #f5222d;
        font-size: 12px;
        margin-left: 4px;
      }
    }

    .ofb-add-cond {
      margin-top: 6px;

      .ant-btn {
        color: #1677ff;
        border-color: #d9d9d9;
      }
    }
  }

  // 排序条件（字段 + 方向 + 移除）
  .ofb-section {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-top: 6px;

    .ofb-lab {
      color: #888;
      min-width: 44px;
      flex-shrink: 0;
    }

    .ofb-sort-field {
      width: 130px;
      flex-shrink: 0;
    }

    .ofb-sort-dir {
      width: 74px;
      flex-shrink: 0;
    }

    .ofb-del {
      color: #f5222d;
      font-size: 12px;
      margin-left: 4px;
      flex-shrink: 0;
    }
  }
}
</style>
