<template>
  <div class="workflow-panel">
    <div class="panel-toolbar">
      <a-button type="primary" @click="handleAdd" v-hasPermi="['ont:action:add']">
        <template #icon><PlusOutlined /></template>新增编排
      </a-button>
      <a-input-search v-model:value="query.name" placeholder="搜索编排名称" class="search-input" allow-clear @search="loadList" />
    </div>

    <a-alert class="runtime-tip" type="info" show-icon message="当前支持图形化编排定义与 DAG 校验；流程运行、条件求值和并行调度将在运行时阶段接入。" />

    <a-table :loading="loading" :columns="columns" :data-source="rows" row-key="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'PUBLISHED' ? 'green' : 'blue'">{{ record.status === 'PUBLISHED' ? '已发布' : '草稿' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '启用' : '停用' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-button type="link" size="small" @click="handleEdit(record)">编排</a-button>
          <a-button type="link" size="small" @click="handleValidate(record)">校验</a-button>
          <a-button v-if="!record.enabled" type="link" size="small" class="publish-btn" @click="handlePublish(record)">{{ record.status === 'PUBLISHED' ? '启用' : '发布' }}</a-button>
          <a-button v-else type="link" size="small" @click="handleDisable(record)">停用</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
        </template>
      </template>
    </a-table>
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <a-modal v-model:open="editorOpen" :title="form.id ? '编辑动作编排' : '新增动作编排'"
      :width="designerBounds.width" :style="designerModalStyle"
      wrap-class-name="workflow-designer-modal" destroy-on-close :footer="null"
      @after-close="disposeGraph">
      <div class="designer-modal-body">
        <div class="designer-topbar">
          <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="workflow-basic-form">
            <a-form-item label="编排名称" name="name"><a-input v-model:value="form.name" placeholder="如：订单发货流程" /></a-form-item>
            <a-form-item label="失败策略"><a-select v-model:value="form.failurePolicy" :options="failurePolicyOptions" /></a-form-item>
            <a-form-item label="事件标识"><a-input v-model:value="form.triggerRef" placeholder="预留：数据到达事件" /></a-form-item>
          </a-form>
          <div class="topbar-actions">
            <a-button @click="fitGraph">适应画布</a-button>
            <a-button @click="editorOpen = false">取消</a-button>
            <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
          </div>
        </div>

        <div class="designer-shell">
          <aside class="node-palette">
            <div class="side-title">控制节点</div>
            <div class="palette-list">
              <div v-for="item in controlNodeTypes" :key="item.value" class="palette-item" @mousedown="startPaletteDrag($event, { nodeType: item.value })">
                <span class="palette-dot" :style="{ background: nodeColor(item.value) }"></span>
                <div><strong>{{ item.label }}</strong><small>{{ item.hint }}</small></div>
              </div>
            </div>

            <div class="side-title action-title">动作节点</div>
            <a-input v-model:value="actionKeyword" size="small" allow-clear placeholder="搜索动作" />
            <div class="palette-list action-list">
              <a-empty v-if="!filteredActions.length" :image="simpleImage" description="暂无可用动作" />
              <div v-for="action in filteredActions" :key="action.id" class="palette-item action-item"
                @mousedown="startPaletteDrag($event, { nodeType: 'ACTION', actionId: action.id })">
                <span class="palette-dot action-dot"></span>
                <div><strong>{{ action.name }}</strong><small>{{ action.actionType }}</small></div>
              </div>
            </div>
            <div class="palette-help">把节点拖到中间画布，再从节点右侧端口拖线到下一个节点。</div>
          </aside>

          <main class="workflow-canvas-wrap">
            <div ref="canvasRef" class="workflow-canvas"></div>
          </main>

          <aside class="config-panel">
            <template v-if="selectedNode">
              <div class="side-title">节点配置</div>
              <div class="selected-type"><span class="palette-dot" :style="{ background: nodeColor(selectedNode.nodeType) }"></span>{{ nodeTypeLabel(selectedNode.nodeType) }}</div>
              <label class="config-field"><span>节点名称</span><a-input v-model:value="selectedNode.name" @change="refreshSelectedNode" /></label>
              <label v-if="selectedNode.nodeType === 'START'" class="config-field"><span>触发动作</span>
                <a-select v-model:value="selectedNode.actionId" :options="triggerActionSelectOptions" show-search option-filter-prop="label"
                  placeholder="选择已有动作作为触发来源" @change="changeSelectedAction" />
              </label>
              <a-alert v-if="selectedNode.nodeType === 'START'" class="node-role-tip" type="info" show-icon :message="startTriggerSummary" />
              <label v-if="selectedNode.nodeType === 'ACTION'" class="config-field"><span>执行动作</span>
                <a-select v-model:value="selectedNode.actionId" :options="actionSelectOptions" show-search option-filter-prop="label" placeholder="选择已有动作" @change="changeSelectedAction" />
              </label>
              <label v-if="selectedNode.nodeType === 'PARALLEL_JOIN'" class="config-field"><span>汇聚策略</span>
                <a-select v-model:value="selectedNode.joinPolicy" :options="joinPolicyOptions" />
              </label>
              <a-alert v-if="selectedNode.nodeType === 'END'" class="node-role-tip" type="success" show-icon
                message="结束守护节点不执行业务动作；流程只有到达这里才算正常结束。" />
              <a-button danger block @click="removeSelectedNode">删除节点</a-button>
            </template>

            <template v-else-if="selectedEdge">
              <div class="side-title">连线配置</div>
              <div class="edge-summary">{{ nodeName(selectedEdge.fromNodeKey) }} → {{ nodeName(selectedEdge.toNodeKey) }}</div>
              <label class="config-field"><span>优先级</span><a-input-number v-model:value="selectedEdge.priority" :min="0" /></label>

              <template v-if="selectedEdgeIsCondition">
                <a-divider orientation="left">分支条件</a-divider>
                <a-alert class="condition-source-tip" type="info" show-icon
                  message="动态值必须明确来自触发对象、Workflow 输入、上游动作输出或运行上下文。" />
                <a-alert v-if="selectedEdge.legacyCondition" class="condition-source-tip" type="warning" show-icon
                  message="这条边是旧版文本条件，请重新选择字段和操作符后保存。" />
                <a-switch v-model:checked="selectedEdge.defaultBranch" checked-children="默认分支" un-checked-children="条件分支" @change="refreshSelectedEdge" />
                <template v-if="!selectedEdge.defaultBranch">
                  <reference-editor title="左侧动态值" :model="selectedEdge.condition.left" :source-options="referenceSourceOptions"
                    :property-options="triggerPropertyOptions" :step-options="upstreamActionOptions" :input-options="workflowInputOptions"
                    :step-output-options="stepOutputOptions" :context-options="contextOptions" @change="refreshSelectedEdge" />
                  <label class="config-field"><span>操作符</span>
                    <a-select v-model:value="selectedEdge.condition.operator" :options="operatorOptions" @change="refreshSelectedEdge" />
                  </label>
                  <template v-if="operatorNeedsRight">
                    <label class="config-field"><span>右值类型</span>
                      <a-radio-group v-model:value="selectedEdge.condition.right.mode" button-style="solid" size="small" @change="refreshSelectedEdge">
                        <a-radio-button value="FIXED">固定值</a-radio-button>
                        <a-radio-button value="REFERENCE">动态引用</a-radio-button>
                      </a-radio-group>
                    </label>
                    <label v-if="selectedEdge.condition.right.mode === 'FIXED'" class="config-field"><span>固定值</span>
                      <a-input v-model:value="selectedEdge.condition.right.value" placeholder="例如 0、待发货" @change="refreshSelectedEdge" />
                    </label>
                    <reference-editor v-else title="右侧动态值" :model="selectedEdge.condition.right" :source-options="referenceSourceOptions"
                      :property-options="triggerPropertyOptions" :step-options="upstreamActionOptions" :input-options="workflowInputOptions"
                      :step-output-options="stepOutputOptions" :context-options="contextOptions" @change="refreshSelectedEdge" />
                  </template>
                  <div class="condition-preview">{{ conditionPreview(selectedEdge) }}</div>
                </template>
              </template>
              <a-alert v-else type="info" show-icon message="只有条件节点的出边需要配置分支条件。" />
              <a-button danger block class="delete-edge" @click="removeSelectedEdge">删除连线</a-button>
            </template>

            <template v-else>
              <div class="empty-config">
                <NodeIndexOutlined />
                <p>点击画布中的节点或连线进行配置</p>
              </div>
            </template>
          </aside>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup name="OntWorkflowPanel">
import { Graph } from '@antv/x6'
import { Dnd } from '@antv/x6-plugin-dnd'
import { Empty } from 'ant-design-vue'
import { NodeIndexOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { getActionsByOntology } from '@/api/ont/action'
import { listConcept } from '@/api/ont/concept'
import { listProperty } from '@/api/ont/property'
import { addWorkflow, delWorkflow, disableWorkflow, getWorkflow, listWorkflow, publishWorkflow, updateWorkflow, validateWorkflow } from '@/api/ont/workflow'
import ReferenceEditor from './workflow/ReferenceEditor.vue'

const props = defineProps({ ontologyId: { type: [String, Number], required: true } })
const { proxy } = getCurrentInstance()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const canvasRef = ref(null)
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const editorOpen = ref(false)
const actionOptions = ref([])
const conceptOptions = ref([])
const triggerPropertyOptions = ref([])
const selectedNodeKey = ref(null)
const selectedEdgeKey = ref(null)
const actionKeyword = ref('')
const designerBounds = reactive({ left: 0, top: 0, width: window.innerWidth, height: window.innerHeight })
let graph = null
let graphDnd = null
let localKey = 0
let renderingGraph = false
const graphNodeWidth = 170
const graphNodeHeight = 58
const graphDropPadding = 16

const query = reactive({ ontologyId: props.ontologyId, name: undefined, pageNum: 1, pageSize: 10 })
const form = reactive({ id: undefined, ontologyId: props.ontologyId, name: '', code: '', triggerConceptId: undefined, failurePolicy: 'STOP', triggerRef: '', description: '', inputSchema: undefined, outputSchema: undefined, nodes: [], edges: [] })
const rules = { name: [{ required: true, message: '请输入编排名称' }] }
const columns = [
  { title: '编排名称', dataIndex: 'name' },
  { title: '版本', dataIndex: 'version', width: 80, align: 'center' }, { title: '状态', key: 'status', width: 100 },
  { title: '启用', key: 'enabled', width: 90 }, { title: '失败策略', dataIndex: 'failurePolicy', width: 110 },
  { title: '操作', key: 'operation', width: 280, fixed: 'right' }
]
const nodeTypes = [
  { value: 'START', label: '开始触发' }, { value: 'ACTION', label: '动作' }, { value: 'CONDITION', label: '条件' },
  { value: 'PARALLEL_SPLIT', label: '并行拆分' }, { value: 'PARALLEL_JOIN', label: '并行汇聚' }, { value: 'END', label: '结束守护' }
]
const controlNodeTypes = [
  { value: 'START', label: '开始触发', hint: '选择已有动作并自动确定触发概念' },
  { value: 'CONDITION', label: '条件判断', hint: '按对象、输入或上游输出分支' },
  { value: 'PARALLEL_SPLIT', label: '并行拆分', hint: '同时启动多个后续分支' },
  { value: 'PARALLEL_JOIN', label: '并行汇聚', hint: '等待多个分支汇合' },
  { value: 'END', label: '结束守护', hint: '守护流程正常结束且不能再向后连线' }
]
const failurePolicyOptions = [{ value: 'STOP', label: '失败即停止' }, { value: 'MANUAL', label: '转人工处理' }, { value: 'RETRY', label: '按节点重试' }]
const joinPolicyOptions = [{ value: 'ALL', label: '全部成功后继续' }, { value: 'ANY', label: '任一成功后继续' }]
const referenceSourceOptions = [
  { value: 'OBJECT', label: '当前触发对象' }, { value: 'INPUT', label: '流程输入' },
  { value: 'STEP_OUTPUT', label: '上游动作输出' }, { value: 'CONTEXT', label: '运行上下文' }
]
const contextOptions = [
  { value: 'objectKey', label: '对象主键 objectKey' }, { value: 'currentUser', label: '当前用户 currentUser' },
  { value: 'now', label: '当前时间 now' }, { value: 'eventId', label: '触发事件 eventId' }
]
const stepOutputOptions = [
  { value: 'status', label: '执行状态 status' },
  { value: 'objectKey', label: '对象主键 objectKey' },
  { value: 'affectedRows', label: '影响行数 affectedRows' },
  { value: 'rowCount', label: '查询行数 rowCount' },
  { value: 'output', label: '函数输出 output' },
  { value: 'preconditionResult.passed', label: '前置检查结果 preconditionResult.passed' },
  { value: 'errorCode', label: '错误码 errorCode' },
  { value: 'actionType', label: '动作类型 actionType' },
  { value: 'actionVersion', label: '动作版本 actionVersion' }
]
const operatorOptions = [
  { value: 'EQ', label: '等于 =' }, { value: 'NE', label: '不等于 ≠' }, { value: 'GT', label: '大于 >' },
  { value: 'GE', label: '大于等于 ≥' }, { value: 'LT', label: '小于 <' }, { value: 'LE', label: '小于等于 ≤' },
  { value: 'CONTAINS', label: '包含' }, { value: 'IN', label: '属于集合' },
  { value: 'IS_EMPTY', label: '为空' }, { value: 'NOT_EMPTY', label: '不为空' }
]

const actionSelectOptions = computed(() => actionOptions.value.map(action => ({ value: action.id, label: `${action.name}（${action.actionType}）` })))
const triggerActionSelectOptions = computed(() => actionOptions.value
  .filter(action => resolveActionConceptId(action))
  .map(action => ({ value: action.id, label: `${action.name}（${action.actionType}）` })))
const designerModalStyle = computed(() => ({
  top: `${designerBounds.top}px`,
  marginLeft: `${designerBounds.left}px`,
  '--workflow-designer-height': `${designerBounds.height}px`
}))
const workflowInputOptions = computed(() => extractInputPaths(form.inputSchema).map(path => ({ value: path, label: path })))
const filteredActions = computed(() => {
  const keyword = actionKeyword.value.trim().toLowerCase()
  return actionOptions.value.filter(action => !keyword || `${action.name} ${action.actionType}`.toLowerCase().includes(keyword))
})
const selectedNode = computed(() => form.nodes.find(node => node.nodeKey === selectedNodeKey.value) || null)
const startTriggerSummary = computed(() => {
  const action = actionOptions.value.find(item => String(item.id) === String(selectedNode.value?.actionId))
  const conceptId = resolveActionConceptId(action)
  if (!action) return '请选择一个已有动作作为 Workflow 的触发来源。'
  if (!conceptId) return '该动作没有明确的对象概念，不能作为开始触发动作。'
  const concept = conceptOptions.value.find(item => String(item.value) === String(conceptId))
  return `触发概念将自动使用：${concept?.label || conceptId}`
})
const selectedEdge = computed(() => form.edges.find(edge => edge.edgeKey === selectedEdgeKey.value) || null)
const selectedEdgeIsCondition = computed(() => {
  const edge = selectedEdge.value
  return !!edge && form.nodes.some(node => node.nodeKey === edge.fromNodeKey && node.nodeType === 'CONDITION')
})
const operatorNeedsRight = computed(() => selectedEdge.value && !['IS_EMPTY', 'NOT_EMPTY'].includes(selectedEdge.value.condition.operator))
const upstreamActionOptions = computed(() => {
  const edge = selectedEdge.value
  if (!edge) return []
  const predecessors = new Set()
  const queue = [edge.fromNodeKey]
  while (queue.length) {
    const current = queue.shift()
    form.edges.filter(item => item.toNodeKey === current).forEach(item => {
      if (!predecessors.has(item.fromNodeKey)) {
        predecessors.add(item.fromNodeKey)
        queue.push(item.fromNodeKey)
      }
    })
  }
  return form.nodes.filter(node => node.nodeType === 'ACTION' && predecessors.has(node.nodeKey))
    .map(node => ({ value: node.nodeKey, label: node.name }))
})

function defaultReference() { return { source: 'OBJECT', path: '', nodeKey: undefined } }
function defaultCondition() { return { type: 'COMPARE', left: defaultReference(), operator: 'EQ', right: { mode: 'FIXED', value: '', source: 'OBJECT', path: '', nodeKey: undefined } } }
function newNode(type, extra = {}) {
  const number = ++localKey
  return { localKey: number, nodeKey: type === 'START' ? 'start' : type === 'END' ? 'end' : `${type.toLowerCase()}_${number}`,
    name: nodeTypeLabel(type), nodeType: type, actionId: undefined, configJson: undefined, timeoutMs: undefined,
    retryPolicy: undefined, compensationActionId: undefined, positionX: 0, positionY: 0, joinPolicy: 'ALL', ...extra }
}
function newEdge(fromNodeKey, toNodeKey, priority = form.edges.length, edgeKey) {
  const number = ++localKey
  return { localKey: number, edgeKey: edgeKey || `edge_${number}`, fromNodeKey, toNodeKey, conditionExpr: '', priority,
    defaultBranch: false, condition: defaultCondition() }
}
function resetForm() {
  Object.assign(form, { id: undefined, ontologyId: props.ontologyId, name: '', code: '', triggerConceptId: undefined,
    failurePolicy: 'STOP', triggerRef: '', description: '', inputSchema: undefined, outputSchema: undefined,
    nodes: [], edges: [] })
  selectedNodeKey.value = null
  selectedEdgeKey.value = null
  triggerPropertyOptions.value = []
}

function nodeTypeLabel(type) { return (nodeTypes.find(item => item.value === type) || {}).label || type }
function nodeColor(type) { return { START: '#52c41a', ACTION: '#1677ff', CONDITION: '#fa8c16', PARALLEL_SPLIT: '#722ed1', PARALLEL_JOIN: '#722ed1', END: '#8c8c8c' }[type] || '#1677ff' }
function nodeName(key) { return (form.nodes.find(node => node.nodeKey === key) || {}).name || key }
function nodePorts(type) {
  const groups = {
    in: { position: 'left', attrs: { circle: { r: 5, magnet: 'passive', stroke: '#8c9bab', strokeWidth: 1.5, fill: '#fff' } } },
    out: { position: 'right', attrs: { circle: { r: 5, magnet: true, stroke: '#1677ff', strokeWidth: 1.5, fill: '#fff' } } }
  }
  const items = []
  if (type !== 'START') items.push({ id: 'in', group: 'in' })
  if (type !== 'END') items.push({ id: 'out', group: 'out' })
  return { groups, items }
}
function graphNodeConfig(node) {
  return {
    id: node.nodeKey, shape: 'rect', x: Number(node.positionX) || 0, y: Number(node.positionY) || 0, width: graphNodeWidth, height: graphNodeHeight,
    label: node.name, ports: nodePorts(node.nodeType), data: { nodeKey: node.nodeKey },
    attrs: {
      body: { fill: '#fff', stroke: nodeColor(node.nodeType), strokeWidth: 2, rx: 8, ry: 8 },
      label: { fill: '#25324b', fontSize: 13, fontWeight: 600, textWrap: { width: -18, height: -12, ellipsis: true } }
    }
  }
}
function edgeLabel(edge) {
  if (!form.nodes.some(node => node.nodeKey === edge.fromNodeKey && node.nodeType === 'CONDITION')) return ''
  if (edge.defaultBranch) return '默认'
  const left = edge.condition.left
  const right = edge.condition.right
  const source = referenceSourceOptions.find(item => item.value === left.source)?.label || left.source
  const op = operatorOptions.find(item => item.value === edge.condition.operator)?.label || edge.condition.operator
  if (['IS_EMPTY', 'NOT_EMPTY'].includes(edge.condition.operator)) return `${source}.${left.path || '?'} ${op}`
  const rightText = right.mode === 'FIXED' ? String(right.value ?? '') : `${right.source}.${right.path || '?'}`
  return `${source}.${left.path || '?'} ${op} ${rightText}`
}
function graphEdgeConfig(edge) {
  const label = edgeLabel(edge)
  return {
    id: edge.edgeKey, source: { cell: edge.fromNodeKey, port: 'out' }, target: { cell: edge.toNodeKey, port: 'in' },
    labels: label ? [{ attrs: { label: { text: label, fill: '#5b6472', fontSize: 11 }, body: { fill: '#fff', stroke: '#d9e2f0', rx: 4, ry: 4 } } }] : [],
    data: { edgeKey: edge.edgeKey }, router: { name: 'manhattan', args: { padding: 18 } }, connector: { name: 'rounded', args: { radius: 8 } },
    attrs: { wrap: { connection: true, strokeWidth: 12, stroke: 'transparent' }, line: { stroke: '#91a4bd', strokeWidth: 1.6, targetMarker: { name: 'classic', size: 8 } } }
  }
}

function initGraph() {
  disposeGraph()
  if (!canvasRef.value) return
  graph = new Graph({
    container: canvasRef.value, width: canvasRef.value.clientWidth || 800, height: canvasRef.value.clientHeight || 600,
    background: { color: '#fbfcfe' }, grid: { visible: true, type: 'dot', size: 12, args: { color: '#dfe7f1', thickness: 1 } },
    panning: true, mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'], minScale: 0.4, maxScale: 2 },
    translating: { restrict: true },
    connecting: {
      snap: true, allowBlank: false, allowLoop: false, allowNode: false, allowEdge: false, allowMulti: false,
      validateConnection({ sourceCell, targetCell, sourceMagnet, targetMagnet }) {
        if (!sourceCell || !targetCell || sourceCell.id === targetCell.id) return false
        if (!sourceMagnet || sourceMagnet.getAttribute('port-group') !== 'out') return false
        if (!targetMagnet || targetMagnet.getAttribute('port-group') !== 'in') return false
        const sourceKey = cellNodeKey(sourceCell)
        const targetKey = cellNodeKey(targetCell)
        return !form.edges.some(edge => edge.fromNodeKey === sourceKey && edge.toNodeKey === targetKey)
      },
      createEdge() {
        return graph.createEdge({ attrs: { line: { stroke: '#1677ff', strokeWidth: 1.6, strokeDasharray: '5 3', targetMarker: { name: 'classic', size: 8 } } } })
      }
    }
  })
  graphDnd = new Dnd({
    target: graph,
    scaled: false,
    getDragNode: sourceNode => sourceNode.clone({ keepId: true }),
    getDropNode: draggingNode => draggingNode.clone({ keepId: true })
  })
  bindGraphEvents()
  renderGraph()
}
function bindGraphEvents() {
  graph.on('node:added', ({ node }) => {
    if (renderingGraph) return
    const model = node.getData()?.workflowModel
    if (!model) return
    if (['START', 'END'].includes(model.nodeType) && form.nodes.some(item => item.nodeType === model.nodeType)) {
      graph.removeNode(node)
      proxy.$modal.msgWarning(model.nodeType === 'START' ? '只能配置一个开始触发节点' : '当前只允许配置一个结束守护节点')
      return
    }
    const position = clampNodePosition(node.getPosition())
    node.position(position.x, position.y)
    node.setData({ nodeKey: model.nodeKey })
    model.positionX = Math.round(position.x)
    model.positionY = Math.round(position.y)
    form.nodes.push(model)
    selectedNodeKey.value = model.nodeKey
    selectedEdgeKey.value = null
    node.toFront()
  })
  graph.on('node:click', ({ node }) => { selectedNodeKey.value = cellNodeKey(node); selectedEdgeKey.value = null })
  graph.on('edge:click', ({ edge }) => { selectedEdgeKey.value = edge.id; selectedNodeKey.value = null })
  graph.on('blank:click', () => { selectedNodeKey.value = null; selectedEdgeKey.value = null })
  graph.on('node:moved', ({ node }) => {
    const model = form.nodes.find(item => item.nodeKey === cellNodeKey(node))
    if (model) { const pos = node.getPosition(); model.positionX = Math.round(pos.x); model.positionY = Math.round(pos.y) }
  })
  graph.on('edge:connected', ({ edge }) => {
    if (renderingGraph) return
    const from = cellNodeKey(edge.getSourceCell())
    const to = cellNodeKey(edge.getTargetCell())
    if (!from || !to) { graph.removeEdge(edge); return }
    const model = newEdge(from, to, form.edges.length, edge.id)
    form.edges.push(model)
    edge.setData({ edgeKey: model.edgeKey })
    edge.attr({ line: { strokeDasharray: null, stroke: '#91a4bd' } })
    selectedEdgeKey.value = model.edgeKey
    selectedNodeKey.value = null
  })
}
function renderGraph(selectKey) {
  if (!graph) return
  renderingGraph = true
  graph.clearCells()
  graph.addNodes(form.nodes.map(graphNodeConfig))
  form.edges.forEach(edge => {
    if (graph.getCellById(edge.fromNodeKey) && graph.getCellById(edge.toNodeKey)) graph.addEdge(graphEdgeConfig(edge))
  })
  renderingGraph = false
  if (selectKey) selectedNodeKey.value = selectKey
}
function cellNodeKey(cell) {
  return cell?.getData()?.nodeKey || cell?.id
}
function syncGraphState() {
  if (!graph) return
  const actualToLogical = new Map()
  graph.getNodes().forEach(cell => {
    const nodeKey = cellNodeKey(cell)
    actualToLogical.set(cell.id, nodeKey)
    const model = form.nodes.find(item => item.nodeKey === nodeKey)
    if (model) {
      const position = cell.getPosition()
      model.positionX = Math.round(position.x)
      model.positionY = Math.round(position.y)
    }
  })
  form.edges.forEach(edge => {
    const cell = graph.getCellById(edge.edgeKey)
    if (!cell) return
    edge.fromNodeKey = actualToLogical.get(cell.getSourceCellId()) || cell.getSourceCellId()
    edge.toNodeKey = actualToLogical.get(cell.getTargetCellId()) || cell.getTargetCellId()
  })
}
function disposeGraph() {
  if (graph) { graph.dispose(); graph = null }
  graphDnd = null
}
function fitGraph() {
  if (!graph) return
  if (!graph.getNodes().length) {
    graph.zoomTo(1)
    graph.translate(0, 0)
    return
  }
  graph.zoomToFit({ padding: 50, maxScale: 1 })
}
function resizeGraph() {
  if (!graph || !canvasRef.value) return
  graph.resize(canvasRef.value.clientWidth || 800, canvasRef.value.clientHeight || 560)
}
function syncDesignerBounds() {
  const target = document.querySelector('.app-main-inner') || document.documentElement
  const rect = target.getBoundingClientRect()
  designerBounds.left = Math.max(0, Math.round(rect.left))
  designerBounds.top = Math.max(0, Math.round(rect.top))
  designerBounds.width = Math.round(rect.width) || window.innerWidth
  designerBounds.height = Math.round(rect.height) || window.innerHeight
}
async function openDesigner() {
  syncDesignerBounds()
  editorOpen.value = true
  await nextTick()
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      initGraph()
      resizeGraph()
      fitGraph()
    })
  })
}

