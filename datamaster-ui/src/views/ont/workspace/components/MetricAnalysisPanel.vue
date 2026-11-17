<template>
  <div class="metric-analysis-panel">
    <AnalysisFlowNavigation stage="analysis" :ontology-id="ontologyId" :disabled="executing || handoffBusy" />
    <a-alert type="info" show-icon message="按已发布版本开展指标分析" description="仅查询已授权并批准的只读来源；来源由查询来源配置页面管理，未批准默认拒绝。保存的是分析条件，不保存结果。支持服务端排序、完整有界趋势、分页、日/月/年分桶、两期总量/分组/时间桶与受限同源多指标比较；不支持跨库 JOIN 或 BI 看板嵌入。" />
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
          <a-col :xs="24" :md="6"><a-form-item label="每页分组数 / TopN"><a-input-number v-model:value="spec.limit" :min="1" :max="500" style="width: 100%" /></a-form-item></a-col>
        </a-row>
        <template v-if="snapshot">
          <a-alert v-if="sourceStatus || sourceStatusError" :type="sourceStatus?.available ? 'info' : 'warning'" show-icon :message="sourceStatusError || sourceStatus?.notice" style="margin-bottom: 16px">
            <template #action><a-space><a-button :loading="sourceStatusLoading" @click="loadSourceStatus">刷新状态</a-button><a-button v-if="canManageSources" @click="openSourceConfig">查询来源配置</a-button></a-space></template>
          </a-alert>
          <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px">
            <a-descriptions-item label="口径">{{ snapshot.description }}</a-descriptions-item>
            <a-descriptions-item label="计算">{{ snapshot.aggregation }} / {{ snapshot.unit }}</a-descriptions-item>
            <a-descriptions-item v-if="snapshot.derivedOperation && snapshot.derivedOperation !== 'NONE'" label="正式派生口径" :span="2">{{ snapshot.aggregation }} / {{ snapshot.derivedOperation }} / {{ snapshot.secondaryAggregation }} · 单位 {{ snapshot.primaryUnit }} / {{ snapshot.secondaryUnit }}，在同一来源筛选内聚合后运算，分母0为缺失。</a-descriptions-item>
            <a-descriptions-item label="来源粒度">{{ snapshot.grain }}</a-descriptions-item>
            <a-descriptions-item label="来源表">{{ sourceLabel }}</a-descriptions-item>
          </a-descriptions>
          <a-form-item label="分组维度（最多五个；无维度且不分桶时计算总量）"><a-select v-model:value="spec.dimensionPropertyIds" mode="multiple" :options="dimensionOptions" placeholder="只能选择该发布版本允许的维度" /></a-form-item>
          <a-row :gutter="16">
            <a-col v-if="snapshot.timePropertyId" :xs="24" :md="8"><a-form-item label="时间分桶"><a-select v-model:value="spec.timeBucket" :options="bucketOptions" /></a-form-item></a-col>
            <a-col :xs="24" :md="8"><a-form-item label="服务端排序（前 N 组）"><a-select v-model:value="spec.sortBy" :options="sortOptions" /></a-form-item></a-col>
            <a-col :xs="24" :md="8"><a-form-item label="排序方向"><a-select v-model:value="spec.sortDirection" :disabled="!spec.sortBy" :options="directionOptions" /></a-form-item></a-col>
          </a-row>
          <p class="analysis-hint">分组、聚合及排序均在来源数据库执行，再返回前 N 组；指标值相同时按分组稳定排序。不在已截断结果上重新排序或聚合。</p>
          <a-space v-if="snapshot.timePropertyId" wrap class="analysis-toolbar">
            <span>业务本地日期快捷范围</span>
            <a-button size="small" @click="applyTimeRange('TODAY')">今天</a-button><a-button size="small" @click="applyTimeRange('LAST_7_DAYS')">近7个日历日</a-button>
            <a-button size="small" @click="applyTimeRange('THIS_MONTH')">本月至今</a-button><a-button size="small" @click="applyTimeRange('LAST_MONTH')">上个完整月</a-button>
            <span class="analysis-hint">按已发布业务偏移填写日期；未声明时沿用浏览器日期，请核对来源口径。不会自动执行。</span>
          </a-space>
          <a-row v-if="snapshot.timePropertyId" :gutter="16">
            <a-col :xs="24" :md="12"><a-form-item :label="`开始时间（含，${timeLabel}）`" required><a-input v-model:value="spec.startTime" :placeholder="dateOnly ? '2026-10-01' : '2026-10-01T00:00:00'" /></a-form-item></a-col>
            <a-col :xs="24" :md="12"><a-form-item label="结束时间（不含，使用该发布版本的业务时间口径）" required><a-input v-model:value="spec.endTime" :placeholder="dateOnly ? '2026-11-01' : '2026-11-01T00:00:00'" /></a-form-item></a-col>
          </a-row>
          <p v-if="snapshot.timePropertyId" class="analysis-hint">{{ snapshot.sourceTimeOffset ? `发布口径：来源 UTC${snapshot.sourceTimeOffset} → 业务 UTC${snapshot.businessTimeOffset}（固定偏移，不自动处理夏令时）` : '未声明时间偏移，沿用来源业务本地时间，不推断时区' }}。起止区间为 [开始, 结束)，按业务时间输入；时间桶按发布口径生成。额外时间字段过滤仍按物理来源时间；未满桶仅统计所选区间、空桶不补零。</p>
          <div class="filter-heading"><strong>分析过滤（AND，不替换指标固定口径）</strong><a-button size="small" :disabled="spec.filters.length >= filterBudget" @click="spec.filters.push({ propertyId: undefined, operator: 'EQ', value: '' })">添加条件</a-button></div>
          <p class="analysis-hint">固定过滤与分析过滤合计最多20项，当前还可添加 {{ Math.max(0, filterBudget - spec.filters.length) }} 项。数值、日期和布尔值按字段类型校验。日期使用 ISO 格式；IN 输入 JSON 数组，大整数及小数请写为字符串，例如 ["9007199254740993","12.50"]。</p>
          <p class="analysis-hint">文本空字符串是有效过滤值（不同于SQL空值）；查询SQL空值请选择IS_NULL。数值、日期和布尔输入不能留空。</p>
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
          <a-checkbox v-model:checked="spec.requireComplete">要求完整有界结果（超限拒绝，不返回部分趋势）</a-checkbox>
          <a-button type="primary" :loading="executing" :disabled="!snapshot || metadataLoading" v-hasPermi="['ont:metric:execute']" @click="execute">执行分析</a-button>
          <a-button :loading="executing" :disabled="!snapshot || metadataLoading || executing || spec.timeBucket === 'NONE' || spec.dimensionPropertyIds.length" v-hasPermi="['ont:metric:execute']" @click="executeCompleteTrend">完整趋势（最多500桶）</a-button>
          <a-button :disabled="!snapshot || metadataLoading" v-hasPermi="['ont:analysis:add']" @click="openSave(true)">另存为分析</a-button>
          <a-button v-if="savedId" :disabled="!snapshot || metadataLoading" v-hasPermi="['ont:analysis:edit']" @click="openSave(false)">保存修改</a-button>
        </div>
      </a-form>
    </a-spin>
    <a-space v-if="drillHistory.length" class="analysis-toolbar"><span>已下钻 {{ drillHistory.length }} 层</span><a-button :disabled="executing || metadataLoading" @click="drillBack">返回上一级条件（不查询）</a-button></a-space>
    <MetricComparisonPanel v-if="snapshot" :key="`${ontologyId}:${spec.metricId}:${spec.version}`" :query="comparisonQuery" :query-error="comparisonInput.error" :disabled="executing || metadataLoading || handoffBusy" />
    <MetricMultiQueryPanel v-if="snapshot" :key="`multi:${ontologyId}:${spec.metricId}:${spec.version}`" :query="comparisonQuery" :query-error="comparisonInput.error" :options="metricOptions" :disabled="executing || metadataLoading" />
    <template v-if="result">
      <a-alert :type="result.truncated ? 'warning' : 'info'" show-icon :message="result.truncated ? `第 ${result.pageNum || 1} 页返回 ${result.rows?.length || 0} 组；当前不是完整分组，不能据此计算全量汇总` : `查询完成，当前 ${result.rows?.length || 0} 组未截断`" :description="result.notice" />
      <p class="analysis-hint">指标：{{ result.metricName }} · V{{ result.version }} · 单位：{{ result.unit }} · 执行时间：{{ result.executedAt }} · 耗时：{{ result.elapsedMs }} ms。执行时间不是源数据刷新时间；历史版本固定口径，不冻结底层数据。</p>
      <div class="analysis-toolbar">
        <a-button type="primary" :loading="handoffBusy" v-hasPermi="['ai:ask-data:query']" @click="formDecision">基于当前分析制定方案</a-button>
        <span class="analysis-hint">仅带入明确版本与分析条件，不传查询结果；进入决策后需重新核验并由你发送。</span>
      </div>
      <a-space class="analysis-toolbar"><span>展示方式</span><a-radio-group v-model:value="visualMode"><a-radio-button value="table">精确表格</a-radio-button><a-radio-button value="bar" :disabled="!chartDimension">分组条形</a-radio-button><a-radio-button value="line" :disabled="!chartDimension">趋势折线</a-radio-button></a-radio-group></a-space>
      <DeclarativeChartRenderer v-if="visualMode !== 'table' && chartDimension" :rows="result.rows" :x-field="chartDimension.key" y-field="metric_value" :chart-type="visualMode" :title="result.metricName" :unit="result.unit" :show-table="false" :notice="result.truncated ? '仅绘制当前页/TopN，不能据此推断完整趋势。缺失值不补零。' : '绘制当前已返回分组，原有精确数据表保留。'" />
      <p class="analysis-hint">点击分组行收窄维度或该时间桶与原区间的交集，再选尚未筛定的下一维度；可返回上一级，只改条件不自动查询。空值使用IS_NULL，缺失桶不当零。</p>
      <a-table :columns="resultColumns" :custom-row="row => ({ onClick: () => drillDown(row) })" :data-source="result.rows || []" :row-key="(_, index) => String(index)" :pagination="false" :scroll="{ x: 'max-content' }" size="middle">
        <template #bodyCell="{ text }">{{ displayValue(text) }}</template>
      </a-table>
      <a-space class="analysis-toolbar">
        <a-button :disabled="executing || (result.pageNum || 1) <= 1" @click="changePage((result.pageNum || 1) - 1)">上一页</a-button>
        <span>第 {{ result.pageNum || 1 }} 页（无估算总数；翻页会重新查询来源）</span>
        <a-button :disabled="executing || !result.hasMore || (result.pageNum || 1) >= 1000" @click="changePage((result.pageNum || 1) + 1)">下一页</a-button>
      </a-space>
    </template>
    <a-empty v-else-if="!executing" description="选择指标和版本后执行；修改条件会清除旧结果，保存分析不会触发查询。" />
    <a-modal v-model:open="saveOpen" :title="saveAsNew ? '另存为我的分析' : '保存分析修改'" :confirm-loading="saving" :closable="!saving" :mask-closable="!saving" :keyboard="!saving" :cancel-button-props="{ disabled: saving }" ok-text="保存" cancel-text="取消" @ok="saveAnalysis">
      <a-form :disabled="saving" layout="vertical">
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
import AnalysisFlowNavigation from '@/components/AnalysisFlowNavigation.vue'
import DeclarativeChartRenderer from '@/components/DeclarativeChartRenderer.vue'
import MetricComparisonPanel from './MetricComparisonPanel.vue'
import MetricMultiQueryPanel from './MetricMultiQueryPanel.vue'
import { useAnalysisNavigation } from '@/utils/analysisNavigation'
import { metricBucketRange, metricDrillFilters, nextMetricDrillDimension } from '@/utils/metricDrilldown'
import { metricQuickTimeRange } from '@/utils/metricTimeRange'
import { metricFilterScalar } from '@/utils/metricFilter'
import { metricDisplayValue } from '@/utils/metricDisplay'
import { metricTemporalRange } from '@/utils/metricTemporal'
import { checkPermi } from '@/utils/permission'
import { listPublishedMetric, listMetricVersions, getMetricVersion } from '@/api/ont/metric'
import { loadPagedOptions } from '@/api/ont/metricOptions'
import { getMetricSourceStatus } from '@/api/ont/metricSourceApproval'
import useUserStore from '@/store/system/user'
import { queryMetric, listMetricAnalysis, getMetricAnalysis, addMetricAnalysis, updateMetricAnalysis, delMetricAnalysis } from '@/api/ont/metricAnalysis'

