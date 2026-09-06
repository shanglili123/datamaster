<template>
  <div class="app-container ont-workspace">
    <!-- 头部：返回 + 本体信息 -->
    <div class="workspace-header">
      <a-button type="text" class="back-btn" @click="goBack">
        <template #icon><ArrowLeftOutlined /></template>
        返回
      </a-button>
      <a-divider type="vertical" />
      <div class="ws-info">
        <span class="ws-name" :title="ontology.name">{{ ontology.name || '加载中...' }}</span>
        <a-tag :color="statusColor(ontology.status)">{{ statusText(ontology.status) }}</a-tag>
      </div>
    </div>

    <!-- 工作台 Tabs -->
    <div class="workspace-body">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="graph" tab="可视化图谱">
          <GraphPanel v-if="visited.graph" v-show="activeTab === 'graph'" :ontology-id="ontologyId" @changed="onGraphChanged" />
        </a-tab-pane>
        <a-tab-pane key="concept" tab="概念管理">
          <ConceptPanel ref="conceptPanelRef" v-if="visited.concept" v-show="activeTab === 'concept'" :ontology-id="ontologyId" />
        </a-tab-pane>
        <a-tab-pane key="property" tab="属性管理">
          <PropertyPanel ref="propertyPanelRef" v-if="visited.property" v-show="activeTab === 'property'" :ontology-id="ontologyId" />
        </a-tab-pane>
        <a-tab-pane key="relation" tab="关系管理">
          <RelationPanel ref="relationPanelRef" v-if="visited.relation" v-show="activeTab === 'relation'" :ontology-id="ontologyId" />
        </a-tab-pane>
        <a-tab-pane key="action" tab="动作管理">
          <ActionPanel ref="actionPanelRef" v-if="visited.action" v-show="activeTab === 'action'" :ontology-id="ontologyId" @changed="onActionChanged" />
        </a-tab-pane>
        <a-tab-pane key="workflow" tab="动作编排">
          <WorkflowPanel ref="workflowPanelRef" v-if="visited.workflow" v-show="activeTab === 'workflow'" :ontology-id="ontologyId" />
        </a-tab-pane>
        <a-tab-pane key="object" tab="对象实例">
          <ObjectPanel ref="objectPanelRef" v-if="visited.object" v-show="activeTab === 'object'" :ontology-id="ontologyId" @switch-tab="onSwitchTab" />
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup name="OntWorkspace">
import { getOntology } from '@/api/ont/ontology'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import ConceptPanel from './components/ConceptPanel.vue'
import PropertyPanel from './components/PropertyPanel.vue'
import RelationPanel from './components/RelationPanel.vue'
import GraphPanel from './components/GraphPanel.vue'
import ActionPanel from './components/ActionPanel.vue'
import WorkflowPanel from './components/WorkflowPanel.vue'
import ObjectPanel from './components/ObjectPanel.vue'

const route = useRoute()
const router = useRouter()

const ontologyId = computed(() => route.params.ontologyId)
const ontology = ref({})
const activeTab = ref('graph')
// 懒挂载常驻：首次进入的 tab 才挂载，之后保持存活（v-show 切换），避免反复销毁重建/重复请求导致的卡顿
const visited = reactive({ graph: true })
// 支持外部直达：对象实例页待审批时 ?tab=action 跳转到审批中心（动作管理 tab）
const queriedTab = route.query.tab
if (queriedTab && ['graph', 'concept', 'property', 'relation', 'action', 'workflow', 'object'].includes(queriedTab)) {
  activeTab.value = queriedTab
  visited[queriedTab] = true
}
const conceptPanelRef = ref(null)
const propertyPanelRef = ref(null)
const relationPanelRef = ref(null)
const actionPanelRef = ref(null)
const workflowPanelRef = ref(null)
const objectPanelRef = ref(null)
// 图谱变更后仅标记各面板为脏，切换到对应面板时再刷新，避免一次性触发全部请求
const dirty = reactive({ concept: false, property: false, relation: false, action: false, workflow: false, object: false })

watch(activeTab, key => {
  visited[key] = true
  if (dirty[key]) {
    const panelRef = { concept: conceptPanelRef, property: propertyPanelRef, relation: relationPanelRef, action: actionPanelRef, workflow: workflowPanelRef, object: objectPanelRef }[key]
    if (panelRef?.value && typeof panelRef.value.reload === 'function') {
      panelRef.value.reload()
      dirty[key] = false
    }
  }
})

function onGraphChanged() {
  dirty.concept = true
  dirty.property = true
  dirty.relation = true
  dirty.action = true
  dirty.workflow = true
  dirty.object = true
}

function onActionChanged() {
  dirty.workflow = true
}

// ObjectPanel 行操作待审批 → 切到「动作管理」tab（审批中心/执行记录区）
function onSwitchTab(key) {
  visited[key] = true
  // 跳到动作管理时强制刷新执行记录，确保能看到刚提交的待审批单
  if (key === 'action') dirty.action = true
  activeTab.value = key
}

function statusText(status) {
  const map = { 0: '草稿', 1: '已发布', 2: '已归档' }
  return map[status] || '未知'
}

function statusColor(status) {
  const map = { 0: 'blue', 1: 'green', 2: 'default' }
  return map[status] || 'default'
}

function goBack() {
  router.push('/ont/ontology')
}

function loadOntology() {
  if (!ontologyId.value) return
  getOntology(ontologyId.value).then(res => {
    ontology.value = res.data || {}
  })
}

watch(ontologyId, () => {
  loadOntology()
})

loadOntology()
</script>

<style lang="scss" scoped>
.ont-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workspace-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);

  .back-btn {
    padding-left: 0;
    color: #4e5969;

    &:hover {
      color: #1677ff;
    }
  }

  .ws-info {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;

    .ws-name {
      font-size: 16px;
      font-weight: 600;
      color: #1f2d3d;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.workspace-body {
  flex: 1;
  min-height: 0;
  padding: 4px 16px 16px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
}
</style>
