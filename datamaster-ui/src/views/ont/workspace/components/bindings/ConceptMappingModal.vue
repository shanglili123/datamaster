<template>
  <a-modal title="映射" v-model:open="visible" width="820px" wrap-class-name="ontology-workspace-modal ontology-modal--data" destroy-on-close :footer="null">
    <p class="modal-hint">概念「{{ conceptName }}」的表绑定与属性到物理字段映射：</p>
    <a-tabs v-model:activeKey="activeTab">
      <!-- 页签一：表绑定（原「绑定数据表」） -->
      <a-tab-pane key="bind" tab="表绑定">
        <div class="bind-list">
          <div v-for="b in bindRows" :key="b.id" class="bind-item">
            <span class="bind-name">{{ b.tableName }}</span>
            <span class="bind-ds">{{ dsName(b.datasourceId) }}</span>
            <a-button type="link" danger size="small" @click="deleteBind(b)">解除绑定</a-button>
          </div>
          <div v-if="!bindLoading && !bindRows.length" class="bind-empty">暂无绑定，请在下方选择数据源与表进行绑定</div>
        </div>
        <a-divider style="margin: 12px 0">新增绑定</a-divider>
        <div class="bind-form">
          <a-select
            v-model:value="bindForm.datasourceId"
            :options="dsOptions"
            placeholder="请选择数据源"
            style="width: 250px"
            show-search
            option-filter-prop="label"
            @change="handleBindDsChange"
          />
          <a-select
            v-model:value="bindForm.tableName"
            :options="bindTableOptions"
            placeholder="请选择表"
            style="width: 230px"
            show-search
            option-filter-prop="label"
          />
          <a-button type="primary" :loading="bindSaving" @click="submitBind">绑定</a-button>
        </div>
      </a-tab-pane>

      <!-- 页签二：字段映射（原「属性绑定字段」） -->
      <a-tab-pane key="field" tab="字段映射">
        <div class="fb-table-picker">
          <span class="fb-label">绑定表：</span>
          <a-select
            v-model:value="fbTableId"
            :options="fbTableOptions"
            placeholder="请选择已绑定的表"
            style="width: 320px"
            @change="handleFbTableChange"
          />
        </div>
        <div v-if="fbNoRegistry" class="fb-registry-hint">该表未在元数据模块登记</div>
        <div v-if="!fbTableId && !fbLoading" class="fb-empty">暂无绑定表，请先在「表绑定」页签完成绑表</div>
        <template v-if="fbTableId">
          <div class="fb-rows">
            <div v-for="(row, idx) in fbRows" :key="idx" class="fb-row">
              <a-select
                v-model:value="row.propertyId"
                :options="fbPropOptions"
                placeholder="请选择属性"
                style="width: 200px"
                show-search
                option-filter-prop="label"
              />
              <span class="fb-arrow">→</span>
              <a-auto-complete
                v-model:value="row.columnName"
                :options="fbColumnOptions"
                placeholder="物理字段名"
                style="width: 220px"
              />
              <a-button type="link" danger size="small" @click="fbRows.splice(idx, 1)">移除</a-button>
            </div>
            <div v-if="!fbRows.length && !fbLoading" class="fb-empty">暂无映射，点击下方按钮添加</div>
          </div>
          <a-button type="dashed" block @click="addFbRow"><PlusOutlined /> 添加映射</a-button>
          <div class="fb-actions">
            <a-button type="primary" :loading="fbSaving" @click="saveFieldBind">保存映射</a-button>
          </div>
        </template>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup name="ConceptMappingModal">