function startPaletteDrag(event, payload) {
  if (!graph || !graphDnd || event.button !== 0) return
  if (['START', 'END'].includes(payload.nodeType) && form.nodes.some(node => node.nodeType === payload.nodeType)) {
    proxy.$modal.msgWarning(payload.nodeType === 'START' ? '只能配置一个开始触发节点' : '当前只允许配置一个结束守护节点')
    return
  }
  const action = payload.actionId ? actionOptions.value.find(item => String(item.id) === String(payload.actionId)) : null
  const model = newNode(payload.nodeType, { actionId: action?.id, name: action?.name || nodeTypeLabel(payload.nodeType) })
  const previewNode = graph.createNode({ ...graphNodeConfig(model), data: { workflowModel: model } })
  graphDnd.start(previewNode, event)
}
function clampNodePosition(position) {
  if (!graph || !canvasRef.value) return position
  const canvasRect = canvasRef.value.getBoundingClientRect()
  const visibleTopLeft = graph.clientToLocal({ x: canvasRect.left, y: canvasRect.top })
  const visibleBottomRight = graph.clientToLocal({ x: canvasRect.right, y: canvasRect.bottom })
  const minX = visibleTopLeft.x + graphDropPadding
  const minY = visibleTopLeft.y + graphDropPadding
  const maxX = Math.max(minX, visibleBottomRight.x - graphNodeWidth - graphDropPadding)
  const maxY = Math.max(minY, visibleBottomRight.y - graphNodeHeight - graphDropPadding)
  return {
    x: Math.min(maxX, Math.max(minX, position.x)),
    y: Math.min(maxY, Math.max(minY, position.y))
  }
}
function refreshSelectedNode() {
  if (!graph || !selectedNode.value) return
  const cell = graph.getCellById(selectedNode.value.nodeKey)
  if (cell) cell.attr('label/text', selectedNode.value.name)
}
async function changeSelectedAction(actionId) {
  const action = actionOptions.value.find(item => String(item.id) === String(actionId))
  if (action && selectedNode.value) {
    selectedNode.value.name = selectedNode.value.nodeType === 'START' ? `开始：${action.name}` : action.name
    if (selectedNode.value.nodeType === 'START') {
      form.triggerConceptId = resolveActionConceptId(action)
      await handleTriggerConceptChange()
    }
  }
  refreshSelectedNode()
}
function refreshSelectedEdge() {
  if (!graph || !selectedEdge.value) return
  selectedEdge.value.legacyCondition = false
  const edge = graph.getCellById(selectedEdge.value.edgeKey)
  if (!edge) return
  const label = edgeLabel(selectedEdge.value)
  edge.setLabels(label ? [{ attrs: { label: { text: label, fill: '#5b6472', fontSize: 11 }, body: { fill: '#fff', stroke: '#d9e2f0', rx: 4, ry: 4 } } }] : [])
}
function removeSelectedNode() {
  if (!selectedNode.value) return
  const key = selectedNode.value.nodeKey
  const removedType = selectedNode.value.nodeType
  form.nodes = form.nodes.filter(node => node.nodeKey !== key)
  form.edges = form.edges.filter(edge => edge.fromNodeKey !== key && edge.toNodeKey !== key)
  if (removedType === 'START') {
    form.triggerConceptId = undefined
    triggerPropertyOptions.value = []
  }
  selectedNodeKey.value = null
  renderGraph()
}
function removeSelectedEdge() {
  if (!selectedEdge.value) return
  const key = selectedEdge.value.edgeKey
  form.edges = form.edges.filter(edge => edge.edgeKey !== key)
  selectedEdgeKey.value = null
  renderGraph()
}

