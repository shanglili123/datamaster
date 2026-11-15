<template>
  <a-collapse>
    <a-collapse-panel key="compare" header="两期总量 / 分组 / 时间桶比较（不使用截断TopN推算）">
      <p>沿用当前明确版本、分组和维度筛选，使用第一页及默认或时间桶升序。日/月/年桶仅支持业务零点完整边界、两期桶数一致的序号对齐，表格显示对应基期桶，不推断业务日期等价。任一期截断拒绝计算，缺失不补零；时间只用起止区间约束。</p>
      <a-space wrap>
        <a-select v-model:value="mode" :disabled="busy" style="width: 230px" aria-label="比较区间方式">
          <a-select-option value="PREVIOUS_PERIOD">上一等长区间</a-select-option>
          <a-select-option value="PREVIOUS_MONTH">上一月（日历平移）</a-select-option>
          <a-select-option value="PREVIOUS_YEAR">上一年（日历平移）</a-select-option>
          <a-select-option value="CUSTOM">自定义基期</a-select-option>
        </a-select>
        <a-input v-if="mode === 'CUSTOM'" v-model:value="baselineStart" :disabled="busy" placeholder="基期开始，格式与当前查询一致" style="width: 260px" />
        <a-input v-if="mode === 'CUSTOM'" v-model:value="baselineEnd" :disabled="busy" placeholder="基期结束（不含）" style="width: 260px" />
        <a-button v-hasPermi="['ont:metric:execute']" :loading="busy" :disabled="disabled || busy || !canCompare" @click="execute">执行两期比较</a-button>
      </a-space>
      <p v-if="blockedReason" role="status">{{ blockedReason }}</p>
      <p v-if="error" role="alert">{{ error }}</p>
      <template v-if="result">
        <p>{{ result.notice }}</p>
        <p>本期：[{{ result.currentStartTime }}, {{ result.currentEndTime }})；基期：[{{ result.baselineStartTime }}, {{ result.baselineEndTime }}) · {{ result.current.unit }}</p>
        <a-table :row-key="(_, index) => String(index)" size="small" :pagination="false" :columns="result.columns.map(column => ({ title: column.label, dataIndex: column.key }))" :data-source="result.rows" :scroll="{ x: 'max-content' }">
          <template #bodyCell="{ text, column }">{{ text == null ? '缺失 / 不可计算' : column.dataIndex === 'change_percent' ? `${text}%` : metricDisplayValue(text) }}</template>
        </a-table>
        <p v-if="!result.grouped">差值（本期−基期）：{{ result.difference ?? '不可计算（缺失值）' }}；变化率：{{ result.changePercent == null ? '不可计算（基期为零或缺失）' : `${result.changePercent}%` }}</p>
        <p v-else>以上为各组独立比较，不累计分组值、均值或去重计数来生成全量结论。</p>
      </template>
    </a-collapse-panel>
  </a-collapse>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { compareMetric } from '@/api/ont/metricAnalysis'
import { metricDisplayValue } from '@/utils/metricDisplay'
import { metricComparisonBlockReason, metricCustomBaselineReason } from '@/utils/metricComparison'
const props = defineProps({ query: { type: Object, default: null }, queryError: { type: String, default: '' }, disabled: Boolean })
const mode = ref('PREVIOUS_PERIOD'), baselineStart = ref(''), baselineEnd = ref(''), busy = ref(false), result = ref(null), error = ref('')
let generation = 0
const blockedReason = computed(() => props.queryError || metricComparisonBlockReason(props.query, { period: true })
  || (mode.value === 'CUSTOM' ? metricCustomBaselineReason(props.query, baselineStart.value, baselineEnd.value) : ''))
const canCompare = computed(() => !blockedReason.value)
async function execute() {
  if (!canCompare.value || busy.value || props.disabled) return
  const token = ++generation; busy.value = true; error.value = ''; result.value = null
  try {
    const res = await compareMetric({ query: JSON.parse(JSON.stringify(props.query)), mode: mode.value, baselineStartTime: mode.value === 'CUSTOM' ? baselineStart.value : null, baselineEndTime: mode.value === 'CUSTOM' ? baselineEnd.value : null })
    if (token === generation) result.value = res.data
  } catch (e) { if (token === generation) error.value = e?.message || '两期比较失败，未形成有效结果' }
  finally { if (token === generation) busy.value = false }
}
onBeforeUnmount(() => { ++generation })
watch(() => JSON.stringify([props.query, props.queryError, mode.value, baselineStart.value, baselineEnd.value, props.disabled]), () => {
  ++generation; busy.value = false; result.value = null; error.value = ''
}, { flush: 'sync' })
</script>
