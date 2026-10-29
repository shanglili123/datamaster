<template>
  <div class="measure-input">
    <select :value="modelValue?.source || 'USER'" :disabled="disabled" @change="change('source', $event.target.value)">
      <option value="USER">人工确认假设</option>
      <option value="METRIC">正式指标单元格</option>
    </select>
    <input v-if="modelValue?.source !== 'METRIC'" :value="modelValue?.value || ''" :disabled="disabled" inputmode="decimal" placeholder="精确十进制数值" @input="change('value', $event.target.value)" />
    <select v-else :value="selection" :disabled="disabled" @change="selectCell($event.target.value)">
      <option value="">选择未截断正式指标值</option>
      <option v-for="cell in cells" :key="cell.key" :value="cell.key">{{ cell.label }}</option>
    </select>
    <small>单位：{{ unit || '请先填写单位' }}</small>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  modelValue: { type: Object, default: () => ({ source: 'USER', value: '' }) },
  unit: { type: String, default: '' },
  cells: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const selection = computed(() => props.modelValue?.source === 'METRIC'
  ? `${props.modelValue.evidenceId}:${props.modelValue.toolIndex}:${props.modelValue.rowIndex}` : '')
function change(key, value) {
  if (props.disabled) return
  const next = { ...props.modelValue, [key]: value, unit: props.unit }
  if (key === 'source') { next.value = ''; next.evidenceId = null; next.toolIndex = null; next.rowIndex = null; next.columnKey = null }
  emit('update:modelValue', next)
}
function selectCell(key) {
  if (props.disabled) return
  const cell = props.cells.find(item => item.key === key)
  emit('update:modelValue', cell ? { source: 'METRIC', evidenceId: cell.evidenceId, toolIndex: cell.toolIndex, rowIndex: cell.rowIndex, columnKey: 'metric_value', unit: props.unit }
    : { source: 'METRIC', unit: props.unit })
}
</script>

<style scoped>
.measure-input { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
select, input { max-width: 100%; border: 1px solid #d9dfe8; border-radius: 4px; padding: 5px; }
small { color: #667085; }
</style>
