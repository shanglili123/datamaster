<template>
  <a-modal title="关联表与字段" v-model:open="visible" width="980px" wrap-class-name="ontology-workspace-modal ontology-modal--data" destroy-on-close :footer="null">
    <p class="modal-hint">关系「{{ relationName }}」的主体与客体已由关系定义确定。原表与目标表映射用于定位两端实体；关联表字段仅用于写入两端关联值。</p>

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
        <div class="rt-column-config">
          <label>
            <span>原表关联字段</span>
            <a-select
              v-model:value="rtSelectedByRow[row.id].sourceColumn"
              :options="rtColsByRow[row.id] || []"
              placeholder="选择原表关联字段"
              show-search
              option-filter-prop="label"
              @change="saveRtColumnConfig(row)"
            />
          </label>
          <label>
            <span>目标表关联字段</span>
            <a-select
              v-model:value="rtSelectedByRow[row.id].targetColumn"
              :options="rtColsByRow[row.id] || []"
              placeholder="选择目标表关联字段"
              show-search
              option-filter-prop="label"
              @change="saveRtColumnConfig(row)"
            />
          </label>
          <label class="rt-attribute-field">
            <span>关系属性</span>
            <a-select
              v-model:value="rtSelectedByRow[row.id].attributeColumns"
              mode="multiple"
              :options="rtAttributeOptions(row)"
              placeholder="可选，如职位、入职时间"
              :max-tag-count="2"
              allow-clear
              @change="saveRtColumnConfig(row)"
            />
          </label>
        </div>
        <a-button type="link" danger size="small" @click="deleteRt(row)">解除绑定</a-button>
      </div>
      <div v-if="!rtLoading && !rtRows.length" class="rc-empty">尚未绑定关联表，请在上方选择数据源与表进行绑定</div>
    </div>

    <a-divider style="margin: 12px 0">原表字段 ⟶ 目标表字段</a-divider>

    <!-- 二、关系两端实体表映射；无论是否存在关联表都必须配置 -->
    <div v-for="(row, idx) in rcRows" :key="idx" class="rc-row">
      <a-select
        v-model:value="row.sourceConceptTableId"
        :options="rcSrcTableOptions"
        placeholder="原表"
        style="width: 190px"
        @change="(v) => handleRcTableChange(row, 'source', v)"
      />
      <a-select
        v-model:value="row.sourceColumn"
        :options="columnOptionsFor(row.sourceConceptTableId)"
        placeholder="原表字段"
        show-search
        option-filter-prop="label"
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
      <a-select
        v-model:value="row.targetColumn"
        :options="columnOptionsFor(row.targetConceptTableId)"
        placeholder="目标表字段"
        show-search
        option-filter-prop="label"
        style="width: 160px"
      />
      <a-button type="link" danger size="small" @click="rcRows.splice(idx, 1)">移除</a-button>
    </div>
    <div v-if="!rcRows.length && !rcLoading" class="rc-empty">暂无原表到目标表的字段映射，点击下方按钮添加</div>
    <a-button type="dashed" block @click="addRcRow"><PlusOutlined /> 添加映射</a-button>
    <div class="rc-actions">
      <a-button type="primary" :loading="rcSaving" @click="saveRelColumns">保存映射</a-button>
    </div>
  </a-modal>
</template>

<script setup name="RelColumnModal">
import { listConceptTable, listConceptTablePhysicalColumns } from '@/api/ont/conceptTable'
import { getConcept } from '@/api/ont/concept'
import { listRelationColumn, batchSaveRelationColumns } from '@/api/ont/relationColumn'
import { listRelationTable, addRelationTable, updateRelationTable, delRelationTable, listRelationTablePhysicalColumns } from '@/api/ont/relationTable'
import { getDaDatasourceList } from '@/api/ast/dataSource/dataSource'
import { getCatalogTableListAsset } from '@/api/cat/catalog/table'
import { getMdColumnList } from '@/api/cat/catalog/column'
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
let rcMappingsPersisted = false
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
const sourceConceptMeta = ref({})
const targetConceptMeta = ref({})
// 关联表字段候选与端点配置，key 为关联表绑定记录 id
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

