<template>
  <a-modal title="属性绑定字段" v-model:open="visible" width="720px" destroy-on-close :footer="null">
    <p class="modal-hint">概念「{{ conceptName }}」的属性到物理字段映射（按绑定表维度整体保存）：</p>
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
  </a-modal>
</template>

<script setup name="FieldBindModal">
import { listProperty } from '@/api/ont/property'
import { listConceptTable } from '@/api/ont/conceptTable'
import { listPropertyColumn, batchSavePropertyColumns } from '@/api/ont/propertyColumn'
import { getCatalogTableListAsset } from '@/api/cat/unreleased/table'
import { getMdColumnList } from '@/api/cat/unreleased/column'
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

const fbLoading = ref(false)
const fbSaving = ref(false)
const fbTables = ref([])
const fbTableId = ref(undefined)
const fbRows = ref([])
const fbPropOptions = ref([])
const fbColumnOptions = ref([])
const fbNoRegistry = ref(false)
// 元数据登记表缓存：datasourceId -> 登记表行数组（组件级）
const catalogCache = new Map()

const fbTableOptions = computed(() => fbTables.value.map(t => ({ value: t.id, label: t.tableName })))

// 统一解析响应数据：list 接口可能返回数组或分页对象
function rowsOf(res) {
  return Array.isArray(res.data) ? res.data : (res.data?.rows || [])
}

watch(visible, val => {
  if (!val || !props.conceptId) return
  fbTableId.value = undefined
  fbRows.value = []
  fbColumnOptions.value = []
  fbNoRegistry.value = false
  // 并行加载：该概念已绑定的表 + 全部属性下拉选项
  fbLoading.value = true
  Promise.all([
    listConceptTable({ conceptId: props.conceptId }),
    listProperty({ conceptId: props.conceptId, pageNum: 1, pageSize: 1000 })
  ]).then(([tRes, pRes]) => {
    fbTables.value = rowsOf(tRes)
    fbPropOptions.value = rowsOf(pRes).map(p => ({ value: p.id, label: p.name }))
    if (!fbTables.value.length) {
      proxy.$modal.msgWarning('该概念尚未绑定数据表，请先在「概念管理」中完成绑表')
    } else {
      // 默认选中第一张绑定表（需同步写入 fbTableId，否则下拉不回显且映射区被 v-if 隐藏）
      fbTableId.value = fbTables.value[0].id
      handleFbTableChange(fbTables.value[0].id)
    }
  }).finally(() => {
    fbLoading.value = false
  })
})

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
  const ct = fbTables.value.find(t => t.id === tableId)
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
</style>
