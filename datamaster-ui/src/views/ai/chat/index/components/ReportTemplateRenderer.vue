<template>
  <div class="report-renderer" :style="pageStyle">
    <template v-for="(section, index) in sections" :key="sectionKey(section, index)">
      <h1 v-if="section.type === 'title'" class="report-title" :style="componentStyle('title', section)">
        {{ valueOf(section.bind) || reportData.title || reportTemplate.templateName || '报告' }}
      </h1>

      <p v-else-if="section.type === 'paragraph'" class="report-paragraph" :style="componentStyle('paragraph', section)">
        {{ valueOf(section.bind) }}
      </p>

      <div v-else-if="section.type === 'metricGrid'" class="metric-grid" :style="gridStyle(section)">
        <div
          v-for="item in section.items || []"
          :key="item.bind || item.label"
          class="metric-card"
          :style="componentStyle('metricCard', section)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ formatValue(valueOf(item.bind), item) }}</strong>
        </div>
      </div>

      <div
        v-else-if="['lineChart', 'barChart', 'pieChart'].includes(section.type)"
        class="report-section"
      >
        <h3>{{ section.title }}</h3>
        <div class="chart" :ref="(el) => setChartRef(sectionKey(section, index), el)" :style="chartStyle(section)" />
      </div>

      <div v-else-if="section.type === 'table'" class="report-section">
        <h3>{{ section.title }}</h3>
        <el-table :data="arrayOf(section.bind)" border size="small">
          <el-table-column
            v-for="col in section.columns || []"
            :key="col.prop"
            :prop="col.prop"
            :label="col.label"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>
      </div>

      <div v-else-if="['insightList', 'warningList'].includes(section.type)" class="report-section">
        <h3>{{ section.title }}</h3>
        <ul class="insight-list">
          <li v-for="(item, index) in arrayOf(section.bind)" :key="index">{{ item }}</li>
        </ul>
      </div>
    </template>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'

const props = defineProps({
  template: {
    type: Object,
    required: true
  },
  data: {
    type: Object,
    required: true
  }
})

const chartRefs = new Map()
const chartInstances = new Map()

const reportTemplate = computed(() => normalizeTemplate(props.template))
const reportData = computed(() => normalizeData(props.data))
const sections = computed(() => normalizeSections(reportTemplate.value))
const pageStyle = computed(() => {
  const page = reportTemplate.value?.layout?.page || {}
  const theme = reportTemplate.value?.style?.theme || {}
  return {
    padding: page.padding || '24px',
    background: page.background || '#f7f8fa',
    color: theme.textColor || '#1d2129',
    fontFamily: theme.fontFamily || 'PingFang SC, Microsoft YaHei, sans-serif'
  }
})

watch(
  () => [props.template, props.data],
  () => nextTick(renderCharts),
  { deep: true, immediate: true }
)

onBeforeUnmount(() => {
  chartInstances.forEach((chart) => chart.dispose())
  chartInstances.clear()
})

function setChartRef(key, el) {
  if (!key || !el) return
  chartRefs.set(key, el)
  nextTick(renderCharts)
}

function renderCharts() {
  sections.value
    .filter((section) => ['lineChart', 'barChart', 'pieChart'].includes(section.type))
    .forEach((section, index) => {
      const key = sectionKey(section, index)
      const el = chartRefs.get(key)
      if (!el) return
      let chart = chartInstances.get(key)
      if (!chart) {
        chart = echarts.init(el)
        chartInstances.set(key, chart)
      }
      chart.setOption(buildChartOption(section), true)
      chart.resize()
    })
}

function sectionKey(section, index) {
  return section?.key || section?.id || section?.bind || `${section?.type || 'section'}-${index}`
}

function buildChartOption(section) {
  const rows = arrayOf(section.bind)
  const theme = reportTemplate.value?.style?.theme || {}
  if (section.type === 'pieChart') {
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['35%', '68%'],
        data: rows.map((row) => ({
          name: row?.[section.nameField || section.xField] || '',
          value: Number(row?.[section.valueField || section.yField] || 0)
        }))
      }]
    }
  }
  return {
    color: [section.style?.color || theme.primaryColor || '#165dff'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 20, top: 24, bottom: 36 },
    xAxis: {
      type: 'category',
      data: rows.map((row) => row?.[section.xField] || '')
    },
    yAxis: { type: 'value' },
    series: [{
      type: section.type === 'lineChart' ? 'line' : 'bar',
      smooth: section.type === 'lineChart',
      data: rows.map((row) => Number(row?.[section.yField] || 0))
    }]
  }
}

function componentStyle(name, section) {
  return normalizeStyle({
    ...(reportTemplate.value?.style?.components?.[name] || {}),
    ...(section?.style || {})
  })
}