function parseConditionExpr(value) {
  if (!value) return { defaultBranch: true, condition: defaultCondition() }
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    if (parsed?.type === 'COMPARE') {
      return { defaultBranch: false, condition: { ...defaultCondition(), ...parsed, left: { ...defaultReference(), ...(parsed.left || {}) }, right: { ...defaultCondition().right, ...(parsed.right || {}) } } }
    }
  } catch { /* legacy expression is intentionally not reused */ }
  return { defaultBranch: false, condition: defaultCondition(), legacyCondition: true }
}
function serializeCondition(edge) {
  if (!form.nodes.some(node => node.nodeKey === edge.fromNodeKey && node.nodeType === 'CONDITION')) return ''
  if (edge.defaultBranch) return ''
  const condition = edge.condition
  const result = { type: 'COMPARE', left: { source: condition.left.source, path: condition.left.path }, operator: condition.operator }
  if (condition.left.source === 'STEP_OUTPUT') result.left.nodeKey = condition.left.nodeKey
  if (!['IS_EMPTY', 'NOT_EMPTY'].includes(condition.operator)) {
    result.right = condition.right.mode === 'FIXED'
      ? { mode: 'FIXED', value: condition.right.value }
      : { mode: 'REFERENCE', source: condition.right.source, path: condition.right.path,
          ...(condition.right.source === 'STEP_OUTPUT' ? { nodeKey: condition.right.nodeKey } : {}) }
  }
  return JSON.stringify(result)
}
function conditionPreview(edge) { return edgeLabel(edge) || '请完整配置条件' }
function resolveActionConceptId(action) {
  if (!action) return undefined
  return action.conceptId || action.sourceConceptId || undefined
}

