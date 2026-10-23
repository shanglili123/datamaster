<template>
  <section class="join-config">
    <div class="section-heading">
      <h3 class="etl-section-title">多表关联</h3>
      <a-button size="small" :disabled="info" @click="initialize">按当前上游初始化</a-button>
    </div>
    <a-alert type="info" show-icon message="按来源顺序逐表关联；初始化会替换当前配置。上游变化不会自动删除或改写已有配置。" />
    <a-alert v-if="validationError" class="notice" type="warning" show-icon :message="validationError" />

    <h4 class="etl-subsection-title">来源与连接顺序</h4>
    <div class="main-source">
      <span>主表</span>
      <a-select :value="sources[0]?.nodeId" :options="sourceOptions" :disabled="info || !sources.length"
        placeholder="请先初始化来源" @change="setMainSource" />
    </div>
    <div v-for="(source, index) in sources" :key="`source-${index}`" class="source-row">
      <span class="order">{{ index === 0 ? '主表' : index + 1 }}</span>
      <span class="source-label">{{ sourceLabel(source?.nodeId) }}</span>
      <a-input :value="source?.alias" :disabled="info" placeholder="表别名" aria-label="表别名"
        @update:value="updateSource(index, 'alias', $event)" />
      <a-button size="small" :disabled="info || index === 0" @click="moveSource(index, -1)">上移</a-button>
      <a-button size="small" :disabled="info || index === sources.length - 1" @click="moveSource(index, 1)">下移</a-button>
    </div>
    <div v-if="!sources.length" class="empty">尚未配置来源，请按当前上游初始化。</div>

    <h4 class="etl-subsection-title">关联条件</h4>
    <div v-for="(join, joinIndex) in joins" :key="`join-${joinIndex}`" class="join-block">
      <div class="join-heading">
        <strong>{{ joinIndex + 1 }}. {{ sourceLabel(join?.rightNodeId) }}</strong>
        <a-select :value="join?.type" :options="joinTypeOptions" :disabled="info" aria-label="连接类型"
          @change="updateJoin(joinIndex, 'type', $event)" />
        <a-select :value="join?.relation" :options="relationOptions" :disabled="info" aria-label="条件关系"
          @change="updateJoin(joinIndex, 'relation', $event)" />
        <a-button size="small" :disabled="info" @click="addCondition(joinIndex)">添加条件</a-button>
      </div>
      <div v-for="(condition, conditionIndex) in conditionRows(join)" :key="conditionIndex" class="condition-row">
        <a-select :value="condition?.leftNodeId" :options="leftSourceOptions(join?.rightNodeId, condition?.leftNodeId)"
          :disabled="info" placeholder="已加入的左表" @change="updateCondition(joinIndex, conditionIndex, 'leftNodeId', $event)" />
        <a-select :value="condition?.leftColumn" :options="columnOptions(condition?.leftNodeId, condition?.leftColumn)"
          :disabled="info" show-search option-filter-prop="label" placeholder="左字段"
          @change="updateCondition(joinIndex, conditionIndex, 'leftColumn', $event)" />
        <a-select :value="condition?.operator" :options="operatorOptions" :disabled="info" placeholder="比较符"
          @change="updateCondition(joinIndex, conditionIndex, 'operator', $event)" />
        <span class="right-source">{{ sourceAlias(join?.rightNodeId) }}</span>
        <a-select :value="condition?.rightColumn" :options="columnOptions(join?.rightNodeId, condition?.rightColumn)"
          :disabled="info" show-search option-filter-prop="label" placeholder="右字段"
          @change="updateCondition(joinIndex, conditionIndex, 'rightColumn', $event)" />
        <a-button size="small" danger :disabled="info" @click="removeCondition(joinIndex, conditionIndex)">删除</a-button>
      </div>
      <div v-if="!conditionRows(join).length" class="empty">至少添加一个字段比较条件。</div>
    </div>

    <div class="section-heading">
      <h4 class="etl-subsection-title">输出字段</h4>
      <a-button size="small" :disabled="info" @click="addOutputField">添加输出字段</a-button>
    </div>
    <div class="field-picker">
      <span class="muted">勾选业务字段（取消勾选将移除对应投影）</span>
      <div v-for="(source, sourceIndex) in sources" :key="`picker-${sourceIndex}`" class="picker-row">
        <strong>{{ sourceAlias(source?.nodeId) }}</strong>
        <a-checkbox v-for="field in businessFields(source?.nodeId)" :key="field.columnName"
          :checked="isProjected(source?.nodeId, field.columnName)" :disabled="info"
          @change="toggleProjection(source?.nodeId, field.columnName, $event.target.checked)">
          {{ field.columnName }}
        </a-checkbox>
      </div>
    </div>
    <div class="output-table">
      <div class="output-row output-header"><span>来源</span><span>业务字段</span><span>输出名称（唯一）</span><span>操作</span></div>
      <div v-for="(field, index) in outputFields" :key="`output-${index}`" class="output-row">
        <a-select :value="field?.nodeId" :options="projectionSourceOptions(field?.nodeId)" :disabled="info" placeholder="来源"
          @change="updateField(index, 'nodeId', $event)" />
        <a-select :value="field?.column" :options="columnOptions(field?.nodeId, field?.column)" :disabled="info"
          show-search option-filter-prop="label" placeholder="业务字段" @change="updateField(index, 'column', $event)" />
        <a-input :value="field?.outputName" :disabled="info" placeholder="输出名称" aria-label="输出名称"
          @update:value="updateField(index, 'outputName', $event)" />
        <a-button size="small" danger :disabled="info" @click="removeOutputField(index)">删除</a-button>
      </div>
      <div v-if="!outputFields.length" class="empty">请勾选或添加输出字段；fields 为空不会隐式输出全部字段。</div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { Alert as AAlert, Button as AButton, Checkbox as ACheckbox, Input as AInput, Select as ASelect } from 'ant-design-vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({ sources: [], joins: [], fields: [] }) },
  upstreams: { type: Array, default: () => [] },
  info: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const asArray = value => Array.isArray(value) ? value : []
