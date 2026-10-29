<template>
  <section class="decision-proposal" aria-label="决策提案">
    <header class="proposal-heading">
      <strong>决策提案</strong>
      <a-tag>{{ proposal.status || 'UNKNOWN' }}</a-tag>
      <span>版本 {{ proposal.revision ?? '未知' }}</span>
    </header>
    <p><strong>目标：</strong>{{ proposal.goal || '尚未提供' }}</p>
    <p class="muted">默认只分析，预览确认后按已有审批执行。估计影响不代表实际改善，也不代表最优方案。</p>
    <p>本体：{{ proposal.ontologyId || '未选择' }} · 对象主键：{{ proposal.objectKey || '未提供，请在当前请求或上下文中补充' }}</p>
    <a-alert v-if="!ontologyId" type="warning" show-icon message="请先选择指标本体；历史提案仍按服务端原本体校验，不会改绑到当前选择。" />
    <a-alert v-if="!canExecute" type="info" show-icon message="当前账号没有业务执行权限（ont:action:edit），可查看提案及刷新状态。" />
    <a-alert v-if="errorText" type="warning" show-icon :message="errorText" />
    <a-alert v-if="!fresh" type="info" show-icon message="展示数据仅供参考，需成功加载服务端最新提案后才能准备或提交。" />
    <p v-if="proposal.message">{{ proposal.message }}</p>

    <details class="proposal-section">
      <summary>证据与可信等级（{{ proposal.evidence?.length || 0 }}）</summary>
      <div v-for="evidence in proposal.evidence || []" :key="evidence.id" class="evidence-item">
        <strong>{{ evidence.id }} · {{ evidence.source || '来源未知' }}</strong>
        <a-tag>{{ verificationLabel(evidence.verification) }}</a-tag>
        <p>{{ evidence.summary || '未提供证据摘要' }}</p>
        <small v-if="evidence.operationId">操作 ID：{{ evidence.operationId }}</small>
        <pre v-if="evidence.details">{{ formatValue(evidence.details) }}</pre>
      </div>
      <p v-if="!proposal.evidence?.length" class="muted">暂无可核验证据，不能据此声称效果已验证。</p>
    </details>

    <DecisionCalculation :proposal="proposal" :context-key="contextKey" :disabled="disabled || Boolean(busy) || !fresh || locked" @updated="applyCalculation" />

    <div class="proposal-section">
      <strong>候选方案（请选择后准备预览）</strong>
      <article v-for="candidate in proposal.candidates || []" :key="candidate.id" class="candidate-card" :class="{ selected: selectedId === candidate.id }">
        <label class="candidate-heading">
          <input v-model="selectedId" type="radio" :value="candidate.id" :disabled="busy || locked || candidate.baseline || !fresh" />
          <strong>{{ candidate.name || candidate.id }}</strong>
          <a-tag v-if="candidate.baseline">不干预基线 · 不可执行</a-tag>
        </label>
        <p>{{ candidate.rationale || '未说明选择依据' }}</p>
        <p class="muted">证据引用：{{ candidate.evidenceIds?.join('、') || '无' }}</p>
        <ul class="plain-list">
          <li v-for="(constraint, index) in candidate.constraints || []" :key="index">
            <a-tag :color="constraint.status === 'PASS' ? 'green' : constraint.status === 'FAIL' ? 'red' : 'orange'">{{ constraint.status || 'UNKNOWN' }}</a-tag>
            {{ constraint.name }}：{{ constraint.reason || '未提供校验说明' }}
          </li>
        </ul>
        <p v-if="!candidate.constraints?.length" class="muted">约束：UNKNOWN（未提供校验结果）</p>
        <ul class="plain-list">
          <li v-for="(impact, index) in candidate.impacts || []" :key="index">
            {{ impact.name }}：{{ impact.value ?? '未知' }} {{ impact.unit || '' }}
            <a-tag>{{ impactLabel(impact.method) }}</a-tag>
            <span class="muted">证据：{{ impact.evidenceId || '无' }}</span>
          </li>
        </ul>
        <p v-if="!candidate.impacts?.length" class="muted">影响：unknown（未知），尚无可用测算。</p>
        <p>评估：{{ candidate.evaluation?.status || 'UNKNOWN' }} · {{ candidate.evaluation?.summary || '尚未评估' }}</p>
        <p>风险：{{ formatValue(candidate.risks) || '未提供，不代表无风险' }}</p>
        <p>未知项：{{ formatValue(candidate.evaluation?.unknowns) || '未提供，不代表不存在未知项' }}</p>
        <p v-if="candidate.suggestedBinding" class="muted">建议绑定（仅供参考）：{{ candidate.suggestedBinding.name || candidate.suggestedBinding.type || '待服务端确认' }}</p>
      </article>
      <p v-if="!proposal.candidates?.length">服务端尚未生成候选方案，请补充目标、本体或对象信息。</p>
    </div>

    <section v-if="proposal.previewDigest && proposal.binding" class="proposal-section preview-section">
      <strong>服务端执行预览</strong>
      <p>已准备候选：{{ proposal.selectedCandidateId || '未知' }} · 绑定：{{ proposal.binding.name || proposal.binding.targetId }}（{{ proposal.binding.type }}）</p>
      <p>对象：{{ proposal.binding.objectKey || proposal.objectKey || '未提供' }} · 定义版本：{{ proposal.binding.version || '未提供' }}</p>
      <p>审批：{{ proposal.binding.requiresApproval === true ? '需要审批，提交不等于执行成功' : proposal.binding.requiresApproval === false ? '按服务端既有策略执行' : '未知，以服务端审批策略为准' }}</p>
      <details><summary>只读动作参数与摘要</summary><pre>{{ formatValue(proposal.binding.inputParams || {}) }}</pre><p class="digest">预览摘要：{{ proposal.previewDigest }}</p><p class="digest">定义摘要：{{ proposal.definitionDigest || proposal.binding.definitionDigest || '未提供' }}</p></details>
      <p>有效期：{{ proposal.expiresAt || '未提供（由服务端校验）' }}</p>
      <p v-if="selectedId !== proposal.selectedCandidateId" class="warning-text">当前选择与预览不一致，请重新准备预览，不能直接确认。</p>
    </section>

    <section class="proposal-section">
      <strong>执行与效果状态</strong>
      <p>执行状态：{{ proposal.executionStatus || '尚未返回' }} · 提案状态：{{ proposal.status || 'UNKNOWN' }}</p>
      <a-alert v-if="approvalPending" type="warning" show-icon message="WAITING_APPROVAL：已进入既有审批流程，尚未完成业务执行。" />
      <a-alert v-if="reconciliationRequired" type="warning" show-icon message="RECONCILIATION_REQUIRED：执行结果待核对；不要重复提交，请刷新并联系业务人员核对。" />
      <p v-if="proposal.status === 'SUBMITTED' || proposal.executionStatus === 'SUBMITTED'" class="warning-text">SUBMITTED 仅表示已提交，不代表执行成功或指标改善。</p>
      <p v-if="proposal.executionId || proposal.runId">执行 ID：{{ proposal.executionId || '无' }} · 运行 ID：{{ proposal.runId || '无' }}</p>
      <p>效果复查：{{ reviewUnknown ? 'UNKNOWN' : (proposal.effectStatus || 'UNKNOWN') }} · {{ reviewNotice || proposal.effectMessage || '尚未进行真实只读复查，实际效果未知。' }}</p>
      <p class="muted">刷新、关闭页面或 SSE 断连不会撤销已提交的业务执行。效果复查只查询真实数据，不自动声称改善。</p>
    </section>
    <div class="proposal-actions">
      <a-button :loading="busy === 'load'" :disabled="Boolean(busy) || disabled" @click="loadLatest">重新加载提案</a-button>
      <a-button v-hasPermi="['ont:action:edit']" :loading="busy === 'prepare'" :disabled="!canPrepare" @click="prepare">准备执行预览</a-button>
      <a-button v-hasPermi="['ont:action:edit']" type="primary" :loading="busy === 'confirm'" :disabled="!canConfirm" @click="confirm">明确确认并提交</a-button>
      <a-button :loading="busy === 'status'" :disabled="Boolean(busy) || disabled || !decisionId" @click="refreshStatus">刷新运行状态</a-button>
      <a-button :loading="busy === 'review'" :disabled="Boolean(busy) || disabled || !decisionId" @click="review">只读复查指标</a-button>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { checkPermi } from '@/utils/permission'