async function loadActions() {
  const res = await getActionsByOntology(props.ontologyId)
  actionOptions.value = res.data || []
}
async function loadConcepts() {
  const res = await listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 })
  conceptOptions.value = (res.data?.rows || []).map(item => ({ value: item.id, label: item.name }))
}
async function loadTriggerProperties() {
  triggerPropertyOptions.value = []
  if (!form.triggerConceptId) return
  const res = await listProperty({ conceptId: form.triggerConceptId, pageNum: 1, pageSize: 1000 })
  triggerPropertyOptions.value = (res.data?.rows || []).map(item => ({ value: item.code, label: `${item.name}（${item.code}）` }))
}
async function handleTriggerConceptChange() {
  await loadTriggerProperties()
  const validProperties = new Set(triggerPropertyOptions.value.map(item => item.value))
  let cleared = 0
  form.edges.forEach(edge => {
    const references = [edge.condition?.left, edge.condition?.right]
    references.forEach(reference => {
      if (reference?.source === 'OBJECT' && reference.path && !validProperties.has(reference.path)) {
        reference.path = ''
        cleared++
      }
    })
  })
  if (cleared) proxy.$modal.msgInfo(`触发概念已变更，已清除 ${cleared} 个失效的对象属性引用`)
  renderGraph()
}
function extractInputPaths(schemaValue) {
  if (!schemaValue) return []
  try {
    const schema = typeof schemaValue === 'string' ? JSON.parse(schemaValue) : schemaValue
    return Object.keys(schema?.properties || {})
  } catch { return [] }
}
async function loadList() {
  loading.value = true
  try { const res = await listWorkflow(query); rows.value = res.data?.rows || []; total.value = Number(res.data?.total) || 0 } finally { loading.value = false }
}
async function handleAdd() {
  resetForm()
  await Promise.all([loadActions(), loadConcepts()])
  await openDesigner()
}
async function handleEdit(row) {
  await Promise.all([loadActions(), loadConcepts()])
  const data = (await getWorkflow(row.id)).data || {}
  Object.assign(form, data)
  form.nodes = (data.nodes || []).map((node, index) => ({ ...node, localKey: ++localKey,
    positionX: Number(node.positionX) || 80 + (index % 4) * 220, positionY: Number(node.positionY) || 100 + Math.floor(index / 4) * 120,
    joinPolicy: parseConfig(node.configJson).joinPolicy || 'ALL' }))
  form.edges = (data.edges || []).map(edge => ({ ...edge, localKey: ++localKey, ...parseConditionExpr(edge.conditionExpr) }))
  const startNode = form.nodes.find(node => node.nodeType === 'START')
  const triggerAction = actionOptions.value.find(action => String(action.id) === String(startNode?.actionId))
  if (triggerAction) form.triggerConceptId = resolveActionConceptId(triggerAction)
  await loadTriggerProperties()
  await openDesigner()
}
function parseConfig(value) { try { return typeof value === 'string' ? JSON.parse(value || '{}') : (value || {}) } catch { return {} } }

