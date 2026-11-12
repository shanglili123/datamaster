<template>
  <a-collapse>
    <a-collapse-panel key="multi" header="同源多指标比较与临时派生">
      <p>复用当前查询的分组、时间区间和用户筛选；可比较 2–4 项指标，每项须选择明确版本。不接受独立 TopN 或分页拼接，若分组超过上限须缩小范围。各指标固定过滤分别保留；是否可比较由后端核验。</p>
      <a-space v-for="(selection, index) in selections" :key="selection.key" wrap class="metric-selection">
        <span>第 {{ index + 2 }} 项</span>
        <a-select v-model:value="selection.metricId" :options="availableOptions(selection)" placeholder="选择指标" style="width: 240px" :disabled="busy" @change="loadVersions(selection)" />
        <a-select v-model:value="selection.version" :options="selection.versions.map(item => ({ value: item.version, label: `V${item.version}` }))" placeholder="明确版本" style="width: 120px" :disabled="busy || selection.loading" />
        <a-button v-if="selections.length > 1" size="small" :disabled="busy" @click="removeSelection(selection)">移除</a-button>
      </a-space>
      <a-space wrap>
        <a-button :disabled="busy || selections.length >= 3" @click="addSelection">添加指标（最多四项）</a-button>
        <a-select v-model:value="operation" style="width: 190px" :disabled="busy || selections.length !== 1">
          <a-select-option value="NONE">并列比较</a-select-option><a-select-option value="RATIO">本指标 / 第二指标</a-select-option>
          <a-select-option value="DIFFERENCE">本指标 − 第二指标</a-select-option><a-select-option value="SUM">两指标相加</a-select-option>
        </a-select>
        <a-button v-hasPermi="['ont:metric:execute']" :loading="busy" :disabled="disabled || busy || !query || !complete" @click="execute">查询并比较</a-button>
      </a-space>
      <p v-if="selections.length > 1">三、四项仅支持并列比较；临时算术须保留两项指标。</p>
      <p v-for="selection in selections.filter(item => item.error)" :key="`error:${selection.key}`" role="alert">{{ selection.error }}</p>
      <p v-if="error" role="alert">{{ error }}</p>
      <template v-if="result">
        <p v-for="metric in result.metrics" :key="`${metric.metricId}:${metric.version}`">{{ metric.metricName }} · V{{ metric.version }} · {{ metric.unit }} · {{ metric.executedAt }}</p>
        <p>{{ result.notice }}</p>
        <a-table :columns="result.columns.map(column => ({ title: column.label, dataIndex: column.key }))" :data-source="result.rows" :row-key="(_, index) => String(index)" :pagination="false" size="small" :scroll="{ x: 'max-content' }">
          <template #bodyCell="{ text }">{{ text == null ? '缺失' : text }}</template>
        </a-table>
      </template>
    </a-collapse-panel>
  </a-collapse>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { listMetricVersions } from '@/api/ont/metric'
import { queryMultipleMetrics } from '@/api/ont/metricAnalysis'
const props = defineProps({ query: Object, options: { type: Array, default: () => [] }, disabled: Boolean })
let nextKey = 0, generation = 0, disposed = false
const createSelection = () => ({ key: ++nextKey, metricId: undefined, version: undefined, versions: [], loading: false, error: '', metadataGeneration: 0 })
const selections = ref([createSelection()]), operation = ref('NONE'), busy = ref(false), result = ref(null), error = ref('')
const complete = computed(() => selections.value.every(item => !item.loading && !item.error && item.metricId && item.version && item.versions.some(version => version.version === item.version)))
function availableOptions(selection) {
  const occupied = new Set([String(props.query?.metricId), ...selections.value.filter(item => item.key !== selection.key).map(item => String(item.metricId))])
  return props.options.filter(item => !occupied.has(String(item.value)))
}
function addSelection() {
  if (busy.value || selections.value.length >= 3) return
  operation.value = 'NONE'
  selections.value.push(createSelection())
}
function removeSelection(selection) {
  if (busy.value || selections.value.length <= 1) return
  ++selection.metadataGeneration
  selections.value = selections.value.filter(item => item.key !== selection.key)
}
async function loadVersions(selection) {
  const token = ++selection.metadataGeneration, selectedId = selection.metricId
  selection.version = undefined; selection.versions = []; selection.loading = true; selection.error = ''
  if (!selectedId) { selection.loading = false; return }
  const current = () => !disposed && selections.value.some(item => item.key === selection.key) && token === selection.metadataGeneration && selectedId === selection.metricId
  try {
    const res = await listMetricVersions(selectedId)
    if (current()) selection.versions = res.data || []
  } catch (e) {
    if (current()) selection.error = e?.message || '版本加载失败，请重新选择指标'
  } finally { if (current()) selection.loading = false }
}
async function execute() {
  if (!props.query || busy.value || props.disabled || !complete.value) return
  const identities = [String(props.query.metricId), ...selections.value.map(item => String(item.metricId))]
  if (new Set(identities).size !== identities.length) { error.value = '请勿重复选择指标'; return }
  const token = ++generation; busy.value = true; result.value = null; error.value = ''
  try {
    const first = JSON.parse(JSON.stringify(props.query))
    const queries = [first, ...selections.value.map(item => ({ ...first, metricId: item.metricId, version: item.version }))]
    const res = await queryMultipleMetrics({ queries, derivedOperation: selections.value.length === 1 ? operation.value : 'NONE' })
    if (token === generation) result.value = res.data
  } catch (e) { if (token === generation) error.value = e?.message || '多指标比较失败，未形成有效结果' }
  finally { if (token === generation) busy.value = false }
}
onBeforeUnmount(() => { disposed = true; ++generation; selections.value.forEach(item => { ++item.metadataGeneration }) })
watch(() => JSON.stringify([props.query, selections.value.map(item => [item.key, item.metricId, item.version]), operation.value, props.disabled]), () => { ++generation; busy.value = false; result.value = null; error.value = '' }, { flush: 'sync' })
</script>
<style scoped>
.metric-selection { display: flex; margin-bottom: 8px; }
</style>
