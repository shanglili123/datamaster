<template>
  <div class="metric-analysis-panel">
    <a-alert type="info" show-icon message="按已发布版本开展指标分析" description="仅查询管理员批准的只读 ADS 来源；后台执行默认关闭。保存的是分析条件，不保存结果。暂不支持跨表、多指标组合、时间自动分桶或 BI 看板嵌入。" />
    <div class="analysis-toolbar">
      <a-select v-model:value="savedId" :options="savedOptions" :loading="savedLoading" allow-clear show-search option-filter-prop="label" placeholder="加载我的分析" style="width: 280px" @change="loadSaved" />
      <a-button @click="resetAnalysis">新分析</a-button>
      <a-button v-hasPermi="['ont:analysis:list']" @click="loadSavedList">刷新列表</a-button>
      <a-button v-if="savedId" danger v-hasPermi="['ont:analysis:remove']" @click="removeSaved">删除分析</a-button>
    </div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-spin :spinning="metadataLoading">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="指标"><a-select v-model:value="spec.metricId" :options="metricOptions" show-search option-filter-prop="label" placeholder="选择已发布指标" @change="changeMetric" /></a-form-item></a-col>
          <a-col :xs="24" :md="6"><a-form-item label="发布版本"><a-select v-model:value="spec.version" :options="versionOptions" placeholder="明确选择版本，不自动升级" @change="changeVersion" /></a-form-item></a-col>
          <a-col :xs="24" :md="6"><a-form-item label="最多返回分组"><a-input-number v-model:value="spec.limit" :min="1" :max="500" style="width: 100%" /></a-form-item></a-col>
        </a-row>
        <template v-if="snapshot">
          <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px">
            <a-descriptions-item label="口径">{{ snapshot.description }}</a-descriptions-item>
            <a-descriptions-item label="计算">{{ snapshot.aggregation }} / {{ snapshot.unit }}</a-descriptions-item>
            <a-descriptions-item label="来源粒度">{{ snapshot.grain }}</a-descriptions-item>
            <a-descriptions-item label="来源表">{{ sourceLabel }}</a-descriptions-item>
          </a-descriptions>
          <a-form-item label="分组维度（最多五个，不选择则计算总量）"><a-select v-model:value="spec.dimensionPropertyIds" mode="multiple" :options="dimensionOptions" placeholder="只能选择该发布版本允许的维度" /></a-form-item>
          <a-row v-if="snapshot.timePropertyId" :gutter="16">
            <a-col :xs="24" :md="12"><a-form-item :label="`开始时间（含，${timeLabel}）`" required><a-input v-model:value="spec.startTime" :placeholder="dateOnly ? '2026-10-01' : '2026-10-01T00:00:00'" /></a-form-item></a-col>
            <a-col :xs="24" :md="12"><a-form-item label="结束时间（不含，使用源数据业务时间口径）" required><a-input v-model:value="spec.endTime" :placeholder="dateOnly ? '2026-11-01' : '2026-11-01T00:00:00'" /></a-form-item></a-col>
          </a-row>
          <div class="filter-heading"><strong>分析过滤（AND，不替换指标固定口径）</strong><a-button size="small" :disabled="spec.filters.length >= 20" @click="spec.filters.push({ propertyId: undefined, operator: 'EQ', value: '' })">添加条件</a-button></div>
          <p class="analysis-hint">数值、日期和布尔值按字段类型校验。日期使用 ISO 格式；IN 输入 JSON 数组，大整数及小数请写为字符串，例如 ["9007199254740993","12.50"]。</p>
          <div v-for="(filter, index) in spec.filters" :key="index" class="filter-row">
            <a-select v-model:value="filter.propertyId" :options="filterOptions" placeholder="已发布维度或时间属性" style="flex: 1" />
            <a-select v-model:value="filter.operator" :options="operators" style="width: 140px" @change="filter.value = ''" />
            <a-input v-if="!nullOperator(filter.operator)" v-model:value="filter.value" placeholder="值" style="flex: 1" />
            <span v-else class="analysis-hint" style="flex: 1">无需条件值</span>
            <a-button danger @click="spec.filters.splice(index, 1)">移除</a-button>
          </div>
          <details class="fixed-filters"><summary>查看版本固定过滤（只读）</summary><pre>{{ JSON.stringify(snapshot.fixedFilters || [], null, 2) }}</pre></details>
        </template>
        <div class="analysis-toolbar">
          <a-button type="primary" :loading="executing" :disabled="!snapshot || metadataLoading" v-hasPermi="['ont:metric:execute']" @click="execute">执行分析</a-button>
          <a-button :disabled="!snapshot || metadataLoading" v-hasPermi="['ont:analysis:add']" @click="openSave(true)">另存为分析</a-button>
          <a-button v-if="savedId" :disabled="!snapshot || metadataLoading" v-hasPermi="['ont:analysis:edit']" @click="openSave(false)">保存修改</a-button>
        </div>
      </a-form>
    </a-spin>
    <template v-if="result">
      <a-alert :type="result.truncated ? 'warning' : 'info'" show-icon :message="result.truncated ? `结果超过 ${result.limit} 组，仅显示前 ${result.limit} 组；不是完整结果` : '查询完成'" :description="result.notice" />
      <p class="analysis-hint">指标：{{ result.metricName }} · V{{ result.version }} · 单位：{{ result.unit }} · 执行时间：{{ result.executedAt }} · 耗时：{{ result.elapsedMs }} ms。执行时间不是源数据刷新时间；历史版本固定口径，不冻结底层数据。</p>
      <a-table :columns="resultColumns" :data-source="result.rows || []" :row-key="(_, index) => String(index)" :pagination="false" :scroll="{ x: 'max-content' }" size="middle">
        <template #bodyCell="{ text }">{{ displayValue(text) }}</template>
      </a-table>
    </template>
    <a-empty v-else-if="!executing" description="选择指标和版本后执行；修改条件会清除旧结果，保存分析不会触发查询。" />
    <a-modal v-model:open="saveOpen" :title="saveAsNew ? '另存为我的分析' : '保存分析修改'" :confirm-loading="saving" ok-text="保存" cancel-text="取消" @ok="saveAnalysis">
      <a-form layout="vertical">
        <a-form-item label="分析名称" required><a-input v-model:value="saveName" :maxlength="100" /></a-form-item>
        <a-form-item label="说明"><a-textarea v-model:value="saveDescription" :maxlength="1000" :rows="3" /></a-form-item>
      </a-form>
      <a-alert type="info" show-icon message="只保存条件和明确的指标版本，不保存数据、SQL 或访问凭据。仅本人可读取和修改。" />
    </a-modal>
  </div>