function validateLocalGraph() {
  const starts = form.nodes.filter(node => node.nodeType === 'START')
  const ends = form.nodes.filter(node => node.nodeType === 'END')
  if (starts.length !== 1) return '请从控制节点中拖入一个开始触发节点'
  if (!starts[0].actionId) return '开始触发节点必须选择一个已有动作'
  if (!form.triggerConceptId) return '开始动作没有可用的触发概念，请更换动作'
  if (ends.length !== 1) return '请从控制节点中拖入一个结束守护节点'
  if (!form.nodes.some(node => node.nodeType === 'ACTION')) return '至少拖入一个已注册动作'
  const invalidAction = form.nodes.find(node => node.nodeType === 'ACTION' && !node.actionId)
  if (invalidAction) return `动作节点“${invalidAction.name}”未绑定动作`
  const invalidEdge = form.edges.find(edge => !edge.fromNodeKey || !edge.toNodeKey)
  if (invalidEdge) return '存在未连接完整的连线'
  const nodeKeys = new Set(form.nodes.map(node => node.nodeKey))
  const missingNodeEdge = form.edges.find(edge => !nodeKeys.has(edge.fromNodeKey) || !nodeKeys.has(edge.toNodeKey))
  if (missingNodeEdge) return `连线“${missingNodeEdge.edgeKey}”引用的节点已不存在，请删除后重新连接`
  for (const edge of form.edges) {
    if (!form.nodes.some(node => node.nodeKey === edge.fromNodeKey && node.nodeType === 'CONDITION') || edge.defaultBranch) continue
    if (!edge.condition.left.path) return `条件分支“${nodeName(edge.fromNodeKey)}”未选择左侧动态值`
    if (edge.condition.left.source === 'OBJECT' && !form.triggerConceptId) return '条件读取当前对象前必须选择触发概念'
    if (edge.condition.left.source === 'STEP_OUTPUT' && !edge.condition.left.nodeKey) return '请选择左侧上游动作节点'
    if (!['IS_EMPTY', 'NOT_EMPTY'].includes(edge.condition.operator)) {
      const right = edge.condition.right
      if (right.mode === 'REFERENCE' && !right.path) return '条件右侧动态引用字段不能为空'
      if (right.mode === 'REFERENCE' && right.source === 'STEP_OUTPUT' && !right.nodeKey) return '请选择右侧上游动作节点'
    }
  }
  return ''
}
async function submit() {
  await formRef.value.validate()
  syncGraphState()
  const graphError = validateLocalGraph()
  if (graphError) { proxy.$modal.msgWarning(graphError); return }
  const payload = {
    id: form.id, ontologyId: form.ontologyId, name: form.name, code: form.code, triggerConceptId: form.triggerConceptId,
    failurePolicy: form.failurePolicy, triggerRef: form.triggerRef, description: form.description,
    inputSchema: form.inputSchema, outputSchema: form.outputSchema,
    nodes: form.nodes.map(node => ({ id: node.id, nodeKey: node.nodeKey, name: node.name, nodeType: node.nodeType,
      actionId: node.actionId, configJson: node.nodeType === 'PARALLEL_JOIN' ? JSON.stringify({ joinPolicy: node.joinPolicy }) : node.configJson,
      timeoutMs: node.timeoutMs, retryPolicy: node.retryPolicy, compensationActionId: node.compensationActionId,
      positionX: node.positionX, positionY: node.positionY })),
    edges: form.edges.map(edge => ({ id: edge.id, edgeKey: edge.edgeKey, fromNodeKey: edge.fromNodeKey,
      toNodeKey: edge.toNodeKey, conditionExpr: serializeCondition(edge), priority: edge.priority }))
  }
  saving.value = true
  try {
    await (form.id ? updateWorkflow(payload) : addWorkflow(payload))
    proxy.$modal.msgSuccess('保存成功')
    editorOpen.value = false
    loadList()
  } finally { saving.value = false }
}

