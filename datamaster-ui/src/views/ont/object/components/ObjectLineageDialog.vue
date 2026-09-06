<template>
  <a-modal
    :open="visible"
    :title="title"
    :width="1100"
    :footer="null"
    :destroy-on-close="true"
    @cancel="emit('update:visible', false)"
  >
    <a-spin :spinning="loading">
      <template v-if="lineage">
        <a-tabs v-model:activeKey="activeTab">
          <!-- 数据维度：对象 -[MATERIALIZES]-> 支撑表 -->
          <a-tab-pane key="graph" tab="血缘图">
            <a-alert
              type="info"
              show-icon
              style="margin-bottom: 12px"
              message="数据维度：语义对象由物理表支撑（MATERIALIZES），并延续该表的表级上/下游血缘。"
            />
            <ObjectLineageGraph
              v-if="activeTab === 'graph'"
              :key="graphKey"
              :current-object="lineage.currentObject"
              :tables="lineage.tables"
              :permission="lineage.permission"
            />
          </a-tab-pane>

          <!-- 决策维度：作用于对象的动作执行 -->
          <a-tab-pane key="decision" tab="决策">
            <a-alert
              type="info"
              show-icon
              style="margin-bottom: 12px"
              message="决策维度：作用于该语义对象的动作执行记录（ActionExecution -> Object）。"
            />
            <a-table
              :columns="decisionColumns"
              :data-source="lineage.decisions || []"
              row-key="executionId"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- 版本维度：快照派生时间线 -->
          <a-tab-pane key="version" tab="版本">
            <a-alert
              type="info"
              show-icon
              style="margin-bottom: 12px"
              message="版本维度：由动作执行的前/后数据快照（beforeData / afterData）派生的对象变更时间线。"
            />
            <a-table
              :columns="versionColumns"
              :data-source="lineage.versions || []"
              row-key="executionId"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
                </template>
              </template>
              <template #expandedRowRender="{ record }">
                <div class="snapshot-box">
                  <div class="snapshot-title">执行前快照（beforeData）</div>
                  <pre>{{ formatSnapshot(record.beforeData) }}</pre>
                  <div class="snapshot-title">执行后快照（afterData）</div>
                  <pre>{{ formatSnapshot(record.afterData) }}</pre>
                </div>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- 权限维度：实时摘要 -->
          <a-tab-pane key="permission" tab="权限">
            <a-alert
              type="info"
              show-icon
              style="margin-bottom: 12px"
              message="权限维度：复用资产统一权限入口（resolveTable）实时计算当前空间的访问摘要。"
            />
            <template v-if="lineage.permission">
              <a-descriptions bordered :column="1" size="small">
                <a-descriptions-item label="允许访问">
                  <a-tag :color="lineage.permission.accessible ? 'green' : 'red'">
                    {{ lineage.permission.accessible ? '允许' : '拒绝' }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="判定消息">{{ lineage.permission.message || '-' }}</a-descriptions-item>
                <a-descriptions-item label="被拒绝字段">
                  {{ formatList(lineage.permission.deniedColumns) }}
                </a-descriptions-item>
                <a-descriptions-item label="允许查询字段">
                  {{ formatList(lineage.permission.allowedColumns) || '(空 = 全量放行)' }}
                </a-descriptions-item>
              </a-descriptions>
            </template>
            <a-empty v-else description="暂无权限摘要" />
          </a-tab-pane>
        </a-tabs>
      </template>
      <a-empty v-else-if="!loading" description="暂无血缘数据" />
    </a-spin>
  </a-modal>
</template>

<script setup name="OntObjectLineageDialog">
import { getObjectLineage } from '@/api/ont/objectInstance'
import useUserStore from '@/store/system/user'
import ObjectLineageGraph from './ObjectLineageGraph.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  conceptId: { type: [Number, String], default: null },
  conceptName: { type: String, default: '' },
  tableName: { type: String, default: '' }
})
const emit = defineEmits(['update:visible'])

const userStore = useUserStore()
const loading = ref(false)
const lineage = ref(null)
const activeTab = ref('graph')
const graphKey = ref(0)

const title = computed(() => `对象血缘 - ${props.conceptName || props.tableName || props.conceptId}`)

const decisionColumns = [
  { title: '执行ID', dataIndex: 'actionId', key: 'actionId', width: 90 },
  { title: '动作名称', dataIndex: 'actionName', key: 'actionName' },
  { title: '动作类型', dataIndex: 'actionType', key: 'actionType', width: 110 },
  { title: '物理表', dataIndex: 'tableName', key: 'tableName' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 130 },
  { title: '执行时间', dataIndex: 'executeTime', key: 'executeTime', width: 180 }
]

const versionColumns = [
  { title: '动作名称', dataIndex: 'actionName', key: 'actionName' },
  { title: '动作类型', dataIndex: 'actionType', key: 'actionType', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 130 },
  { title: '执行时间', dataIndex: 'executeTime', key: 'executeTime', width: 180 }
]

function statusColor(status) {
  const map = {
    EXECUTED: 'green',
    APPROVED: 'blue',
    RUNNING: 'cyan',
    PENDING_APPROVAL: 'orange',
    FAILED: 'red',
    ROLLED_BACK: 'purple',
    RECONCILIATION_REQUIRED: 'volcano',
    DRAFT: 'default',
    REJECTED: 'red'
  }
  return map[status] || 'default'
}

function formatSnapshot(data) {
  if (!data) return '（空）'
  try {
    return typeof data === 'string' ? data : JSON.stringify(data, null, 2)
  } catch (e) {
    return String(data)
  }
}

function formatList(list) {
  if (!list || list.length === 0) return ''
  return list.join('、')
}

watch(
  () => props.visible,
  async (val) => {
    if (!val || !props.conceptId) return
    loading.value = true
    lineage.value = null
    activeTab.value = 'graph'
    graphKey.value = Date.now()
    try {
      const res = await getObjectLineage(props.conceptId, userStore.spaceId, userStore.spaceCode)
      lineage.value = res.data || null
    } finally {
      loading.value = false
    }
  }
)
</script>

<style lang="scss" scoped>
.snapshot-box {
  padding: 8px 16px;
  background: #fafafa;

  .snapshot-title {
    font-size: 12px;
    font-weight: 600;
    color: #722ed1;
    margin: 8px 0 4px;
  }

  pre {
    margin: 0 0 8px;
    padding: 8px;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    font-size: 12px;
    max-height: 220px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