import { listProperty } from '@/api/ont/property'
import { listConceptTable, addConceptTable, delConceptTable } from '@/api/ont/conceptTable'
import { listPropertyColumn, batchSavePropertyColumns } from '@/api/ont/propertyColumn'
import { getDaDatasourceList } from '@/api/ast/dataSource/dataSource'
import { getCatalogTableListAsset } from '@/api/cat/catalog/table'
import { getMdColumnList } from '@/api/cat/catalog/column'
import { PlusOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  conceptId: {
    type: String,
    default: ''
  },
  conceptName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const { proxy } = getCurrentInstance()
const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const activeTab = ref('bind')

// 统一解析响应数据：list 接口可能返回数组或分页对象
function rowsOf(res) {
  return Array.isArray(res.data) ? res.data : (res.data?.rows || [])
}

// ===== 共享：已绑定表列表（表绑定页签的列表 = 字段映射页签的表候选，一次加载两处使用）=====
const bindLoading = ref(false)
const bindRows = ref([])

function reloadBoundRows() {
  return listConceptTable({ conceptId: props.conceptId }).then(res => {
    const next = rowsOf(res)
    // 字段映射页签同步：当前选中表被解绑时清空其映射状态；无选中且存在绑定时自动选第一张
    if (!next.some(t => t.id === fbTableId.value)) {
      resetFbSelection()
    }
    if (!fbTableId.value && next.length) {
      selectFbTable(next[0].id)
    }
    bindRows.value = next
  })
}

// ===== 表绑定页签 =====
const bindSaving = ref(false)
let dsCache = []
const dsOptions = ref([])
const bindForm = ref({})
const bindTableOptions = ref([])

function dsName(datasourceId) {
  const hit = dsCache.find(d => d.id === datasourceId)
  return hit ? hit.datasourceName : ''
}

function handleBindDsChange(dsId) {
  bindForm.value = { datasourceId: dsId }
  bindTableOptions.value = []
  if (!dsId) return
  // 表候选改为元数据模块已发布的登记表
  getCatalogTableListAsset({ datasourceId: dsId, status: '1' }).then(res => {
    bindTableOptions.value = rowsOf(res).map(t => ({
      value: t.tableName,
      label: t.tableComment ? `${t.tableName} (${t.tableComment})` : t.tableName
    })).filter(t => !!t.value)
  }).catch(() => {
    proxy.$modal.msgError('获取表列表失败')
  })
}

function submitBind() {
  if (!bindForm.value.datasourceId || !bindForm.value.tableName) {
    proxy.$modal.msgError('请先选择数据源和数据表')
    return
  }
  bindSaving.value = true
  addConceptTable({
    conceptId: props.conceptId,
    datasourceId: bindForm.value.datasourceId,
    tableName: bindForm.value.tableName
  }).then(() => {
    proxy.$modal.msgSuccess('绑定成功')
    emit('success')
    bindForm.value = {}
    bindTableOptions.value = []
    return reloadBoundRows()
  }).finally(() => {
    bindSaving.value = false
  })
}

function deleteBind(row) {
  delConceptTable(row.id).then(() => {
    proxy.$modal.msgSuccess('已解除绑定')
    emit('success')
    return reloadBoundRows()
  })
}

// ===== 字段映射页签 =====
const fbLoading = ref(false)
const fbSaving = ref(false)
const fbTableId = ref(undefined)
const fbRows = ref([])
const fbPropOptions = ref([])
const fbColumnOptions = ref([])
const fbNoRegistry = ref(false)
// 元数据登记表缓存：datasourceId -> 登记表行数组（组件级）
const catalogCache = new Map()

const fbTableOptions = computed(() => bindRows.value.map(t => ({ value: t.id, label: t.tableName })))

watch(visible, val => {
  if (!val || !props.conceptId) return
  activeTab.value = 'bind'
  bindForm.value = {}
  bindTableOptions.value = []
  resetFbSelection()
  // 并行加载：已绑定表（两页签共用）+ 全部属性下拉选项 + 数据源下拉（仅首次）
  bindLoading.value = true
  fbLoading.value = true
  Promise.all([
    listConceptTable({ conceptId: props.conceptId }),
    listProperty({ conceptId: props.conceptId, pageNum: 1, pageSize: 1000 }),
    dsCache.length ? Promise.resolve(null) : getDaDatasourceList()
  ]).then(([tRes, pRes, dRes]) => {
    bindRows.value = rowsOf(tRes)
    fbPropOptions.value = rowsOf(pRes).map(p => ({ value: p.id, label: p.name }))
    if (dRes) {
      dsCache = rowsOf(dRes)
      dsOptions.value = dsCache.map(d => ({ value: d.id, label: d.datasourceName }))
    }
    // 字段映射页签默认选中第一张绑定表
    if (bindRows.value.length) {
      selectFbTable(bindRows.value[0].id)
    }
  }).finally(() => {
    bindLoading.value = false
    fbLoading.value = false
  })
})

function resetFbSelection() {
  fbTableId.value = undefined
  fbRows.value = []
  fbColumnOptions.value = []
  fbNoRegistry.value = false
}

function selectFbTable(tableId) {
  fbTableId.value = tableId
  handleFbTableChange(tableId)
}

// 按数据源拉取元数据登记表（Promise 级缓存，避免重复请求）
function getCatalogRows(datasourceId) {
  if (!catalogCache.has(datasourceId)) {
    catalogCache.set(
      datasourceId,
      getCatalogTableListAsset({ datasourceId, status: '1' })
        .then(res => rowsOf(res))
        .catch(e => {
          catalogCache.delete(datasourceId)
          throw e
        })
    )
  }
  return catalogCache.get(datasourceId)
}

function handleFbTableChange(tableId) {
  fbRows.value = []
  fbColumnOptions.value = []
  fbNoRegistry.value = false
  if (!tableId) return
  // 已有映射（batch 接口为整体替换语义，编辑时载入全量）
  listPropertyColumn(tableId).then(res => {
    fbRows.value = rowsOf(res).map(m => ({ propertyId: m.propertyId, columnName: m.columnName }))
  })
  loadFbColumns(tableId)
}

// 通过元数据模块解析物理字段候选：绑定表 -> 同数据源登记表中精确匹配表名 -> 表字段列表
function loadFbColumns(tableId) {
  const ct = bindRows.value.find(t => t.id === tableId)
  if (!ct) return
  getCatalogRows(ct.datasourceId).then(rows => {
    const hit = rows.find(r => r.tableName === ct.tableName)
    if (!hit) {
      fbNoRegistry.value = true
      fbColumnOptions.value = []
      return
    }
    return getMdColumnList({ tableId: hit.id }).then(res => {
      fbColumnOptions.value = rowsOf(res).map(c => ({
        value: c.columnName,
        label: c.columnComment ? `${c.columnName} (${c.columnComment})` : c.columnName
      })).filter(o => !!o.value)
    })
  }).catch(() => {
    fbColumnOptions.value = []
  })
}

function addFbRow() {
  fbRows.value.push({ propertyId: undefined, columnName: undefined })
}

function saveFieldBind() {
  if (fbRows.value.some(r => !r.propertyId || !r.columnName)) {
    proxy.$modal.msgError('每条映射都需要选择属性并填写物理字段')
    return
  }
  const dup = fbRows.value.find((r, i) => fbRows.value.some((o, j) => j !== i && o.propertyId === r.propertyId))
  if (dup) {
    proxy.$modal.msgError('同一个属性只能映射一个物理字段')
    return
  }
  fbSaving.value = true
  batchSavePropertyColumns(fbTableId.value, fbRows.value.map(r => ({
    propertyId: r.propertyId,
    conceptTableId: fbTableId.value,
    columnName: r.columnName
  }))).then(() => {
    proxy.$modal.msgSuccess('映射已保存')
    emit('success')
  }).finally(() => {
    fbSaving.value = false
  })
}
</script>

<style lang="scss" scoped>
.modal-hint {
  color: #999;
  font-size: 12px;
  margin: 0 0 8px;
}

.bind-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 6px;

  .bind-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px;

    & + .bind-item {
      border-top: 1px solid #f5f5f5;
    }

    .bind-name {
      font-weight: 500;
    }

    .bind-ds {
      color: #999;
      font-size: 12px;
      flex: 1;
    }
  }

  .bind-empty {
    padding: 16px;
    text-align: center;
    color: #bbb;
  }
}

.bind-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  :deep(.ant-select) {
    flex: 1 1 220px;
    min-width: 180px;
  }
}

.fb-table-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  .fb-label {
    color: #666;
    white-space: nowrap;
  }
}

.fb-registry-hint {
  color: #faad14;
  font-size: 12px;
  margin: -4px 0 8px;
}

.fb-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;

  .fb-arrow {
    color: #999;
  }
}

.fb-empty {
  padding: 12px;
  text-align: center;
  color: #bbb;
}

.fb-actions {
  margin-top: 12px;
  text-align: right;
}

@media (max-width: 720px) {
  .bind-form,
  .fb-row,
  .fb-table-picker {
    align-items: stretch;
  }

  .bind-form :deep(.ant-select),
  .fb-row :deep(.ant-select),
  .fb-row :deep(.ant-select-auto-complete),
  .fb-table-picker :deep(.ant-select) {
    width: 100% !important;
    flex: 1 1 100%;
  }

  .fb-row .fb-arrow {
    display: none;
  }
}
</style>
