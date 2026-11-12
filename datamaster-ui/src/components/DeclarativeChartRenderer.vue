<template>
  <section class="declarative-chart" :aria-label="title || '数据图表'">
    <p v-if="notice">{{ notice }}</p>
    <p v-if="!points.length">暂无可绘制的数据；缺失值不按零处理。</p>
    <p v-else-if="!drawable">当前值超出安全绘图范围或超过500项，请使用精确数据表；不会丢弃数据后伪装为完整图。</p>
    <template v-else>
      <p>{{ title }}<span v-if="unit"> · {{ unit }}</span>（单序列；图形用于近似比较，精确数值见悬浮提示与数据表）</p>
      <p v-if="activePoint" class="chart-inspector" role="status">{{ activePoint.label }}：{{ activePoint.exact ?? '缺失（不补零）' }}<span v-if="unit"> {{ unit }}</span></p>
      <svg viewBox="0 0 840 330" role="img" @pointerleave="activeIndex = null" :aria-label="`${title || '单指标'}，${points.length}项，${chartType === 'line' ? '趋势线' : '条形图'}`">
        <line v-if="activeIndex != null" :x1="x(activeIndex)" y1="20" :x2="x(activeIndex)" y2="290" stroke="currentColor" stroke-dasharray="3 3" stroke-opacity="0.5" />
        <line x1="60" :y1="baseline" x2="820" :y2="baseline" stroke="currentColor" stroke-opacity="0.4" />
        <text x="5" y="25" fill="currentColor">{{ maxLabel }}</text>
        <text x="5" y="290" fill="currentColor">{{ minLabel }}</text>
        <template v-if="chartType === 'line'">
          <path v-for="(segment, index) in segments" :key="index" :d="segment" fill="none" stroke="currentColor" stroke-width="2" />
          <g v-for="(point, index) in points" :key="index" tabindex="0" @pointerenter="activeIndex = index" @focus="activeIndex = index" @blur="activeIndex = null" :aria-label="`${point.label}：${point.exact ?? '缺失'}`">
            <circle v-if="point.value != null" :cx="x(index)" :cy="y(point.value)" r="4" fill="currentColor" />
            <circle :cx="x(index)" :cy="point.value == null ? baseline : y(point.value)" r="12" fill="transparent"><title>{{ point.label }}：{{ point.exact ?? '缺失（不补零）' }}</title></circle>
          </g>
        </template>
        <template v-else>
          <g v-for="(point, index) in points" :key="index" tabindex="0" @pointerenter="activeIndex = index" @focus="activeIndex = index" @blur="activeIndex = null" :aria-label="`${point.label}：${point.exact ?? '缺失'}`">
            <rect v-if="point.value != null" :x="x(index) - barWidth / 2" :y="Math.min(baseline, y(point.value))" :width="barWidth" :height="Math.abs(baseline - y(point.value))" rx="4" fill="currentColor" />
            <rect :x="x(index) - Math.max(12, barWidth) / 2" y="20" :width="Math.max(12, barWidth)" height="270" fill="transparent"><title>{{ point.label }}：{{ point.exact ?? '缺失（不补零）' }}</title></rect>
          </g>
        </template>
        <text v-for="index in labelIndices" :key="index" :x="x(index)" y="315" text-anchor="middle" fill="currentColor">{{ shortLabel(points[index].label) }}</text>
      </svg>
    </template>
    <details v-if="showTable"><summary>查看精确数据表</summary>
      <a-table :columns="[{ title: '分组', dataIndex: 'label' }, { title: title || '值', dataIndex: 'exact' }]" :data-source="points" :row-key="(_, index) => String(index)" :pagination="false" size="small">
        <template #bodyCell="{ text }">{{ text == null ? '缺失' : text }}</template>
      </a-table>
    </details>
  </section>
</template>
<script setup>
import { computed, ref, watch } from 'vue'
const props = defineProps({ rows: { type: Array, default: () => [] }, xField: { type: String, required: true }, yField: { type: String, required: true }, chartType: { type: String, default: 'bar' }, title: String, unit: String, notice: String, showTable: { type: Boolean, default: true } })
const activeIndex = ref(null)
const points = computed(() => props.rows.map(row => {
  const exact = row?.[props.yField], numeric = exact == null || exact === '' ? null : Number(exact)
  return { label: row?.[props.xField] == null ? '缺失分组' : String(row[props.xField]), exact: exact === '' ? null : exact, value: numeric }
}))
const activePoint = computed(() => activeIndex.value == null ? null : points.value[activeIndex.value])
watch(() => props.rows, () => { activeIndex.value = null })
const drawable = computed(() => points.value.length <= 500 && points.value.every(point => point.value == null || Number.isFinite(point.value) && Math.abs(point.value) <= Number.MAX_SAFE_INTEGER))
const lower = computed(() => Math.min(0, ...points.value.filter(point => point.value != null && Number.isFinite(point.value)).map(point => point.value)))
const upper = computed(() => Math.max(0, ...points.value.filter(point => point.value != null && Number.isFinite(point.value)).map(point => point.value)))
const span = computed(() => upper.value - lower.value || 1)
const x = index => 60 + (index + 0.5) * 760 / Math.max(1, points.value.length)
const y = value => 290 - (value - lower.value) / span.value * 270
const baseline = computed(() => y(0))
const barWidth = computed(() => Math.min(24, Math.max(1, 760 / Math.max(1, points.value.length) - 2)))
const maxLabel = computed(() => upper.value.toLocaleString(undefined, { maximumSignificantDigits: 5 }))
const minLabel = computed(() => lower.value.toLocaleString(undefined, { maximumSignificantDigits: 5 }))
const labelIndices = computed(() => [...new Set([0, Math.floor(points.value.length / 2), points.value.length - 1])].filter(index => index >= 0 && index < points.value.length))
const shortLabel = label => label.length > 14 ? `${label.slice(0, 13)}…` : label
const segments = computed(() => {
  const paths = []; let path = ''
  points.value.forEach((point, index) => {
    if (point.value == null) { if (path) paths.push(path); path = ''; return }
    path += `${path ? ' L' : 'M'} ${x(index)} ${y(point.value)}`
  })
  if (path) paths.push(path)
  return paths
})
</script>
<style scoped>
.declarative-chart { color: inherit; margin: 16px 0; }
.declarative-chart svg { display: block; width: 100%; max-height: 360px; }
.declarative-chart svg text { font-size: 12px; }
.declarative-chart svg g:focus { outline: 2px solid currentColor; outline-offset: 3px; }
.chart-inspector { min-height: 24px; font-variant-numeric: tabular-nums; }
</style>