const sources = computed(() => asArray(props.modelValue?.sources))
const joins = computed(() => asArray(props.modelValue?.joins))
const outputFields = computed(() => asArray(props.modelValue?.fields))
const joinTypes = ['INNER', 'LEFT', 'RIGHT', 'FULL']
const operators = ['=', '!=', '>', '>=', '<', '<=']
const joinTypeOptions = joinTypes.map(value => ({ label: value, value }))
const relationOptions = ['AND', 'OR'].map(value => ({ label: value, value }))
const operatorOptions = operators.map(value => ({ label: value, value }))
const identifier = /^[A-Za-z_][A-Za-z0-9_]*$/
const validId = id => (typeof id === 'string' && id.trim() !== '') || (typeof id === 'number' && Number.isFinite(id))
const sameId = (a, b) => a === b
const upstream = id => props.upstreams.find(item => item && sameId(item.id, id))
const sourceAlias = id => sources.value.find(item => item && sameId(item.nodeId, id))?.alias || sourceLabel(id)
function sourceLabel(id) {
  const item = upstream(id)
  return item ? String(item.name || item.table || item.id) : `失效上游：${id ?? '未选择'}`
}
function businessFields(id) {
  const seen = new Set()
  return asArray(upstream(id)?.fields).filter(field => {
    if (!field || typeof field.columnName !== 'string' || !field.columnName.trim() || seen.has(field.columnName)) return false
    seen.add(field.columnName)
    return true
  })
}
function columnOptions(id, current) {
  const options = businessFields(id).map(field => ({
    value: field.columnName,
    label: `${field.columnName}${field.columnType ? ` (${field.columnType})` : ''}`
  }))
  if (current && !options.some(item => item.value === current)) options.push({ value: current, label: `失效字段：${current}`, disabled: true })
  return options
}
const sourceOptions = computed(() => sources.value.filter(Boolean).map(item => ({ value: item.nodeId, label: `${sourceLabel(item.nodeId)} (${item.alias || '未设别名'})` })))
function projectionSourceOptions(current) {
  const options = [...sourceOptions.value]
  if (validId(current) && !options.some(item => sameId(item.value, current))) options.push({ value: current, label: sourceLabel(current), disabled: true })
  return options
}
function leftSourceOptions(rightId, current) {
  const rightIndex = sources.value.findIndex(item => item && sameId(item.nodeId, rightId))
  const options = sources.value.slice(0, Math.max(0, rightIndex)).filter(Boolean)
    .map(item => ({ value: item.nodeId, label: `${sourceLabel(item.nodeId)} (${item.alias || '未设别名'})` }))
  if (validId(current) && !options.some(item => sameId(item.value, current))) options.push({ value: current, label: `无效左表：${sourceLabel(current)}`, disabled: true })
  return options
}
function change(edit) {
  if (props.info) return
  // Clone before editing: never mutate the parent's model or watch/rewrite upstream changes.
  const config = JSON.parse(JSON.stringify(props.modelValue || {}))
  config.sources = asArray(config.sources)
  config.joins = asArray(config.joins)
  config.fields = asArray(config.fields)
  edit(config)
  emit('update:modelValue', config)
}
function defaultJoin(rightNodeId) {
  return { rightNodeId, type: 'INNER', relation: 'AND', conditions: [{ leftNodeId: '', leftColumn: '', operator: '=', rightColumn: '' }] }
}
function uniqueOutputName(nodeId, column, fields, forceAlias = false, sourceList = sources.value) {
  const alias = sourceList.find(item => sameId(item.nodeId, nodeId))?.alias || 'field'
  const used = new Set(fields.map(item => String(item?.outputName || '').toLowerCase()))
  const base = forceAlias || used.has(column.toLowerCase()) ? `${alias}_${column}` : column
  let name = base
  let suffix = 2
  while (used.has(name.toLowerCase())) name = `${base}_${suffix++}`
  return name
}
function initialize() {
  if (props.info) return
  joinDrafts.clear()
  const nextSources = props.upstreams.map((item, index) => ({ nodeId: item?.id, alias: `t${index + 1}` }))
  const counts = new Map()
  nextSources.forEach(source => businessFields(source.nodeId).forEach(field => {
    const key = field.columnName.toLowerCase()
    counts.set(key, (counts.get(key) || 0) + 1)
  }))
  const fields = []
  nextSources.forEach(source => businessFields(source.nodeId).forEach(field => fields.push({
    nodeId: source.nodeId,
    column: field.columnName,
    outputName: uniqueOutputName(source.nodeId, field.columnName, fields, counts.get(field.columnName.toLowerCase()) > 1, nextSources)
  })))
  emit('update:modelValue', { sources: nextSources, joins: nextSources.slice(1).map(source => defaultJoin(source.nodeId)), fields })
}
// Keep a table's join draft while it is temporarily selected as the main table.
// The persisted contract contains only joins for non-main sources.
const joinDrafts = new Map()
function reorderJoins(config) {
  config.joins.forEach(join => {
    if (join && validId(join.rightNodeId)) joinDrafts.set(join.rightNodeId, join)
  })
  const remaining = [...config.joins]
  config.joins = config.sources.slice(1).map(source => {
    const index = remaining.findIndex(join => join && sameId(join.rightNodeId, source.nodeId))
    if (index >= 0) return remaining.splice(index, 1)[0]
    return joinDrafts.has(source.nodeId)
      ? JSON.parse(JSON.stringify(joinDrafts.get(source.nodeId)))
      : defaultJoin(source.nodeId)
  })
  // Preserve unrelated/orphaned joins visibly instead of silently discarding them.
  const mainId = config.sources[0]?.nodeId
  config.joins.push(...remaining.filter(join => !join || !sameId(join.rightNodeId, mainId)))
}
function setMainSource(id) {
  change(config => {
    const index = config.sources.findIndex(source => source && sameId(source.nodeId, id))
    if (index <= 0) return
    config.sources.unshift(config.sources.splice(index, 1)[0])
    reorderJoins(config)
  })
}
function moveSource(index, delta) {
  change(config => {
    const target = index + delta
    if (target < 0 || target >= config.sources.length) return
    const source = config.sources.splice(index, 1)[0]
    config.sources.splice(target, 0, source)
    reorderJoins(config)
  })
}
function updateSource(index, key, value) { change(config => { config.sources[index] = { ...config.sources[index], [key]: value } }) }
function updateJoin(index, key, value) { change(config => { config.joins[index] = { ...config.joins[index], [key]: value } }) }
const conditionRows = join => asArray(join?.conditions)
function addCondition(index) {
  change(config => {
    const join = config.joins[index] || {}
    join.conditions = [...asArray(join.conditions), { leftNodeId: '', leftColumn: '', operator: '=', rightColumn: '' }]
    config.joins[index] = join
  })
}
function updateCondition(joinIndex, index, key, value) {
  change(config => {
    const join = config.joins[joinIndex]
    join.conditions[index] = { ...join.conditions[index], [key]: value }
  })
}
function removeCondition(joinIndex, index) { change(config => { config.joins[joinIndex].conditions.splice(index, 1) }) }
function isProjected(nodeId, column) { return outputFields.value.some(field => field && sameId(field.nodeId, nodeId) && field.column === column) }
function toggleProjection(nodeId, column, checked) {
  change(config => {
    if (checked) {
      if (!config.fields.some(field => field && sameId(field.nodeId, nodeId) && field.column === column)) {
        config.fields.push({ nodeId, column, outputName: uniqueOutputName(nodeId, column, config.fields) })
      }
    } else config.fields = config.fields.filter(field => !field || !sameId(field.nodeId, nodeId) || field.column !== column)
  })
}
function addOutputField() { change(config => { config.fields.push({ nodeId: '', column: '', outputName: '' }) }) }
function updateField(index, key, value) { change(config => { config.fields[index] = { ...config.fields[index], [key]: value } }) }
function removeOutputField(index) { change(config => { config.fields.splice(index, 1) }) }

