<template>
  <a-modal
    :open="visible"
    :title="title"
    :width="860"
    :footer="null"
    :destroy-on-close="true"
    @cancel="emit('update:visible', false)"
  >
    <a-spin :spinning="loading">
      <template v-if="!loading">
        <div class="row-id-bar" v-if="primaryKey && Object.keys(primaryKey).length">
          数据行主键：<a-tag v-for="(v, k) in primaryKey" :key="k">{{ k }} = {{ v }}</a-tag>
        </div>
        <div class="row-id-bar" v-else>
          <a-alert type="warning" show-icon message="该对象集未配置主键属性，无法精确匹配该行所属的变更记录。" />
        </div>

        <template v-if="matched.length > 0">
          <div class="graph-wrap" id="row_lineage_graph"></div>
          <TeleportContainer />
          <div class="tl-more" v-if="matchedCount > 10">
            命中 {{ matchedCount }} 条记录，仅展示最近 10 条
          </div>
        </template>
        <a-empty v-else description="该数据行没有命中的变更记录（骨架：无主键或快照未含该行）" />
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup name="OntObjectRowChangesDialog">
import { defineComponent, h } from 'vue'
import { Modal } from 'ant-design-vue'
import { getObjectLineage } from '@/api/ont/objectInstance'
import useUserStore from '@/store/system/user'
import { getTeleport, register } from '@antv/x6-vue-shape'
import { createBaseGraph } from '@/views/meta/components/common'
import RowLineageObjectNode from './RowLineageObjectNode.vue'
import RowLineageExecNode from './RowLineageExecNode.vue'

const TeleportContainer = defineComponent(getTeleport())

// 注册节点（防重复）
try {
  register({
    shape: 'RowLineageObjectNode',
    width: 220,
    height: 110,
    component: RowLineageObjectNode
  })
} catch { /* no-empty */ }
try {
  register({
    shape: 'RowLineageExecNode',
    width: 260,
    height: 72,
    component: RowLineageExecNode
  })
} catch { /* no-empty */ }

const props = defineProps({
  visible: { type: Boolean, default: false },
  conceptId: { type: [Number, String], default: null },
  conceptName: { type: String, default: '' },
  tableName: { type: String, default: '' },
  primaryKey: { type: Object, default: null },
  rowPayload: { type: Object, default: null }
})
const emit = defineEmits(['update:visible'])

const userStore = useUserStore()
const loading = ref(false)
const versions = ref([])
const matched = ref([])
const matchedCount = ref(0)

let graph = null

const title = computed(() => `数据变化 - ${props.conceptName || props.tableName || props.conceptId}`)

// 快照可能形态：{ value: [行...] }（原数组被包一层） 或 { 列: 值 }（单对象） 或 null
function snapshotRows(snapshot) {
  if (!snapshot) return []
  const rows = []
  const pushRow = (m) => {
    if (m && typeof m === 'object' && !Array.isArray(m)) rows.push(m)
  }
  if (Array.isArray(snapshot)) {
    snapshot.forEach(pushRow)
  } else if (snapshot.value !== undefined) {
    const v = snapshot.value
    if (Array.isArray(v)) v.forEach(pushRow)
    else pushRow(v)
  } else {
    pushRow(snapshot)
  }
  return rows
}

// 行是否命中：主键列值全部相等（物理列名做 key）
function rowHit(row, pk) {
  if (!row || !pk) return false
  return Object.keys(pk).every(key => row[key] !== undefined && String(row[key]) === String(pk[key]))
}

function snapshotHits(snapshot, pk) {
  if (!pk) return false
  return snapshotRows(snapshot).some(row => rowHit(row, pk))
}

// 按主键值匹配该行参与过的执行记录
function computeMatched() {
  const pk = props.primaryKey
  const all = versions.value || []
  const hits = all.filter(v => {
    if (!pk || !Object.keys(pk).length) return false
    const type = v.actionType
    if (type === 'CREATE') return snapshotHits(v.afterData, pk)
    if (type === 'DELETE') return snapshotHits(v.beforeData, pk)
    return snapshotHits(v.beforeData, pk) || snapshotHits(v.afterData, pk)
  })
  hits.sort((a, b) => new Date(a.executeTime || 0) - new Date(b.executeTime || 0))
  matchedCount.value = hits.length
  matched.value = hits.slice(-10)
}