function chartStyle(section) {
  return normalizeStyle({
    ...(reportTemplate.value?.style?.components?.chart || {}),
    ...(section?.style || {}),
    height: section?.style?.height || reportTemplate.value?.style?.components?.chart?.height || '260px'
  })
}

function normalizeStyle(style) {
  const result = { ...style }
  if (result.align && !result.textAlign) {
    result.textAlign = result.align
    delete result.align
  }
  return result
}

function gridStyle(section) {
  const columns = section.columns || 3
  return {
    gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`
  }
}

function valueOf(path) {
  if (!path) return ''
  return path.split('.').reduce((obj, key) => obj == null ? undefined : obj[key], reportData.value)
}

function arrayOf(path) {
  const value = valueOf(path)
  return Array.isArray(value) ? value : []
}

function formatValue(value, item) {
  if (value == null || value === '') return '-'
  if (item?.format === 'integer') {
    return `${Number(value).toLocaleString()}${item.unit || ''}`
  }
  if (item?.format === 'decimal') {
    return `${Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}${item.unit || ''}`
  }
  return `${value}${item?.unit || ''}`
}

function normalizeTemplate(template) {
  const parsed = parseMaybeJson(template) || template || {}
  if (parsed.templateContent) {
    return normalizeTemplate(parsed.templateContent)
  }
  if (parsed.content && (typeof parsed.content === 'string' || parsed.content.layout || parsed.content.dataSchema)) {
    return normalizeTemplate(parsed.content)
  }
  return parsed
}

function normalizeData(data) {
  const parsed = parseMaybeJson(data) || data || {}
  const candidates = [
    parsed.reportData,
    parsed.report_data,
    parsed.data,
    parsed.result,
    parsed.payload
  ]
  for (const item of candidates) {
    const normalized = parseMaybeJson(item) || item
    if (normalized && typeof normalized === 'object' && !Array.isArray(normalized)) {
      return normalized
    }
  }
  return parsed
}

function normalizeSections(template) {
  if (Array.isArray(template?.layout?.sections)) return template.layout.sections.map(normalizeSection)
  if (Array.isArray(template?.sections)) return template.sections.map(normalizeSection)
  if (Array.isArray(template?.components)) return template.components.map(normalizeSection)
  if (Array.isArray(template?.layout)) return template.layout.map(normalizeSection)
  const fields = Array.isArray(template?.dataSchema?.fields) ? template.dataSchema.fields : []
  if (!fields.length) return []
  return fields.map((field) => {
    if (field.type === 'array') {
      return {
        key: field.key,
        type: 'table',
        title: field.label,
        bind: field.key,
        columns: Object.keys(field.itemSchema || {}).map((key) => ({ prop: key, label: key }))
      }
    }
    if (field.type === 'number') {
      return {
        key: field.key,
        type: 'metricGrid',
        columns: 1,
        items: [{ label: field.label, bind: field.key, format: field.format, unit: field.unit }]
      }
    }
    return {
      key: field.key,
      type: field.key === 'title' ? 'title' : 'paragraph',
      bind: field.key,
      title: field.label
    }
  }).map(normalizeSection)
}

function normalizeSection(section) {
  const typeMap = {
    heading: 'title',
    header: 'title',
    text: 'paragraph',
    markdown: 'paragraph',
    metrics: 'metricGrid',
    metric: 'metricGrid',
    chart: 'barChart',
    line: 'lineChart',
    bar: 'barChart',
    pie: 'pieChart',
    list: 'insightList',
    insights: 'insightList',
    warnings: 'warningList',
    grid: 'metricGrid'
  }
  const normalized = { ...section }
  normalized.type = typeMap[section?.type] || section?.type
  normalized.bind = normalized.bind || normalized.field || normalized.key || normalized.dataKey
  if (normalized.type === 'metricGrid' && !Array.isArray(normalized.items) && Array.isArray(normalized.metrics)) {
    normalized.items = normalized.metrics
  }
  if (normalized.type === 'table' && !Array.isArray(normalized.columns) && normalized.itemSchema) {
    normalized.columns = Object.keys(normalized.itemSchema).map((key) => ({ prop: key, label: key }))
  }
  return normalized
}

function parseMaybeJson(value) {
  if (!value || typeof value !== 'string') return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}
</script>

<style scoped>
.report-renderer {
  border: 1px solid #e5e6eb;
  border-radius: 8px;
}

.report-title {
  margin: 0 0 18px;
}

.report-paragraph {
  margin: 0 0 18px;
  white-space: pre-wrap;
}

.metric-grid {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.metric-card {
  padding: 14px 16px;
}

.metric-card span {
  display: block;
  color: #86909c;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  color: #1d2129;
}

.report-section {
  margin-top: 18px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
}

.report-section h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.chart {
  width: 100%;
}

.insight-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}
</style>
