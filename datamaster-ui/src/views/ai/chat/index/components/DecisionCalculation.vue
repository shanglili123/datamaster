<template>
  <details class="calculation-panel">
    <summary>标准测算：成本、预算与需求缺口</summary>
    <p>公式：增量成本＝Σ投入数量×单价＋一次固定费；新增供给＝Σ投入数量×单位产出；缺口＝max(需求－已有供给－新增供给, 0)。不证明交期或设备可行。</p>
    <label>对象范围 <input v-model="form.scope" :disabled="disabled || busy" placeholder="同一批订单/业务对象" /></label>
    <label>统计窗口 <input v-model="form.period" :disabled="disabled || busy" placeholder="明确日期、时区或生产窗口" /></label>
    <label>币种 <input v-model="form.currency" :disabled="disabled || busy" placeholder="例如 CNY" /></label>
    <label>产出数量单位 <input v-model="form.quantityUnit" :disabled="disabled || busy" placeholder="例如 件" /></label>
    <div class="field"><strong>需求数量</strong><DecisionMeasureInput v-model="form.demand" :unit="form.quantityUnit" :cells="cells" :disabled="disabled || busy" /></div>
    <div class="field"><strong>已有供给</strong><DecisionMeasureInput v-model="form.existingSupply" :unit="form.quantityUnit" :cells="cells" :disabled="disabled || busy" /></div>
    <label><input v-model="form.noBudgetLimit" type="checkbox" :disabled="disabled || busy" />明确不设预算限制（不是漏填）</label>
    <div v-if="!form.noBudgetLimit" class="field"><strong>增量成本预算</strong><DecisionMeasureInput v-model="form.budget" :unit="form.currency" :cells="cells" :disabled="disabled || busy" /></div>
    <label>比较目标 <select v-model="form.strategy" :disabled="disabled || busy"><option value="MEET_DEMAND_MIN_COST">预算内满足需求，增量成本最低</option><option value="MIN_GAP_THEN_COST">预算内缺口最小，再比较成本</option></select></label>
    <article v-for="item in candidateForms" :key="item.candidateId" class="candidate-input">
      <label><input v-model="item.enabled" type="checkbox" :disabled="disabled || busy" />测算 {{ item.name }}</label>
      <template v-if="item.enabled">
        <label><input v-model="item.alignExecutionQuantities" type="checkbox" :disabled="disabled || busy" />明确同步到映射的已有数值执行参数（不新增字段，之后仍需重新预览确认）</label>
        <div class="field"><strong>一次固定费（无固定费请明确填0）</strong><DecisionMeasureInput v-model="item.fixedCost" :unit="form.currency" :cells="cells" :disabled="disabled || busy" /></div>
        <div v-for="(line, index) in item.lines" :key="index" class="line-input">
          <label>投入名称 <input v-model="line.name" :disabled="disabled || busy" /></label>
          <label>投入单位 <input v-model="line.inputUnit" :disabled="disabled || busy" placeholder="件/小时等" /></label>
          <label><input v-model="line.fractionalQuantity" type="checkbox" :disabled="disabled || busy" />允许小数数量（件/台仍限制整数）</label>
          <div class="field"><strong>投入数量</strong><DecisionMeasureInput v-model="line.quantity" :unit="line.inputUnit" :cells="cells" :disabled="disabled || busy" /></div>
          <div class="field"><strong>单价</strong><DecisionMeasureInput v-model="line.unitPrice" :unit="`${form.currency}/${line.inputUnit}`" :cells="cells" :disabled="disabled || busy" /></div>
          <div class="field"><strong>每单位投入的新增产出</strong><DecisionMeasureInput v-model="line.outputPerUnit" :unit="`${form.quantityUnit}/${line.inputUnit}`" :cells="cells" :disabled="disabled || busy" /></div>
          <label>执行数量参数 <input v-model="line.quantityParameter" :disabled="disabled || busy" placeholder="已有动作顶层参数，例如 quantity；留空仅比较" /></label>
          <button type="button" :disabled="disabled || busy || item.lines.length === 1" @click="item.lines.splice(index, 1)">删除投入</button>
        </div>
        <button type="button" :disabled="disabled || busy || item.lines.length >= 10" @click="item.lines.push(newLine())">增加投入</button>
      </template>
    </article>
    <p>正式指标必须发布单位一致且未截断；人工参数是经确认的假设。所有候选按同一范围、窗口和单位比较，不自动换算。不干预基线由共同需求/供给自动测算。</p>
    <a-button :loading="busy" :disabled="disabled || busy || !candidateForms.some(item => item.enabled)" @click="submit">核对来源并确认测算（不执行）</a-button>
    <p v-if="error" role="alert">{{ error }}</p>
    <section v-if="proposal.comparison">
      <h4>服务端计算结果：{{ proposal.comparison.status }}</h4>
      <p>{{ proposal.comparison.summary }}</p>
      <p>条件性推荐：{{ proposal.comparison.recommendedIds?.join('、') || '无' }}；未参与排名：{{ proposal.comparison.excludedIds?.join('、') || '无' }}</p>
      <table><thead><tr><th>候选</th><th>增量成本</th><th>新增供给</th><th>剩余缺口</th><th>覆盖率%</th><th>预算</th><th>名次</th></tr></thead>
        <tbody><tr v-for="candidate in proposal.candidates || []" :key="candidate.id"><td>{{ candidate.name }}</td><td>{{ candidate.calculation?.totalCost ?? '未知' }}</td><td>{{ candidate.calculation?.additionalSupply ?? '未知' }}</td><td>{{ candidate.calculation?.remainingGap ?? '未知' }}</td><td>{{ candidate.calculation?.coveragePercent ?? '未知' }}</td><td>{{ candidate.calculation?.budgetStatus || '未测算' }}</td><td>{{ candidate.calculation?.rank ?? '不参与' }}</td></tr></tbody>
      </table>
      <details v-for="candidate in proposal.candidates || []" :key="`${candidate.id}:detail`"><summary>{{ candidate.name }}：{{ candidate.calculation?.status || '未测算' }}，公式、来源与限制</summary><pre>{{ JSON.stringify(candidate.calculation || {}, null, 2) }}</pre></details>
    </section>
  </details>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { calculateDecision } from '@/api/ai/decision'