async function handleValidate(row) { await validateWorkflow(row.id); proxy.$modal.msgSuccess('DAG 校验通过') }
function handlePublish(row) {
  const label = row.status === 'PUBLISHED' ? '启用' : '发布'
  proxy.$modal.confirm(`确认${label}编排“${row.name}”？`).then(async () => { await publishWorkflow(row.id); proxy.$modal.msgSuccess(`${label}成功`); loadList() })
}
function handleDisable(row) { proxy.$modal.confirm(`确认停用编排“${row.name}”？`).then(async () => { await disableWorkflow(row.id); proxy.$modal.msgSuccess('已停用'); loadList() }) }
function handleDelete(row) { proxy.$modal.confirm(`确认删除编排“${row.name}”？`).then(async () => { await delWorkflow(row.id); proxy.$modal.msgSuccess('删除成功'); loadList() }) }

watch(() => props.ontologyId, value => { query.ontologyId = value; form.ontologyId = value; loadActions(); loadConcepts(); loadList() })
onMounted(() => window.addEventListener('resize', syncDesignerBounds))
onBeforeUnmount(() => { window.removeEventListener('resize', syncDesignerBounds); disposeGraph() })
loadActions(); loadConcepts(); loadList()
defineExpose({ reload() { loadActions(); loadConcepts(); loadList() } })
</script>

