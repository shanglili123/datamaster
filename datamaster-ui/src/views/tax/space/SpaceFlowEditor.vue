<template>
  <section class="flow-editor" aria-label="空间功能流程编辑器">
    <p>拖动节点整理布局；选择起点、终点新增连线。节点与连线仅修改草稿，不会运行任务。</p>
    <a-space wrap>
      <a-checkbox :checked="modelValue.nodes.includes('ASSET')" :disabled="disabled" @change="event => toggle('ASSET', event.target.checked)">资产权限节点</a-checkbox>
      <a-checkbox :checked="modelValue.nodes.includes('QUALITY')" :disabled="disabled" @change="event => toggle('QUALITY', event.target.checked)">质量准入节点</a-checkbox>
      <a-select v-model:value="from" aria-label="连线起点" placeholder="起点" style="width: 130px" :disabled="disabled">
        <a-select-option v-for="node in modelValue.nodes.filter(item => !['API', 'ANALYSIS'].includes(item))" :key="node" :value="node">{{ labels[node] }}</a-select-option>
      </a-select>
      <span aria-hidden="true">→</span>
      <a-select v-model:value="to" aria-label="连线终点" placeholder="终点" style="width: 130px" :disabled="disabled">
        <a-select-option v-for="node in modelValue.nodes.filter(item => item !== 'METADATA')" :key="node" :value="node">{{ labels[node] }}</a-select-option>
      </a-select>
      <a-button :disabled="disabled || !from || !to || from === to" @click="connect">新增连线</a-button>
    </a-space>
    <div ref="stage" class="flow-stage" @pointermove="move" @pointerup="stop" @pointercancel="stop">
      <svg class="flow-edges" viewBox="0 0 840 280" preserveAspectRatio="none" role="img" aria-label="当前草稿节点连接关系">
        <defs><marker :id="arrowId" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="currentColor" /></marker></defs>
        <path v-for="edge in modelValue.edges" :key="`${edge.from}:${edge.to}`" :d="path(edge)" fill="none" stroke="currentColor" stroke-width="1.5" :marker-end="`url(#${arrowId})`"><title>{{ labels[edge.from] }} → {{ labels[edge.to] }}</title></path>
      </svg>
      <button v-for="node in modelValue.nodes" :key="node" type="button" class="flow-node" :style="{ left: `${positions[node].x / 8.4}%`, top: `${positions[node].y / 2.8}%` }" :disabled="disabled" :aria-label="`${labels[node]}节点，可拖动；连线通过上方控件编辑`" @pointerdown="event => start(event, node)">{{ labels[node] }}</button>
    </div>
    <p v-if="!modelValue.edges.length" role="alert">尚无连线，请连接元数据到 API、分析，并让所有选中前置节点约束两种消费。</p>
    <ul class="flow-connection-list" aria-label="连线列表">
      <li v-for="(edge, index) in modelValue.edges" :key="`${edge.from}:${edge.to}`">
        {{ labels[edge.from] }} → {{ labels[edge.to] }}
        <a-button type="link" size="small" :disabled="disabled" :aria-label="`删除${labels[edge.from]}到${labels[edge.to]}的连线`" @click="remove(index)">删除连线</a-button>
      </li>
    </ul>
    <p>系统不允许循环、断开或绕过已选准入节点；请在保存前使用“校验草稿”。布局随草稿保存，不改变准入规则语义。</p>
  </section>
</template>
<script setup>
import { reactive, ref, useId, watch } from 'vue'
const props = defineProps({ modelValue: { type: Object, required: true }, disabled: Boolean })
const emit = defineEmits(['update:modelValue'])
const labels = { METADATA: '元数据', ASSET: '资产权限', QUALITY: '质量准入', API: 'API 开放', ANALYSIS: '数据分析' }
const defaults = { METADATA: { x: 25, y: 115 }, ASSET: { x: 220, y: 115 }, QUALITY: { x: 420, y: 115 }, API: { x: 655, y: 45 }, ANALYSIS: { x: 655, y: 190 } }
const positions = reactive(JSON.parse(JSON.stringify(defaults)))
const stage = ref(null), from = ref(), to = ref()
const arrowId = `space-flow-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
let dragging = null
function update(nodes, edges) {
  const layout = Object.fromEntries(nodes.map(node => [node, { ...positions[node] }]))
  emit('update:modelValue', { nodes: [...nodes], edges: edges.map(edge => ({ ...edge })), layout })
}
function toggle(node, checked) {
  if (props.disabled) return
  const nodes = checked ? [...props.modelValue.nodes, node] : props.modelValue.nodes.filter(value => value !== node)
  const edges = props.modelValue.edges.filter(edge => nodes.includes(edge.from) && nodes.includes(edge.to))
  if (from.value === node) from.value = undefined
  if (to.value === node) to.value = undefined
  update(nodes, edges)
}
function connect() {
  if (props.disabled || from.value === to.value || !props.modelValue.nodes.includes(from.value) || !props.modelValue.nodes.includes(to.value)) return
  if (props.modelValue.edges.some(edge => edge.from === from.value && edge.to === to.value)) return
  update(props.modelValue.nodes, [...props.modelValue.edges, { from: from.value, to: to.value }])
}
function remove(index) { if (!props.disabled) update(props.modelValue.nodes, props.modelValue.edges.filter((_, i) => i !== index)) }
function path(edge) {
  const a = positions[edge.from], b = positions[edge.to]
  if (!a || !b) return ''
  const x1 = a.x + 140, y1 = a.y + 22, x2 = b.x, y2 = b.y + 22
  return `M ${x1} ${y1} C ${x1 + 45} ${y1}, ${x2 - 45} ${y2}, ${x2} ${y2}`
}
function start(event, node) {
  if (props.disabled || event.button !== 0) return
  event.preventDefault(); event.currentTarget.setPointerCapture(event.pointerId)
  dragging = { node, x: event.clientX, y: event.clientY, initialX: positions[node].x, initialY: positions[node].y }
}
function move(event) {
  if (!dragging || !stage.value || props.disabled) return
  const rect = stage.value.getBoundingClientRect()
  positions[dragging.node].x = Math.max(0, Math.min(695, dragging.initialX + (event.clientX - dragging.x) * 840 / rect.width))
  positions[dragging.node].y = Math.max(0, Math.min(232, dragging.initialY + (event.clientY - dragging.y) * 280 / rect.height))
}
function stop() {
  if (dragging && !props.disabled) update(props.modelValue.nodes, props.modelValue.edges)
  dragging = null
}
watch(() => props.modelValue.layout, layout => {
  if (dragging) return
  for (const node of Object.keys(defaults)) positions[node] = { ...(layout?.[node] || defaults[node]) }
}, { immediate: true, deep: true })
</script>
<style scoped>
.flow-editor { margin: 16px 0; }
.flow-stage { position: relative; width: 100%; aspect-ratio: 3; min-height: 180px; border: 1px solid var(--ant-color-border, #d9d9d9); border-radius: 6px; margin-top: 12px; overflow: hidden; }
.flow-edges { position: absolute; inset: 0; width: 100%; height: 100%; opacity: .65; }
.flow-node { position: absolute; width: 16.667%; height: 15.714%; border: 1px solid currentColor; border-radius: 6px; background: var(--ant-color-bg-container, Canvas); color: inherit; cursor: grab; touch-action: none; }
.flow-node:focus-visible { outline: 2px solid currentColor; outline-offset: 3px; }
.flow-node:disabled { cursor: default; }
.flow-connection-list { display: flex; flex-wrap: wrap; gap: 4px 24px; padding-left: 20px; }
</style>
