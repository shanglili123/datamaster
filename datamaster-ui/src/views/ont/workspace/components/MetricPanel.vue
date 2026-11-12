<template>
  <div class="metric-panel">
    <AnalysisFlowNavigation stage="metric" :ontology-id="ontologyId" />
    <a-alert type="info" show-icon message="指标定义与版本管理" description="本页管理指标口径和字段映射；发布校验不执行 SQL，不代表数据质量、数据权限或 BI 集成已验证。受控查询请进入指标分析，BI 看板待后续接入。" />
    <a-alert v-if="recentlyPublished" type="success" show-icon :message="`已发布「${recentlyPublished.name}」V${recentlyPublished.publishedVersion}，下一步可核对时间范围后试查。发布不代表查询已验证。`">
      <template #action><a-button v-hasPermi="['ont:metric:execute']" @click="tryMetric(recentlyPublished)">前往试查</a-button></template>
    </a-alert>
    <div class="panel-toolbar">
      <a-input v-model:value="query.name" placeholder="指标名称" allow-clear style="width: 180px" @pressEnter="search" />
      <a-select v-model:value="query.conceptId" :options="conceptOptions" placeholder="全部概念" allow-clear style="width: 180px" @change="search" />
      <a-select v-model:value="query.status" :options="statuses" placeholder="全部状态" allow-clear style="width: 150px" @change="search" />
      <a-button @click="search">查询</a-button>
      <a-button type="primary" v-hasPermi="['ont:metric:add']" @click="openCreate">新增指标</a-button>
    </div>
    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1150 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'concept'">{{ conceptLabel(record.conceptId) }}</template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 'PUBLISHED' ? 'green' : 'default'">{{ statusLabel(record.status) }}</a-tag>
          <a-tag v-if="record.publishedVersion > 0 && record.draftChanged" color="orange">有未发布修改</a-tag>
        </template>
        <template v-else-if="column.key === 'version'">{{ record.publishedVersion ? `V${record.publishedVersion}` : '未发布' }}</template>
        <template v-else-if="column.key === 'action'">
          <a-button v-if="record.status === 'PUBLISHED'" type="link" size="small" v-hasPermi="['ont:metric:execute']" @click="tryMetric(record)">试查/分析</a-button>
          <a-button type="link" size="small" v-hasPermi="['ont:metric:query']" @click="openRead(record)">详情</a-button>
          <a-button type="link" size="small" v-hasPermi="['ont:metric:edit']" @click="openEdit(record)">编辑</a-button>
          <a-button type="link" size="small" v-hasPermi="['ont:metric:edit']" @click="check(record)">校验</a-button>
          <a-button type="link" size="small" v-hasPermi="['ont:metric:publish']" @click="publish(record)">{{ record.status === 'PUBLISHED' ? '重新发布' : '发布' }}</a-button>
          <a-button type="link" size="small" v-if="record.status === 'PUBLISHED'" v-hasPermi="['ont:metric:publish']" @click="disable(record)">停用</a-button>
          <a-button type="link" size="small" v-if="record.publishedVersion > 0" v-hasPermi="['ont:metric:query']" @click="showVersions(record)">版本</a-button>
          <a-button type="link" size="small" danger v-if="!record.publishedVersion" v-hasPermi="['ont:metric:remove']" @click="remove(record)">删除</a-button>
        </template>
      </template>
    </a-table>
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <a-modal v-model:open="editorOpen" :title="form.id ? '编辑指标草稿' : '新增指标草稿'" width="940px" wrap-class-name="ontology-workspace-modal ontology-modal--form" :confirm-loading="saving" destroy-on-close ok-text="保存草稿" cancel-text="取消" @ok="save">
      <a-alert v-if="form.publishedVersion > 0" type="warning" show-icon message="保存仅修改草稿，原发布版本保持不变；重新发布后生成新版本。" style="margin-bottom: 16px" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="所属概念" name="conceptId"><a-select v-model:value="form.conceptId" :options="conceptOptions" :disabled="!!form.id" @change="changeConcept" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="指标编码" name="code"><a-input v-model:value="form.code" :disabled="!!form.id" :maxlength="100" placeholder="例如 paid_amount；创建后不可修改" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="指标名称" name="name"><a-input v-model:value="form.name" :maxlength="100" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="负责人"><a-input v-model:value="form.owner" :maxlength="100" placeholder="发布前必填" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="业务定义" name="description"><a-textarea v-model:value="form.description" :maxlength="2000" :rows="3" placeholder="统计对象、业务含义及适用范围" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="单位"><a-input v-model:value="form.unit" :maxlength="110" placeholder="元 / 人 / 次；发布前必填" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="数据粒度"><a-input v-model:value="form.grain" :maxlength="500" placeholder="一行代表什么，例如 日期 × 机构；发布前必填" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="来源时间偏移（可选）"><a-input v-model:value="form.sourceTimeOffset" :maxlength="6" placeholder="如 +00:00；留空沿用原来源时间" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="业务时间偏移（与来源成对）"><a-input v-model:value="form.businessTimeOffset" :maxlength="6" placeholder="如 +08:00；固定偏移，不自动处理夏令时" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="来源表绑定"><a-select v-model:value="form.conceptTableId" :options="tableOptions" allow-clear placeholder="选择概念已有表绑定；支持 Doris/MySQL/PostgreSQL，消费按空间流程检查" @change="loadMappings" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="聚合方式"><a-select v-model:value="form.aggregation" :options="aggregations" allow-clear /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="度量属性"><a-select v-model:value="form.valuePropertyId" :options="measureOptions" allow-clear :placeholder="form.aggregation === 'COUNT' ? '留空表示 COUNT(*)' : '请选择度量属性'" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="正式派生运算"><a-select v-model:value="form.derivedOperation" :options="derivedOptions" @change="changeDerived" /></a-form-item></a-col>
          <template v-if="form.derivedOperation && form.derivedOperation !== 'NONE'">
            <a-col :span="12"><a-form-item label="第二聚合方式"><a-select v-model:value="form.secondaryAggregation" :options="aggregations" /></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="第二度量属性"><a-select v-model:value="form.secondaryValuePropertyId" :options="secondaryMeasureOptions" allow-clear placeholder="聚合结果须为数值，COUNT/去重计数可选文本标识" /></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="主聚合单位"><a-input v-model:value="form.primaryUnit" :maxlength="50" placeholder="如 元 / 笔" /></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="第二聚合单位"><a-input v-model:value="form.secondaryUnit" :maxlength="50" placeholder="比率同单位时结果单位填1，否则填主单位/次单位" /></a-form-item></a-col>
            <a-col :span="24"><p class="hint">同一来源和筛选内先分别聚合再派生，不是逐行比率再求和。分母零返回缺失；不支持任意表达式、跨表或其他指标实时依赖。相加仅同类SUM/COUNT，单位需一致。</p></a-col>
          </template>
          <a-col :span="12"><a-form-item label="时间属性"><a-select v-model:value="form.timePropertyId" :options="timeOptions" allow-clear placeholder="可选，必须已绑定来源表字段" @change="value => { if (!value) { form.sourceTimeOffset = null; form.businessTimeOffset = null } }" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="允许分析的维度"><a-select v-model:value="form.dimensionPropertyIds" mode="multiple" :options="propertyOptions" placeholder="选择已映射属性" /></a-form-item></a-col>
          <a-col :span="24"><p class="hint">固定过滤中的日期时间须严格ISO格式（例如2026-01-01T00:00:00）；这是物理来源字段时间，不随业务偏移自动平移。</p></a-col>
        </a-row>
        <div class="filter-heading"><strong>固定口径过滤</strong><a-button size="small" @click="form.fixedFilters.push({ propertyId: undefined, operator: 'EQ', value: '' })">添加条件</a-button></div>
        <p class="hint">条件之间为 AND。条件值按属性类型转换；日期使用 YYYY-MM-DD，布尔值使用 true/false。IN 使用 JSON 数组，例如 [1,2] 或 ["已支付","已完成"]。这不是权限过滤。</p>
        <div v-for="(filter, index) in form.fixedFilters" :key="index" class="filter-row">
          <a-select v-model:value="filter.propertyId" :options="propertyOptions" placeholder="属性" style="flex: 1" />
          <a-select v-model:value="filter.operator" :options="operators" style="width: 140px" @change="filter.value = ''" />
          <a-input v-if="!nullOperator(filter.operator)" v-model:value="filter.value" placeholder="条件值" style="flex: 1" />
          <span v-else class="hint" style="flex: 1">无需条件值</span>
          <a-button danger @click="form.fixedFilters.splice(index, 1)">移除</a-button>
        </div>
        <a-alert v-if="optionsError" type="error" show-icon :message="optionsError" />
        <a-alert v-if="saveError" type="error" show-icon :message="saveError" />
      </a-form>
    </a-modal>

    <a-modal v-model:open="versionsOpen" title="已发布指标版本" width="660px" :footer="null" wrap-class-name="ontology-workspace-modal ontology-modal--compact">
      <a-table :data-source="versions" :loading="versionsLoading" row-key="id" :pagination="false" :columns="versionColumns">
        <template #bodyCell="{ column, record }"><template v-if="column.key === 'action'"><a-button type="link" @click="readVersion(record.version)">查看快照 V{{ record.version }}</a-button><a-button type="link" v-hasPermi="['ont:metric:edit']" @click="restoreVersion(record.version)">复制为草稿</a-button></template></template>
      </a-table>
    </a-modal>
    <a-modal v-model:open="detailOpen" :title="detailTitle" width="900px" :footer="null" wrap-class-name="ontology-workspace-modal ontology-modal--form">
      <a-descriptions v-if="detail" bordered :column="2" size="small">
        <a-descriptions-item label="指标">{{ detail.name }} / {{ detail.code }}</a-descriptions-item>
        <a-descriptions-item label="聚合">{{ detail.aggregation || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="单位">{{ detail.unit || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="负责人">{{ detail.owner || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="业务定义" :span="2">{{ detail.description }}</a-descriptions-item>
        <a-descriptions-item label="粒度" :span="2">{{ detail.grain || '未设置' }}</a-descriptions-item>
      </a-descriptions>
      <p class="hint">以下为完整定义；历史版本包含发布时冻结的数据源、表和属性字段映射，不随当前绑定修改。</p>
      <pre class="snapshot">{{ JSON.stringify(detail, null, 2) }}</pre>
    </a-modal>
  </div>
</template>

<script setup name="MetricPanel">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { loadPagedOptions } from '@/api/ont/metricOptions'
import { listConcept } from '@/api/ont/concept'
import { listConceptTable } from '@/api/ont/conceptTable'
import { listProperty } from '@/api/ont/property'
import { listPropertyColumn } from '@/api/ont/propertyColumn'
import AnalysisFlowNavigation from '@/components/AnalysisFlowNavigation.vue'
import { useAnalysisNavigation } from '@/utils/analysisNavigation'
import { addMetric, updateMetric, getMetric, listMetric, delMetric, validateMetric, publishMetric, disableMetric, listMetricVersions, getMetricVersion, restoreMetricDraft } from '@/api/ont/metric'

const props = defineProps({ ontologyId: { type: [String, Number], required: true } })
const { openMetricAnalysis } = useAnalysisNavigation()
const recentlyPublished = ref(null)
function tryMetric(row) { return openMetricAnalysis({ ontologyId: props.ontologyId, metricId: row.id, version: row.publishedVersion }) }
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const concepts = ref([])
const query = ref({ pageNum: 1, pageSize: 10 })
const editorOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const properties = ref([])
const tables = ref([])
const mappings = ref([])
const optionsError = ref('')
const saveError = ref('')
const versionsOpen = ref(false)
const versionsLoading = ref(false)
const versions = ref([])
const versionMetricId = ref()
const detailOpen = ref(false)
const detail = ref(null)
const detailTitle = ref('指标定义')
let listSequence = 0
let optionsSequence = 0
let contextSequence = 0
const statuses = [{ value: 'DRAFT', label: '草稿' }, { value: 'PUBLISHED', label: '已发布' }, { value: 'DISABLED', label: '已停用' }]
const derivedOptions = [{ value: 'NONE', label: '无（单聚合）' }, { value: 'RATIO', label: '主聚合 / 第二聚合' }, { value: 'DIFFERENCE', label: '主聚合 − 第二聚合' }, { value: 'SUM', label: '两聚合相加' }]
function changeDerived(value) {
  if (!value || value === 'NONE') Object.assign(form.value, { secondaryAggregation: null, secondaryValuePropertyId: null, primaryUnit: null, secondaryUnit: null })
}
const aggregations = ['SUM', 'COUNT', 'COUNT_DISTINCT', 'AVG', 'MIN', 'MAX'].map(value => ({ value, label: value }))
const operators = ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'IN', 'IS_NULL', 'IS_NOT_NULL'].map(value => ({ value, label: value }))
const columns = [
  { title: '指标名称', dataIndex: 'name', width: 160 }, { title: '编码', dataIndex: 'code', width: 160 },
  { title: '所属概念', key: 'concept', width: 130 }, { title: '聚合', dataIndex: 'aggregation', width: 100 },
  { title: '状态', key: 'status', width: 200 }, { title: '发布版本', key: 'version', width: 100 },
  { title: '操作', key: 'action', width: 360, fixed: 'right' }
]
const versionColumns = [{ title: '版本', dataIndex: 'version' }, { title: '发布人', dataIndex: 'createBy' }, { title: '发布时间', dataIndex: 'createTime' }, { title: '操作', key: 'action' }]
const rules = {
  conceptId: [{ required: true, message: '请选择所属概念' }],
  code: [{ required: true, pattern: /^[a-z][a-z0-9_]*$/, message: '编码须以小写字母开头，仅含小写字母、数字和下划线' }],
  name: [{ required: true, whitespace: true, message: '请输入指标名称' }],
  description: [{ required: true, whitespace: true, message: '请输入业务定义' }]
}
const conceptOptions = computed(() => concepts.value.map(item => ({ value: item.id, label: item.name })))
const tableOptions = computed(() => tables.value.map(item => ({ value: item.id, label: [item.databaseName, item.schemaName, item.tableName].filter(Boolean).join('.') })))
const boundProperties = computed(() => properties.value.filter(item => mappings.value.some(binding => String(binding.propertyId) === String(item.id))))
const propertyOptions = computed(() => boundProperties.value.map(item => ({ value: item.id, label: `${item.name} (${item.dataType})` })))
const numericPropertyOptions = computed(() => propertyOptions.value.filter(item => ['integer', 'decimal', 'number', 'long', 'int', 'double', 'float', 'numeric', 'bigint', 'smallint'].includes(boundProperties.value.find(prop => String(prop.id) === String(item.value))?.dataType?.toLowerCase())))
const measureOptions = computed(() => ['SUM', 'AVG'].includes(form.value.aggregation)
  || form.value.derivedOperation && form.value.derivedOperation !== 'NONE' && !['COUNT', 'COUNT_DISTINCT'].includes(form.value.aggregation)
  ? numericPropertyOptions.value
  : propertyOptions.value)
const secondaryMeasureOptions = computed(() => ['COUNT', 'COUNT_DISTINCT'].includes(form.value.secondaryAggregation) ? propertyOptions.value : numericPropertyOptions.value)
const timeOptions = computed(() => propertyOptions.value.filter(item => ['date', 'datetime', 'timestamp'].includes(boundProperties.value.find(prop => String(prop.id) === String(item.value))?.dataType?.toLowerCase())))
const nullOperator = operator => ['IS_NULL', 'IS_NOT_NULL'].includes(operator)
const conceptLabel = id => concepts.value.find(item => String(item.id) === String(id))?.name || id
const statusLabel = value => statuses.find(item => item.value === value)?.label || value
const errorText = error => error?.response?.data?.msg || error?.message || '操作失败，请重试'

async function loadList() {
  const sequence = ++listSequence
  loading.value = true
  try {
    const response = await listMetric({ ...query.value, ontologyId: props.ontologyId })
    if (sequence !== listSequence) return
    rows.value = response.data?.rows || []
    total.value = response.data?.total || 0
  } catch (error) {
    if (sequence === listSequence) { rows.value = []; total.value = 0 }
  } finally {
    if (sequence === listSequence) loading.value = false
  }
}
function search() { query.value.pageNum = 1; loadList() }
async function reload() {
  const sequence = ++contextSequence
  recentlyPublished.value = null; saving.value = false
  concepts.value = []
  try {
    const items = await loadPagedOptions(listConcept, { ontologyId: props.ontologyId })
    if (sequence !== contextSequence) return
    concepts.value = items
  } catch (error) {
    if (sequence !== contextSequence) return
  }
  if (sequence === contextSequence) loadList()
}
async function loadOptions() {
  const sequence = ++optionsSequence
  properties.value = []; tables.value = []; mappings.value = []; optionsError.value = ''
  if (!form.value.conceptId) return
  try {
    const [propertyResponse, tableResponse] = await Promise.all([
      loadPagedOptions(listProperty, { conceptId: form.value.conceptId }),
      listConceptTable({ conceptId: form.value.conceptId })
    ])
    if (sequence !== optionsSequence) return
    properties.value = propertyResponse
    tables.value = tableResponse.data || []
    if (form.value.conceptTableId) {
      const response = await listPropertyColumn(form.value.conceptTableId)
      if (sequence === optionsSequence) mappings.value = response.data || []
    }
  } catch (error) {
    if (sequence === optionsSequence) optionsError.value = '字段或表绑定加载失败，请检查查询权限后重新打开编辑器。'
  }
}
async function loadMappings() {
  const sequence = ++optionsSequence
  mappings.value = []; optionsError.value = ''
  if (!form.value.conceptTableId) return
  try {
    const response = await listPropertyColumn(form.value.conceptTableId)
    if (sequence === optionsSequence) mappings.value = response.data || []
  } catch (error) {
    if (sequence === optionsSequence) optionsError.value = '字段映射加载失败，请重新选择表绑定。'
  }
}
function changeConcept() {
  Object.assign(form.value, { conceptTableId: undefined, valuePropertyId: undefined, timePropertyId: undefined, sourceTimeOffset: null, businessTimeOffset: null, derivedOperation: 'NONE', secondaryAggregation: null, secondaryValuePropertyId: null, primaryUnit: null, secondaryUnit: null, dimensionPropertyIds: [], fixedFilters: [] })
  loadOptions()
}
function openCreate() {
  if (!concepts.value.length) { message.warning('请先创建业务概念'); return }
  form.value = { ontologyId: props.ontologyId, conceptId: concepts.value[0].id, code: '', name: '', description: '', owner: '', unit: '', grain: '', aggregation: 'COUNT', derivedOperation: 'NONE', dimensionPropertyIds: [], fixedFilters: [] }
  saveError.value = ''
  editorOpen.value = true
  loadOptions()
}
async function openEdit(row) {
  const context = contextSequence
  const response = await getMetric(row.id)
  if (context !== contextSequence) return
  form.value = { ...response.data, derivedOperation: response.data.derivedOperation || 'NONE', dimensionPropertyIds: response.data.dimensionPropertyIds || [], fixedFilters: (response.data.fixedFilters || []).map(filter => ({ ...filter, value: filter.operator === 'IN' ? JSON.stringify(filter.value) : filter.value == null ? '' : String(filter.value) })) }
  saveError.value = ''
  editorOpen.value = true
  loadOptions()
}
function normalizeFilters(filters) {
  return filters.map(filter => {
    if (!filter.propertyId) throw new Error('请选择固定过滤的属性')
    if (nullOperator(filter.operator)) return { propertyId: filter.propertyId, operator: filter.operator, value: null }
    const property = properties.value.find(item => String(item.id) === String(filter.propertyId))
    if (!property) throw new Error('固定过滤属性已失效，请重新选择')
    const convert = value => {
      if (value == null || typeof value === 'object') throw new Error('过滤值不能为 null、对象或嵌套数组')
      if (['integer', 'decimal'].includes(property.dataType)) {
        const text = String(value).trim()
        if (typeof value === 'number' && (!Number.isSafeInteger(value) || !Number.isInteger(value))) {
          throw new Error('IN 数组中的大整数和小数请加双引号，避免 JSON 数值解析丢失精度')
        }
        if (!text || !Number.isFinite(Number(text))) throw new Error('数值属性的过滤条件必须是有效数字')
        // 数值保留文本，避免大整数或高精度小数经过 JavaScript Number 丢失精度。
        return text
      }
      if (property.dataType === 'boolean') {
        if (![true, false, 'true', 'false'].includes(value)) throw new Error('布尔属性的过滤值只能为 true 或 false')
        return value === true || value === 'true'
      }
      return String(value)
    }
    let value
    if (filter.operator === 'IN') {
      let items
      try { items = JSON.parse(filter.value) } catch (error) { throw new Error('IN 条件请输入合法 JSON 数组') }
      if (!Array.isArray(items) || !items.length) throw new Error('IN 条件必须是非空数组')
      value = items.map(convert)
    } else {
      if (filter.value === '') throw new Error('请填写固定过滤值')
      value = convert(filter.value)
    }
    return { propertyId: filter.propertyId, operator: filter.operator, value }
  })
}
async function save() {
  const context = contextSequence
  try { await formRef.value.validate() } catch (error) { return }
  if (context !== contextSequence) return
  saving.value = true; saveError.value = ''
  try {
    const data = { ...form.value, derivedOperation: form.value.derivedOperation === 'NONE' ? null : form.value.derivedOperation, sourceTimeOffset: form.value.sourceTimeOffset?.trim() || null, businessTimeOffset: form.value.businessTimeOffset?.trim() || null, conceptTableId: form.value.conceptTableId ?? null, valuePropertyId: form.value.valuePropertyId ?? null, timePropertyId: form.value.timePropertyId ?? null, fixedFilters: normalizeFilters(form.value.fixedFilters) }
    if (data.id) await updateMetric(data)
    else await addMetric(data)
    if (context !== contextSequence) return
    editorOpen.value = false
    message.success('指标草稿已保存')
    loadList()
  } catch (error) { if (context === contextSequence) saveError.value = errorText(error) }
  finally { if (context === contextSequence) saving.value = false }
}
async function check(row) {
  await validateMetric(row.id, row.revision)
  message.success('元数据与指标定义校验通过；未执行数据查询')
}
function publish(row) {
  const context = contextSequence
  Modal.confirm({ title: `发布指标「${row.name}」？`, content: '发布将冻结当前口径和字段映射并生成版本。此操作不执行 SQL，也不代表数据质量、数据权限已验证。', async onOk() {
    if (context !== contextSequence) return
    await publishMetric(row.id, row.revision)
    if (context !== contextSequence) return
    message.success('指标已发布')
    loadList()
    try {
      const published = (await getMetric(row.id)).data
      if (context !== contextSequence) return
      recentlyPublished.value = published?.status === 'PUBLISHED' ? published : null
    } catch { if (context === contextSequence) message.warning('发布成功，但详情暂不可读取；请从指标列表选择试查') }
  } })
}
function disable(row) {
  const context = contextSequence
  Modal.confirm({ title: `停用指标「${row.name}」？`, content: '保留历史版本；后续重新发布将生成新版本。', async onOk() {
    if (context !== contextSequence) return
    await disableMetric(row.id, row.revision)
    if (context === contextSequence) { message.success('指标已停用'); loadList() }
  } })
}
function remove(row) {
  const context = contextSequence
  Modal.confirm({ title: `删除草稿「${row.name}」？`, content: '仅允许删除从未发布的指标。', async onOk() {
    if (context !== contextSequence) return
    await delMetric(row.id)
    if (context === contextSequence) loadList()
  } })
}
async function openRead(row) {
  const context = contextSequence
  const response = await getMetric(row.id)
  if (context !== contextSequence) return
  detail.value = response.data; detailTitle.value = '当前指标定义'; detailOpen.value = true
}
async function showVersions(row) {
  versionMetricId.value = row.id; versions.value = []; versionsOpen.value = true; versionsLoading.value = true
  const context = contextSequence
  try {
    const response = await listMetricVersions(row.id)
    if (context === contextSequence && versionMetricId.value === row.id) versions.value = response.data || []
  } finally { if (context === contextSequence) versionsLoading.value = false }
}
async function restoreVersion(version) {
  const id = versionMetricId.value, context = contextSequence
  try {
    const current = (await getMetric(id)).data
    if (context !== contextSequence || id !== versionMetricId.value) return
    Modal.confirm({ title: `复制历史 V${version} 为当前草稿？`, content: '覆盖当前已保存草稿，保留线上发布版本与停用状态。只复制业务口径、固定偏移和派生定义，不复制来源权限；重新发布时重新校验并冻结当前来源。', async onOk() {
      if (context !== contextSequence || id !== versionMetricId.value) return
      await restoreMetricDraft(id, version, current.revision)
      if (context === contextSequence) { message.success('已恢复到草稿，线上规则未改变'); await loadList() }
    } })
  } catch (failure) { if (context === contextSequence) message.error(errorText(failure)) }
}
async function readVersion(version) {
  const id = versionMetricId.value
  const context = contextSequence
  const response = await getMetricVersion(id, version)
  if (context !== contextSequence || id !== versionMetricId.value) return
  detail.value = response.data; detailTitle.value = `指标发布快照 V${version}`; detailOpen.value = true
}
watch(() => props.ontologyId, () => {
  ++listSequence; ++optionsSequence
  editorOpen.value = false; versionsOpen.value = false; detailOpen.value = false
  query.value = { pageNum: 1, pageSize: 10 }; rows.value = []; total.value = 0
  reload()
}, { immediate: true })
onBeforeUnmount(() => { ++listSequence; ++optionsSequence; ++contextSequence })
defineExpose({ reload })
</script>

<style lang="scss" scoped>
.metric-panel { display: flex; flex-direction: column; gap: 16px; }
.panel-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.filter-heading, .filter-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.filter-heading { justify-content: space-between; }
.hint { color: #606b7b; font-size: 13px; line-height: 1.6; }
.snapshot { max-height: 420px; overflow: auto; padding: 16px; background: #f5f7fa; white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
