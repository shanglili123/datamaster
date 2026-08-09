<template>
  <div class="app-container ai-ops-page">
    <div class="ops-header">
      <div>
        <div class="ops-title">AI运维</div>
        <div class="ops-subtitle">{{ report.summary || '正在等待诊断结果' }}</div>
      </div>
      <div class="ops-actions">
        <a-tag :color="statusType">{{ statusText }}</a-tag>
        <a-button type="primary" :icon="h(ReloadOutlined)" :loading="loading" @click="loadReport">重新诊断</a-button>
      </div>
    </div>

    <a-row :gutter="15">
      <a-col :xs="24" :sm="8">
        <a-card class="summary-card" :bordered="false">
          <div class="summary-label">健康评分</div>
          <div class="summary-score" :class="'score-' + report.status">{{ report.score ?? '--' }}</div>
          <div class="summary-time">{{ report.diagnoseTime || '--' }}</div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="16">
        <a-card class="summary-card" :bordered="false">
          <div class="summary-label">智能建议</div>
          <div v-for="(item, index) in report.suggestions" :key="index" class="suggestion-line">
            {{ index + 1 }}. {{ item }}
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="15">
      <a-col :span="24" class="card-box">
        <a-card :bordered="false">
          <template #title>
            <MonitorOutlined style="width: 1em; height: 1em; vertical-align: middle;" />
            <span style="vertical-align: middle;">关键指标</span>
          </template>
          <a-table :data-source="report.metrics" :columns="metricsColumns" :pagination="false" bordered size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="metricType(record.level)">{{ metricText(record.level) }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="24" class="card-box">
        <a-card :bordered="false">
          <template #title>
            <WarningOutlined style="width: 1em; height: 1em; vertical-align: middle;" />
            <span style="vertical-align: middle;">风险发现</span>
          </template>
          <a-empty v-if="!report.findings || !report.findings.length" description="暂无风险发现" />
          <a-table v-else :data-source="report.findings" :columns="findingsColumns" :pagination="false" bordered size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="findingType(record.level)">{{ findingText(record.level) }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="24" class="card-box bottom">
        <a-card :bordered="false">
          <template #title>
            <FileTextOutlined style="width: 1em; height: 1em; vertical-align: middle;" />
            <span style="vertical-align: middle;">近期异常日志</span>
          </template>
          <a-empty v-if="!report.logEvents || !report.logEvents.length" description="未发现异常日志片段" />
          <a-table v-else :data-source="report.logEvents" :columns="logColumns" :pagination="false" bordered size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="findingType(record.level)">{{ findingText(record.level) }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { diagnoseAiOps } from '@/api/system/monitor/aiOps.js'
import { h } from 'vue'
import { FileTextOutlined, MonitorOutlined, ReloadOutlined, WarningOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const report = ref({
  status: 'healthy',
  score: undefined,
  summary: '',
  diagnoseTime: '',
  metrics: [],
  findings: [],
  logEvents: [],
  suggestions: []
})

const statusType = computed(() => {
  if (report.value.status === 'error') return 'error'
  if (report.value.status === 'warn') return 'warning'
  return 'success'
})

const statusText = computed(() => {
  if (report.value.status === 'error') return '高风险'
  if (report.value.status === 'warn') return '需关注'
  return '健康'
})

function metricType(level) {
  if (level === 'error') return 'error'
  if (level === 'warn') return 'warning'
  return 'success'
}

function metricText(level) {
  if (level === 'error') return '异常'
  if (level === 'warn') return '预警'
  return '正常'
}

function findingType(level) {
  if (level === 'error') return 'error'
  if (level === 'warn') return 'warning'
  return 'default'
}

function findingText(level) {
  if (level === 'error') return '严重'
  if (level === 'warn') return '警告'
  return '提示'
}

const metricsColumns = [
  { title: '指标', dataIndex: 'name', key: 'name', minWidth: 160 },
  { title: '当前值', dataIndex: 'value', key: 'value', width: 140 },
  { title: '状态', key: 'level', width: 120 },
  { title: '说明', dataIndex: 'description', key: 'description', minWidth: 240, ellipsis: true },
];

const findingsColumns = [
  { title: '级别', key: 'level', width: 100 },
  { title: '问题', dataIndex: 'title', key: 'title', minWidth: 180 },
  { title: '详情', dataIndex: 'detail', key: 'detail', minWidth: 260, ellipsis: true },
  { title: '处理建议', dataIndex: 'suggestion', key: 'suggestion', minWidth: 320, ellipsis: true },
];

const logColumns = [
  { title: '文件', dataIndex: 'fileName', key: 'fileName', width: 220, ellipsis: true },
  { title: '级别', key: 'level', width: 100 },
  { title: '日志片段', dataIndex: 'message', key: 'message', minWidth: 520, ellipsis: true },
];

function loadReport() {
  loading.value = true
  diagnoseAiOps().then(response => {
    report.value = response.data || report.value
  }).catch(() => {
    proxy.$modal.msgError('AI运维诊断失败')
  }).finally(() => {
    loading.value = false
  })
}

loadReport()
</script>

<style lang="scss" scoped>
.ai-ops-page {
  .ops-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 15px;
  }

  .ops-title {
    font-size: 22px;
    font-weight: 600;
    line-height: 32px;
    color: #1f2937;
  }

  .ops-subtitle {
    margin-top: 4px;
    font-size: 13px;
    color: #606266;
  }

  .ops-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .summary-card {
    min-height: 142px;
    margin-bottom: 15px;
  }

  .summary-label {
    color: #606266;
    font-size: 13px;
    margin-bottom: 10px;
  }

  .summary-score {
    font-size: 44px;
    font-weight: 700;
    line-height: 54px;
    color: #16a34a;
  }

  .score-warn {
    color: #d97706;
  }

  .score-error {
    color: #dc2626;
  }

  .summary-time {
    margin-top: 10px;
    color: #909399;
    font-size: 12px;
  }

  .suggestion-line {
    color: #303133;
    font-size: 13px;
    line-height: 28px;
  }

  .card-box {
    margin-bottom: 15px;
  }

  .bottom {
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .ai-ops-page {
    .ops-header {
      align-items: flex-start;
      flex-direction: column;
    }

    .ops-actions {
      width: 100%;
      justify-content: space-between;
    }
  }
}
</style>