function validate() {
  const config = props.modelValue
  if (!config || !Array.isArray(config.sources) || !Array.isArray(config.joins) || !Array.isArray(config.fields)) return '配置必须包含 sources、joins、fields 数组'
  const upstreamIds = new Set()
  for (const item of props.upstreams) {
    if (!item || !validId(item.id) || upstreamIds.has(item.id)) return '当前上游节点 ID 为空或重复'
    upstreamIds.add(item.id)
  }
  if (config.sources.length < 2) return '多表关联至少需要两个来源'
  const ids = new Set()
  const aliases = new Set()
  for (let i = 0; i < config.sources.length; i++) {
    const source = config.sources[i]
    if (!source || !validId(source.nodeId) || ids.has(source.nodeId)) return `来源 ${i + 1} 的节点 ID 为空或重复`
    if (!upstreamIds.has(source.nodeId)) return `来源 ${i + 1} 引用了已移除上游：${source.nodeId}`
    ids.add(source.nodeId)
    if (typeof source.alias !== 'string' || !identifier.test(source.alias)) return `来源 ${i + 1} 的别名须以字母/下划线开头，且仅含字母、数字、下划线`
    const key = source.alias.toLowerCase()
    if (aliases.has(key)) return `来源别名重复：${source.alias}`
    aliases.add(key)
  }
  if (ids.size !== upstreamIds.size || [...upstreamIds].some(id => !ids.has(id))) return '来源与当前上游不匹配，请显式初始化或修正配置'
  if (config.joins.length !== config.sources.length - 1) return '关联数量应为来源数量减一，存在遗漏或失效关联'
  const hasColumn = (id, column) => typeof column === 'string' && businessFields(id).some(field => field.columnName === column)
  for (let i = 0; i < config.joins.length; i++) {
    const join = config.joins[i]
    const prefix = `关联 ${i + 1}`
    if (!join || !sameId(join.rightNodeId, config.sources[i + 1].nodeId)) return `${prefix} 的右表与来源顺序不一致`
    if (!joinTypes.includes(join.type)) return `${prefix} 的连接类型无效`
    if (!['AND', 'OR'].includes(join.relation)) return `${prefix} 的条件关系须为 AND 或 OR`
    if (!Array.isArray(join.conditions) || !join.conditions.length) return `${prefix} 至少需要一个比较条件`
    const leftIds = new Set(config.sources.slice(0, i + 1).map(source => source.nodeId))
    for (let j = 0; j < join.conditions.length; j++) {
      const condition = join.conditions[j]
      const conditionPrefix = `${prefix} 条件 ${j + 1}`
      if (!condition || !leftIds.has(condition.leftNodeId)) return `${conditionPrefix} 的左表必须为此前已加入的来源`
      if (!hasColumn(condition.leftNodeId, condition.leftColumn)) return `${conditionPrefix} 的左字段为空或已失效`
      if (!operators.includes(condition.operator)) return `${conditionPrefix} 的比较运算符无效`
      if (!hasColumn(join.rightNodeId, condition.rightColumn)) return `${conditionPrefix} 的右字段为空或已失效`
    }
  }
  if (!config.fields.length) return '至少选择一个输出字段'
  const outputNames = new Set()
  const projectedColumns = new Map()
  for (let i = 0; i < config.fields.length; i++) {
    const field = config.fields[i]
    if (!field || !ids.has(field.nodeId)) return `输出字段 ${i + 1} 的来源无效`
    if (!hasColumn(field.nodeId, field.column)) return `输出字段 ${i + 1} 的业务字段为空或已失效`
    const columns = projectedColumns.get(field.nodeId) || new Set()
    if (columns.has(field.column)) return `输出字段 ${i + 1} 重复选择了同一来源字段`
    columns.add(field.column)
    projectedColumns.set(field.nodeId, columns)
    if (typeof field.outputName !== 'string' || !identifier.test(field.outputName)) return `输出字段 ${i + 1} 的名称须以字母/下划线开头，且仅含字母、数字、下划线`
    const name = field.outputName.toLowerCase()
    if (['source_table', 'write_time', 'source_natural_key', 'source_row_hash', 'source_datasource', 'source_database', 'source_task', 'source_batch', 'source_update_time', 'record_op'].includes(name)) return `输出名称与系统追溯列冲突：${field.outputName}`
    if (outputNames.has(name)) return `输出名称重复：${field.outputName}`
    outputNames.add(name)
  }
  return ''
}
const validationError = computed(() => validate())
defineExpose({ validate })
</script>

