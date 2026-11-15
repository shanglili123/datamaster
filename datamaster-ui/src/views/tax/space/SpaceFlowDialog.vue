<template>
  <a-modal :open="open" title="空间功能流程与准入" width="1000px" :footer="null" @cancel="requestClose" destroy-on-close>
    <a-alert type="info" show-icon message="未发布时沿用原空间配置；发布后，选中的资产/质量环节严格约束API与分析，缺项禁止回退。分层分类不是门禁，数据开发不在流程中。" />
    <a-divider>当前生效规则</a-divider>
    <p v-if="!loaded">{{ busy ? '正在加载规则，请稍候。' : '规则尚未成功加载，请重新加载；不能据此判断当前线上流程。' }}</p>
    <p v-else-if="!publishedDefinition">尚未发布空间流程，沿用原有治理配置；下方草稿尚未生效。</p>
    <template v-else>
      <p>版本 {{ publishedVersion }} · {{ describeDefinition(publishedDefinition) }}</p>
      <p>已配置环节约束 API 与分析，不能回退绕过。此处仅表示规则已发布，不表示数据或任务已执行。</p>
    </template>
    <a-divider>流程草稿</a-divider>
    <p>草稿 revision：{{ revision }} · {{ dirty ? '有未保存修改' : '无未保存修改' }}（保存不改变线上规则）</p>
    <a-radio-group v-model:value="template" :disabled="busy || !loaded" @change="onDraftChange">
      <a-radio value="LIGHT">元数据 → API / 分析</a-radio>
      <a-radio value="ASSET">元数据 → 资产权限 → API / 分析</a-radio>
      <a-radio value="QUALITY">元数据 → 质量准入 → API / 分析</a-radio>
      <a-radio value="FULL">元数据 → 资产权限 → 质量准入 → API / 分析</a-radio>
    </a-radio-group>
    <a-alert v-if="customDraft" type="warning" show-icon message="已保存流程与当前模板连线不完全一致，正在保留原定义。选择模板会明确替换草稿，保存前不会生效。" />
    <a-button v-if="customDraft" :disabled="busy || !loaded" @click="onDraftChange">按所选模板重新生成草稿</a-button>
    <p>每个选中的前置节点都必须通过，不能用旁路连线绕过。校验只检查规则，不执行采集、质量或分析任务。</p>
    <SpaceFlowEditor :model-value="definition" :disabled="busy || !loaded" @update:model-value="editDefinition" />
    <details><summary>查看草稿节点与连线定义</summary><pre>{{ JSON.stringify(definition, null, 2) }}</pre></details>
    <p v-if="error" role="alert">{{ error }}</p>
    <a-space>
      <a-button :loading="busy" :disabled="busy" @click="reloadWithConfirmation">重新加载</a-button>
      <a-button v-hasPermi="['tax:space:edit']" :disabled="busy || !loaded" @click="validateDraft">校验草稿（不执行任务）</a-button>
      <a-button v-hasPermi="['tax:space:edit']" :disabled="busy || !loaded" @click="save">保存草稿</a-button>
      <a-button v-hasPermi="['tax:space:edit']" type="primary" :disabled="busy || !loaded || dirty || !revision" @click="previewPublication">预览变更并发布</a-button>
    </a-space>
    <p v-if="validationMessage">{{ validationMessage }}</p>
    <a-divider>发布版本历史</a-divider>
    <a-button :loading="versionsLoading" :disabled="busy || !loaded || versionsLoading" @click="loadVersions(1)">查看发布历史</a-button>
    <p v-if="versionsError" role="alert">{{ versionsError }}</p>
    <a-table v-if="versions" row-key="version" size="small" :loading="versionsLoading" :columns="versionColumns" :data-source="versions.rows" :pagination="{ current: versionsPage, pageSize: 10, total: versions.total, showSizeChanger: false }" @change="page => loadVersions(page.current)">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'definition'">{{ describeDefinition(record.definition) }}</template>
        <template v-else-if="column.key === 'actions'">
          <a-button v-hasPermi="['tax:space:edit']" type="link" size="small" :disabled="busy || !loaded" @click="copyVersionToDraft(record)">复制为草稿</a-button>
        </template>
      </template>
    </a-table>
    <a-collapse style="margin-top: 16px">
      <a-collapse-panel key="quality-admission" header="质量节点准入维护（可选，不执行质量检查）">
    <a-divider>质量准入确认（不修改原检查结果）</a-divider>
    <p>仅空间管理员可对本空间元数据登记表确认。检查通过依据须同表、成功完成且零问题；人工确认允许不合格但必须说明原因。发布新流程版本后需重新确认。</p>
    <a-form layout="vertical">
      <a-form-item label="数据源ID"><a-input v-model:value="quality.datasourceId" :disabled="busy" /></a-form-item>
      <a-form-item label="表名（与元数据精确一致）"><a-input v-model:value="quality.tableName" :maxlength="200" :disabled="busy" /></a-form-item>
      <a-form-item label="通过依据"><a-switch v-model:checked="quality.manual" checked-children="人工确认可用" un-checked-children="检查合格" :disabled="busy" /></a-form-item>
      <a-form-item v-if="!quality.manual" label="成功质量记录ID"><a-input v-model:value="quality.qualityLogId" :disabled="busy" /></a-form-item>
      <a-form-item label="原因/说明"><a-textarea v-model:value="quality.reason" :maxlength="1000" :disabled="busy" /></a-form-item>
      <a-space>
        <a-button :loading="historyLoading" :disabled="busy || !loaded || !tableSelected" @click="loadQualityHistory(1)">查询当前准入与历史</a-button>
        <a-button v-hasPermi="['tax:space:edit']" :disabled="busy || !loaded || !publishedQualityRequired || !tableSelected" @click="confirmQuality(false)">明确确认该表可用</a-button>
        <a-button v-hasPermi="['tax:space:edit']" danger :disabled="busy || !loaded || !publishedQualityRequired || !tableSelected" @click="confirmQuality(true)">撤销该表可用性确认</a-button>
      </a-space>
    </a-form>
    <p v-if="historyError" role="alert">{{ historyError }}</p>
    <template v-if="history">
      <p>所查对象：数据源 {{ historyTarget.datasourceId }} / {{ historyTarget.tableName }}</p>
      <a-alert :type="history.status === 'AVAILABLE' ? 'success' : 'warning'" show-icon :message="qualityStatusLabels[history.status] || '未知准入状态'" :description="`当前发布版本：${history.publishedVersion}。质量准入不代表资产、字段权限已通过。`" />
      <p v-if="history.current">最近确认依据：版本 {{ history.current.flow_version }} · {{ basisLabels[history.current.basis] || history.current.basis }} · 最近操作人ID {{ history.current.confirmed_by }} · {{ history.current.confirmed_at }}（是否有效以当前准入状态为准）</p>
      <a-table row-key="id" size="small" :loading="historyLoading" :columns="historyColumns" :data-source="history.rows" :pagination="{ current: historyPage, pageSize: 10, total: history.total, showSizeChanger: false }" :scroll="{ x: 900 }" @change="page => loadQualityHistory(page.current)">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'basis'">{{ basisLabels[record.basis] || record.basis }}</template>
        </template>
      </a-table>
    </template>
      </a-collapse-panel>
    </a-collapse>
    <a-modal :open="Boolean(publicationPreview)" title="确认流程规则变更" width="1100px" :confirm-loading="busy" :closable="!busy" :mask-closable="!busy" :keyboard="!busy" :cancel-button-props="{ disabled: busy }" :ok-button-props="{ disabled: dirty || !loaded }" ok-text="确认发布新版本" cancel-text="返回修改" @ok="publish" @cancel="publicationPreview = null" destroy-on-close>
      <template v-if="publicationPreview">
        <p>已保存草稿 revision {{ publicationPreview.revision }}：版本 {{ publicationPreview.publishedVersion }} → {{ publicationPreview.nextVersion }}</p>
        <p>当前规则：{{ describeDefinition(publicationPreview.published) }}</p>
        <p>发布后：{{ describeDefinition(publicationPreview.draft) }}</p>
        <p>新增前置要求：{{ describeRequirements(publicationPreview.addedRequirements) }}</p>
        <p>移除前置要求：{{ describeRequirements(publicationPreview.removedRequirements) }}</p>
        <div v-if="publicationPreview.addedEdges?.length || publicationPreview.removedEdges?.length">
          <p>新增连线：</p><ul v-if="publicationPreview.addedEdges?.length"><li v-for="edge in publicationPreview.addedEdges" :key="`added:${edge}`">{{ describeEdge(edge) }}</li></ul><p v-else>无</p>
          <p>移除连线：</p><ul v-if="publicationPreview.removedEdges?.length"><li v-for="edge in publicationPreview.removedEdges" :key="`removed:${edge}`">{{ describeEdge(edge) }}</li></ul><p v-else>无</p>
          <p>连线变化按规则展示；发布仍只更新准入约束，不按画布顺序启动任务。</p>
        </div>
        <a-alert v-if="publicationPreview.removedRequirements.length" type="warning" show-icon message="本次降低了空间准入要求，请确认这是有意的配置变更。" />
        <a-alert v-if="!publicationPreview.definitionChanged" type="warning" show-icon message="规则定义未变化；发布仍会生成新版本，不是无副作用操作。" />
        <a-alert v-if="publicationPreview.qualityEvidenceReset" type="warning" show-icon message="新版本要求质量准入：旧版本可用性确认不会自动继承，需要重新建立依据。" />
        <p>{{ publicationPreview.impactNotice }}</p>
        <section v-for="inventory in publicationPreview.inventories" :key="inventory.type">
          <h4>{{ inventory.type === 'API' ? '本空间已登记API' : '本空间已发布正式指标' }}（{{ inventory.available ? inventory.total : '未知' }}）</h4>
          <p>{{ inventory.notice }}</p>
          <a-table v-if="inventory.available" row-key="id" size="small" :columns="impactColumns" :data-source="inventory.rows" :loading="impactLoading[inventory.type]" :pagination="{ current: impactPages[inventory.type] || 1, pageSize: 10, total: inventory.total, showSizeChanger: false }" :scroll="{ x: 1300 }" @change="page => loadImpactPage(inventory.type, page.current)">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'readiness'">{{ record.readiness === 'MISSING_REQUIREMENTS' ? '已发现缺项' : '仍待消费核验' }}</template>
              <template v-else-if="column.dataIndex === 'nextSteps'">
                <ul v-if="record.nextSteps?.length"><li v-for="step in record.nextSteps" :key="step">{{ step }}</li></ul><span v-else>无已识别缺项；不代表准入已通过</span>
              </template>
              <template v-else-if="column.dataIndex === 'pendingChecks'">{{ (record.pendingChecks || []).join('；') }}</template>
            </template>
          </a-table>
        </section>
        <p v-if="impactError" role="alert">{{ impactError }}</p>
        <p>发布只更新准入规则，不会启动采集、质量检查、数据开发或指标查询任务。</p>
      </template>
    </a-modal>
  </a-modal>
