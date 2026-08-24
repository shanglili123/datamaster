<template>
  <a-modal title="关联表与字段" v-model:open="visible" width="960px" destroy-on-close :footer="null">
    <p class="modal-hint">关系「{{ relationName }}」：先绑定关联表（来自元数据模块已发布目录表）并勾选字段；下方为源表⟶目标表的物理字段映射（整体保存）。</p>

    <!-- 一、关联表绑定 -->
    <div class="section-title">关联表</div>
    <div class="bind-form">
      <a-select
        v-model:value="rtForm.datasourceId"
        :options="rtDsOptions"
        placeholder="请选择数据源"
        style="width: 240px"
        show-search
        option-filter-prop="label"
        @change="handleRtDsChange"
      />
      <a-select
        v-model:value="rtForm.tableName"
        :options="rtTableOptions"
        placeholder="请选择表（仅已发布登记表）"
        style="width: 260px"
        show-search
        option-filter-prop="label"
      />
      <a-button type="primary" :loading="rtBinding" @click="submitRtBind">绑定关联表</a-button>
    </div>
    <div class="bind-list">
      <div v-for="row in rtRows" :key="row.id" class="rt-item">
        <span class="rt-name">{{ row.tableName }}</span>
        <span class="rt-ds">{{ rtDsName(row.datasourceId) }}</span>
        <a-select
          mode="multiple"
          :value="rtSelectedByRow[row.id] || []"
          :options="rtColsByRow[row.id] || []"
          :placeholder="rtColsByRow[row.id] && rtColsByRow[row.id].length ? '选择该表参与关系的字段' : '未在元数据模块找到该表字段'"
          style="flex: 1; min-width: 260px"
          :max-tag-count="3"
          allow-clear
          @change="(vals) => onRtColsChange(row, vals)"
        />
        <a-button type="link" danger size="small" @click="deleteRt(row)">解除绑定</a-button>
      </div>
      <div v-if="!rtLoading && !rtRows.length" class="rc-empty">尚未绑定关联表，请在上方选择数据源与表进行绑定</div>
    </div>

    <a-divider style="margin: 12px 0">源表 ⟶ 目标表 字段映射</a-divider>

    <!-- 二、源/目标概念表字段映射（原有能力保持不变） -->
    <div v-for="(row, idx) in rcRows" :key="idx" class="rc-row">
      <a-select
        v-model:value="row.sourceConceptTableId"
        :options="rcSrcTableOptions"
        placeholder="源表"
        style="width: 190px"
        @change="(v) => handleRcTableChange(row, 'source', v)"
      />
      <a-auto-complete
        v-model:value="row.sourceColumn"
        :options="columnOptionsFor(row.sourceConceptTableId)"
        placeholder="源字段"
        style="width: 160px"
      />
      <span class="rc-arrow">⟶</span>
      <a-select
        v-model:value="row.targetConceptTableId"
        :options="rcTgtTableOptions"
        placeholder="目标表"
        style="width: 190px"
        @change="(v) => handleRcTableChange(row, 'target', v)"
      />
      <a-auto-complete
        v-model:value="row.targetColumn"
        :options="columnOptionsFor(row.targetConceptTableId)"
        placeholder="目标字段"
        style="width: 160px"
      />
      <a-button type="link" danger size="small" @click="rcRows.splice(idx, 1)">移除</a-button>
    </div>
    <div v-if="!rcRows.length && !rcLoading" class="rc-empty">暂无关联字段映射，点击下方按钮添加</div>
    <a-button type="dashed" block @click="addRcRow"><PlusOutlined /> 添加映射</a-button>
    <div class="rc-actions">
      <a-button type="primary" :loading="rcSaving" @click="saveRelColumns">保存映射</a-button>
    </div>
  </a-modal>
</template>