<style scoped>
.join-config { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.section-heading, .join-heading, .source-row, .main-source, .picker-row { display: flex; align-items: center; gap: 8px; }
.section-heading { justify-content: space-between; }
.etl-section-title { margin: 0; font-size: 15px; font-weight: 600; }
.etl-subsection-title { margin: 8px 0 0; font-size: 13px; font-weight: 600; }
.section-heading .etl-subsection-title { margin: 8px 0; }
.main-source > .ant-select { flex: 1; }
.source-row .order { width: 34px; flex-shrink: 0; color: #64748b; }
.source-label { flex: 1; min-width: 80px; overflow-wrap: anywhere; }
.source-row .ant-input { width: 130px; }
.join-block { padding: 10px; border: 1px solid var(--ant-color-border, #e5e7eb); border-radius: 4px; }
.join-heading { flex-wrap: wrap; margin-bottom: 8px; }
.join-heading strong { flex: 1; overflow-wrap: anywhere; }
.join-heading .ant-select { width: 100px; }
.condition-row { display: grid; grid-template-columns: minmax(100px, 1fr) minmax(110px, 1fr) 76px 80px minmax(110px, 1fr) 52px; gap: 6px; align-items: center; margin-top: 6px; }
.right-source { overflow-wrap: anywhere; font-size: 12px; }
.empty, .muted { color: #8c8c8c; font-size: 12px; }
.empty { padding: 8px 0; }
.field-picker { padding: 8px 10px; background: var(--ant-color-fill-alter, #fafafa); border-radius: 4px; }
.picker-row { flex-wrap: wrap; margin-top: 6px; }
.picker-row strong { font-size: 12px; }
.output-table { overflow-x: auto; }
.output-row { display: grid; grid-template-columns: minmax(130px, 1fr) minmax(140px, 1fr) minmax(140px, 1fr) 52px; gap: 8px; align-items: center; margin-bottom: 6px; min-width: 520px; }
.output-header { font-size: 12px; color: #64748b; }
.condition-row > .ant-select, .output-row > .ant-select { width: 100%; min-width: 0; }
@media (max-width: 760px) {
  .condition-row { grid-template-columns: minmax(100px, 1fr) minmax(110px, 1fr) 76px; }
  .source-row { flex-wrap: wrap; }
}
</style>