</template>
<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { Modal, message } from 'ant-design-vue'
import SpaceFlowEditor from './SpaceFlowEditor.vue'
import { getSpaceFlow, saveSpaceFlow, publishSpaceFlow, confirmSpaceQuality, getSpaceQualityHistory, validateSpaceFlow, previewSpaceFlow, getSpaceFlowVersions, getSpaceFlowImpacts } from '@/api/tax/space/flow'
const props = defineProps({ open: Boolean, space: { type: Object, default: null } })
const emit = defineEmits(['close'])
function requestClose() {
  const token = generation
  if (busy.value) { message.info('操作处理中，请等待结果再关闭'); return }
  if (!dirty.value) { emit('close'); return }
  Modal.confirm({ title: '放弃未保存的流程草稿修改？', content: '只放弃本地修改，不改变已保存草稿或已发布规则。', onOk: () => { if (token === generation && !busy.value) emit('close') } })
}
const revision = ref(0), publishedVersion = ref(0), busy = ref(false), loaded = ref(false), error = ref(''), template = ref('LIGHT'), dirty = ref(false)
const publishedQualityRequired = ref(false), publishedDefinition = ref(null), savedDefinition = ref(null)
const validationMessage = ref(''), publicationPreview = ref(null)
const impactPages = reactive({}), impactLoading = reactive({}), impactError = ref('')
const impactColumns = [
  { title: '名称', dataIndex: 'name', width: 160 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '数据源ID', dataIndex: 'datasourceId', width: 100 },
  { title: '声明表', dataIndex: 'tableName', width: 140 },
  { title: '草稿准入预检', dataIndex: 'readiness', width: 140 },
  { title: '缺项处理 / 下一步', dataIndex: 'nextSteps', width: 300 },
  { title: '尚需核验', dataIndex: 'pendingChecks', width: 250 },
  { title: '影响说明（非执行结论）', dataIndex: 'notice', width: 260 }
]
const versions = ref(null), versionsLoading = ref(false), versionsError = ref(''), versionsPage = ref(1)
const versionColumns = [
  { title: '版本', dataIndex: 'version', width: 80 },
  { title: '生效规则', dataIndex: 'definition' },
  { title: '发布人ID', dataIndex: 'published_by', width: 110 },
  { title: '发布时间', dataIndex: 'published_at', width: 190 },
  { title: '操作', key: 'actions', width: 120 }
]
const nodeLabels = { METADATA: '元数据', ASSET: '资产权限', QUALITY: '质量准入', API: 'API', ANALYSIS: '分析' }
function describeRequirements(nodes = []) { return nodes.map(node => nodeLabels[node] || node).join('、') || '无' }
function describeEdge(edge) { return String(edge).split(' → ').map(node => nodeLabels[node] || node).join(' → ') }
function describeDefinition(value) {
  if (!value) return '尚未发布（沿用原治理配置）'
  return value.edges.map(edge => `${nodeLabels[edge.from] || edge.from} → ${nodeLabels[edge.to] || edge.to}`).join('；')
}
const quality = reactive({ datasourceId: '', tableName: '', qualityLogId: '', manual: true, reason: '' })
const history = ref(null), historyTarget = ref(null), historyLoading = ref(false), historyError = ref(''), historyPage = ref(1)
const tableSelected = computed(() => /^[1-9]\d*$/.test(quality.datasourceId) && Boolean(quality.tableName.trim()))
const qualityStatusLabels = {
  NOT_REQUIRED: '当前已发布流程未要求质量准入（或尚未发布）',
  PENDING: '待取得质量准入：尚无该表可用性确认',
  REVOKED: '已撤销：该表当前不可凭此记录通过质量准入',
  VERSION_EXPIRED: '流程版本已变更：旧确认不再有效，请重新确认',
  AVAILABLE: '该表当前版本质量准入有效'
}
const basisLabels = { MANUAL_AVAILABLE: '人工确认可用', CHECK_PASSED: '检查合格', REVOKED: '撤销可用性确认' }
const historyColumns = [
  { title: '流程版本', dataIndex: 'flow_version', width: 90 },
  { title: '操作依据', dataIndex: 'basis', width: 150 },
  { title: '检查记录ID', dataIndex: 'quality_log_id', width: 110 },
  { title: '原因/说明', dataIndex: 'reason' },
  { title: '操作人ID', dataIndex: 'confirmed_by', width: 100 },
  { title: '操作时间', dataIndex: 'confirmed_at', width: 190 }
]
let generation = 0, historyGeneration = 0, versionsGeneration = 0
const templateDefinition = computed(() => {
  const chain = ['METADATA', ...(['ASSET','FULL'].includes(template.value) ? ['ASSET'] : []), ...(['QUALITY','FULL'].includes(template.value) ? ['QUALITY'] : [])]
  const edges = chain.slice(1).map((to,i) => ({ from: chain[i], to }))
  edges.push({ from: chain[chain.length - 1], to: 'API' }, { from: chain[chain.length - 1], to: 'ANALYSIS' })
  return { nodes: [...chain, 'API', 'ANALYSIS'], edges }
})
const definition = computed(() => savedDefinition.value || templateDefinition.value)
function definitionKey(value) {
  return JSON.stringify({ nodes: [...value.nodes].sort(), edges: value.edges.map(edge => `${edge.from}:${edge.to}`).sort() })
}
const customDraft = computed(() => Boolean(savedDefinition.value && definitionKey(savedDefinition.value) !== definitionKey(templateDefinition.value)))
function editDefinition(value) {
  setDraft(value); dirty.value = true; validationMessage.value = ''; publicationPreview.value = null
}
function onDraftChange() {
  savedDefinition.value = null; dirty.value = true; validationMessage.value = ''; publicationPreview.value = null
}
const context = () => ({ spaceId: props.space?.id, spaceCode: props.space?.code || props.space?.spaceCode })
function clearHistory() {
  ++historyGeneration; history.value = null; historyTarget.value = null; historyLoading.value = false; historyError.value = ''; historyPage.value = 1
}
function setDraft(value) {
  savedDefinition.value = JSON.parse(JSON.stringify(value))
  const nodes = value.nodes || []
  template.value = nodes.includes('ASSET') ? (nodes.includes('QUALITY') ? 'FULL' : 'ASSET') : (nodes.includes('QUALITY') ? 'QUALITY' : 'LIGHT')
}
function copyVersionToDraft(record) {
  const token = generation
  const previous = JSON.parse(JSON.stringify(record.definition))
  Modal.confirm({
    title: `复制版本 ${record.version} 为草稿？`,
    content: '只替换本地草稿，未保存修改将被覆盖；当前生效规则不会改变。恢复也必须重新保存、预览并发布新版本，旧版本号不会复用。',
    onOk() {
      if (token !== generation || busy.value) return
      setDraft(previous); dirty.value = true; validationMessage.value = ''; publicationPreview.value = null
    }
  })
}
function apply(data) {
  revision.value = data.revision; publishedVersion.value = data.publishedVersion
  publishedDefinition.value = data.published || null
  if (data.draft) setDraft(data.draft)
  else { savedDefinition.value = null; template.value = 'LIGHT' }
  publishedQualityRequired.value = Boolean(data.published?.nodes?.includes('QUALITY'))
  publicationPreview.value = null; validationMessage.value = ''
  dirty.value = false; loaded.value = true
}
function reloadWithConfirmation() {
  if (busy.value) return
  if (!dirty.value) { load(); return }
  const token = generation
  Modal.confirm({ title: '重新加载将丢弃未保存修改？', content: '线上规则不会改变。', onOk() { if (token === generation && !busy.value) return load() } })
}
async function load() {
  const token = ++generation; busy.value = true; loaded.value = false; error.value = ''; publicationPreview.value = null; validationMessage.value = ''; clearHistory(); clearVersions()
  try { const res = await getSpaceFlow(context()); if (token === generation) apply(res.data) }
  catch (e) { if (token === generation) error.value = e?.message || '加载失败，确认权限及流程迁移已完成' }
  finally { if (token === generation) busy.value = false }
}
async function loadQualityHistory(page = 1) {
  if (!tableSelected.value) return
  const token = generation, historyToken = ++historyGeneration
  const target = { datasourceId: quality.datasourceId, tableName: quality.tableName }
  historyLoading.value = true; historyError.value = ''; history.value = null
  try {
    const res = await getSpaceQualityHistory({ ...context(), ...target, pageNum: page, pageSize: 10 })
    if (token === generation && historyToken === historyGeneration) {
      history.value = res.data; historyTarget.value = target; historyPage.value = page
    }
  } catch (e) {
    if (token === generation && historyToken === historyGeneration) historyError.value = e?.message || '查询准入历史失败'
  } finally {
    if (token === generation && historyToken === historyGeneration) historyLoading.value = false
  }
}
async function save() {
  const token = generation; busy.value = true; error.value = ''
  try { const res = await saveSpaceFlow({ ...context(), revision: revision.value, definition: definition.value }); if (token === generation) { apply(res.data); message.success('草稿已保存，线上规则未改变') } }
  catch (e) { if (token === generation) error.value = e?.message || '保存失败' }
  finally { if (token === generation) busy.value = false }
}
function clearVersions() {
  ++versionsGeneration; versions.value = null; versionsLoading.value = false; versionsError.value = ''; versionsPage.value = 1
}
async function loadVersions(page = 1) {
  const token = generation, versionToken = ++versionsGeneration
  versionsLoading.value = true; versionsError.value = ''
  try {
    const res = await getSpaceFlowVersions({ ...context(), pageNum: page, pageSize: 10 })
    if (token === generation && versionToken === versionsGeneration) { versions.value = res.data; versionsPage.value = page }
  } catch (e) {
    if (token === generation && versionToken === versionsGeneration) versionsError.value = e?.message || '加载发布历史失败'
  } finally {
    if (token === generation && versionToken === versionsGeneration) versionsLoading.value = false
  }
}
async function validateDraft() {
  const token = generation; busy.value = true; error.value = ''; validationMessage.value = ''
  try {
    await validateSpaceFlow({ ...context(), definition: definition.value })
    if (token === generation) validationMessage.value = '草稿结构校验通过；未保存、未发布，也未执行任何数据任务。'
  } catch (e) { if (token === generation) error.value = e?.message || '草稿校验失败' }
  finally { if (token === generation) busy.value = false }
}
async function previewPublication() {
  if (busy.value || dirty.value || !loaded.value || !revision.value) return
  const token = generation; busy.value = true; error.value = ''; publicationPreview.value = null
  try {
    const res = await previewSpaceFlow({ ...context(), revision: revision.value })
    if (token === generation) {
      publicationPreview.value = res.data; impactError.value = ''
      for (const type of ['API', 'METRIC']) { impactPages[type] = 1; impactLoading[type] = false }
    }
  } catch (e) { if (token === generation) error.value = e?.message || '发布预览失败，请重新加载草稿' }
  finally { if (token === generation) busy.value = false }
}
async function loadImpactPage(type, page) {
  const preview = publicationPreview.value, token = generation
  if (!preview || impactLoading[type]) return
  impactLoading[type] = true; impactError.value = ''
  try {
    const res = await getSpaceFlowImpacts({ ...context(), revision: preview.revision, type, pageNum: page, pageSize: 10 })
    if (token === generation && publicationPreview.value === preview) {
      const index = preview.inventories.findIndex(inventory => inventory.type === type)
      if (index >= 0) preview.inventories[index] = res.data
      impactPages[type] = page
    }
  } catch (e) {
    if (token === generation && publicationPreview.value === preview) impactError.value = e?.message || '消费影响清单加载失败，不能视为无影响'
  } finally { if (token === generation && publicationPreview.value === preview) impactLoading[type] = false }
}
async function publish() {
  const preview = publicationPreview.value
  if (!preview || busy.value || dirty.value || !loaded.value || preview.revision !== revision.value) return
  const token = generation; busy.value = true; error.value = ''
  try {
    const res = await publishSpaceFlow({ ...context(), revision: preview.revision })
    if (token === generation) {
      apply(res.data); clearHistory(); clearVersions(); message.success('流程规则已发布，未启动数据任务')
      await loadVersions(1)
    }
  } catch (e) { if (token === generation) { publicationPreview.value = null; error.value = e?.message || '发布失败，请重新预览' } }
  finally { if (token === generation) busy.value = false }
}
function confirmQuality(revoke = false) {
  if ((revoke || quality.manual) && !quality.reason.trim()) { message.warning('请填写原因/说明'); return }
  if (!revoke && !quality.manual && !/^[1-9]\d*$/.test(quality.qualityLogId)) { message.warning('请填写有效质量记录ID'); return }
  const token = generation
  const request = { ...context(), ...quality, qualityLogId: !revoke && !quality.manual ? quality.qualityLogId : null, revoke }
  Modal.confirm({
    title: revoke ? '撤销指定表数据可用性确认' : '明确确认指定表数据可用',
    content: `数据源 ${request.datasourceId} / ${request.tableName}。${revoke ? '撤销后，依赖质量准入的开放与分析将被拒绝。' : '本操作只产生质量准入依据，不授予资产或字段权限，不把不合格结果改成合格。'}`,
    async onOk() {
      if (token !== generation || busy.value) return
      busy.value = true; error.value = ''
      try {
        await confirmSpaceQuality(request)
        if (token === generation) {
          message.success(revoke ? '已撤销该表可用性确认' : '已记录该表可用性确认')
          if (request.datasourceId === quality.datasourceId && request.tableName === quality.tableName) await loadQualityHistory(1)
        }
      } catch (e) { if (token === generation) error.value = e?.message || (revoke ? '撤销失败' : '确认失败') }
      finally { if (token === generation) busy.value = false }
    }
  })
}
onBeforeUnmount(() => { ++generation; ++historyGeneration; ++versionsGeneration })
watch(() => [quality.datasourceId, quality.tableName], clearHistory, { flush: 'sync' })
watch(() => [props.open, props.space?.id, props.space?.code, props.space?.spaceCode], () => {
  ++generation; busy.value = false; loaded.value = false; error.value = ''; revision.value = 0; publishedVersion.value = 0; publishedQualityRequired.value = false
  clearHistory(); clearVersions(); publicationPreview.value = null; validationMessage.value = ''; publishedDefinition.value = null; savedDefinition.value = null; template.value = 'LIGHT'
  Object.assign(quality, { datasourceId: '', tableName: '', qualityLogId: '', manual: true, reason: '' })
  if (props.open && props.space?.id) load()
}, { immediate: true })
</script>