import DecisionMeasureInput from './DecisionMeasureInput.vue'
const props = defineProps({ proposal: { type: Object, required: true }, disabled: { type: Boolean, default: false }, contextKey: { type: String, required: true } })
const emit = defineEmits(['updated'])
const measure = () => ({ source: 'USER', value: '' })
const newLine = () => ({ name: '', inputUnit: '', fractionalQuantity: false, quantity: measure(), unitPrice: measure(), outputPerUnit: measure(), quantityParameter: '' })
const form = reactive({ scope: '', period: '', currency: '', quantityUnit: '', strategy: 'MEET_DEMAND_MIN_COST', noBudgetLimit: false, demand: measure(), existingSupply: measure(), budget: measure() })
const candidateForms = ref([])
const busy = ref(false)
const error = ref('')
let generation = 0
const cells = computed(() => {
  const result = []
  for (const evidence of props.proposal.evidence || []) {
    if (evidence.verification !== 'VERIFIED_METRIC') continue
    for (const [toolIndex, tool] of (evidence.details?.queryEvidence || []).entries()) {
      const data = tool.data
      if (tool.toolName !== 'execute_metric_analysis' || tool.status !== 'SUCCEEDED' || data?.truncated !== false) continue
      for (const [rowIndex, row] of (data.rows || []).entries()) {
        if (typeof row?.metric_value !== 'string') continue
        const dimensions = Object.fromEntries(Object.entries(row).filter(([key]) => key !== 'metric_value'))
        result.push({ key: `${evidence.id}:${toolIndex}:${rowIndex}`, evidenceId: evidence.id, toolIndex, rowIndex, label: `${evidence.id} ${data.metricName || data.metricId} 第${rowIndex + 1}行 ${JSON.stringify(dimensions)}：${row.metric_value} ${data.unit || '单位未定义'}；${data.startTime || ''}~${data.endTime || ''}` })
      }
    }
  }
  return result
})
watch(() => [props.proposal.id, props.proposal.revision, props.contextKey], () => {
  ++generation; busy.value = false; error.value = ''
  const saved = props.proposal.calculationContext
  Object.assign(form, { scope: '', period: '', currency: '', quantityUnit: '', strategy: 'MEET_DEMAND_MIN_COST', noBudgetLimit: false, demand: measure(), existingSupply: measure(), budget: measure() }, saved ? JSON.parse(JSON.stringify(saved)) : {})
  if (!form.budget) form.budget = measure()
  candidateForms.value = (props.proposal.candidates || []).filter(item => !item.baseline).map(item => ({ candidateId: item.id, name: item.name, enabled: Boolean(item.calculationSpec), fixedCost: measure(), lines: [newLine()], ...(item.calculationSpec ? JSON.parse(JSON.stringify(item.calculationSpec)) : {}) }))
}, { immediate: true })
function withUnit(value, unit) { return { ...value, unit } }
async function submit() {
  if (busy.value || props.disabled) return
  const token = generation
  const id = props.proposal.id
  const revision = props.proposal.revision
  const context = { ...JSON.parse(JSON.stringify(form)), demand: withUnit(form.demand, form.quantityUnit), existingSupply: withUnit(form.existingSupply, form.quantityUnit), budget: form.noBudgetLimit ? null : withUnit(form.budget, form.currency) }
  const candidates = candidateForms.value.filter(item => item.enabled).map(item => ({ candidateId: item.candidateId, alignExecutionQuantities: Boolean(item.alignExecutionQuantities), fixedCost: withUnit(item.fixedCost, form.currency), lines: item.lines.map(line => ({ ...line, quantity: withUnit(line.quantity, line.inputUnit), unitPrice: withUnit(line.unitPrice, `${form.currency}/${line.inputUnit}`), outputPerUnit: withUnit(line.outputPerUnit, `${form.quantityUnit}/${line.inputUnit}`) })) }))
  busy.value = true; error.value = ''
  try {
    await ElMessageBox.confirm('请确认对象范围、统计窗口、单位、指标单元格映射与人工假设准确；固定费每方案一次，预算比较干预增量成本。确认测算将创建新版本并作废旧执行预览。若勾选同步，会更新映射的已有数值参数；仍不会提交执行。', '确认测算输入与来源', { type: 'warning', closeOnClickModal: false, confirmButtonText: '确认并测算', cancelButtonText: '取消' })
    if (token !== generation || props.disabled) return
    const response = await calculateDecision({ decisionId: id, revision, confirmedInputs: true, context, candidates })
    if (token === generation && id === props.proposal.id) emit('updated', response.data)
  } catch (failure) {
    if (token === generation && failure !== 'cancel' && failure !== 'close') error.value = failure?.message || '测算未完成，请重新加载提案核对版本'
  } finally { if (token === generation) busy.value = false }
}
onBeforeUnmount(() => { ++generation })
</script>

<style scoped>
.calculation-panel { margin: 16px 0; border: 1px solid #d9dfe8; padding: 12px; border-radius: 6px; }
label, .field { display: block; margin: 8px 0; }
input:not([type=checkbox]), select { border: 1px solid #d9dfe8; padding: 5px; border-radius: 4px; max-width: 100%; }
.candidate-input, .line-input { border-top: 1px solid #e5e7eb; margin-top: 12px; padding-top: 8px; }
table { width: 100%; border-collapse: collapse; font-size: 12px; }
th, td { border: 1px solid #d9dfe8; padding: 6px; text-align: left; }
pre { white-space: pre-wrap; max-height: 240px; overflow: auto; }
summary { cursor: pointer; }
</style>