const STATUS_STROKE = {
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

function buildGraph() {
  const container = document.getElementById('row_lineage_graph')
  if (!container) return
  graph = createBaseGraph(container).graph
  bindGraphEvents()
  renderGraph()
}

function bindGraphEvents() {
  if (!graph) return
  graph.on('node:click', ({ node }) => {
    const d = node.getData() || {}
    if (d.full && d.full.beforeData !== undefined && d.full.executeTime) {
      // 变更记录节点
      showExecDetail(d)
    } else {
      // 数据行节点
      showRowDetail(d)
    }
  })
}

function stringify(obj) {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

function showExecDetail(d) {
  const f = d.full || {}
  Modal.info({
    title: `执行详情 - ${d.actionName || '动作执行'}`,
    width: 640,
    content: h('div', {}, [
      h('p', { style: 'margin-bottom:8px' }, [
        `类型：${d.actionType || '-'}　状态：${d.status || '-'}　时间：${d.executeTime || '-'}`
      ]),
      h('pre', { style: 'white-space:pre-wrap;background:#fafafa;padding:10px;border-radius:4px;max-height:360px;overflow:auto;font-size:12px' },
        `【变更前 beforeData】\n${stringify(f.beforeData)}\n\n【变更后 afterData】\n${stringify(f.afterData)}`
      )
    ]),
    okText: '关闭'
  })
}

function showRowDetail(d) {
  const f = d.full || {}
  const pk = d.primaryKey
  const pkText = pk && Object.keys(pk).length ? Object.entries(pk).map(([k, v]) => `${k}=${v}`).join(', ') : '无主键'
  Modal.info({
    title: `数据行 - ${d.conceptName || d.tableName || ''}`,
    width: 640,
    content: h('div', {}, [
      h('p', { style: 'margin-bottom:8px' }, [
        `表：${d.tableName || '-'}　主键：${pkText}`
      ]),
      h('pre', { style: 'white-space:pre-wrap;background:#fafafa;padding:10px;border-radius:4px;max-height:360px;overflow:auto;font-size:12px' },
        stringify(Object.keys(f).length ? f : pk)
      )
    ]),
    okText: '关闭'
  })
}

function renderGraph() {
  if (!graph) return
  graph.clearCells()

  const pk = props.primaryKey
  const pkLabel = pk ? Object.entries(pk).map(([k, v]) => `${k}=${v}`).join(', ') : ''
  const rootId = 'row_' + (pkLabel || Math.random().toString(36).slice(2, 8))

  // 数据行：数据行节点
  graph.addNode({
    id: rootId,
    shape: 'RowLineageObjectNode',
    x: 0,
    y: 0,
    data: {
      conceptName: props.conceptName,
      tableName: props.tableName,
      primaryKey: props.primaryKey,
      full: props.rowPayload || props.primaryKey || {}
    }
  })

  const NODE_W = 260
  const NODE_H = 72
  const GAP_X = 50
  const ROW_H = NODE_H + 40  // 行间距
  const MAX_W = 780           // 一行最大宽度（弹窗 860 减去内边距）

  let lastId = rootId
  let rowIdx = 0
  let colIdx = 0

  matched.value.forEach((item, idx) => {
    const execId = 'exec_' + (item.executionId || idx)
    const stroke = STATUS_STROKE[item.status] || '#1890ff'

    // 计算当前位置
    const x = (colIdx + 1) * (NODE_W + GAP_X)
    const y = rowIdx * ROW_H

    graph.addNode({
      id: execId,
      shape: 'RowLineageExecNode',
      x: x,
      y: y,
      data: {
        actionName: item.actionName,
        actionType: item.actionType,
        status: item.status,
        executeTime: item.executeTime,
        full: item
      }
    })

    // 边：上一个节点 → 当前节点（链式）
    graph.addEdge({
      id: lastId + '_' + execId,
      source: lastId,
      target: execId,
      attrs: {
        line: {
          stroke: stroke,
          strokeWidth: 1.5,
          targetMarker: { name: 'classic', size: 6 }
        }
      }
    })

    lastId = execId
    colIdx++

    // 折行：超出一行最大宽度时换行
    if ((colIdx + 1) * (NODE_W + GAP_X) > MAX_W) {
      colIdx = 0
      rowIdx++
    }
  })

  graph.centerContent()
}

watch(
  () => props.visible,
  async (val) => {
    if (!val || !props.conceptId) return
    loading.value = true
    versions.value = []
    matched.value = []
    matchedCount.value = 0
    try {
      const res = await getObjectLineage(props.conceptId, userStore.spaceId, userStore.spaceCode)
      const data = res.data || {}
      versions.value = Array.isArray(data.versions) ? data.versions : []
      computeMatched()
      if (matched.value.length > 0) {
        // nextTick 等 DOM 更新，再等 modal 动画完成
        nextTick(() => {
          setTimeout(() => {
            const el = document.getElementById('row_lineage_graph')
            if (el && el.offsetHeight > 0) {
              buildGraph()
            } else {
              // 容器还没就绪，再等一轮
              setTimeout(() => buildGraph(), 500)
            }
          }, 300)
        })
      }
    } finally {
      loading.value = false
    }
  }
)

onBeforeUnmount(() => {
  if (graph) {
    graph.dispose()
    graph = null
  }
})
</script>

<style lang="scss" scoped>
.row-id-bar {
  margin-bottom: 16px;
  font-size: 13px;
  color: #595959;
}

.graph-wrap {
  width: 100%;
  height: 480px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  background: #f2f7fa;
}

.tl-more {
  color: #999;
  font-size: 12px;
  text-align: center;
  padding: 4px 0 8px;
}
</style>
