<template>
  <div class="graph-panel" :class="{ 'preview-open': previewOpen }">
    <!-- 工具条：拖拽建模芯片 + 刷新/适应画布 -->
    <div class="panel-toolbar">
      <div
        class="create-chip"
        draggable="true"
        title="按住我拖到画布上松开，快速创建概念节点"
        @dragstart="onChipDragStart"
        @dragend="onChipDragEnd"
      >⬡ 概念</div>
      <a-button @click="loadGraph">
        <template #icon><ReloadOutlined /></template>
        刷新图谱
      </a-button>
      <a-button @click="fitView">适应画布</a-button>
      <span class="toolbar-tip">拖动「概念」到画布新增节点 · 从端口拉线创建关系 · 点击节点看详情 · 滚轮缩放 / 空白处拖拽平移</span>
    </div>

    <div class="graph-container" :class="{ 'graph-shrunk': previewOpen }" @dragover.prevent @drop="onCanvasDrop">
      <!-- X6 被隔离在内层宿主中：autoResize 类的尺寸回写只会作用于这层 absolute 元素，
           物理上无法再把外层容器（及下方面板）撑爆 -->
      <div ref="containerRef" class="x6-host"></div>
    </div>

    <!-- 数据预览：点击概念节点后位于可视化下方；画布随之收缩一半，页面总高不变；表头=属性名，最多 5 行，支持属性值过滤 -->
    <div v-if="selectedNode && previewOpen" class="preview-panel">
      <div class="preview-header">
        <span class="preview-title">数据预览</span>
        <a-select
          v-if="nodeBindings.length > 1"
          v-model:value="previewBindingId"
          size="small"
          :options="previewBindingOptions"
          style="width: 220px"
          @change="loadPreview"
        />
        <span v-else-if="previewTableName" class="preview-table">{{ previewTableName }}</span>
        <a-button size="small" :loading="previewLoading" @click="loadPreview">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <span class="preview-hint">展示前 5 条数据</span>
        <a-button type="text" size="small" class="preview-collapse" title="收起数据预览，恢复画布" @click="previewOpen = false">
          <template #icon><DownOutlined /></template>
        </a-button>
      </div>
      <div v-if="previewColumns.length" class="preview-filters">
        <a-select
          v-model:value="previewFilterField"
          placeholder="选择属性"
          allow-clear
          size="small"
          :options="previewFilterFields"
          class="preview-filter-field"
        />
        <a-input
          v-model:value="previewFilterValue"
          placeholder="属性值筛选"
          allow-clear
          size="small"
          class="preview-filter-value"
          @press-enter="loadPreview"
        />
        <div class="preview-filter-actions">
          <a-button size="small" type="primary" :loading="previewLoading" @click="loadPreview">过滤</a-button>
          <a-button size="small" @click="resetPreviewFilters">重置</a-button>
        </div>
      </div>
      <a-table
        :columns="previewColumns"
        :data-source="previewRows"
        :loading="previewLoading"
        size="small"
        :pagination="false"
        row-key="__previewKey"
        :scroll="{ x: 640 }"
      />
    </div>

    <!-- 概念详情 -->
    <div v-if="selectedNode" class="node-detail">
      <h4>{{ selectedNode.label }}</h4>
      <div class="detail-section">
        <div class="detail-title">节点信息</div>
        <p>名称：{{ selectedNode.label }}</p>
        <p>编码：{{ selectedNode.code || '暂无编码' }}</p>
        <p>描述：{{ selectedNode.description || '暂无描述' }}</p>
      </div>
      <div class="detail-section">
        <div class="detail-title">绑定表</div>
        <div v-if="nodeBindings.length" class="detail-line">
          <a-tag v-for="b in nodeBindings" :key="b.id" color="purple">{{ b.tableName }}</a-tag>
        </div>
        <p v-else-if="nodeBindingsLoading" class="detail-empty">加载中...</p>
        <p v-else class="detail-empty">未绑定数据表</p>
      </div>
      <div class="detail-section">
        <div class="detail-title">属性</div>
        <div v-if="selectedNode.properties && selectedNode.properties.length" class="detail-line">
          <a-tag v-for="p in selectedNode.properties" :key="p">{{ p }}</a-tag>
        </div>
        <p v-else class="detail-empty">暂无属性</p>
      </div>
      <a-space size="small" wrap class="detail-actions">
          <a-button size="small" @click="openNodeEdit" v-hasPermi="['ont:concept:edit']">编辑</a-button>
        <a-button size="small" danger @click="removeSelectedNode" v-hasPermi="['ont:concept:remove']">删除</a-button>
        <a-button size="small" @click="openMapping" v-hasPermi="['ont:concept:edit', 'ont:property:edit']">映射</a-button>
        <a-button type="primary" size="small" @click="openQuickProp(selectedNode)">
          <template #icon><PlusOutlined /></template>
          添加属性
        </a-button>
      </a-space>
    </div>

    <!-- 关系详情 -->
    <div v-else-if="selectedEdge" class="node-detail">
      <h4>{{ selectedEdge.label || selectedEdge.name || '(未命名关系)' }}</h4>
      <p>{{ selectedEdge.sourceName }} → {{ selectedEdge.targetName }}</p>
      <p v-if="selectedEdge.type">类型：{{ relationTypeText(selectedEdge.type) }}</p>
      <a-space size="small" wrap class="detail-actions">
        <a-button size="small" @click="openEdgeEdit" v-hasPermi="['ont:relation:edit']">编辑</a-button>
        <a-button size="small" danger @click="removeSelectedEdge" v-hasPermi="['ont:relation:remove']">删除</a-button>
        <a-button size="small" @click="openRelColumn" v-hasPermi="['ont:relation:edit']">关联表/字段</a-button>
      </a-space>
    </div>

    <!-- 新增概念（拖放画布触发） -->
    <a-modal v-model:open="conceptOpen" title="新增概念" width="620px" wrap-class-name="ontology-workspace-modal ontology-modal--compact" destroy-on-close ok-text="确定" cancel-text="取消" :confirm-loading="saving" @ok="submitConcept" @cancel="conceptOpen = false">
      <a-form class="ontology-form-grid" :model="conceptForm" :label-col="{ style: { width: '70px' } }">
        <a-form-item label="概念名称" required>
          <a-input v-model:value="conceptForm.name" placeholder="请输入概念名称" />
        </a-form-item>
        <a-form-item class="graph-color-field" label="颜色">
          <input type="color" class="mini-color-picker" :value="conceptForm.color || '#1677ff'" @input="conceptForm.color = $event.target.value" />
          <a-input v-model:value="conceptForm.color" placeholder="#1677ff" allow-clear />
        </a-form-item>
        <a-form-item class="ontology-form-grid__full" label="描述">
          <a-textarea v-model:value="conceptForm.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 拉线创建关系 -->
    <a-modal v-model:open="relationOpen" title="创建关系" width="620px" wrap-class-name="ontology-workspace-modal ontology-modal--compact" destroy-on-close ok-text="确定" cancel-text="取消" :confirm-loading="saving" @ok="submitRelation" @cancel="cancelRelation">
      <a-alert type="info" show-icon :message="'源概念：' + (pendingLink ? pendingLink.sourceName : '') + '　→　目标概念：' + (pendingLink ? pendingLink.targetName : '')" style="margin-bottom: 12px;" />
      <a-form class="ontology-form-grid" :model="relationForm" :label-col="{ style: { width: '70px' } }">
        <a-form-item label="关系名称" required>
          <a-input v-model:value="relationForm.name" placeholder="请输入关系名称，如 拥有订单" />
        </a-form-item>
        <a-form-item label="关系类型">
          <a-select v-model:value="relationForm.relationType">
            <a-select-option value="one_to_one">一对一</a-select-option>
            <a-select-option value="one_to_many">一对多</a-select-option>
            <a-select-option value="many_to_one">多对一</a-select-option>
            <a-select-option value="many_to_many">多对多</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <p class="modal-hint">取消将丢弃本次连线</p>
    </a-modal>

    <!-- 快捷添加属性 -->
    <a-modal v-model:open="quickPropOpen" :title="'添加属性 - ' + quickPropConceptName" width="660px" wrap-class-name="ontology-workspace-modal ontology-modal--compact" destroy-on-close ok-text="确定" cancel-text="取消" :confirm-loading="saving" @ok="submitQuickProp" @cancel="quickPropOpen = false">
      <a-form class="ontology-form-grid" :model="quickPropForm" :label-col="{ style: { width: '70px' } }">
        <a-form-item label="属性名称" required>
          <a-input v-model:value="quickPropForm.name" placeholder="请输入属性名称" />
        </a-form-item>
        <a-form-item label="数据类型">
          <a-select v-model:value="quickPropForm.dataType">
            <a-select-option value="string">string</a-select-option>
            <a-select-option value="integer">integer</a-select-option>
            <a-select-option value="decimal">decimal</a-select-option>
            <a-select-option value="date">date</a-select-option>
            <a-select-option value="boolean">boolean</a-select-option>
            <a-select-option value="text">text</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="quickPropForm.sortOrder" :min="0" />
        </a-form-item>
        <a-form-item label="主键">
          <a-checkbox v-model:checked="quickPropForm.isPrimary" />
        </a-form-item>
        <a-form-item label="必填">
          <a-checkbox v-model:checked="quickPropForm.isRequired" />
        </a-form-item>
        <a-form-item label="默认值">
          <a-input v-model:value="quickPropForm.defaultValue" placeholder="默认值" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑概念（详情面板触发，先拉详情再打开） -->
    <a-modal v-model:open="nodeEditOpen" title="编辑概念" width="620px" wrap-class-name="ontology-workspace-modal ontology-modal--compact" destroy-on-close ok-text="确定" cancel-text="取消" :confirm-loading="saving" @ok="submitNodeEdit" @cancel="nodeEditOpen = false">
      <a-spin :spinning="nodeEditLoading">
        <a-form class="ontology-form-grid" :model="nodeEditForm" :label-col="{ style: { width: '70px' } }">
          <a-form-item label="概念名称" required>
            <a-input v-model:value="nodeEditForm.name" placeholder="请输入概念名称" />
          </a-form-item>
          <a-form-item class="graph-color-field" label="颜色">
            <input type="color" class="mini-color-picker" :value="nodeEditForm.color || '#1677ff'" @input="nodeEditForm.color = $event.target.value" />
            <a-input v-model:value="nodeEditForm.color" placeholder="#1677ff" allow-clear />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="nodeEditForm.sortOrder" :min="0" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="nodeEditForm.status">
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">已发布</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item class="ontology-form-grid__full" label="描述">
            <a-textarea v-model:value="nodeEditForm.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>

    <!-- 编辑关系（详情面板触发，先拉详情再打开） -->
    <a-modal v-model:open="edgeEditOpen" title="编辑关系" width="660px" wrap-class-name="ontology-workspace-modal ontology-modal--form" destroy-on-close ok-text="确定" cancel-text="取消" :confirm-loading="saving" @ok="submitEdgeEdit" @cancel="edgeEditOpen = false">
      <a-spin :spinning="edgeEditLoading">
        <a-form class="ontology-form-grid" :model="edgeEditForm" :label-col="{ style: { width: '70px' } }">
          <a-form-item label="关系名称" required>
            <a-input v-model:value="edgeEditForm.name" placeholder="请输入关系名称" />
          </a-form-item>
          <a-form-item label="关系类型">
            <a-select v-model:value="edgeEditForm.relationType">
              <a-select-option value="one_to_one">一对一</a-select-option>
              <a-select-option value="one_to_many">一对多</a-select-option>
              <a-select-option value="many_to_one">多对一</a-select-option>
              <a-select-option value="many_to_many">多对多</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="源概念">
            <a-select v-model:value="edgeEditForm.sourceConceptId" :options="conceptOptions" placeholder="请选择源概念" show-search :filter-option="filterOption" />
          </a-form-item>
          <a-form-item label="目标概念">
            <a-select v-model:value="edgeEditForm.targetConceptId" :options="conceptOptions" placeholder="请选择目标概念" show-search :filter-option="filterOption" />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="edgeEditForm.sortOrder" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item class="ontology-form-grid__full" label="描述">
            <a-textarea v-model:value="edgeEditForm.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>

    <!-- 共享弹窗：概念映射（表绑定+字段映射页签）/ 关系关联字段 -->
    <concept-mapping-modal v-model="mappingOpen" :concept-id="mappingConceptId" :concept-name="mappingConceptName" @success="onBindingChanged" />
    <rel-column-modal v-model="relColumnOpen" :relation-id="relColRelationId" :source-concept-id="relColSourceId" :target-concept-id="relColTargetId" :relation-name="relColRelationName" @success="onBindingChanged" />
  </div>
