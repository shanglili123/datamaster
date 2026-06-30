<template>
  <div class="app-container ai-ops-page">
    <div class="ops-header">
      <div>
        <div class="ops-title">AI运维</div>
        <div class="ops-subtitle">{{ report.summary || '正在等待诊断结果' }}</div>
      </div>
      <div class="ops-actions">
        <el-tag :type="statusType" effect="dark">{{ statusText }}</el-tag>
        <el-button type="primary" icon="Refresh" :loading="loading" @click="loadReport">重新诊断</el-button>
      </div>
    </div>

    <el-row :gutter="15">
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card">
          <div class="summary-label">健康评分</div>
          <div class="summary-score" :class="'score-' + report.status">{{ report.score ?? '--' }}</div>
          <div class="summary-time">{{ report.diagnoseTime || '--' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="16">
        <el-card class="summary-card">
          <div class="summary-label">智能建议</div>
          <div v-for="(item, index) in report.suggestions" :key="index" class="suggestion-line">
            {{ index + 1 }}. {{ item }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="15">
      <el-col :span="24" class="card-box">
        <el-card>
          <template #header>
            <Monitor style="width: 1em; height: 1em; vertical-align: middle;" />
            <span style="vertical-align: middle;">关键指标</span>
          </template>
          <el-table :data="report.metrics" border>
            <el-table-column prop="name" label="指标" min-width="160" />
            <el-table-column prop="value" label="当前值" width="140" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="metricType(row.level)">{{ metricText(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="24" class="card-box">
        <el-card>
          <template #header>
            <Warning style="width: 1em; height: 1em; vertical-align: middle;" />
            <span style="vertical-align: middle;">风险发现</span>
          </template>
          <el-empty v-if="!report.findings || !report.findings.length" description="暂无风险发现" />
          <el-table v-else :data="report.findings" border>
            <el-table-column label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="findingType(row.level)">{{ findingText(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="问题" min-width="180" />
            <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
            <el-table-column prop="suggestion" label="处理建议" min-width="320" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="24" class="card-box bottom">
        <el-card>
          <template #header>
            <Document style="width: 1em; height: 1em; vertical-align: middle;" />
            <span style="vertical-align: middle;">近期异常日志</span>
          </template>
          <el-empty v-if="!report.logEvents || !report.logEvents.length" description="未发现异常日志片段" />
          <el-table v-else :data="report.logEvents" border>
            <el-table-column prop="fileName" label="文件" width="220" show-overflow-tooltip />
            <el-table-column label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="findingType(row.level)">{{ findingText(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="日志片段" min-width="520" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { diagnoseAiOps } from '@/api/system/monitor/aiOps.js'

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
  if (report.value.status === 'error') return 'danger'
  if (report.value.status === 'warn') return 'warning'
  return 'success'
})

const statusText = computed(() => {
  if (report.value.status === 'error') return '高风险'
  if (report.value.status === 'warn') return '需关注'
  return '健康'
})

function metricType(level) {
  if (level === 'error') return 'danger'
  if (level === 'warn') return 'warning'
  return 'success'
}

function metricText(level) {
  if (level === 'error') return '异常'
  if (level === 'warn') return '预警'
  return '正常'
}

function findingType(level) {
  if (level === 'error') return 'danger'
  if (level === 'warn') return 'warning'
  return 'info'
}

function findingText(level) {
  if (level === 'error') return '严重'
  if (level === 'warn') return '警告'
  return '提示'
}

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