<style lang="scss" scoped>
.workflow-panel { position:relative; }
.panel-toolbar { display:flex; gap:10px; margin-bottom:12px; }
.search-input { width:240px; }
.runtime-tip { margin-bottom:12px; }
.publish-btn { color:#52c41a; }
.designer-modal-body { height:calc(var(--workflow-designer-height, 100vh) - 72px); min-height:0; max-height:none; display:flex; flex-direction:column; overflow:hidden; }
.designer-topbar { display:flex; align-items:flex-end; gap:12px; padding:0 2px 12px; border-bottom:1px solid #e8edf5; }
.workflow-basic-form { display:grid; min-width:0; grid-template-columns:minmax(220px,1.5fr) 180px minmax(220px,1fr); gap:10px; flex:1; :deep(.ant-form-item) { min-width:0; margin-bottom:0; } }
.topbar-actions { display:flex; flex:0 0 auto; gap:8px; padding-bottom:1px; }
.designer-shell { display:grid; grid-template-columns:clamp(180px,16vw,220px) minmax(360px,1fr) clamp(250px,22vw,300px); flex:1; min-height:0; margin-top:12px; border:1px solid #e3e9f2; border-radius:9px; overflow:hidden; }
.node-palette,.config-panel { min-height:0; padding:14px; background:#fff; overflow:auto; }
.node-palette { border-right:1px solid #e8edf5; }
.config-panel { border-left:1px solid #e8edf5; }
.side-title { margin-bottom:10px; color:#24324a; font-size:14px; font-weight:650; }
.action-title { margin-top:18px; }
.palette-list { display:flex; flex-direction:column; gap:8px; margin-top:8px; }
.action-list { max-height:calc(100vh - 460px); overflow:auto; }
.palette-item { display:flex; align-items:center; gap:9px; padding:9px 10px; border:1px solid #dfe6ef; border-radius:7px; background:#fafcff; cursor:grab; user-select:none; }
.palette-item:hover { border-color:#91caff; box-shadow:0 3px 10px rgba(22,119,255,.08); }
.palette-item:active { cursor:grabbing; }
.palette-item div { min-width:0; display:flex; flex-direction:column; }
.palette-item strong { color:#344054; font-size:13px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.palette-item small { margin-top:2px; color:#98a2b3; font-size:11px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.palette-dot { width:9px; height:9px; flex:0 0 auto; border-radius:50%; }
.action-dot { background:#1677ff; }
.palette-help { margin-top:12px; padding:8px; color:#7a8699; font-size:11px; line-height:1.6; background:#f5f8fc; border-radius:6px; }
.workflow-canvas-wrap { min-width:0; min-height:0; background:#fbfcfe; }
.workflow-canvas { width:100%; height:100%; min-height:0; }
.selected-type { display:flex; align-items:center; gap:7px; margin-bottom:14px; padding:8px 10px; color:#344054; background:#f5f8fc; border-radius:6px; }
.config-field { display:flex; flex-direction:column; gap:5px; margin-bottom:13px; color:#667085; font-size:12px; }
.config-field :deep(.ant-input-number) { width:120px; }
.edge-summary { margin-bottom:12px; padding:8px 10px; color:#344054; font-size:12px; background:#f5f8fc; border-radius:6px; }
.condition-preview { margin:4px 0 13px; padding:8px; color:#475467; font-size:11px; line-height:1.5; word-break:break-all; background:#fff8e6; border:1px solid #ffe2a8; border-radius:6px; }
.condition-source-tip { margin-bottom:10px; :deep(.ant-alert-message) { font-size:11px; line-height:1.45; } }
.node-role-tip { margin-bottom:13px; :deep(.ant-alert-message) { font-size:11px; line-height:1.45; } }
.delete-edge { margin-top:16px; }
.empty-config { display:flex; height:100%; min-height:300px; flex-direction:column; align-items:center; justify-content:center; color:#a5afbd; text-align:center; }
.empty-config :deep(svg) { font-size:32px; }
.empty-config p { margin-top:10px; font-size:12px; }
@media (max-width:1350px) {
  .designer-shell { grid-template-columns:180px minmax(320px,1fr) 250px; }
  .workflow-basic-form { grid-template-columns:repeat(3,minmax(150px,1fr)); }
  .designer-topbar { align-items:flex-end; }
}
@media (max-width:980px) {
  .designer-modal-body { height:calc(var(--workflow-designer-height, 100vh) - 72px); overflow:auto; }
  .designer-topbar { align-items:stretch; flex-direction:column; }
  .workflow-basic-form { width:100%; grid-template-columns:repeat(2,minmax(150px,1fr)); }
  .topbar-actions { align-self:flex-end; }
  .designer-shell { min-height:560px; grid-template-columns:170px minmax(360px,1fr) 240px; overflow:auto; }
}
</style>

<style lang="scss">
.workflow-designer-modal { z-index:1100; }
.workflow-designer-modal .ant-modal { max-width:none; height:var(--workflow-designer-height, 100vh); margin-right:0; margin-bottom:0; padding:0; }
.workflow-designer-modal .ant-modal-content { height:var(--workflow-designer-height, 100vh); max-height:none; overflow:hidden; border-radius:0; padding:18px 20px 14px; }
.workflow-designer-modal .ant-modal-body { height:calc(100% - 38px); }
</style>