</template>

<script setup name="GraphPanel">
import { Graph } from '@antv/x6'
import { getGraphData } from '@/api/ont/function'
import { listConcept, getConcept, addConcept, updateConcept, delConcept } from '@/api/ont/concept'
import { getRelation, addRelation, updateRelation, delRelation } from '@/api/ont/relation'
import { listConceptTable, previewConceptTable } from '@/api/ont/conceptTable'
import { listPropertyColumn } from '@/api/ont/propertyColumn'
import { addProperty, listProperty } from '@/api/ont/property'
import ConceptMappingModal from './bindings/ConceptMappingModal.vue'
import RelColumnModal from './bindings/RelColumnModal.vue'
import { ReloadOutlined, PlusOutlined, DownOutlined } from '@ant-design/icons-vue'
import { genCode } from '@/utils/codeGen'

const props = defineProps({
  ontologyId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['changed'])

const { proxy } = getCurrentInstance()
const containerRef = ref(null)
const selectedNode = ref(null)
const selectedEdge = ref(null)
const saving = ref(false)

// 可视化建模状态
let graph = null
let draggingChip = false
let pendingTempEdge = null
let quickPropConceptId = null

// 节点位置缓存：手动拖过的位置在刷新图谱后仍保留
const positionMap = {}

// 新增概念（拖放画布）
const conceptOpen = ref(false)
const conceptForm = ref({})
const pendingDropPos = ref(null)

// 拉线建关系
const relationOpen = ref(false)
const relationForm = ref({})
const pendingLink = ref(null)

// 节点快捷加属性
const quickPropOpen = ref(false)
const quickPropConceptName = ref('')
const quickPropForm = ref({})

// 节点详情：绑定表列表（后端图谱接口不含 boundTables，点击节点时单独拉取）
const nodeBindings = ref([])
const nodeBindingsLoading = ref(false)

// 数据预览：位于可视化下方，点击概念时加载；表头=属性名，最多 5 行，支持属性值过滤
const previewLoading = ref(false)
const previewTableName = ref('')
const previewBindingId = ref(null)
const previewColumns = ref([])
const previewRows = ref([])
// 简单筛选：属性选择 + 属性值（替代 OntFilterBuilder）
const previewFilterField = ref(undefined)
const previewFilterValue = ref('')
// OntFilterBuilder 可选字段 → 改为简单下拉选项（语义属性名 → 物理列）
const previewFilterFields = ref([])
// 当前预览绑定对应的 物理列 → 属性名 映射（listPropertyColumn + propertyDict 构建）
const previewColMap = ref({})
// 本体全部属性字典（id -> property）
const propertyDict = ref({})
// 数据预览展开开关：展开时画布收缩一半、下方展示预览，页面总高不变；收起时画布恢复整高
const previewOpen = ref(false)
// 画布目标高度：预览展开 300px，收起 600px（与 .graph-container / .graph-shrunk 样式一致）
const CANVAS_FULL_HEIGHT = 600
const CANVAS_SHRUNK_HEIGHT = 300

const previewBindingOptions = computed(() => nodeBindings.value.map(b => ({ value: b.id, label: b.tableName })))

// 加载本体属性字典（属性名 → 表头 / 过滤条件翻译）
function loadProperties() {
  listProperty({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 200 }).then(res => {
    const rows = (res.data && res.data.rows) || []
    const dict = {}
    rows.forEach(p => { dict[p.id] = p })
    propertyDict.value = dict
  })
}

// 按选中绑定加载预览：列表头=属性名（映射失败退化为物理列名），最多 5 行；过滤值走服务端 WHERE
async function loadPreview() {
  const bindingId = previewBindingId.value
  if (bindingId == null || bindingId === '') {
    previewColumns.value = []
    previewRows.value = []
    previewTableName.value = ''
    return
  }
  previewLoading.value = true
  try {
    // 1. 构建 物理列 → 属性名（复用 ActionPanel 同款链路：概念表绑定 → 列映射 → 属性字典）
    const colRes = await listPropertyColumn(bindingId)
    const cols = (colRes.data && colRes.data.rows) || colRes.data || []
    const map = {}
    cols.forEach(c => {
      const p = propertyDict.value[c.propertyId] || {}
      map[c.columnName] = p.name || c.columnName
    })
    previewColMap.value = map
    // 2. 构建属性下拉选项（语义属性名 → 物理列），并随绑定切换重置不适用字段
    previewFilterFields.value = Object.keys(map).map(col => ({
      label: map[col] || col,
      value: col
    }))
    // 3. 组装简单筛选 spec：属性 + 属性值 → 服务端 WHERE
    const filter = buildPreviewFilter()
    const spec = { groups: [{ connector: 'AND', filters: filter ? [filter] : [] }], orderBy: [], columns: [], keyword: '' }
    // 4. 拉取数据：最多 5 行（filters = 类型化查询 spec，服务端白名单编译 WHERE/排序/投影）
    const res = await previewConceptTable(bindingId, 5, spec)
    const data = res.data || {}
    previewTableName.value = data.tableName || ''
    const srcCols = data.columns || []
    // 表头=属性名；未映射到属性的物理列不展示（与对比表一致），全未映射时退化为物理列名
    const mapped = srcCols.filter(c => map[c])
    const colsToShow = mapped.length ? mapped : srcCols
    previewColumns.value = colsToShow.map(c => ({
      title: map[c] || c,
      dataIndex: c,
      ellipsis: { showTitle: true },
      width: 100
    }))
    previewRows.value = (data.rows || []).map((r, i) => ({ __previewKey: i, ...r }))
  } catch {
    previewColumns.value = []
    previewRows.value = []
  } finally {
    previewLoading.value = false
  }
}

// 由「属性 + 属性值」组装单条件筛选（无属性或无值时返回 null）
function buildPreviewFilter() {
  const field = previewFilterField.value
  const value = previewFilterValue.value
  if (!field) return null
  const trimmed = String(value == null ? '' : value).trim()
  if (trimmed === '') return null
  return { field, op: 'eq', value: trimmed }
}

function resetPreviewFilters() {
  previewFilterField.value = undefined
  previewFilterValue.value = ''
  return loadPreview()
}

// 共享绑定弹窗（概念映射 / 关系关联字段）
const mappingOpen = ref(false)
const mappingConceptId = ref('')
const mappingConceptName = ref('')
const relColumnOpen = ref(false)
const relColRelationId = ref('')
const relColSourceId = ref('')
const relColTargetId = ref('')
const relColRelationName = ref('')

// 编辑弹窗（先拉详情再打开）
const nodeEditOpen = ref(false)
const nodeEditLoading = ref(false)
const nodeEditForm = ref({})
const edgeEditOpen = ref(false)
const edgeEditLoading = ref(false)
const edgeEditForm = ref({})
// 概念下拉选项（编辑关系时选源/目标概念用），随 loadGraph 一并刷新
const conceptOptions = ref([])

// 兼容旧后端：优先取数据里的 conceptId，否则从 "concept_xxx" 提取；统一返回字符串
function conceptIdOfData(d) {
  if (!d) return null
  if (d.conceptId != null && d.conceptId !== '') return String(d.conceptId)
  return conceptIdOfCellId(d.id)
}

function conceptIdOfCellId(cellId) {
  const m = String(cellId == null ? '' : cellId).match(/^concept_(.+)$/)
  return m ? m[1] : null
}

// 统一解析响应数据：list 接口可能返回数组或分页对象
function rowsOf(res) {
  return Array.isArray(res.data) ? res.data : ((res.data && res.data.rows) || [])
}

async function loadGraph() {
  if (!props.ontologyId) {
    proxy.$modal.msgError('缺少本体ID，无法加载图谱')
    return
  }
  try {
    // 图谱数据与概念颜色并行加载：接口未返回 color 时用「概念管理」中设置的颜色兜底，
    // 保证节点颜色始终与概念配置一致
    const [res, cRes] = await Promise.all([
      getGraphData(props.ontologyId),
      listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 })
    ])
    const { nodes, edges } = res.data || {}
    const rows = (cRes.data && cRes.data.rows) || []
    const colorMap = {}
    rows.forEach(c => {
      if (c && c.id != null) colorMap[c.id] = c.color
    })
    // 同步维护概念下拉（编辑关系弹窗的源/目标概念选项）
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
    const mergedNodes = (nodes || []).map(n => {
      const cid = n.conceptId != null ? n.conceptId : String(n.id == null ? '' : n.id).replace(/^concept_/, '')
      return Object.assign({}, n, { color: n.color || colorMap[cid] })
    })
    renderGraph(mergedNodes, edges || [])
  } catch (e) {
    proxy.$modal.msgError('加载图谱失败: ' + e.message)
  }
}

