<template>
  <div class="quality-risk-card" v-if="warning">
    <div class="card-header">
      <el-icon><WarningFilled /></el-icon>
      <span>质量探查风险提示</span>
    </div>
    <div class="card-content">
      <div class="warning-message">{{ warning }}</div>
      <div class="risk-actions" v-if="showActions">
        <el-button size="small" type="primary" plain @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          重新核检
        </el-button>
        <el-button size="small" type="info" plain @click="handleContact">
          联系负责人
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { WarningFilled, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  warning: {
    type: String,
    default: ''
  },
  showActions: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['on-refresh', 'on-contact'])

const handleRefresh = () => {
  emit('on-refresh')
}

const handleContact = () => {
  emit('on-contact')
}
</script>

<style lang="scss" scoped>
.quality-risk-card {
  background: #fff7e6;
  border-radius: 8px;
  border: 1px solid #ffd591;
  overflow: hidden;

  .card-header {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 14px;
    background: #fff1b8;
    border-bottom: 1px solid #ffd591;

    .el-icon {
      color: #fa8c16;
    }

    span {
      font-size: 13px;
      font-weight: 500;
      color: #d46b08;
    }
  }

  .card-content {
    padding: 14px;

    .warning-message {
      font-size: 13px;
      line-height: 1.6;
      color: #873800;
      white-space: pre-wrap;
    }

    .risk-actions {
      display: flex;
      gap: 8px;
      margin-top: 12px;
    }
  }
}
</style>
