<template>
  <div class="app-container ingestion-page">
    <a-alert
      v-if="runtime.message"
      :message="runtime.message"
      :type="runtimeAlertType"
      show-icon
      closable
      class="runtime-alert"
    />

    <a-row :gutter="16" class="summary-row">
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card size="small"><a-statistic title="运行状态" :value="runtimeStateText" :value-style="{ color: runtimeStateColor }" /></a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card size="small"><a-statistic title="处理成功" :value="metricValue('logs', 'SUCCESS')" /></a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card size="small"><a-statistic title="处理失败" :value="metricValue('logs', 'FAILED')" :value-style="{ color: '#dc2626' }" /></a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card size="small"><a-statistic title="待处理死信" :value="metricValue('logs', 'DEAD_LETTER')" :value-style="{ color: '#d97706' }" /></a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card size="small"><a-statistic title="Kafka 待消费积压" :value="runtime.totalLag || 0" :value-style="{ color: runtime.totalLag > 0 ? '#d97706' : '#16a34a' }" /></a-card>
      </a-col>
    </a-row>

    <a-card title="接入配置摘要" size="small" class="section-card">
      <template #extra>
        <a-space>
          <a-button
            v-if="!pauseDesired"
            v-hasPermi="['ingestion:control']"
            size="small"
            danger
            :loading="controlLoading"
            :disabled="!runtime.active"
            @click="pauseConsumer"
          >暂停消费</a-button>
          <a-button
            v-else
            v-hasPermi="['ingestion:control']"
            size="small"
            type="primary"
            :loading="controlLoading"
            :disabled="!runtime.active"
            @click="resumeConsumer"
          >恢复消费</a-button>
          <a-button size="small" @click="refreshAll">刷新</a-button>
        </a-space>
      </template>
      <a-descriptions bordered size="small" :column="{ xs: 1, sm: 2, md: 3 }">
        <a-descriptions-item label="启用">{{ config.enabled ? '是' : '否' }}</a-descriptions-item>
        <a-descriptions-item label="Kafka 地址">{{ config.kafkaBootstrapServers || '-' }}</a-descriptions-item>
        <a-descriptions-item label="消费组">{{ config.kafkaGroupId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="Topic">{{ (config.kafkaTopics || []).join(', ') || '-' }}</a-descriptions-item>
        <a-descriptions-item label="目标数据源">{{ config.dorisDatasourceId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="冲突策略">{{ config.conflictStrategy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="目标表白名单" :span="2">{{ (config.allowedTables || []).join(', ') || '按消息路由（仍会校验标识符）' }}</a-descriptions-item>
        <a-descriptions-item label="死信 Topic">{{ config.kafkaDeadLetterTopic || '未配置' }}</a-descriptions-item>
        <a-descriptions-item label="积压采样时间">{{ formatTime(runtime.lagMeasuredAt) }}</a-descriptions-item>
        <a-descriptions-item label="分区积压" :span="2">{{ formatLag(runtime.lagByPartition) }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card title="死信消息" size="small" class="section-card">
      <template #extra><span class="muted">最多显示 200 条</span></template>
      <div class="filter-row">
        <a-input v-model:value="filters.topic" allow-clear placeholder="来源 Topic" @pressEnter="loadDeadLetters" />
        <a-input v-model:value="filters.targetTable" allow-clear placeholder="目标表" @pressEnter="loadDeadLetters" />
        <a-button type="primary" @click="loadDeadLetters">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
        <a-button v-hasPermi="['ingestion:replay']" :disabled="selectedRowKeys.length === 0" @click="replaySelected">批量重放</a-button>
      </div>
      <a-table
        :loading="deadLoading"
        :data-source="deadLetters"
        :columns="deadColumns"
        :pagination="false"
        :row-selection="rowSelection"
        row-key="id"
        :scroll="{ x: 1350 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'messageBody'">
            <a-typography-paragraph :ellipsis="{ rows: 2 }" :content="record.messageBody || '-'" />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space size="small">
              <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
              <a-button v-hasPermi="['ingestion:replay']" type="link" size="small" @click="replayOne(record)">重放</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="detailOpen" title="死信详情" width="900px" :footer="null">
      <a-descriptions bordered size="small" :column="2">
        <a-descriptions-item label="日志 ID">{{ detail.id }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ detail.status }}</a-descriptions-item>
        <a-descriptions-item label="Topic">{{ detail.topic }}</a-descriptions-item>
        <a-descriptions-item label="分区 / Offset">{{ detail.partition }} / {{ detail.msgOffset }}</a-descriptions-item>
        <a-descriptions-item label="目标表">{{ detail.targetTable || '-' }}</a-descriptions-item>
        <a-descriptions-item label="冲突策略">{{ detail.conflictAction || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="消息 Key">{{ detail.messageKey || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2">{{ detail.message || '-' }}</a-descriptions-item>
      </a-descriptions>
      <h4 class="json-title">消息体</h4>
      <pre class="message-body">{{ prettyJson(detail.messageBody) }}</pre>
    </a-modal>
  </div>
</template>

<script setup name="IngestionConsole">
import { computed, getCurrentInstance, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  getIngestionConfig,
  getIngestionMetrics,
  getIngestionRuntime,
  listIngestionDeadLetters,
  pauseIngestionConsumer,
  replayIngestionDeadLetter,
  replayIngestionDeadLetters,
  resumeIngestionConsumer,
} from '@/api/ingestion'

const { proxy } = getCurrentInstance()
const config = reactive({})
const runtime = reactive({})
const metrics = reactive({ logs: {}, events: {} })
const filters = reactive({ topic: undefined, targetTable: undefined })
const deadLetters = ref([])
const deadLoading = ref(false)
const controlLoading = ref(false)
const selectedRowKeys = ref([])
const detailOpen = ref(false)
const detail = reactive({})
let refreshTimer

const deadColumns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: 'Topic', dataIndex: 'topic', width: 180 },
  { title: '分区', dataIndex: 'partition', width: 70 },
  { title: 'Offset', dataIndex: 'msgOffset', width: 120 },
  { title: '目标表', dataIndex: 'targetTable', width: 170, ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 110 },
  { title: '错误信息', dataIndex: 'message', width: 260, ellipsis: true },
  { title: '消息体', dataIndex: 'messageBody', width: 280 },
  { title: '时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'actions', fixed: 'right', width: 120 },
]
const rowSelection = computed(() => ({ selectedRowKeys: selectedRowKeys.value, onChange: keys => { selectedRowKeys.value = keys } }))
const runtimeStateText = computed(() => ({ DISABLED: '未启用', NOT_STARTED: '未启动', STARTING: '启动中', RUNNING: '运行中', PAUSING: '暂停中', PAUSED: '已暂停', RESUMING: '恢复中', NO_TOPICS: '无 Topic', CONFIG_ERROR: '配置错误', ERROR: '异常', STOPPED: '已停止' }[runtime.state] || runtime.state || '未知'))
const runtimeStateColor = computed(() => {
  if (runtime.state === 'RUNNING') return '#16a34a'
  if (['ERROR', 'CONFIG_ERROR'].includes(runtime.state)) return '#dc2626'
  return '#d97706'
})
const runtimeAlertType = computed(() => {
  if (['ERROR', 'CONFIG_ERROR'].includes(runtime.state)) return 'error'
  if (['PAUSING', 'PAUSED', 'RESUMING'].includes(runtime.state)) return 'info'
  return runtime.active ? 'success' : 'warning'
})
const pauseDesired = computed(() => runtime.paused || ['PAUSING', 'PAUSED'].includes(runtime.state))

function metricValue(group, key) { return metrics[group]?.[key] ?? 0 }
function formatTime(value) {
  if (!value) return '-'
  return typeof proxy?.parseTime === 'function' ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : value
}
function prettyJson(value) {
  if (!value) return '-'
  try { return JSON.stringify(JSON.parse(value), null, 2) } catch (_error) { return value }
}
function formatLag(value) {
  if (!value || typeof value !== 'object') return '-'
  const entries = Object.entries(value)
  return entries.length ? entries.map(([partition, lag]) => `${partition}: ${lag}`).join('，') : '0'
}
async function loadRuntime() {
  const response = await getIngestionRuntime()
  Object.assign(runtime, response.data || {})
}
async function loadConfig() {
  const response = await getIngestionConfig()
  Object.assign(config, response.data || {})
}
async function loadMetrics() {
  const response = await getIngestionMetrics()
  Object.assign(metrics, response.data || {})
}
async function loadDeadLetters() {
  deadLoading.value = true
  try {
    const response = await listIngestionDeadLetters({ ...filters, limit: 200 })
    const data = response.data
    deadLetters.value = Array.isArray(data) ? data : (data?.rows || data?.list || [])
    selectedRowKeys.value = []
  } finally {
    deadLoading.value = false
  }
}
async function refreshAll() { await Promise.all([loadRuntime(), loadConfig(), loadMetrics(), loadDeadLetters()]) }
function pauseConsumer() {
  Modal.confirm({
    title: '确认暂停数据接入消费？',
    content: '已拉取的消息会处理完成；暂停期间消费者仍维持 Kafka 心跳，但不会拉取新的业务消息。',
    onOk: async () => {
      controlLoading.value = true
      try {
        const response = await pauseIngestionConsumer()
        Object.assign(runtime, response.data || {})
        message.success('暂停请求已提交')
      } finally {
        controlLoading.value = false
      }
    },
  })
}
async function resumeConsumer() {
  controlLoading.value = true
  try {
    const response = await resumeIngestionConsumer()
    Object.assign(runtime, response.data || {})
    message.success('恢复请求已提交')
  } finally {
    controlLoading.value = false
  }
}
function resetFilters() { filters.topic = undefined; filters.targetTable = undefined; loadDeadLetters() }
function showDetail(row) { Object.keys(detail).forEach(key => delete detail[key]); Object.assign(detail, row); detailOpen.value = true }
function replayOne(row) {
  Modal.confirm({ title: '确认重放这条死信？', content: `日志 ID：${row.id}`, onOk: async () => { await replayIngestionDeadLetter(row.id); message.success('已提交重放'); await refreshAll() } })
}
function replaySelected() {
  Modal.confirm({ title: '确认批量重放？', content: `将重放 ${selectedRowKeys.value.length} 条消息。`, onOk: async () => { const response = await replayIngestionDeadLetters(selectedRowKeys.value); message.success(`已成功重放 ${response.data || 0} 条`); await refreshAll() } })
}

onMounted(async () => {
  await refreshAll()
  refreshTimer = window.setInterval(() => { loadRuntime(); loadMetrics() }, 30000)
})
onBeforeUnmount(() => { if (refreshTimer) window.clearInterval(refreshTimer) })
</script>

<style scoped>
.runtime-alert { margin-bottom: 16px; }
.summary-row { margin-bottom: 16px; }
.section-card { margin-bottom: 16px; }
.filter-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.filter-row .ant-input { width: 190px; }
.muted { color: #8c8c8c; font-size: 12px; }
.json-title { margin: 16px 0 8px; }
.message-body { max-height: 420px; overflow: auto; padding: 12px; border-radius: 4px; background: #f6f8fa; white-space: pre-wrap; word-break: break-word; }
</style>