</template>

<script setup name="MetricAnalysisPanel">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { checkPermi } from '@/utils/permission'
import { listMetric, listMetricVersions, getMetricVersion } from '@/api/ont/metric'
import { loadPagedOptions } from '@/api/ont/metricOptions'
import { queryMetric, listMetricAnalysis, getMetricAnalysis, addMetricAnalysis, updateMetricAnalysis, delMetricAnalysis } from '@/api/ont/metricAnalysis'

const props = defineProps({ ontologyId: { type: [String, Number], required: true } })
const emptySpec = () => ({ metricId: undefined, version: undefined, dimensionPropertyIds: [], filters: [], startTime: '', endTime: '', limit: 100 })
const spec = ref(emptySpec())
const metrics = ref([])
const versions = ref([])
const snapshot = ref(null)
const metadataLoading = ref(false)
const savedLoading = ref(false)
const savedRows = ref([])
const savedId = ref()
const savedRecord = ref(null)
const result = ref(null)
const error = ref('')
const executing = ref(false)
const saveOpen = ref(false)
const saving = ref(false)
const saveAsNew = ref(true)
const saveName = ref('')
const saveDescription = ref('')
let contextSequence = 0
let metadataSequence = 0
let executionSequence = 0
let savedSequence = 0
let savedDetailSequence = 0
let alive = true
const operators = ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'IN', 'IS_NULL', 'IS_NOT_NULL'].map(value => ({ value, label: value }))
const metricOptions = computed(() => metrics.value.map(item => ({ value: item.id, label: `${item.name} (${item.code})` })))
const savedOptions = computed(() => savedRows.value.map(item => ({ value: item.id, label: item.name })))
const versionOptions = computed(() => versions.value.map(item => ({ value: item.version, label: `V${item.version}` })))
const propertyMap = computed(() => snapshot.value?.sourceSnapshot?.propertyColumns || {})
const propertyOption = id => ({ value: id, label: propertyMap.value[String(id)]?.propertyName || propertyMap.value[String(id)]?.propertyCode || String(id) })
const dimensionOptions = computed(() => (snapshot.value?.dimensionPropertyIds || []).map(propertyOption))
const filterOptions = computed(() => [...new Set([...(snapshot.value?.dimensionPropertyIds || []), snapshot.value?.timePropertyId].filter(id => id != null))].map(propertyOption))
const timeLabel = computed(() => propertyMap.value[String(snapshot.value?.timePropertyId)]?.propertyName || '业务时间')
const dateOnly = computed(() => propertyMap.value[String(snapshot.value?.timePropertyId)]?.dataType?.toLowerCase() === 'date')
const sourceLabel = computed(() => [snapshot.value?.sourceSnapshot?.databaseName, snapshot.value?.sourceSnapshot?.tableName].filter(Boolean).join('.'))
const resultColumns = computed(() => (result.value?.columns || []).map(column => ({ title: column.label, key: column.key, dataIndex: column.key })))
const nullOperator = operator => ['IS_NULL', 'IS_NOT_NULL'].includes(operator)
const errorText = failure => failure?.response?.data?.msg || failure?.message || '请求失败，请重试'
const displayValue = value => value == null ? '—' : typeof value === 'object' ? JSON.stringify(value) : String(value)

