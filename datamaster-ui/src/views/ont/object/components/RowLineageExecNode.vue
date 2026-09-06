<template>
  <div class="row-lineage-exec" :class="'status-' + (store.status || 'default')">
    <div class="exec-left" :style="{ background: barColor }"></div>
    <div class="exec-content">
      <div class="exec-top">
        <span class="exec-name">{{ store.actionName || '动作执行' }}</span>
        <span class="exec-type" :class="'tag-' + (store.actionType || '').toLowerCase()">{{ store.actionType || '-' }}</span>
      </div>
      <div class="exec-bottom">
        <span class="exec-time">{{ fmtTime(store.executeTime) }}</span>
        <span class="exec-status" :class="'st-' + (store.status || '').toLowerCase()">{{ statusLabel(store.status) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup name="RowLineageExecNode">
import { reactive, computed, inject, onMounted } from 'vue'

const store = reactive({
  actionName: '',
  actionType: '',
  status: '',
  executeTime: null
})
const getNode = inject('getNode')

function setupData(data) {
  store.actionName = data.actionName || ''
  store.actionType = data.actionType || ''
  store.status = data.status || ''
  store.executeTime = data.executeTime || null
}

onMounted(() => {
  const node = getNode()
  setupData(node.data)
  node.on('change:data', (d) => setupData(d))
})

const STATUS_COLOR = {
  EXECUTED: '#52c41a',
  ROLLED_BACK: '#722ed1',
  FAILED: '#ff4d4f',
  RUNNING: '#13c2c2',
  RECONCILIATION_REQUIRED: '#fa541c',
  REJECTED: '#ff4d4f',
  APPROVED: '#1890ff',
  PENDING_APPROVAL: '#faad14',
  DRAFT: '#bfbfbf'
}
const barColor = computed(() => STATUS_COLOR[store.status] || '#1890ff')

function fmtTime(t) {
  if (!t) return '-'
  try {
    return String(t).replace('T', ' ').slice(0, 16)
  } catch {
    return String(t)
  }
}

function statusLabel(s) {
  const m = { EXECUTED: '已执行', ROLLED_BACK: '已回滚', FAILED: '失败', RUNNING: '执行中', RECONCILIATION_REQUIRED: '需对账', APPROVED: '已审批', PENDING_APPROVAL: '待审批', DRAFT: '草稿', REJECTED: '已拒绝' }
  return m[s] || s || '-'
}
</script>

<style lang="scss" scoped>
.row-lineage-exec {
  width: 100%;
  height: 100%;
  display: flex;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  }
}

.exec-left {
  width: 4px;
  flex-shrink: 0;
}

.exec-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 6px 10px;
  min-width: 0;
}

.exec-top {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
}

.exec-name {
  font-weight: 600;
  font-size: 12px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exec-type {
  font-size: 10px;
  padding: 0 4px;
  border-radius: 2px;
  white-space: nowrap;
  &.tag-update { background: #e6f7ff; color: #1890ff; }
  &.tag-create { background: #f6ffed; color: #52c41a; }
  &.tag-delete { background: #fff2f0; color: #ff4d4f; }
  &.tag-select { background: #e6fffb; color: #13c2c2; }
  &.tag-function { background: #f9f0ff; color: #722ed1; }
}

.exec-bottom {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.exec-time {
  font-size: 11px;
  color: #999;
}

.exec-status {
  font-size: 10px;
  &.st-executed { color: #52c41a; }
  &.st-rolled_back { color: #722ed1; }
  &.st-failed { color: #ff4d4f; }
  &.st-rejected { color: #ff4d4f; }
  &.st-approved { color: #1890ff; }
  &.st-pending_approval { color: #faad14; }
  &.st-draft { color: #bfbfbf; }
}
</style>