// 重建图谱并尽量保持当前选中节点的详情面板（含绑定表）
async function reloadGraphKeepSelection() {
  const prevCid = selectedNode.value ? conceptIdOfData(selectedNode.value) : null
  await loadGraph()
  if (prevCid != null && graph) {
    const cell = graph.getNodes().find(n => conceptIdOfData(n.getData()) === prevCid)
    if (cell) {
      selectedNode.value = cell.getData() || null
      selectedEdge.value = null
      await loadNodeBindings(prevCid)
    }
  }
}

// 拉取节点当前绑定的物理表；绑定就绪后联动下方数据预览（默认预览第一张绑定表）
async function loadNodeBindings(conceptId) {
  nodeBindings.value = []
  if (conceptId == null || conceptId === '') return
  nodeBindingsLoading.value = true
  try {
    const res = await listConceptTable({ conceptId })
    nodeBindings.value = rowsOf(res)
    const hasBindings = nodeBindings.value.length > 0
    previewBindingId.value = hasBindings ? nodeBindings.value[0].id : null
    // 有绑定表才展开预览（有真实数据可看）；无绑定时保持画布整高，不出现空白预览面板
    previewOpen.value = hasBindings
    // 切换概念时重置过滤条件，避免上一概念的属性值残留串扰
    previewFilterField.value = undefined
    previewFilterValue.value = ''
    previewFilterFields.value = []
    if (hasBindings) await loadPreview()
  } catch {
    nodeBindings.value = []
    previewBindingId.value = null
    previewOpen.value = false
  } finally {
    nodeBindingsLoading.value = false
  }
}