function clearResult() { ++executionSequence; result.value = null; executing.value = false }
watch(spec, clearResult, { deep: true, flush: 'sync' })
async function loadSavedList() {
  if (!checkPermi(['ont:analysis:list'])) { savedRows.value = []; return }
  const context = contextSequence
  const sequence = ++savedSequence
  savedLoading.value = true
  try {
    const items = await loadPagedOptions(listMetricAnalysis, { ontologyId: props.ontologyId })
    if (context === contextSequence && sequence === savedSequence) savedRows.value = items
  } catch (failure) {
    if (context === contextSequence && sequence === savedSequence) { savedRows.value = []; error.value = '我的分析加载失败，请检查分析列表权限' }
  } finally { if (context === contextSequence && sequence === savedSequence) savedLoading.value = false }
}
async function loadSnapshot(sequence) {
  if (!spec.value.metricId || !spec.value.version) return
  const metricId = spec.value.metricId
  const response = await getMetricVersion(metricId, spec.value.version)
  if (sequence !== metadataSequence || !alive) return
  snapshot.value = response.data
}
async function changeMetric() {
  saveOpen.value = false
  const sequence = ++metadataSequence
  snapshot.value = null; versions.value = []; error.value = ''
  Object.assign(spec.value, { version: undefined, dimensionPropertyIds: [], filters: [], startTime: '', endTime: '' })
  if (!spec.value.metricId) return
  metadataLoading.value = true
  try {
    const response = await listMetricVersions(spec.value.metricId)
    if (sequence !== metadataSequence || !alive) return
    versions.value = response.data || []
    // 初次选择才取最新版本；保存后的分析始终沿用其显式版本。
    spec.value.version = versions.value[0]?.version
    await loadSnapshot(sequence)
  } catch (failure) {
    if (sequence === metadataSequence) error.value = errorText(failure)
  } finally { if (sequence === metadataSequence) metadataLoading.value = false }
}
async function changeVersion() {
  saveOpen.value = false
  const sequence = ++metadataSequence
  snapshot.value = null; error.value = ''
  Object.assign(spec.value, { dimensionPropertyIds: [], filters: [], startTime: '', endTime: '' })
  metadataLoading.value = true
  try { await loadSnapshot(sequence) }
  catch (failure) { if (sequence === metadataSequence) error.value = errorText(failure) }
  finally { if (sequence === metadataSequence) metadataLoading.value = false }
}
function resetAnalysis() {
  ++metadataSequence; ++savedDetailSequence
  savedId.value = undefined; savedRecord.value = null
  spec.value = emptySpec(); snapshot.value = null; versions.value = []
  error.value = ''; metadataLoading.value = false
  saveOpen.value = false
}
async function loadSaved(id) {
  if (!id) { resetAnalysis(); return }
  const sequence = ++savedDetailSequence
  const metadata = ++metadataSequence
  const context = contextSequence
  snapshot.value = null; savedRecord.value = null; versions.value = []; clearResult()
  error.value = ''; metadataLoading.value = true
  try {
    const response = await getMetricAnalysis(id)
    if (sequence !== savedDetailSequence || metadata !== metadataSequence || context !== contextSequence) return
    const record = response.data
    savedRecord.value = record
    spec.value = { ...emptySpec(), ...record.query, filters: (record.query.filters || []).map(filter => ({ ...filter, value: filter.operator === 'IN' ? JSON.stringify(filter.value) : filter.value == null ? '' : String(filter.value) })) }
    const versionResponse = await listMetricVersions(spec.value.metricId)
    if (metadata !== metadataSequence || context !== contextSequence) return
    versions.value = versionResponse.data || []
    await loadSnapshot(metadata)
  } catch (failure) {
    if (sequence === savedDetailSequence && metadata === metadataSequence && context === contextSequence) error.value = errorText(failure)
  } finally { if (metadata === metadataSequence) metadataLoading.value = false }
}
function payload() {
  if (!snapshot.value || !spec.value.version) throw new Error('请先选择有效指标版本')
  if (spec.value.dimensionPropertyIds.length > 5) throw new Error('最多选择五个维度')
  if (snapshot.value.timePropertyId && (!spec.value.startTime || !spec.value.endTime)) throw new Error('该指标必须提供开始时间和结束时间')
  const filters = spec.value.filters.map(filter => {
    if (!filter.propertyId) throw new Error('请选择分析过滤属性')
    if (nullOperator(filter.operator)) return { propertyId: filter.propertyId, operator: filter.operator, value: null }
    let value = filter.value
    if (filter.operator === 'IN') {
      try { value = JSON.parse(value) } catch (failure) { throw new Error('IN 值必须为合法 JSON 数组') }
      if (!Array.isArray(value) || !value.length || value.length > 100) throw new Error('IN 数组必须包含 1～100 个值')
    }
    const convert = item => {
      if (item == null || typeof item === 'object') throw new Error('过滤值不能为 null、对象或嵌套数组')
      const type = propertyMap.value[String(filter.propertyId)]?.dataType?.toLowerCase()
      if (['integer', 'decimal', 'number', 'long', 'int', 'double', 'float', 'numeric', 'bigint', 'smallint'].includes(type)) {
        if (typeof item === 'number' && !Number.isSafeInteger(item)) throw new Error('大整数及小数请使用带双引号的字符串，避免精度丢失')
        const text = String(item).trim()
        if (!text || !Number.isFinite(Number(text))) throw new Error('数值属性需要有效数字')
        return text
      }
      if (type === 'boolean') {
        if (![true, false, 'true', 'false'].includes(item)) throw new Error('布尔值只能为 true 或 false')
        return item === true || item === 'true'
      }
      return String(item)
    }
    if (value === '') throw new Error('请填写分析过滤值')
    return { propertyId: filter.propertyId, operator: filter.operator, value: Array.isArray(value) ? value.map(convert) : convert(value) }
  })
  return { metricId: spec.value.metricId, version: spec.value.version, dimensionPropertyIds: [...spec.value.dimensionPropertyIds], filters, startTime: spec.value.startTime || null, endTime: spec.value.endTime || null, limit: spec.value.limit }
}
async function execute() {
  let request
  try { request = payload() } catch (failure) { error.value = errorText(failure); return }
  const sequence = ++executionSequence
  const context = contextSequence
  error.value = ''; result.value = null; executing.value = true
  try {
    const response = await queryMetric(request)
    if (sequence === executionSequence && context === contextSequence && alive) result.value = response.data
  } catch (failure) {
    if (sequence === executionSequence && context === contextSequence) error.value = errorText(failure)
  } finally { if (sequence === executionSequence) executing.value = false }
}
function openSave(asNew) {
  try { payload() } catch (failure) { error.value = errorText(failure); return }
  saveAsNew.value = asNew
  saveName.value = savedRecord.value?.name || `${snapshot.value.name}分析`
  saveDescription.value = savedRecord.value?.description || ''
  saveOpen.value = true
}
async function saveAnalysis() {
  if (!saveName.value.trim()) { message.warning('请填写分析名称'); return }
  const context = contextSequence
  saving.value = true; error.value = ''
  try {
    const data = { ontologyId: props.ontologyId, name: saveName.value.trim(), description: saveDescription.value, query: payload() }
    if (!saveAsNew.value) { data.id = savedId.value; data.revision = savedRecord.value?.revision }
    const response = saveAsNew.value ? await addMetricAnalysis(data) : await updateMetricAnalysis(data)
    if (context !== contextSequence || !alive) return
    const id = saveAsNew.value ? response.data : savedId.value
    saveOpen.value = false; message.success('分析条件已保存，未执行查询')
    await loadSavedList()
    if (context !== contextSequence || !alive) return
    savedId.value = id
    await loadSaved(id)
  } catch (failure) { if (context === contextSequence) error.value = errorText(failure) }
  finally { if (context === contextSequence) saving.value = false }
}
function removeSaved() {
  const id = savedId.value
  const context = contextSequence
  Modal.confirm({ title: '删除当前保存的分析？', content: '仅删除分析条件，不删除指标或数据。', async onOk() {
    await delMetricAnalysis(id)
    if (context !== contextSequence || !alive) return
    resetAnalysis(); loadSavedList()
  } })
}
async function reload() {
  const context = ++contextSequence
  resetAnalysis(); metrics.value = []; savedRows.value = []; saving.value = false
  metadataLoading.value = true
  try {
    const items = await loadPagedOptions(listMetric, { ontologyId: props.ontologyId, status: 'PUBLISHED' })
    if (context !== contextSequence || !alive) return
    metrics.value = items
  } catch (failure) { if (context === contextSequence) error.value = '已发布指标加载失败，请检查指标列表权限' }
  finally { if (context === contextSequence) metadataLoading.value = false }
  if (context === contextSequence && alive) loadSavedList()
}
watch(() => props.ontologyId, reload, { immediate: true })
onBeforeUnmount(() => { alive = false; ++contextSequence; ++metadataSequence; ++executionSequence; ++savedSequence; ++savedDetailSequence })
defineExpose({ reload })
</script>

<style lang="scss" scoped>
.metric-analysis-panel { display: flex; flex-direction: column; gap: 16px; }
.analysis-toolbar, .filter-row, .filter-heading { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-bottom: 12px; }
.filter-heading { justify-content: space-between; }
.analysis-hint { color: #606b7b; font-size: 13px; line-height: 1.7; }
.fixed-filters { margin: 16px 0; }
.fixed-filters pre { max-height: 200px; overflow: auto; white-space: pre-wrap; }
</style>
