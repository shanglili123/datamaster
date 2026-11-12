<template>
  <details class="effect-plan">
    <summary>执行后效果观察计划（提交前配置，可选）</summary>
    <p>选择本次已核验的无分组单值指标作为基线。仅改变观察日期，版本与过滤冻结；两期必须等长。变化达标不是因果收益证明。</p>
    <fieldset :disabled="disabled || busy">
      <label>基线指标<select v-model="form.baselineKey"><option value="">不配置效果计划</option><option v-for="cell in cells" :key="cell.key" :value="cell.key">{{ cell.label }}</option></select></label>
      <label>观察开始（含，源业务时间）<input v-model="form.observationStart" placeholder="2026-11-11 或 2026-11-11T00:00:00" /></label>
      <label>观察结束（不含）<input v-model="form.observationEnd" placeholder="与基线窗口等长" /></label>
      <label>方向<select v-model="form.direction"><option value="INCREASE">提升</option><option value="DECREASE">降低</option></select></label>
      <label>最小绝对变化（沿用指标单位）<input v-model="form.minimumChange" inputmode="decimal" /></label>
      <label>预计数据就绪时间（含时区偏移）<input v-model="form.readyAt" placeholder="2026-11-12T08:00:00+08:00" /></label>
    </fieldset>
    <p>就绪时间是人工约定的刷新等待时间，不是源数据已刷新证明；执行完成晚于观察开始时不判定效果。</p>
    <a-button :disabled="disabled || busy" :loading="busy" @click="save">保存观察计划并作废旧预览</a-button>
    <p v-if="error" role="alert">{{ error }}</p>
  </details>
</template>
<script setup>
import { computed, reactive, ref, watch, onBeforeUnmount } from 'vue'
import { configureDecisionEffect } from '@/api/ai/decision'
const props = defineProps({ proposal: { type: Object, required: true }, disabled: Boolean, contextKey: String })
const emit = defineEmits(['updated'])
const form = reactive({ baselineKey: '', observationStart: '', observationEnd: '', direction: 'INCREASE', minimumChange: '0', readyAt: '' })
const busy = ref(false)
const error = ref('')
let generation = 0
const cells = computed(() => {
  const rows = []
  for (const e of props.proposal.evidence || []) {
    if (e.verification !== 'VERIFIED_METRIC') continue
    for (const [toolIndex, t] of (e.details?.queryEvidence || []).entries()) {
      const d = t.data
      if (t.toolName !== 'execute_metric_analysis' || t.status !== 'SUCCEEDED' || d?.truncated !== false || d.rows?.length !== 1 || d.queryDefinition?.dimensionPropertyIds?.length) continue
      if (typeof d.rows[0]?.metric_value !== 'string') continue
      rows.push({ key: `${e.id}:${toolIndex}`, evidenceId: e.id, toolIndex, label: `${d.metricName || d.metricId} V${d.version}：${d.rows[0].metric_value} ${d.unit || ''}；${d.startTime || ''}~${d.endTime || ''}` })
    }
  }
  return rows
})
watch(() => [props.proposal.id, props.proposal.revision, props.contextKey], () => {
  ++generation; busy.value = false; error.value = ''
  const plan = props.proposal.effectPlan
  Object.assign(form, { baselineKey: plan?.baseline ? `${plan.baseline.evidenceId}:${plan.baseline.toolIndex}` : '', observationStart: plan?.observationStart || '', observationEnd: plan?.observationEnd || '', direction: plan?.direction || 'INCREASE', minimumChange: plan?.minimumChange || '0', readyAt: plan?.readyAt || '' })
}, { immediate: true })
async function save() {
  if (props.disabled || busy.value) return
  const token = generation
  const cell = cells.value.find(c => c.key === form.baselineKey)
  if (form.baselineKey && !cell) { error.value = '基线证据已失效，请重新分析'; return }
  const plan = cell ? { baseline: { source: 'METRIC', evidenceId: cell.evidenceId, toolIndex: cell.toolIndex, rowIndex: 0, columnKey: 'metric_value' }, observationStart: form.observationStart, observationEnd: form.observationEnd, direction: form.direction, minimumChange: form.minimumChange, readyAt: form.readyAt } : null
  busy.value = true; error.value = ''
  try { const res = await configureDecisionEffect({ decisionId: props.proposal.id, revision: props.proposal.revision, plan }); if (token === generation) emit('updated', res.data) }
  catch (failure) { if (token === generation) error.value = failure?.message || '观察计划保存失败' }
  finally { if (token === generation) busy.value = false }
}
onBeforeUnmount(() => { ++generation })
</script>
<style scoped>
.effect-plan { margin-top: 12px; border: 1px solid #d9dfe8; padding: 12px; border-radius: 6px; }
fieldset { border: 0; padding: 0; } label { display: grid; gap: 4px; margin: 8px 0; } input, select { max-width: 100%; padding: 5px; border: 1px solid #d9dfe8; border-radius: 4px; }
</style>
