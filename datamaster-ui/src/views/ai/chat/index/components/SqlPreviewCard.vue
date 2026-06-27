<template>
  <div class="sql-preview-card" v-if="sql">
    <div class="card-header">
      <div class="header-left">
        <el-icon><Document /></el-icon>
        <span>生成的SQL</span>
        <el-tag v-if="executed" size="small" type="success">已执行</el-tag>
        <el-tag v-else-if="executeError" size="small" type="danger">执行失败</el-tag>
      </div>
      <div class="header-right">
        <el-button
          v-if="!executed && !executeError"
          type="primary"
          size="small"
          :loading="executing"
          @click="handleExecute"
        >
          <el-icon><VideoPlay /></el-icon>
          执行SQL
        </el-button>
        <el-button
          type="primary"
          size="small"
          link
          @click="handleCopy"
        >
          <el-icon><CopyDocument /></el-icon>
          复制
        </el-button>
      </div>
    </div>
    <div class="sql-content">
      <pre><code>{{ sql }}</code></pre>
    </div>
    <div class="card-footer" v-if="resultCount !== null">
      <span class="result-count">查询结果: {{ resultCount }} 条记录</span>
    </div>
    <div class="execute-error" v-if="executeError">
      <el-alert
        :title="executeError"
        type="error"
        show-icon
        :closable="false"
      />
    </div>
  </div>
</template>

<script setup>
import { Document, VideoPlay, CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  sql: {
    type: String,
    default: ''
  },
  executed: {
    type: Boolean,
    default: false
  },
  executing: {
    type: Boolean,
    default: false
  },
  executeError: {
    type: String,
    default: ''
  },
  resultCount: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['on-execute'])

const handleExecute = () => {
  emit('on-execute')
}

const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.sql)
    ElMessage.success('SQL已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}
</script>

<style lang="scss" scoped>
.sql-preview-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  overflow: hidden;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    background: #f8f9fa;
    border-bottom: 1px solid #e9ecef;

    .header-left {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 500;
      color: #606266;

      .el-icon {
        color: #409eff;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .sql-content {
    padding: 14px;
    background: #1e1e1e;
    overflow-x: auto;

    pre {
      margin: 0;

      code {
        font-family: 'Consolas', 'Monaco', monospace;
        font-size: 13px;
        line-height: 1.6;
        color: #d4d4d4;
      }
    }
  }

  .card-footer {
    padding: 8px 14px;
    background: #f8f9fa;
    border-top: 1px solid #e9ecef;

    .result-count {
      font-size: 12px;
      color: #909399;
    }
  }

  .execute-error {
    padding: 0 14px 14px;
  }
}
</style>
