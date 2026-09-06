<template>
  <div class="reference-editor">
    <div class="reference-title">{{ title }}</div>
    <a-select v-model:value="model.source" :options="sourceOptions" @change="sourceChanged" />
    <div class="source-hint">{{ sourceHint }}</div>
    <a-select v-if="model.source === 'OBJECT'" v-model:value="model.path" :options="propertyOptions" show-search option-filter-prop="label" placeholder="选择对象属性" @change="changed" />
    <a-auto-complete v-else-if="model.source === 'INPUT'" v-model:value="model.path" :options="inputOptions" allow-clear
      placeholder="选择或输入启动参数字段，如 order.amount" @change="changed" />
    <template v-else-if="model.source === 'STEP_OUTPUT'">
      <a-select v-model:value="model.nodeKey" :options="stepOptions" placeholder="选择上游动作" @change="changed" />
      <a-auto-complete v-model:value="model.path" :options="stepOutputOptions" allow-clear
        placeholder="选择标准输出字段，也可输入扩展字段" @change="changed" />
    </template>
    <a-select v-else-if="model.source === 'CONTEXT'" v-model:value="model.path" :options="contextOptions" placeholder="选择上下文字段" @change="changed" />
  </div>
</template>

<script setup name="WorkflowReferenceEditor">
const props = defineProps({
  title: { type: String, required: true },
  model: { type: Object, required: true },
  sourceOptions: { type: Array, default: () => [] },
  propertyOptions: { type: Array, default: () => [] },
  stepOptions: { type: Array, default: () => [] },
  inputOptions: { type: Array, default: () => [] },
  stepOutputOptions: { type: Array, default: () => [] },
  contextOptions: { type: Array, default: () => [] }
})
const emit = defineEmits(['change'])
const sourceHint = computed(() => ({
  OBJECT: '来自本次触发对象，字段由上方“触发概念”的属性决定。',
  INPUT: '来自启动 Workflow 时传入的 input 参数。',
  STEP_OUTPUT: '来自所选上游动作的标准 resultContext。',
  CONTEXT: '由 Workflow 运行时生成，不由业务表直接提供。'
}[props.model.source] || '请选择动态值来源。'))
function sourceChanged() {
  props.model.path = ''
  props.model.nodeKey = undefined
  emit('change')
}
function changed() { emit('change') }
</script>

<style lang="scss" scoped>
.reference-editor { display:flex; flex-direction:column; gap:7px; margin:12px 0; padding:10px; border:1px solid #e5eaf2; border-radius:7px; background:#fafbfc; }
.reference-title { color:#667085; font-size:12px; font-weight:600; }
.source-hint { margin-top:-2px; color:#98a2b3; font-size:11px; line-height:1.45; }
</style>
