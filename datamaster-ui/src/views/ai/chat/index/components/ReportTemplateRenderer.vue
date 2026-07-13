<template>
  <div class="report-renderer" :style="pageStyle">
    <template v-for="section in sections" :key="section.key || section.type">
      <h1 v-if="section.type === 'title'" class="report-title" :style="componentStyle('title', section)">
        {{ valueOf(section.bind) || data.title || template.templateName || '报告' }}
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
        <div class="chart" :ref="(el) => setChartRef(section.key, el)" :style="chartStyle(section)" />
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

const sections = computed(() => props.template?.layout?.sections || [])
const pageStyle = computed(() => {
  const page = props.template?.layout?.page || {}
  const theme = props.template?.style?.theme || {}
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
    .forEach((section) => {
      const el = chartRefs.get(section.key)
      if (!el) return
      let chart = chartInstances.get(section.key)
      if (!chart) {
        chart = echarts.init(el)
        chartInstances.set(section.key, chart)
      }
      chart.setOption(buildChartOption(section), true)
      chart.resize()
    })
}

function buildChartOption(section) {
  const rows = arrayOf(section.bind)
  const theme = props.template?.style?.theme || {}
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
    ...(props.template?.style?.components?.[name] || {}),
    ...(section?.style || {})
  })
}

function chartStyle(section) {
  return normalizeStyle({
    ...(props.template?.style?.components?.chart || {}),
    ...(section?.style || {}),
    height: section?.style?.height || props.template?.style?.components?.chart?.height || '260px'
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
  return path.split('.').reduce((obj, key) => obj == null ? undefined : obj[key], props.data)
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