// 节点四向端口配置（每个节点需独立一份；defaultNode.ports 在 v2 不作用于逐节点元数据）
function nodePorts() {
  const portAttr = () => ({ r: 4, magnet: true, stroke: '#1677ff', strokeWidth: 1, fill: '#ffffff', cursor: 'crosshair' })
  return {
    groups: {
      top: { position: 'top', attrs: { circle: portAttr() } },
      bottom: { position: 'bottom', attrs: { circle: portAttr() } },
      left: { position: 'left', attrs: { circle: portAttr() } },
      right: { position: 'right', attrs: { circle: portAttr() } }
    },
    items: [
      { id: 'top-port', group: 'top' },
      { id: 'bottom-port', group: 'bottom' },
      { id: 'left-port', group: 'left' },
      { id: 'right-port', group: 'right' }
    ]
  }
}

function renderGraph(nodes, edges) {
  if (graph) { graph.dispose(); graph = null }
  selectedNode.value = null
  selectedEdge.value = null
  nodeBindings.value = []
  previewOpen.value = false
  previewBindingId.value = null
  previewColumns.value = []
  previewRows.value = []
  previewTableName.value = ''
  const container = containerRef.value
  if (!container) return

  const width = container.offsetWidth || 800
  const height = container.offsetHeight || 600

  graph = new Graph({
    container: container,
    width: width,
    height: height,
    // 不开启 autoResize：其尺寸回写在部分布局下会与容器形成 ResizeObserver
    // 正反馈循环，导致容器高度无限膨胀（面板被推到数万像素之外）
    background: { color: '#fafafa' },
    grid: { visible: true, type: 'dot', size: 10, args: { color: '#e0e0e0', thickness: 1 } },
    snapline: true,
    // 高DPI/浏览器缩放下，按下抬起间会有亚像素漂移；默认阈值0会把点击误判为拖拽导致 node:click 不触发
    clickThreshold: 5,
    panning: true,
    mousewheel: { enabled: true, minScale: 0.4, maxScale: 2 },
    connecting: {
      anchor: 'center',
      connectionPoint: 'boundary',
      allowBlank: false,
      allowLoop: false,
      allowNode: false,
      allowEdge: false,
      allowMulti: false,
      highlight: true,
      createEdge() {
        return graph.createEdge({
          shape: 'edge',
          attrs: {
            wrap: { connection: true, strokeWidth: 12, strokeLinejoin: 'round' },
            line: { stroke: '#91caff', strokeWidth: 1.5, strokeDasharray: '5 4', targetMarker: { name: 'classic', size: 8 }, pointerEvents: 'none' }
          }
        })
      }
    },
    // 注意：v2 已移除 defaultNode/defaultEdge 图选项，
    // 节点与边样式需在各自元数据中显式给出
  })

  const x6Nodes = nodes.map((n, i) => {
    const saved = positionMap[n.id]
    return {
      id: n.id,
      shape: 'circle',
      width: 64,
      height: 64,
      x: saved ? saved.x : 100 + (i % 5) * 160,
      y: saved ? saved.y : 80 + Math.floor(i / 5) * 130,
      label: n.label,
      ports: nodePorts(),
      attrs: {
        body: { fill: n.color || '#e6f7ff', stroke: '#1677ff', strokeWidth: 1.5 },
        label: { fill: '#333', fontSize: 11, fontWeight: 'bold', textWrap: { width: -10, height: -10, ellipsis: true } }
      },
      data: n
    }
  })

  const x6Edges = edges.map(e => ({
    id: e.id,
    shape: 'edge',
    source: { cell: e.source, connectionPoint: 'boundary' },
    target: { cell: e.target, connectionPoint: 'boundary' },
    label: e.label || '',
    // wrap 为 12px 透明点击热区，避免细线难以点选；line 设 pointerEvents:none 让点击穿透到 wrap
    attrs: {
      wrap: { connection: true, strokeWidth: 12, strokeLinejoin: 'round' },
      line: { stroke: '#91caff', strokeWidth: 1.5, targetMarker: { name: 'classic', size: 8 }, pointerEvents: 'none' }
    },
    data: e
  }))

  graph.addNodes(x6Nodes)
  graph.addEdges(x6Edges)

  // 点击节点 -> 概念详情（同时拉取绑定表）
  graph.on('node:click', ({ node }) => {
    selectedNode.value = node.getData() || null
    selectedEdge.value = null
    loadNodeBindings(conceptIdOfData(selectedNode.value))
  })

  // 点击边 -> 关系详情
  graph.on('edge:click', ({ edge }) => {
    const data = edge.getData() || {}
    const sourceCellId = edge.getSourceCellId()
    const targetCellId = edge.getTargetCellId()
    const sourceCell = sourceCellId ? graph.getCellById(sourceCellId) : null
    const targetCell = targetCellId ? graph.getCellById(targetCellId) : null
    selectedEdge.value = Object.assign({}, data, {
      sourceCellId,
      targetCellId,
      sourceName: sourceCell && sourceCell.getData() ? sourceCell.getData().label : '-',
      targetName: targetCell && targetCell.getData() ? targetCell.getData().label : '-'
    })
    selectedNode.value = null
    nodeBindings.value = []
    previewOpen.value = false
    previewBindingId.value = null
    previewColumns.value = []
    previewRows.value = []
    previewTableName.value = ''
  })

  graph.on('blank:click', () => {
    selectedNode.value = null
    selectedEdge.value = null
    nodeBindings.value = []
    previewOpen.value = false
    previewBindingId.value = null
    previewColumns.value = []
    previewRows.value = []
    previewTableName.value = ''
  })

  // 拖动节点后记录位置，刷新图谱不丢位置
  graph.on('node:moved', ({ node }) => {
    positionMap[node.id] = node.getPosition()
  })

  // 端口拉线松开 -> 弹出创建关系弹窗
  graph.on('edge:connected', ({ edge }) => {
    const s = edge.getSourceNode()
    const t = edge.getTargetNode()
    if (!s || !t) return
    const sc = conceptIdOfData(s.getData())
    const tc = conceptIdOfData(t.getData())
    if (!sc || !tc) return
    const sd = s.getData() || {}
    const td = t.getData() || {}
    pendingTempEdge = edge
    pendingLink.value = {
      sourceConceptId: sc,
      targetConceptId: tc,
      sourceName: sd.label,
      targetName: td.label
    }
    relationForm.value = { name: undefined, relationType: 'one_to_many' }
    relationOpen.value = true
  })

  if (nodes.length > 0) {
    graph.centerContent()
  }
}