function uniqueOptions(options) {
  const seen = new Set()
  return (options || []).filter(option => {
    const key = String(option?.value ?? '')
    if (!key || seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function normalizeRtColumnConfig(raw) {
  let value = raw
  if (typeof raw === 'string') {
    try { value = JSON.parse(raw) } catch (e) { value = [] }
  }
  if (Array.isArray(value)) {
    return {
      sourceColumn: value[0],
      targetColumn: value[1],
      attributeColumns: value.slice(2).filter(Boolean)
    }
  }
  return {
    sourceColumn: value?.sourceColumn,
    targetColumn: value?.targetColumn,
    attributeColumns: Array.isArray(value?.attributeColumns)
      ? value.attributeColumns.filter(Boolean)
      : (Array.isArray(value?.columns) ? value.columns.filter(Boolean) : [])
  }
}

function rtAttributeOptions(row) {
  const cfg = rtSelectedByRow[row.id] || {}
  return (rtColsByRow[row.id] || []).filter(option =>
    option.value !== cfg.sourceColumn && option.value !== cfg.targetColumn)
}

watch(visible, async val => {
  if (!val || !props.relationId) return
  // 数据源下拉只加载一次
  if (!rtDsCache.length) {
    getDaDatasourceList().then(res => {
      rtDsCache = rowsOf(res)
      rtDsOptions.value = rtDsCache.map(d => ({ value: d.id, label: d.datasourceName }))
    })
  }
  await Promise.allSettled([loadEndpointConcepts(), loadRtRows()])
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
  ]).then(async ([sRes, tRes, mRes]) => {
    rcSrcTables.value = rowsOf(sRes)
    rcTgtTables.value = rowsOf(tRes)
    const savedMappings = rowsOf(mRes)
    rcMappingsPersisted = savedMappings.length > 0
    rcRows.value = savedMappings.map(m => ({
      sourceConceptTableId: m.sourceConceptTableId,
      sourceColumn: m.sourceColumn,
      targetConceptTableId: m.targetConceptTableId,
      targetColumn: m.targetColumn
    }))
    if (!rcSrcTables.value.length || !rcTgtTables.value.length) {
      proxy.$modal.msgWarning('源/目标概念尚未绑定数据表，请先在「概念管理」中完成绑表')
    }
    await autoPopulateRelationColumns()
    fetchRcColumnsForCurrentRows()
    rtRows.value.forEach(row => autoConfigureRtEndpoints(row))
  }).finally(() => {
    rcLoading.value = false
  })
})

async function loadEndpointConcepts() {
  const tasks = []
  if (props.sourceConceptId) {
    tasks.push(getConcept(props.sourceConceptId).then(res => { sourceConceptMeta.value = res.data || {} }))
  }
  if (props.targetConceptId) {
    tasks.push(getConcept(props.targetConceptId).then(res => { targetConceptMeta.value = res.data || {} }))
  }
  await Promise.allSettled(tasks)
}

// ===== 关联表绑定 =====
function loadRtRows() {
  rtForm.value = {}
  rtTableOptions.value = []
  Object.keys(rtColsByRow).forEach(k => delete rtColsByRow[k])
  Object.keys(rtSelectedByRow).forEach(k => delete rtSelectedByRow[k])
  rtLoading.value = true
  return listRelationTable(props.relationId).then(res => {
    const rows = rowsOf(res)
    // 先建立每行配置，再更新 rtRows，避免模板在异步渲染间隙访问 undefined。
    rows.forEach(row => {
      rtSelectedByRow[row.id] = normalizeRtColumnConfig(row.columnNames)
      ensureRtColumns(row)
    })
    rtRows.value = rows
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
    rtTableOptions.value = uniqueOptions(rowsOf(res).map(t => ({
      value: t.tableName,
      label: t.tableComment ? `${t.tableName} (${t.tableComment})` : t.tableName
    })).filter(t => !!t.value))
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
    loadRtRows().then(async () => {
      if (!rcMappingsPersisted) {
        rcRows.value.forEach(row => {
          row.sourceColumn = undefined
          row.targetColumn = undefined
        })
        await autoPopulateRelationColumns()
      }
    })
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
  const loadPhysicalFallback = () => listRelationTablePhysicalColumns(rt.id).then(res => {
    rtColsByRow[rt.id] = uniqueOptions(rowsOf(res).map(c => ({
      value: c.colName,
      label: c.colComment ? `${c.colName} (${c.colComment})` : c.colName,
      pkFlag: c.colKey ? '1' : '0'
    })).filter(option => !!option.value))
    return rtColsByRow[rt.id]
  })
  getCatalogRows(rt.datasourceId).then(rows => {
    const hit = rows.find(r => r.tableName === rt.tableName)
    if (!hit) return loadPhysicalFallback()
    return getMdColumnList({ tableId: hit.id }).then(res => {
      rtColsByRow[rt.id] = uniqueOptions(rowsOf(res).map(c => ({
        value: c.columnName,
        label: c.columnComment ? `${c.columnName} (${c.columnComment})` : c.columnName,
        pkFlag: c.pkFlag,
        fkFlag: c.fkFlag
      })).filter(o => !!o.value))
      return rtColsByRow[rt.id].length ? rtColsByRow[rt.id] : loadPhysicalFallback()
    })
  }).catch(() => loadPhysicalFallback()).then(() => {
    autoConfigureRtEndpoints(rt)
  }).catch(() => {})
}

function normalizedIdentifier(value) {
  return String(value || '').toLowerCase().replace(/[^a-z0-9]/g, '')
}

function inferEndpointColumn(options, concept, conceptTable, excluded) {
  const tableName = normalizedIdentifier(conceptTable?.tableName)
  const aliases = [...new Set([
    normalizedIdentifier(concept?.code),
    normalizedIdentifier(concept?.name),
    tableName,
    tableName.replace(/^(dm|t|tbl)/, '')
  ].filter(Boolean))]
  if (!aliases.length) return undefined
  let best
  let bestScore = 0
  options.forEach(option => {
    if (option.value === excluded) return
    const column = normalizedIdentifier(option.value)
    let score = 0
    aliases.forEach(alias => {
      if (column === `${alias}id`) score = Math.max(score, 100)
      else if (column === alias) score = Math.max(score, 80)
      else if (column.startsWith(alias) && column.endsWith('id')) score = Math.max(score, 70)
      else if (column.includes(alias)) score = Math.max(score, 50)
    })
    if (score > bestScore) {
      best = option.value
      bestScore = score
    }
  })
  return best
}

async function autoConfigureRtEndpoints(row) {
  const config = rtSelectedByRow[row.id]
  const options = rtColsByRow[row.id] || []
  if (!config || !options.length) return
  const mapping = rcRows.value[0]
  if (!mapping) return
  let changed = false
  if (!config.sourceColumn) {
    const sourceTable = rcSrcTables.value.find(table => String(table.id) === String(mapping.sourceConceptTableId))
    const sourceColumn = inferEndpointColumn(options, sourceConceptMeta.value, sourceTable, config.targetColumn)
    if (sourceColumn) {
      config.sourceColumn = sourceColumn
      changed = true
    }
  }
  if (!config.targetColumn) {
    const targetTable = rcTgtTables.value.find(table => String(table.id) === String(mapping.targetConceptTableId))
    const targetColumn = inferEndpointColumn(options, targetConceptMeta.value, targetTable, config.sourceColumn)
    if (targetColumn) {
      config.targetColumn = targetColumn
      changed = true
    }
  }
  if (changed && config.sourceColumn && config.targetColumn) saveRtColumnConfig(row, true)
}

function saveRtColumnConfig(row, silent = false) {
  const config = normalizeRtColumnConfig(rtSelectedByRow[row.id])
  config.attributeColumns = config.attributeColumns.filter(column =>
    column !== config.sourceColumn && column !== config.targetColumn)
  rtSelectedByRow[row.id] = config
  updateRelationTable({
    id: row.id,
    relationId: props.relationId,
    datasourceId: row.datasourceId,
    databaseName: row.databaseName,
    tableName: row.tableName,
    schemaName: row.schemaName,
    columnNames: JSON.stringify(config)
  }).then(() => {
    if (!silent) proxy.$modal.msgSuccess('字段已保存')
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
  if (!ct || !ct.id) return Promise.resolve([])
  const cached = rcColOptionsByTable[ct.id]
  // 空数组视为未加载成功，允许重试
  if (Array.isArray(cached) && cached.length) return Promise.resolve(cached)
  rcColOptionsByTable[ct.id] = []
  const loadPhysicalFallback = () => listConceptTablePhysicalColumns(ct.id).then(res => {
    rcColOptionsByTable[ct.id] = uniqueOptions(rowsOf(res).map(c => ({
      value: c.colName,
      label: c.colComment ? `${c.colName} (${c.colComment})` : c.colName,
      pkFlag: c.colKey ? '1' : '0'
    })).filter(option => !!option.value))
    return rcColOptionsByTable[ct.id]
  })
  return getCatalogRows(ct.datasourceId).then(rows => {
    const hit = rows.find(r => r.tableName === ct.tableName)
    // 只有元数据登记缺失时才直读绑定物理表。
    if (!hit) return loadPhysicalFallback()
    return getMdColumnList({ tableId: hit.id }).then(res => {
      rcColOptionsByTable[ct.id] = uniqueOptions(rowsOf(res).map(c => ({
        value: c.columnName,
        label: c.columnComment ? `${c.columnName} (${c.columnComment})` : c.columnName,
        pkFlag: c.pkFlag,
        fkFlag: c.fkFlag
      })).filter(o => !!o.value))
      return rcColOptionsByTable[ct.id].length ? rcColOptionsByTable[ct.id] : loadPhysicalFallback()
    })
  }).catch(() => loadPhysicalFallback()).catch(() => [])
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
  autoFillRelationRow(row)
}

async function autoFillRelationRow(row) {
  const sourceTable = rcSrcTables.value.find(table => String(table.id) === String(row.sourceConceptTableId))
  const targetTable = rcTgtTables.value.find(table => String(table.id) === String(row.targetConceptTableId))
  await Promise.all([ensureRcColumns(sourceTable), ensureRcColumns(targetTable)])
  const sourceOptions = columnOptionsFor(row.sourceConceptTableId)
  const targetOptions = columnOptionsFor(row.targetConceptTableId)
  const primaryColumn = options => options.find(option => String(option.pkFlag) === '1')?.value
    || options.find(option => String(option.value).toLowerCase() === 'id')?.value
  if (rtRows.value.length) {
    if (!row.sourceColumn) row.sourceColumn = primaryColumn(sourceOptions)
    if (!row.targetColumn) row.targetColumn = primaryColumn(targetOptions)
  } else {
    if (!row.targetColumn) row.targetColumn = primaryColumn(targetOptions)
    if (!row.sourceColumn) {
      row.sourceColumn = inferEndpointColumn(sourceOptions, targetConceptMeta.value, targetTable, undefined)
        || sourceOptions.find(option => String(option.fkFlag) === '1')?.value
    }
  }
}

async function autoPopulateRelationColumns() {
  if (!rcSrcTables.value.length || !rcTgtTables.value.length) return
  if (!rcRows.value.length) {
    rcRows.value.push({
      sourceConceptTableId: rcSrcTables.value[0].id,
      sourceColumn: undefined,
      targetConceptTableId: rcTgtTables.value[0].id,
      targetColumn: undefined
    })
  }
  await Promise.all(rcRows.value.map(row => autoFillRelationRow(row)))
}

async function addRcRow() {
  const firstSrc = rcSrcTables.value[0]
  const firstTgt = rcTgtTables.value[0]
  const row = {
    sourceConceptTableId: firstSrc ? firstSrc.id : undefined,
    sourceColumn: undefined,
    targetConceptTableId: firstTgt ? firstTgt.id : undefined,
    targetColumn: undefined
  }
  rcRows.value.push(row)
  if (firstSrc) ensureRcColumns(firstSrc)
  if (firstTgt) ensureRcColumns(firstTgt)
  await autoFillRelationRow(row)
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
  flex-wrap: wrap;
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
  flex-wrap: wrap;
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

.rt-column-config {
  display: flex;
  flex: 1 1 560px;
  align-items: flex-end;
  gap: 8px;

  label {
    display: flex;
    flex: 1 1 170px;
    flex-direction: column;
    gap: 4px;
    color: #666;
    font-size: 12px;
  }

  .rt-attribute-field {
    flex-basis: 220px;
  }

  :deep(.ant-select) {
    width: 100%;
  }

  .rc-arrow {
    padding-bottom: 6px;
    color: #999;
  }
}

.rc-row {
  display: flex;
  flex-wrap: wrap;
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

@media (max-width: 820px) {
  .bind-form :deep(.ant-select),
  .rc-row :deep(.ant-select),
  .rc-row :deep(.ant-select-auto-complete) {
    width: auto !important;
    min-width: 180px;
    flex: 1 1 180px;
  }

  .rt-item {
    align-items: flex-start;

    .rt-name,
    .rt-ds {
      min-width: 0;
      flex: 1 1 140px;
    }

    :deep(.ant-select) {
      min-width: 100% !important;
      flex-basis: 100%;
    }
  }

  .rt-column-config {
    flex-basis: 100%;

    label {
      min-width: 180px;
    }
  }

  .rc-row .rc-arrow {
    display: none;
  }
}
</style>