const props = defineProps({
  ontologyId: { type: [String, Number], required: true },
  initialSelection: { type: Object, default: null }
})
const { openManualDecision, open } = useAnalysisNavigation()
const sourceUser = useUserStore()
const canManageSources = computed(() => checkPermi(['ont:metric-source:query', 'ont:metric-source:approve', 'ont:metric-source:revoke']))
function openSourceConfig() { return open(`/ont/workspace/${props.ontologyId}`, { spaceId: String(sourceUser.spaceId || ''), tab: 'metric-source' }) }
const queryPage = ref(1)
const drillHistory = ref([])
const lastExecutedQuery = ref(null)
const handoffBusy = ref(false)
const emptySpec = () => ({ metricId: undefined, version: undefined, dimensionPropertyIds: [], filters: [], startTime: '', endTime: '', timeBucket: 'NONE', sortBy: '', sortDirection: 'ASC', requireComplete: false, limit: 100 })
const spec = ref(emptySpec())
const metrics = ref([])
const versions = ref([])
const snapshot = ref(null)
const sourceStatus = ref(null), sourceStatusError = ref(''), sourceStatusLoading = ref(false)
let sourceStatusSequence = 0
async function loadSourceStatus() {
  const token = ++sourceStatusSequence
  sourceStatus.value = null; sourceStatusError.value = ''; sourceStatusLoading.value = false
  if (!snapshot.value?.conceptTableId || !checkPermi(['ont:metric:execute'])) return
  sourceStatusLoading.value = true
  try {
    const response = await getMetricSourceStatus({ ontologyId: props.ontologyId, bindingId: snapshot.value.conceptTableId })
    if (token !== sourceStatusSequence) return
    sourceStatus.value = response.data
  } catch (failure) { if (token === sourceStatusSequence) sourceStatusError.value = failure?.message || '来源批准状态暂不可用，执行时仍由服务端核验' }
  finally { if (token === sourceStatusSequence) sourceStatusLoading.value = false }
}
watch(() => [props.ontologyId, sourceUser.spaceId, sourceUser.spaceCode, snapshot.value], loadSourceStatus)
onBeforeUnmount(() => { ++sourceStatusSequence })
const metadataLoading = ref(false)
const savedLoading = ref(false)
const savedRows = ref([])
const savedId = ref()
const savedRecord = ref(null)
const result = ref(null)
const visualMode = ref('table')
const chartDimension = computed(() => {
  const groups = (result.value?.columns || []).filter(column => column.key !== 'metric_value')
  return groups.length === 1 ? groups[0] : null
})
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
let conditionsSequence = 0
let alive = true
const bucketOptions = [{ value: 'NONE', label: '不分桶' }, { value: 'DAY', label: '按日' }, { value: 'MONTH', label: '按月' }, { value: 'YEAR', label: '按年' }]
const directionOptions = [{ value: 'ASC', label: '升序 ASC' }, { value: 'DESC', label: '降序 DESC' }]
const operators = ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'IN', 'IS_NULL', 'IS_NOT_NULL'].map(value => ({ value, label: value }))
const metricOptions = computed(() => metrics.value.map(item => ({ value: item.id, label: `${item.name} (${item.code})` })))
const savedOptions = computed(() => savedRows.value.map(item => ({ value: item.id, label: item.name })))
const versionOptions = computed(() => versions.value.map(item => ({ value: item.version, label: `V${item.version}` })))
const propertyMap = computed(() => snapshot.value?.sourceSnapshot?.propertyColumns || {})
const propertyOption = id => ({ value: id, label: propertyMap.value[String(id)]?.propertyName || propertyMap.value[String(id)]?.propertyCode || String(id) })
const dimensionOptions = computed(() => (snapshot.value?.dimensionPropertyIds || []).map(propertyOption))
const sortOptions = computed(() => [
  { value: '', label: '默认顺序（按分组升序）' },
  { value: 'metric_value', label: '指标值' },
  ...(spec.value.timeBucket !== 'NONE' ? [{ value: 'time_bucket', label: '时间分桶' }] : []),
  ...(spec.value.dimensionPropertyIds || []).map(id => ({ value: `d_${id}`, label: propertyOption(id).label }))
])
const filterBudget = computed(() => Math.max(0, 20 - (snapshot.value?.fixedFilters?.length || 0)))
const filterOptions = computed(() => [...new Set([...(snapshot.value?.dimensionPropertyIds || []), snapshot.value?.timePropertyId].filter(id => id != null))].map(propertyOption))
const timeLabel = computed(() => propertyMap.value[String(snapshot.value?.timePropertyId)]?.propertyName || '业务时间')
const dateOnly = computed(() => propertyMap.value[String(snapshot.value?.timePropertyId)]?.dataType?.toLowerCase() === 'date')
const sourceLabel = computed(() => [snapshot.value?.sourceSnapshot?.databaseName, snapshot.value?.sourceSnapshot?.tableName].filter(Boolean).join('.'))
const resultColumns = computed(() => (result.value?.columns || []).map(column => ({ title: column.label, key: column.key, dataIndex: column.key })))
const nullOperator = operator => ['IS_NULL', 'IS_NOT_NULL'].includes(operator)
const errorText = failure => failure?.response?.data?.msg || failure?.message || '请求失败，请重试'
const displayValue = metricDisplayValue