<script setup name="RelColumnModal">
import { listConceptTable } from '@/api/ont/conceptTable'
import { listRelationColumn, batchSaveRelationColumns } from '@/api/ont/relationColumn'
import { listRelationTable, addRelationTable, updateRelationTable, delRelationTable } from '@/api/ont/relationTable'
import { getDaDatasourceList } from '@/api/ast/dataSource/dataSource'
import { getCatalogTableListAsset } from '@/api/cat/unreleased/table'
import { getMdColumnList } from '@/api/cat/unreleased/column'
import { PlusOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  relationId: {
    type: String,
    default: ''
  },
  sourceConceptId: {
    type: String,
    default: ''
  },
  targetConceptId: {
    type: String,
    default: ''
  },
  relationName: {
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

const rcLoading = ref(false)
const rcSaving = ref(false)
const rcRows = ref([])
const rcSrcTables = ref([])
const rcTgtTables = ref([])
// 每张绑定表的字段候选缓存，key 为绑定记录 id；空数组表示未加载成功，允许重试
const rcColOptionsByTable = reactive({})
// 元数据登记表缓存：datasourceId -> 登记表行数组（组件级）
const catalogCache = new Map()

// ===== 关联表绑定状态 =====
const rtLoading = ref(false)
const rtBinding = ref(false)
const rtRows = ref([])
let rtDsCache = []
const rtDsOptions = ref([])
const rtForm = ref({})
const rtTableOptions = ref([])
// 关联表字段候选与选中值，key 为关联表绑定记录 id
const rtColsByRow = reactive({})
const rtSelectedByRow = reactive({})

const rcSrcTableOptions = computed(() => rcSrcTables.value.map(t => ({ value: t.id, label: t.tableName })))
const rcTgtTableOptions = computed(() => rcTgtTables.value.map(t => ({ value: t.id, label: t.tableName })))

function columnOptionsFor(tableId) {
  return rcColOptionsByTable[tableId] || []
}

function rtDsName(datasourceId) {
  const hit = rtDsCache.find(d => d.id === datasourceId)
  return hit ? hit.datasourceName : ''
}

// 统一解析响应数据：list 接口可能返回数组或分页对象
function rowsOf(res) {
  return Array.isArray(res.data) ? res.data : (res.data?.rows || [])
}

watch(visible, val => {
  if (!val || !props.relationId) return
  // 数据源下拉只加载一次
  if (!rtDsCache.length) {
    getDaDatasourceList().then(res => {
      rtDsCache = rowsOf(res)
      rtDsOptions.value = rtDsCache.map(d => ({ value: d.id, label: d.datasourceName }))
    })
  }
  loadRtRows()
  if (!props.sourceConceptId || !props.targetConceptId) return
  rcRows.value = []
  rcSrcTables.value = []
  rcTgtTables.value = []
  Object.keys(rcColOptionsByTable).forEach(k => delete rcColOptionsByTable[k])
  // 并行加载：源/目标概念绑定的表 + 已有映射（batch 接口整体替换语义，需载入全量）
  rcLoading.value = true
  Promise.all([
    listConceptTable({ conceptId: props.sourceConceptId }),
    listConceptTable({ conceptId: props.targetConceptId }),
    listRelationColumn(props.relationId)
  ]).then(([sRes, tRes, mRes]) => {
    rcSrcTables.value = rowsOf(sRes)
    rcTgtTables.value = rowsOf(tRes)
    rcRows.value = rowsOf(mRes).map(m => ({
      sourceConceptTableId: m.sourceConceptTableId,
      sourceColumn: m.sourceColumn,
      targetConceptTableId: m.targetConceptTableId,
      targetColumn: m.targetColumn
    }))
    if (!rcSrcTables.value.length || !rcTgtTables.value.length) {
      proxy.$modal.msgWarning('源/目标概念尚未绑定数据表，请先在「概念管理」中完成绑表')
    }
    fetchRcColumnsForCurrentRows()
  }).finally(() => {
    rcLoading.value = false
  })
})

// ===== 关联表绑定 =====
function loadRtRows() {
  rtForm.value = {}
  rtTableOptions.value = []
  Object.keys(rtColsByRow).forEach(k => delete rtColsByRow[k])
  Object.keys(rtSelectedByRow).forEach(k => delete rtSelectedByRow[k])
  rtLoading.value = true
  listRelationTable(props.relationId).then(res => {
    rtRows.value = rowsOf(res)
    rtRows.value.forEach(row => {
      try {
        rtSelectedByRow[row.id] = row.columnNames ? JSON.parse(row.columnNames) : []
      } catch (e) {
        rtSelectedByRow[row.id] = []
      }
      ensureRtColumns(row)
    })
  }).finally(() => {
    rtLoading.value = false
  })
}

function handleRtDsChange(dsId) {
  rtForm.value = { datasourceId: dsId }
  rtTableOptions.value = []
  if (!dsId) return
  // 表候选来自元数据模块已发布的登记表
  getCatalogTableListAsset({ datasourceId: dsId, status: '1' }).then(res => {
    rtTableOptions.value = rowsOf(res).map(t => ({
      value: t.tableName,
      label: t.tableComment ? `${t.tableName} (${t.tableComment})` : t.tableName
    })).filter(t => !!t.value)
  }).catch(() => {
    proxy.$modal.msgError('获取表列表失败')
  })
}

function submitRtBind() {
  if (!rtForm.value.datasourceId || !rtForm.value.tableName) {
    proxy.$modal.msgError('请先选择数据源和数据表')
    return
  }
  rtBinding.value = true
  addRelationTable({
    relationId: props.relationId,
    datasourceId: rtForm.value.datasourceId,
    tableName: rtForm.value.tableName
  }).then(() => {
    proxy.$modal.msgSuccess('关联表绑定成功')
    emit('success')
    loadRtRows()
  }).finally(() => {
    rtBinding.value = false
  })
}

// 通过元数据模块解析关联表的物理字段候选
function ensureRtColumns(rt) {
  if (!rt || !rt.id) return
  const cached = rtColsByRow[rt.id]
  if (Array.isArray(cached) && cached.length) return
  rtColsByRow[rt.id] = []
  getCatalogRows(rt.datasourceId).then(rows => {
    const hit = rows.find(r => r.tableName === rt.tableName)
    if (!hit) return
    return getMdColumnList({ tableId: hit.id }).then(res => {
      rtColsByRow[rt.id] = rowsOf(res).map(c => ({
        value: c.columnName,
        label: c.columnComment ? `${c.columnName} (${c.columnComment})` : c.columnName
      })).filter(o => !!o.value)
    })
  }).catch(() => {})
}

function onRtColsChange(row, vals) {
  rtSelectedByRow[row.id] = vals || []
  updateRelationTable({
    id: row.id,
    relationId: props.relationId,
    datasourceId: row.datasourceId,
    databaseName: row.databaseName,
    tableName: row.tableName,
    schemaName: row.schemaName,
    columnNames: JSON.stringify(vals || [])
  }).then(() => {
    proxy.$modal.msgSuccess('字段已保存')
    emit('success')
  })
}

function deleteRt(row) {
  delRelationTable(row.id).then(() => {
    proxy.$modal.msgSuccess('已解除绑定')
    emit('success')
    loadRtRows()
  })
}

// ===== 原有源/目标映射逻辑 =====
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

// 通过元数据模块解析物理字段候选：绑定表 -> 同数据源登记表中精确匹配表名 -> 表字段列表
function ensureRcColumns(ct) {
  if (!ct || !ct.id) return
  const cached = rcColOptionsByTable[ct.id]
  // 空数组视为未加载成功，允许重试
  if (Array.isArray(cached) && cached.length) return
  rcColOptionsByTable[ct.id] = []
  getCatalogRows(ct.datasourceId).then(rows => {
    const hit = rows.find(r => r.tableName === ct.tableName)
    // 未在元数据模块登记：保持空数组，仍可手动输入并允许重试
    if (!hit) return
    return getMdColumnList({ tableId: hit.id }).then(res => {
      rcColOptionsByTable[ct.id] = rowsOf(res).map(c => ({
        value: c.columnName,
        label: c.columnComment ? `${c.columnName} (${c.columnComment})` : c.columnName
      })).filter(o => !!o.value)
    })
  }).catch(() => {})
}

// 为当前映射行涉及的表拉取字段候选
function fetchRcColumnsForCurrentRows() {
  const usedIds = new Set(rcRows.value.flatMap(r => [r.sourceConceptTableId, r.targetConceptTableId]).filter(Boolean))
  ;[...rcSrcTables.value, ...rcTgtTables.value]
    .filter(t => usedIds.has(t.id))
    .forEach(t => ensureRcColumns(t))
}

// 行内切换表时清空字段并拉取候选
function handleRcTableChange(row, side, tableId) {
  if (side === 'source') row.sourceColumn = undefined
  else row.targetColumn = undefined
  if (!tableId) return
  const ct = [...rcSrcTables.value, ...rcTgtTables.value].find(t => t.id === tableId)
  ensureRcColumns(ct)
}

function addRcRow() {
  const firstSrc = rcSrcTables.value[0]
  const firstTgt = rcTgtTables.value[0]
  rcRows.value.push({
    sourceConceptTableId: firstSrc ? firstSrc.id : undefined,
    sourceColumn: undefined,
    targetConceptTableId: firstTgt ? firstTgt.id : undefined,
    targetColumn: undefined
  })
  if (firstSrc) ensureRcColumns(firstSrc)
  if (firstTgt) ensureRcColumns(firstTgt)
}

function saveRelColumns() {
  if (!rcRows.value.length) {
    proxy.$modal.msgError('请至少添加一条映射')
    return
  }
  if (rcRows.value.some(r => !r.sourceConceptTableId || !r.sourceColumn || !r.targetConceptTableId || !r.targetColumn)) {
    proxy.$modal.msgError('每条映射都需要完整选择源表、源字段、目标表和目标字段')
    return
  }
  rcSaving.value = true
  batchSaveRelationColumns(props.relationId, rcRows.value.map(r => ({
    relationId: props.relationId,
    sourceConceptTableId: r.sourceConceptTableId,
    sourceColumn: r.sourceColumn,
    targetConceptTableId: r.targetConceptTableId,
    targetColumn: r.targetColumn
  }))).then(() => {
    proxy.$modal.msgSuccess('关联字段映射已保存')
    emit('success')
  }).finally(() => {
    rcSaving.value = false
  })
}
</script>

<style lang="scss" scoped>
.modal-hint {
  color: #999;
  font-size: 12px;
  margin: 0 0 8px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.bind-form {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.bind-list {
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 6px;

  &:empty {
    display: none;
  }
}

.rt-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;

  & + .rt-item {
    border-top: 1px solid #f5f5f5;
  }

  .rt-name {
    font-weight: 500;
    min-width: 120px;
  }

  .rt-ds {
    color: #999;
    font-size: 12px;
    min-width: 90px;
  }
}

.rc-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;

  .rc-arrow {
    color: #999;
  }
}

.rc-empty {
  padding: 12px;
  text-align: center;
  color: #bbb;
}

.rc-actions {
  margin-top: 12px;
  text-align: right;
}
</style>