function relationTypeText(type) {
  return { one_to_one: '一对一', one_to_many: '一对多', many_to_one: '多对一', many_to_many: '多对多' }[type] || type
}

function filterOption(input, option) {
  if (!option || !option.label) return false
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

function fitView() {
  if (graph) graph.centerContent()
}

// 画布高度随预览展开/收起在 600px/300px 间切换；X6 未开启 autoResize，
// 必须在 DOM 高度变化后再手动 resize，否则命中检测与渲染视口仍停留在旧尺寸
watch(previewOpen, async (open) => {
  await nextTick()
  const container = containerRef.value
  if (!graph || !container) return
  const width = container.offsetWidth || 800
  const height = open ? CANVAS_SHRUNK_HEIGHT : CANVAS_FULL_HEIGHT
  graph.resize(width, height)
  graph.centerContent()
})

/* ================= 可视化建模 ================= */

// 工具条「概念」芯片开始拖拽
function onChipDragStart(e) {
  draggingChip = true
  if (e && e.dataTransfer) {
    e.dataTransfer.setData('text/plain', 'concept')
    e.dataTransfer.effectAllowed = 'copy'
  }
}

function onChipDragEnd() {
  draggingChip = false
}

// 落到画布 -> 打开新增概念弹窗，记住落点
function onCanvasDrop(e) {
  e.preventDefault()
  if (!draggingChip) return
  draggingChip = false
  if (!graph) {
    proxy.$modal.msgError('画布尚未就绪，请先点击「刷新图谱」')
    return
  }
  const local = graph.clientToLocal({ x: e.clientX, y: e.clientY })
  pendingDropPos.value = { x: Math.round(local.x - 32), y: Math.round(local.y - 32) }
  conceptForm.value = { name: undefined, color: '#1677ff', description: undefined }
  conceptOpen.value = true
}

function submitConcept() {
  if (!conceptForm.value.name) {
    proxy.$modal.msgError('请输入概念名称')
    return
  }
  saving.value = true
  addConcept({
    ontologyId: props.ontologyId,
    name: conceptForm.value.name,
    code: genCode('cpt'),
    color: conceptForm.value.color,
    description: conceptForm.value.description
  }).then(res => {
    proxy.$modal.msgSuccess('概念已创建')
    const newId = res.data != null && typeof res.data !== 'object' ? res.data : null
    if (newId != null && pendingDropPos.value) {
      positionMap['concept_' + newId] = Object.assign({}, pendingDropPos.value)
    }
    conceptOpen.value = false
    pendingDropPos.value = null
    return loadGraph().then(() => emit('changed'))
  }).catch(() => {}).finally(() => {
    saving.value = false
  })
}

// 拉线创建关系
function submitRelation() {
  if (!relationForm.value.name) {
    proxy.$modal.msgError('请输入关系名称')
    return
  }
  saving.value = true
  addRelation({
    ontologyId: props.ontologyId,
    name: relationForm.value.name,
    code: genCode('rel'),
    sourceConceptId: pendingLink.value.sourceConceptId,
    targetConceptId: pendingLink.value.targetConceptId,
    relationType: relationForm.value.relationType
  }).then(res => {
    proxy.$modal.msgSuccess('关系已创建')
    const rid = res.data != null && typeof res.data !== 'object' ? res.data : null
    const edge = pendingTempEdge
    if (edge && graph) {
      edge.attr('line/strokeDasharray', null)
      // X6 v2 没有 setLabel，需用 setLabels（默认 label markup 选择器为 label）
      edge.setLabels([{ attrs: { label: { text: relationForm.value.name } } }])
      edge.setData(Object.assign({}, edge.getData() || {}, {
        relationId: rid,
        name: relationForm.value.name,
        relationType: relationForm.value.relationType
      }))
    }
    relationOpen.value = false
    pendingTempEdge = null
    pendingLink.value = null
    // 以服务端数据为准重建图谱，保证边与详情一致
    return loadGraph().then(() => emit('changed'))
  }).catch(() => {}).finally(() => {
    saving.value = false
  })
}

function cancelRelation() {
  if (pendingTempEdge && graph) {
    graph.removeEdge(pendingTempEdge)
  }
  pendingTempEdge = null
  pendingLink.value = null
}

/* ================= 节点详情操作 ================= */

// 节点详情面板快捷添加属性
function openQuickProp(nodeData) {
  if (!nodeData) return
  const cid = conceptIdOfData(nodeData)
  if (!cid) {
    proxy.$modal.msgError('无法识别节点对应的概念')
    return
  }
  quickPropConceptId = cid
  quickPropConceptName.value = nodeData.label || ''
  quickPropForm.value = { name: undefined, dataType: 'string', isPrimary: false, isRequired: false, defaultValue: undefined, sortOrder: 0 }
  quickPropOpen.value = true
}

function submitQuickProp() {
  if (!quickPropForm.value.name) {
    proxy.$modal.msgError('请输入属性名称')
    return
  }
  saving.value = true
  addProperty(Object.assign({
    conceptId: quickPropConceptId,
    code: genCode('prp')
  }, quickPropForm.value)).then(() => {
    proxy.$modal.msgSuccess('属性已添加')
    quickPropOpen.value = false
    // 本地不再手工拼接，直接以服务端数据为准重建图谱
    return reloadGraphKeepSelection().then(() => emit('changed'))
  }).catch(() => {}).finally(() => {
    saving.value = false
  })
}

// 编辑概念：先拉详情再打开弹窗
async function openNodeEdit() {
  const n = selectedNode.value
  if (!n) return
  const cid = conceptIdOfData(n)
  if (!cid) {
    proxy.$modal.msgError('无法识别节点对应的概念')
    return
  }
  nodeEditLoading.value = true
  try {
    const res = await getConcept(cid)
    nodeEditForm.value = Object.assign({}, res.data)
    nodeEditOpen.value = true
  } catch {
    // 错误提示由请求拦截器统一弹出
  } finally {
    nodeEditLoading.value = false
  }
}

function submitNodeEdit() {
  if (!nodeEditForm.value.name) {
    proxy.$modal.msgError('请输入概念名称')
    return
  }
  saving.value = true
  updateConcept(nodeEditForm.value).then(() => {
    proxy.$modal.msgSuccess('已保存')
    nodeEditOpen.value = false
    return reloadGraphKeepSelection().then(() => emit('changed'))
  }).catch(() => {}).finally(() => {
    saving.value = false
  })
}

// 删除概念
function removeSelectedNode() {
  const n = selectedNode.value
  if (!n) return
  const cid = conceptIdOfData(n)
  if (!cid) {
    proxy.$modal.msgError('无法识别节点对应的概念')
    return
  }
  proxy.$modal.confirm('删除后不可恢复，确定删除概念「' + (n.label || cid) + '」吗？').then(async () => {
    await delConcept(cid)
    proxy.$modal.msgSuccess('已删除')
    selectedNode.value = null
    nodeBindings.value = []
    await loadGraph()
    emit('changed')
  }).catch(() => {})
}

/* ================= 关系详情操作 ================= */

// 编辑关系：先拉详情再打开弹窗
async function openEdgeEdit() {
  const e = selectedEdge.value
  if (!e || e.relationId == null) {
    proxy.$modal.msgError('该连线尚未保存为关系')
    return
  }
  edgeEditLoading.value = true
  try {
    const res = await getRelation(e.relationId)
    edgeEditForm.value = Object.assign({}, res.data)
    edgeEditOpen.value = true
  } catch {
    // 错误提示由请求拦截器统一弹出
  } finally {
    edgeEditLoading.value = false
  }
}

function submitEdgeEdit() {
  if (!edgeEditForm.value.name) {
    proxy.$modal.msgError('请输入关系名称')
    return
  }
  saving.value = true
  updateRelation(edgeEditForm.value).then(() => {
    proxy.$modal.msgSuccess('已保存')
    edgeEditOpen.value = false
    return loadGraph().then(() => emit('changed'))
  }).catch(() => {}).finally(() => {
    saving.value = false
  })
}

// 删除关系
function removeSelectedEdge() {
  const e = selectedEdge.value
  if (!e) return
  if (e.relationId == null) {
    proxy.$modal.msgError('该连线尚未保存为关系')
    return
  }
  proxy.$modal.confirm('删除后不可恢复，确定删除关系「' + (e.label || e.name || e.relationId) + '」吗？').then(async () => {
    await delRelation(e.relationId)
    proxy.$modal.msgSuccess('已删除')
    selectedEdge.value = null
    await loadGraph()
    emit('changed')
  }).catch(() => {})
}

/* ================= 共享绑定弹窗 ================= */

// 打开「概念映射」共享弹窗（表绑定 + 字段映射两个页签）
function openMapping() {
  const n = selectedNode.value
  if (!n) return
  const cid = conceptIdOfData(n)
  if (!cid) {
    proxy.$modal.msgError('无法识别节点对应的概念')
    return
  }
  mappingConceptId.value = String(cid)
  mappingConceptName.value = n.label || ''
  mappingOpen.value = true
}

// 打开「关系关联字段」共享弹窗（源/目标概念从边的两端单元格ID解析）
function openRelColumn() {
  const e = selectedEdge.value
  if (!e) return
  if (e.relationId == null) {
    proxy.$modal.msgError('该连线尚未保存为关系')
    return
  }
  relColRelationId.value = String(e.relationId)
  relColSourceId.value = e.sourceCellId != null ? (conceptIdOfCellId(e.sourceCellId) || '') : ''
  relColTargetId.value = e.targetCellId != null ? (conceptIdOfCellId(e.targetCellId) || '') : ''
  relColRelationName.value = e.label || e.name || ''
  relColumnOpen.value = true
}

// 绑定类弹窗保存成功：重建图谱、刷新选中节点绑定表，并通知兄弟面板刷新
async function onBindingChanged() {
  await reloadGraphKeepSelection()
  emit('changed')
}

onMounted(() => {
  nextTick(() => {
    loadProperties()
    loadGraph()
  })
})

onBeforeUnmount(() => {
  if (graph) {
    graph.dispose()
    graph = null
  }
})
</script>

<style lang="scss" scoped>
.graph-panel {
  position: relative;

  .panel-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .create-chip {
      padding: 4px 14px;
      border: 1px dashed #1677ff;
      border-radius: 16px;
      color: #1677ff;
      font-size: 13px;
      cursor: grab;
      user-select: none;
      background: #f0f7ff;

      &:active {
        cursor: grabbing;
      }
    }

    .toolbar-tip {
      margin-left: auto;
      font-size: 12px;
      color: #8a95a6;
    }
  }

  .graph-container {
    position: relative;
    width: 100%;
    height: 600px;
    border: 1px solid #d9d9d9;
    border-radius: 8px;
    background: #fafafa;
    overflow: hidden;
    transition: height 0.2s ease;

    // 预览展开时画布收缩到一半高度，配合下方预览面板，整体页面高度保持不变
    &.graph-shrunk {
      height: 300px;
    }

    .x6-host {
      position: absolute;
      inset: 0;
    }
  }

  // 数据预览面板：位于可视化下方
  .preview-panel {
    margin-top: 12px;
    padding: 12px;
    border: 1px solid #e8edf5;
    border-radius: 8px;
    background: #ffffff;

    .preview-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;

      .preview-title {
        font-size: 14px;
        font-weight: 600;
        color: #1f2d3d;
      }

      .preview-table {
        font-size: 12px;
        color: #8a95a6;
      }

      .preview-hint {
        margin-left: auto;
        font-size: 12px;
        color: #8a95a6;
      }

      .preview-collapse {
        color: #8a95a6;

        &:hover {
          color: #1677ff;
        }
      }
    }

    .preview-filters {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;
    }

    .preview-filter-field {
      width: 160px;
    }

    .preview-filter-value {
      flex: 1;
      min-width: 120px;
    }

    .preview-filter-actions {
      display: flex;
      flex-shrink: 0;
      gap: 8px;
    }

    // 预览表格单元格：强制截断过长值，hover 时由 tooltip 显示完整内容
    .ant-table-cell {
      max-width: 100px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  // 节点/关系详情浮层：固定在画布右上角，任何布局变化都不会把它推走。
  // pointer-events:none 让点击穿透到画布节点（卡片悬浮区不再吞掉“点其他节点”），内部按钮单独恢复可点
  .node-detail {
    position: absolute;
    top: 46px;
    right: 12px;
    width: 300px;
    max-height: 540px;
    overflow: auto;
    z-index: 20;
    margin-top: 0;
    padding: 12px;
    background: #f0f5ff;
    border: 1px solid #d6e4ff;
    border-radius: 6px;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    pointer-events: none;

    // 卡片内按钮仍需可点（编辑/删除/映射/添加属性）
    .detail-actions {
      pointer-events: auto;
    }

    h4 {
      margin: 0 0 8px;
    }

    p {
      margin: 0 0 4px;
      color: #666;
    }

    .detail-desc {
      color: #666;
      margin: 0 0 6px;
    }

    .detail-section {
      margin-bottom: 8px;

      .detail-title {
        font-size: 12px;
        font-weight: 600;
        color: #555;
        margin-bottom: 4px;
      }
    }

    .detail-empty {
      color: #999;
      font-size: 12px;
      margin: 0;
    }

    .detail-line {
      margin-bottom: 6px;
    }

    .detail-actions {
      margin-top: 8px;
    }
  }

  // 预览展开时：详情卡片收进半高画布内（top:46px + 230px < 300px），避免盖住下方预览面板
  &.preview-open {
    .node-detail {
      max-height: 230px;
    }
  }
}

.mini-color-picker {
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #ffffff;
  cursor: pointer;
  vertical-align: middle;

  &::-webkit-color-swatch-wrapper {
    padding: 2px;
  }

  &::-webkit-color-swatch {
    border: none;
    border-radius: 4px;
  }
}

.graph-color-field {
  :deep(.ant-form-item-control-input-content) {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  :deep(.ant-input-affix-wrapper) {
    min-width: 0;
    flex: 1;
  }
}

.modal-hint {
    color: #999;
    font-size: 12px;
    margin: 0;
  }
</style>