function clearResult() { ++executionSequence; result.value = null; lastExecutedQuery.value = null; executing.value = false }
watch(spec, () => { ++conditionsSequence; queryPage.value = 1; clearResult() }, { deep: true, flush: 'sync' })
watch(queryPage, () => { ++conditionsSequence }, { flush: 'sync' })
watch(() => [spec.value.dimensionPropertyIds, spec.value.timeBucket], () => {
  if (spec.value.sortBy && !sortOptions.value.some(option => option.value === spec.value.sortBy)) {
    spec.value.sortBy = ''; spec.value.sortDirection = 'ASC'
  }
}, { deep: true, flush: 'sync' })
watch(() => spec.value.sortBy, value => {
  if (!value || !['ASC', 'DESC'].includes(spec.value.sortDirection)) spec.value.sortDirection = 'ASC'
}, { flush: 'sync' })
async function loadSavedList() {
  if (!checkPermi(['ont:analysis:list'])) { savedRows.value = []; return }
  const context = contextSequence
  const sequence = ++savedSequence
  savedLoading.value = true
  try {
    const items = await loadPagedOptions(listMetricAnalysis, { ontologyId: props.ontologyId }, { isCurrent: () => alive && context === contextSequence && sequence === savedSequence })
    if (context === contextSequence && sequence === savedSequence) savedRows.value = items
  } catch (failure) {
    if (context === contextSequence && sequence === savedSequence) { savedRows.value = []; error.value = `我的分析加载失败：${errorText(failure)}` }
  } finally { if (context === contextSequence && sequence === savedSequence) savedLoading.value = false }
}
async function loadSnapshot(sequence) {
  if (!spec.value.metricId || !spec.value.version) return
  const metricId = spec.value.metricId, version = spec.value.version
  const response = await getMetricVersion(metricId, version)
  if (sequence !== metadataSequence || !alive) return
  const publication = response.data
  if (!publication || String(publication.id) !== String(metricId) || Number(publication.publishedVersion) !== Number(version)
      || String(publication.ontologyId) !== String(props.ontologyId) || publication.status !== 'PUBLISHED') throw new Error('指标发布快照与当前本体/版本不一致')
  snapshot.value = publication
}
async function changeMetric() {
  drillHistory.value = []
  saveOpen.value = false
  const sequence = ++metadataSequence
  snapshot.value = null; versions.value = []; error.value = ''
  Object.assign(spec.value, { version: undefined, dimensionPropertyIds: [], filters: [], startTime: '', endTime: '', timeBucket: 'NONE', sortBy: '', sortDirection: 'ASC', requireComplete: false })
  if (!spec.value.metricId) { metadataLoading.value = false; return }
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
  drillHistory.value = []
  saveOpen.value = false
  const sequence = ++metadataSequence
  snapshot.value = null; error.value = ''
  Object.assign(spec.value, { dimensionPropertyIds: [], filters: [], startTime: '', endTime: '', timeBucket: 'NONE', sortBy: '', sortDirection: 'ASC', requireComplete: false })
  metadataLoading.value = true
  try { await loadSnapshot(sequence) }
  catch (failure) { if (sequence === metadataSequence) error.value = errorText(failure) }
  finally { if (sequence === metadataSequence) metadataLoading.value = false }
}
function resetAnalysis() {
  drillHistory.value = []
  ++metadataSequence; ++savedDetailSequence
  savedId.value = undefined; savedRecord.value = null
  spec.value = emptySpec(); snapshot.value = null; versions.value = []
  error.value = ''; metadataLoading.value = false
  saveOpen.value = false
}
async function loadSaved(id) {
  drillHistory.value = []
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
    spec.value = {
      ...emptySpec(), ...record.query,
      dimensionPropertyIds: record.query.dimensionPropertyIds || [],
      timeBucket: record.query.timeBucket || 'NONE', sortBy: record.query.sortBy || '', sortDirection: record.query.sortDirection || 'ASC',
      filters: (record.query.filters || []).map(filter => ({ ...filter, value: filter.operator === 'IN' ? JSON.stringify(filter.value) : filter.value == null ? '' : String(filter.value) }))
    }
    queryPage.value = record.query.pageNum || 1
    const versionResponse = await listMetricVersions(spec.value.metricId)
    if (metadata !== metadataSequence || context !== contextSequence) return
    versions.value = versionResponse.data || []
    await loadSnapshot(metadata)
  } catch (failure) {
    if (sequence === savedDetailSequence && metadata === metadataSequence && context === contextSequence) error.value = errorText(failure)
  } finally { if (metadata === metadataSequence) metadataLoading.value = false }
}
function applyTimeRange(mode) {
  try { Object.assign(spec.value, metricQuickTimeRange(mode, snapshot.value?.businessTimeOffset, dateOnly.value)) }
  catch (failure) { message.warning(errorText(failure)) }
}
function payload() {
  if (!snapshot.value || !spec.value.version) throw new Error('请先选择有效指标版本')
  if (spec.value.dimensionPropertyIds.length > 5) throw new Error('最多选择五个维度')
  if (spec.value.filters.length > filterBudget.value) throw new Error('固定过滤与分析过滤合计最多20项')
  if (snapshot.value.timePropertyId && (!spec.value.startTime || !spec.value.endTime)) throw new Error('该指标必须提供开始时间和结束时间')
  if (snapshot.value.timePropertyId) {
    metricTemporalRange(spec.value.startTime, spec.value.endTime, propertyMap.value[String(snapshot.value.timePropertyId)]?.dataType)
  } else if (spec.value.startTime || spec.value.endTime) throw new Error('该版本未声明可用时间属性，不能附加时间区间')
  if (!bucketOptions.some(option => option.value === spec.value.timeBucket)) throw new Error('请选择有效时间分桶')
  if (!snapshot.value.timePropertyId && spec.value.timeBucket !== 'NONE') throw new Error('该版本没有可用的分桶时间属性')
  if (!sortOptions.value.some(option => option.value === spec.value.sortBy)) throw new Error('排序字段必须为指标值或当前分组')
  if (spec.value.sortBy && !['ASC', 'DESC'].includes(spec.value.sortDirection)) throw new Error('请选择排序方向')
  const filters = spec.value.filters.map(filter => {
    if (!filter.propertyId) throw new Error('请选择分析过滤属性')
    if (nullOperator(filter.operator)) return { propertyId: filter.propertyId, operator: filter.operator, value: null }
    let value = filter.value
    if (filter.operator === 'IN') {
      try { value = JSON.parse(value) } catch { throw new Error('IN 值必须为合法 JSON 数组') }
      if (!Array.isArray(value) || !value.length || value.length > 100) throw new Error('IN 数组必须包含 1～100 个值')
    }
    const convert = item => metricFilterScalar(item, propertyMap.value[String(filter.propertyId)]?.dataType)
    return { propertyId: filter.propertyId, operator: filter.operator, value: Array.isArray(value) ? value.map(convert) : convert(value) }
  })
  return {
    metricId: spec.value.metricId, version: spec.value.version, dimensionPropertyIds: [...spec.value.dimensionPropertyIds], filters,
    startTime: spec.value.startTime || null, endTime: spec.value.endTime || null, timeBucket: spec.value.timeBucket,
    sortBy: spec.value.sortBy || null, sortDirection: spec.value.sortBy ? spec.value.sortDirection : null, limit: spec.value.limit, pageNum: queryPage.value, requireComplete: Boolean(spec.value.requireComplete)
  }
}
const comparisonInput = computed(() => {
  try { return { query: payload(), error: '' } }
  catch (failure) { return { query: null, error: errorText(failure) } }
})
const comparisonQuery = computed(() => comparisonInput.value.query)
function drillDown(row) {
  if (!lastExecutedQuery.value || executing.value || metadataLoading.value) return
  const selected = [...spec.value.dimensionPropertyIds]
  const hasBucket = spec.value.timeBucket !== 'NONE' && row.time_bucket != null
  if (!selected.length && !hasBucket) { message.info('当前没有可下钻的分组或时间桶'); return }
  if (spec.value.filters.length + selected.length > filterBudget.value || drillHistory.value.length >= 20) { message.info('固定过滤与下钻筛选合计最多20项，历史最多20层'); return }
  const next = nextMetricDrillDimension(snapshot.value?.dimensionPropertyIds, selected, [...spec.value.filters, ...(snapshot.value?.fixedFilters || [])], snapshot.value?.timePropertyId)
  const previous = JSON.parse(JSON.stringify(spec.value))
  try {
    const bucket = hasBucket ? metricBucketRange(String(row.time_bucket), spec.value.timeBucket, dateOnly.value, spec.value.startTime, spec.value.endTime) : null
    const newFilters = metricDrillFilters(selected, row)
    drillHistory.value.push(previous)
    spec.value = { ...previous, ...(bucket || {}), filters: [...previous.filters, ...newFilters], dimensionPropertyIds: next == null ? [] : [next],
      timeBucket: bucket ? 'NONE' : previous.timeBucket, sortBy: '', sortDirection: 'ASC', requireComplete: false }
    message.success('已收窄到该分组/时间桶并准备下一维度，尚未执行查询')
  } catch (failure) { message.warning(errorText(failure)) }
}
function drillBack() {
  if (!drillHistory.value.length || executing.value || metadataLoading.value) return
  spec.value = drillHistory.value.pop()
  message.success('已恢复上一级条件，尚未执行查询')
}
function changePage(page) { spec.value.requireComplete = false; queryPage.value = page; execute() }
function executeCompleteTrend() {
  if (executing.value || !snapshot.value || spec.value.timeBucket === 'NONE' || spec.value.dimensionPropertyIds.length) return
  queryPage.value = 1
  execute({ complete: true })
}
async function execute(options = {}) {
  if (executing.value || metadataLoading.value || !snapshot.value) return
  let request
  try {
    request = payload()
    if (options.complete) {
      Object.assign(spec.value, { limit: 500, sortBy: 'time_bucket', sortDirection: 'ASC', requireComplete: true })
      request = { ...payload(), pageNum: 1, requireComplete: true }
    }
  } catch (failure) { error.value = errorText(failure); return }
  const sequence = ++executionSequence
  const context = contextSequence
  error.value = ''; result.value = null; lastExecutedQuery.value = null; executing.value = true
  try {
    const response = await queryMetric(request)
    if (sequence === executionSequence && context === contextSequence && alive) {
      result.value = response.data
      lastExecutedQuery.value = JSON.parse(JSON.stringify(request))
    }
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
  if (saving.value) return
  if (!saveName.value.trim()) { message.warning('请填写分析名称'); return }
  const context = contextSequence, metadata = metadataSequence, detailSequence = savedDetailSequence, conditions = conditionsSequence
  const asNew = saveAsNew.value, targetId = savedId.value, revision = savedRecord.value?.revision
  const current = () => context === contextSequence && metadata === metadataSequence && detailSequence === savedDetailSequence && conditions === conditionsSequence && alive
  saving.value = true; error.value = ''
  try {
    const data = { ontologyId: props.ontologyId, name: saveName.value.trim(), description: saveDescription.value, query: payload() }
    if (!asNew) { data.id = targetId; data.revision = revision }
    const response = asNew ? await addMetricAnalysis(data) : await updateMetricAnalysis(data)
    if (!current()) return
    const id = asNew ? response.data : targetId
    saveOpen.value = false; message.success('分析条件已保存，未执行查询')
    await loadSavedList()
    if (!current()) return
    savedId.value = id
    await loadSaved(id)
  } catch (failure) { if (current()) error.value = errorText(failure) }
  finally { if (context === contextSequence) saving.value = false }
}
function removeSaved() {
  const id = savedId.value
  const context = contextSequence, detailSequence = savedDetailSequence
  Modal.confirm({ title: '删除当前保存的分析？', content: '仅删除分析条件，不删除指标或数据。', async onOk() {
    if (context !== contextSequence || !alive || detailSequence !== savedDetailSequence || id !== savedId.value) return
    await delMetricAnalysis(id)
    if (context !== contextSequence || !alive) return
    if (detailSequence === savedDetailSequence && id === savedId.value) resetAnalysis()
    loadSavedList()
  } })
}
async function formDecision() {
  if (handoffBusy.value || !result.value || !lastExecutedQuery.value) return
  handoffBusy.value = true
  try { await openManualDecision({ ontologyId: props.ontologyId, query: lastExecutedQuery.value, metricName: result.value.metricName }) }
  catch (failure) { error.value = errorText(failure) }
  finally { handoffBusy.value = false }
}
async function applySelection() {
  const initial = props.initialSelection
  if (!initial?.metricId || !initial?.version) return
  const context = contextSequence
  const selected = metrics.value.find(item => String(item.id) === String(initial.metricId))
  if (!selected) { error.value = '指定指标未发布或不在当前本体中'; return }
  resetAnalysis()
  // resetAnalysis invalidates earlier metadata; pin this exact published version, never silently use latest.
  const metadata = ++metadataSequence
  spec.value.metricId = selected.id; spec.value.version = Number(initial.version)
  metadataLoading.value = true
  try {
    const response = await listMetricVersions(selected.id)
    if (context !== contextSequence || metadata !== metadataSequence || !alive) return
    versions.value = response.data || []
    if (!versions.value.some(item => Number(item.version) === Number(initial.version))) throw new Error('指定发布版本不存在，请返回指标管理核对')
    if (context === contextSequence && metadata === metadataSequence) error.value = ''
    await loadSnapshot(metadata)
    if (context === contextSequence && metadata === metadataSequence &&
        (String(snapshot.value?.id) !== String(selected.id) || Number(snapshot.value?.publishedVersion) !== Number(initial.version) || String(snapshot.value?.ontologyId) !== String(props.ontologyId))) {
      snapshot.value = null
      throw new Error('发布版本与当前指标或本体不一致')
    }
  } catch (failure) { if (context === contextSequence && metadata === metadataSequence) error.value = errorText(failure) }
  finally { if (metadata === metadataSequence) metadataLoading.value = false }
}
async function reload() {
  const context = ++contextSequence
  resetAnalysis(); metrics.value = []; savedRows.value = []; saving.value = false
  metadataLoading.value = true
  try {
    const items = await loadPagedOptions(listPublishedMetric, { ontologyId: props.ontologyId }, { isCurrent: () => alive && context === contextSequence })
    if (context !== contextSequence || !alive) return
    metrics.value = items
  } catch (failure) { if (context === contextSequence) error.value = `已发布指标加载失败：${errorText(failure)}` }
  finally { if (context === contextSequence) metadataLoading.value = false }
  if (context === contextSequence && alive) {
    await applySelection()
    if (context === contextSequence && alive) loadSavedList()
  }
}
watch(() => props.ontologyId, reload, { immediate: true })
watch(() => props.initialSelection, () => { if (metrics.value.length) applySelection() }, { deep: true })
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