import DecisionCalculation from './DecisionCalculation.vue'
import { confirmDecision, getDecision, getDecisionStatus, prepareDecision, reviewDecision } from '@/api/ai/decision'

const props = defineProps({
  decisionId: { type: String, required: true },
  snapshot: { type: Object, default: () => ({}) },
  contextKey: { type: String, required: true },
  ontologyId: { type: [String, Number], default: null },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['updated'])
const proposal = ref({})
const selectedId = ref('')
const busy = ref('')
const fresh = ref(false)
const errorText = ref('')
const reviewNotice = ref('')
const reviewUnknown = ref(false)
let generation = 0
let disposed = false
const canExecute = computed(() => checkPermi(['ont:action:edit']))
const approvalPending = computed(() => [proposal.value.status, proposal.value.executionStatus].some(value => ['WAITING_APPROVAL', 'PENDING_APPROVAL'].includes(value)))
const reconciliationRequired = computed(() => [proposal.value.status, proposal.value.executionStatus].includes('RECONCILIATION_REQUIRED'))
const locked = computed(() => Boolean(proposal.value.executionId || proposal.value.runId || proposal.value.executionStatus ||
  !['PROPOSED', 'PREVIEW_READY'].includes(proposal.value.status)))
const selected = computed(() => (proposal.value.candidates || []).find(item => item.id === selectedId.value))
const canPrepare = computed(() => !busy.value && !props.disabled && fresh.value && canExecute.value && Boolean(props.ontologyId) &&
  !locked.value && Boolean(selected.value) && !selected.value.baseline && !hasFailedConstraint(selected.value))
const canConfirm = computed(() => canPrepare.value && proposal.value.status === 'PREVIEW_READY' && Boolean(proposal.value.binding && proposal.value.previewDigest) && proposal.value.selectedCandidateId === selectedId.value)

function hasFailedConstraint(candidate) {
  return (candidate?.constraints || []).some(item => item.status === 'FAIL') || candidate?.evaluation?.status === 'FAIL'
}
function context() {
  return { generation, key: props.contextKey, id: props.decisionId }
}
function isCurrent(token) {
  return !disposed && token.generation === generation && token.key === props.contextKey && token.id === props.decisionId
}
function applyProposal(data, token) {
  if (!isCurrent(token)) return false
  if (!data || String(data.id) !== props.decisionId || !Number.isInteger(data.revision)) throw new Error('服务端未返回有效的提案 ID 或版本，请重新加载。')
  proposal.value = data
  selectedId.value = data.selectedCandidateId || ''
  fresh.value = true
  errorText.value = ''
  emit('updated', data)
  return true
}
function applyCalculation(data) {
  applyProposal(data, context())
}
function isExpired() {
  const raw = proposal.value.expiresAt
  if (!raw) return false
  const time = typeof raw === 'number' ? raw : Date.parse(raw)
  return Number.isFinite(time) && time <= Date.now()
}
function mutationAllowed() {
  if (!fresh.value || props.disabled || !canExecute.value || !props.ontologyId || locked.value || !selected.value || selected.value.baseline || hasFailedConstraint(selected.value)) return false
  if (isExpired()) {
    errorText.value = '提案已过期，请重新发起决策分析。'
    return false
  }
  return true
}
async function loadLatest() {
  if (busy.value || props.disabled || !props.decisionId) return
  reviewNotice.value = ''
  reviewUnknown.value = false
  const token = context()
  busy.value = 'load'
  fresh.value = false
  try {
    applyProposal((await getDecision(token.id)).data, token)
  } catch (error) {
    if (isCurrent(token)) errorText.value = error?.message || '提案加载失败，历史预览不能直接执行。'
  } finally {
    if (isCurrent(token)) busy.value = ''
  }
}
async function prepare() {
  if (!canPrepare.value || !mutationAllowed()) return
  const token = context()
  const request = { decisionId: props.decisionId, revision: proposal.value.revision, candidateId: selectedId.value }
  busy.value = 'prepare'
  let requested = false
  try {
    await ElMessageBox.confirm('将由服务端校验候选方案并生成敏感业务操作的只读预览。此步骤不会提交执行，后续仍需明确确认。是否准备预览？', '准备决策执行预览', { type: 'warning', confirmButtonText: '准备预览', cancelButtonText: '取消' })
    if (!isCurrent(token) || !mutationAllowed() || request.candidateId !== selectedId.value || request.revision !== proposal.value.revision) return
    requested = true
    applyProposal((await prepareDecision(request)).data, token)
  } catch (error) {
    if (isCurrent(token) && error !== 'cancel' && error !== 'close') {
      fresh.value = !requested && fresh.value
      errorText.value = error?.message || '准备失败，请重新加载提案后重试。'
    }
  } finally {
    if (isCurrent(token)) busy.value = ''
  }
}
async function confirm() {
  if (!canConfirm.value || !mutationAllowed()) return
  const token = context()
  const request = { decisionId: props.decisionId, revision: proposal.value.revision, candidateId: selectedId.value, previewDigest: proposal.value.previewDigest }
  busy.value = 'confirm'
  let requested = false
  try {
    await ElMessageBox.confirm('确认按上方服务端预览提交业务操作？提交后可能进入审批或执行；提交成功不等于执行成功，关闭页面不能撤销。请核对对象、只读参数和风险。', '明确确认提交决策', { type: 'warning', confirmButtonText: '确认并提交', cancelButtonText: '取消', closeOnClickModal: false })
    if (!isCurrent(token) || !mutationAllowed()) return
    // 历史消息和确认弹窗中的预览都不能直接作为执行依据，提交前再读取最新版本。
    const latest = (await getDecision(token.id)).data
    if (!isCurrent(token)) return
    const unchanged = latest?.revision === request.revision && latest?.selectedCandidateId === request.candidateId && latest?.previewDigest === request.previewDigest
    applyProposal(latest, token)
    if (!unchanged || latest.status !== 'PREVIEW_READY' || !mutationAllowed()) {
      errorText.value = '提案已更新或不可执行，未提交。请核对最新提案并重新准备/确认。'
      return
    }
    requested = true
    applyProposal((await confirmDecision(request)).data, token)
  } catch (error) {
    if (isCurrent(token) && error !== 'cancel' && error !== 'close') {
      fresh.value = false
      errorText.value = requested ? '提交结果未知，请刷新运行状态核对，勿重复提交；断连不表示撤销执行。' : (error?.message || '校验失败，未提交，请重新加载。')
    }
  } finally {
    if (isCurrent(token)) busy.value = ''
  }
}
async function refreshStatus() {
  await readOperation('status', getDecisionStatus)
}
async function review() {
  reviewNotice.value = ''
  reviewUnknown.value = false
  await readOperation('review', reviewDecision)
}
async function readOperation(kind, api) {
  if (busy.value || props.disabled || !props.decisionId) return
  const token = context()
  busy.value = kind
  try {
    const applied = applyProposal((await api(token.id)).data, token)
    if (applied && kind === 'review' && !proposal.value.effectMessage) reviewNotice.value = '服务端未提供可核实的复查结论，实际效果未知。'
  } catch (error) {
    if (isCurrent(token)) {
      if (kind === 'review') {
        reviewUnknown.value = true
        reviewNotice.value = 'UNKNOWN：当前服务不支持复查或真实只读查询失败，实际效果未知。'
      }
      else { fresh.value = false; errorText.value = error?.message || '状态刷新失败；不表示业务执行已取消。' }
    }
  } finally {
    if (isCurrent(token)) busy.value = ''
  }
}
function formatValue(value) {
  if (value == null) return ''
  return typeof value === 'string' ? value : JSON.stringify(value, null, 2)
}
function verificationLabel(value) {
  const key = String(value || 'UNKNOWN').toUpperCase()
  return ({ VERIFIED_METRIC: 'VERIFIED_METRIC · 正式指标已核验', UNVERIFIED_EXPLORATORY: 'UNVERIFIED_EXPLORATORY · 探索数据未核验', VERIFIED: 'VERIFIED · 已核验', UNVERIFIED: 'UNVERIFIED · 未核验', UNKNOWN: 'UNKNOWN · 可信度未知', FAILED: 'FAILED · 核验失败' })[key] || `${value} · 服务端核验标记`
}
function impactLabel(value) {
  const key = String(value || 'unknown').toLowerCase()
  return ({ computed: 'computed · 已计算（非实际效果）', estimated: 'estimated · 估计', unverified: 'unverified · 未核验', unknown: 'unknown · 未知' })[key] || `unknown · 未识别测算方式（${value}）`
}
watch(() => [props.decisionId, props.contextKey], () => {
  ++generation
  busy.value = ''
  fresh.value = false
  errorText.value = ''
  reviewNotice.value = ''
  reviewUnknown.value = false
  proposal.value = props.snapshot || {}
  selectedId.value = ''
  loadLatest()
}, { immediate: true, flush: 'sync' })
watch(() => props.disabled, (disabled) => {
  if (!disabled && !fresh.value && !busy.value) loadLatest()
})
onBeforeUnmount(() => { disposed = true; ++generation })
</script>

<style scoped>
.decision-proposal { margin-top: 16px; padding: 16px; border: 1px solid #d9dfe8; border-radius: 8px; overflow-wrap: anywhere; }
.proposal-heading, .candidate-heading, .proposal-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.proposal-heading > strong { margin-right: auto; }
.proposal-section { margin-top: 16px; border-top: 1px solid #e5e7eb; padding-top: 12px; }
.candidate-card { margin-top: 12px; padding: 12px; border: 1px solid #d9dfe8; border-radius: 6px; }
.candidate-card.selected { border-color: #1677ff; }
.candidate-heading { cursor: pointer; }
.muted { color: #667085; font-size: 12px; }
.warning-text { color: #92400e; }
.plain-list { margin: 8px 0; padding-left: 20px; }
.plain-list li { margin: 6px 0; }
.evidence-item { margin: 12px 0; }
.proposal-actions { margin-top: 16px; }
.decision-proposal :deep(.ant-alert) { margin: 10px 0; }
pre { max-height: 240px; overflow: auto; white-space: pre-wrap; background: #f6f8fa; padding: 10px; border-radius: 4px; }
.digest { font-size: 12px; word-break: break-all; }
summary { cursor: pointer; }
</style>
