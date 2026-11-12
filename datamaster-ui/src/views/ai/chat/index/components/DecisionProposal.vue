<template>
  <section class="decision-proposal" aria-label="决策提案">
    <header class="proposal-heading">
      <strong>决策提案</strong>
      <a-tag>{{ proposal.status || 'UNKNOWN' }}</a-tag>
      <span>版本 {{ proposal.revision ?? '未知' }}</span>
    </header>
    <p><strong>目标：</strong>{{ proposal.goal || '尚未提供' }}</p>
    <p class="muted">默认只分析，预览确认后按已有审批执行。估计影响不代表实际改善，也不代表最优方案。</p>
    <p>本体：{{ proposal.ontologyId || '未选择' }} · 对象主键：{{ proposal.objectKey || '未提供，请明确填写对象主键' }}</p>
    <a-alert v-if="!ontologyId" type="warning" show-icon message="请先选择指标本体；历史提案仍按服务端原本体校验，不会改绑到当前选择。" />
    <a-alert v-if="!canExecute" type="info" show-icon message="当前账号没有业务执行权限（ont:action:edit），可查看提案及刷新状态。" />
    <a-alert v-if="errorText" type="warning" show-icon :message="errorText" />
    <a-alert v-if="!fresh" type="info" show-icon message="展示数据仅供参考，需成功加载服务端最新提案后才能准备或提交。" />
    <p v-if="proposal.message">{{ proposal.message }}</p>
    <p class="muted">草稿有效期：{{ proposal.expiresAt || '未提供' }} · 证据有效期：{{ proposal.evidenceExpiresAt || proposal.expiresAt || '未提供' }}</p>
    <p v-if="proposal.dispositionReason">人工处置：{{ proposal.dispositionReason }} · 复查时间：{{ proposal.reviewAfter || '未约定' }}</p>
    <details v-if="canDispose || canReopen" class="proposal-section">
      <summary>记录不干预、暂缓、放弃或重新打开（不执行业务动作）</summary>
      <label>处置原因 <input v-model="dispositionReason" :disabled="Boolean(busy) || disabled" maxlength="1000" /></label>
      <label>复查时间（暂缓必填，ISO含时区）<input v-model="reviewAfter" :disabled="Boolean(busy) || disabled" placeholder="2026-11-12T08:00:00+08:00" /></label>
      <div class="proposal-actions">
        <a-button v-if="canDispose && proposal.candidates?.some(c => c.baseline)" :disabled="Boolean(busy) || disabled" @click="dispose('ACCEPT_NO_ACTION')">采纳不干预</a-button>
        <a-button v-if="canDispose" :disabled="Boolean(busy) || disabled" @click="dispose('DEFER')">暂缓</a-button>
        <a-button v-if="canDispose" :disabled="Boolean(busy) || disabled" @click="dispose('ABANDON')">放弃</a-button>
        <a-button v-if="canReopen" :disabled="Boolean(busy) || disabled" @click="dispose('REOPEN')">重新打开</a-button>
      </div>
    </details>

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

    <div class="proposal-section">
      <strong>1. 查看候选与证据，选择需要准备的方案</strong>
      <article v-for="candidate in proposal.candidates || []" :key="candidate.id" class="candidate-card" :class="{ selected: selectedId === candidate.id }">
        <label class="candidate-heading">
          <input v-model="selectedId" type="radio" :value="candidate.id" :disabled="busy || disabled || locked || candidate.baseline || !fresh || draftDirty" />
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

    <details v-if="(selected && !selected.baseline && !locked) || draftDirty" class="proposal-section">
      <summary>2. 核对所选方案的执行绑定与参数（{{ draft.candidateId || '未选择' }}）</summary>
      <p class="muted">需要按测算结果执行时，先核对绑定及数量映射。修改会作废计算结果、排名和旧预览，但保留测算输入便于重新核验；不会直接执行。</p>
      <a-alert v-if="draftDirty" type="info" show-icon message="有未保存补充，已暂停切换候选、测算和预览提交。后台刷新不会覆盖草稿；放弃草稿后才能切换。" />
      <a-alert v-if="draft.revision !== proposal.revision" type="warning" show-icon message="提案版本已变化，草稿不会自动覆盖新版本。请放弃草稿并核对最新内容后重新补充。" />
      <fieldset class="amend-fields" :disabled="!canEditBinding || Boolean(busy)">
        <label>绑定类型
          <select v-model="draft.type"><option value="ACTION">ACTION · 业务动作</option><option value="WORKFLOW">WORKFLOW · 工作流</option></select>
        </label>
        <label>目标 ID（必填，字符串）<input v-model="draft.targetId" type="text" placeholder="填写动作或工作流 ID" /></label>
        <label>对象主键（更新/删除必填，其他按定义校验）<input v-model="draft.objectKey" type="text" placeholder="明确填写业务对象主键；不从问题自动提取" /></label>
        <label>输入参数（JSON 对象）<textarea v-model="draft.inputParams" rows="6" spellcheck="false" placeholder="{}" /></label>
      </fieldset>
      <div class="proposal-actions">
        <a-button v-hasPermi="['ont:action:edit']" :loading="busy === 'amend'" :disabled="!canAmend" @click="amend">确认保存补充</a-button>
        <a-button :disabled="Boolean(busy) || disabled" @click="discardDraft">放弃草稿并载入当前候选建议</a-button>
      </div>
    </details>

    <section class="proposal-section">
      <strong>3. 测算比较（可选）</strong>
      <p class="muted">可不绑定动作先比较；按测算方案执行前仍必须完成绑定、数量映射并重新测算。未测算不代表约束已通过。</p>
      <p v-if="proposal.comparison" class="muted">当前比较结论：{{ proposal.comparison.summary }}；条件性推荐：{{ proposal.comparison.recommendedIds?.join('、') || '无' }}。</p>
      <p v-if="selected?.calculationSpec && !selected?.calculation" class="warning-text">此方案保留了旧测算输入，但结果已作废。请重新测算后再准备执行。</p>
      <DecisionCalculation :proposal="proposal" :context-key="contextKey" :disabled="disabled || Boolean(busy) || !fresh || locked || draftDirty" @updated="applyCalculation" />
    </section>

    <DecisionEffectPlan :proposal="proposal" :context-key="contextKey" :disabled="disabled || Boolean(busy) || !fresh || locked || draftDirty" @updated="applyCalculation" />

    <section v-if="proposal.previewDigest && proposal.binding" class="proposal-section preview-section">
      <strong>4. 核对执行影响与参数预览</strong>
      <p class="muted">匹配数是当前只读观测，不是实际写入数，不锁定对象集合。插入只表示计划；函数、回调和工作流等未知副作用不会试跑。</p>
      <p>已准备候选：{{ proposal.selectedCandidateId || '未知' }} · 绑定：{{ proposal.binding.name || proposal.binding.targetId }}（{{ proposal.binding.type }}）</p>
      <p>对象：{{ proposal.binding.objectKey || proposal.objectKey || '未提供' }} · 定义版本：{{ proposal.binding.version || '未提供' }}</p>
      <p>审批：{{ proposal.binding.requiresApproval === true ? '需要审批，提交不等于执行成功' : proposal.binding.requiresApproval === false ? '按服务端既有策略执行' : '未知，以服务端审批策略为准' }}</p>
      <details><summary>只读动作参数与摘要</summary><pre>{{ formatValue(proposal.binding.inputParams || {}) }}</pre><p class="digest">预览摘要：{{ proposal.previewDigest }}</p><p class="digest">定义摘要：{{ proposal.definitionDigest || proposal.binding.definitionDigest || '未提供' }}</p></details>
      <p>预览有效期：{{ proposal.previewExpiresAt || proposal.expiresAt || '未提供（由服务端校验）' }}</p>
      <pre v-if="proposal.impactPreview">{{ formatValue(proposal.impactPreview) }}</pre>
      <p v-if="selectedId !== proposal.selectedCandidateId" class="warning-text">当前选择与预览不一致，请重新准备预览，不能直接确认。</p>
    </section>

    <section class="proposal-section">
      <strong>5. 审批执行与效果状态</strong>
      <p>执行状态：{{ proposal.executionStatus || '尚未返回' }} · 提案状态：{{ proposal.status || 'UNKNOWN' }}</p>
      <a-alert v-if="approvalPending" type="warning" show-icon message="WAITING_APPROVAL：已进入既有审批流程，尚未完成业务执行。" />
      <a-alert v-if="reconciliationRequired" type="warning" show-icon message="RECONCILIATION_REQUIRED：执行结果待核对；不要重复提交，请刷新并联系业务人员核对。" />
      <p v-if="proposal.status === 'SUBMITTED' || proposal.executionStatus === 'SUBMITTED'" class="warning-text">SUBMITTED 仅表示已提交，不代表执行成功或指标改善。</p>
      <p v-if="proposal.executionId || proposal.runId">执行 ID：{{ proposal.executionId || '无' }} · 运行 ID：{{ proposal.runId || '无' }}</p>
      <p v-if="proposal.resumeMessage">续跑说明：{{ proposal.resumeMessage }}</p>
      <p v-if="resumeAvailable" class="muted">续跑仅推进服务端确认已执行（EXECUTED）的审批节点，不重试 FAILED 节点，不代表执行成功或实际改善。</p>
      <details v-if="proposal.executionDetails"><summary>只读执行详情（服务端有限脱敏结果）</summary><pre>{{ formatValue(proposal.executionDetails) }}</pre></details>
      <p>效果复查：{{ reviewUnknown ? 'UNKNOWN' : (proposal.effectStatus || 'UNKNOWN') }} · {{ reviewNotice || proposal.effectMessage || '尚未进行真实只读复查，实际效果未知。' }}</p>
      <details v-if="proposal.effectObservation"><summary>结构化效果观测（非因果归因）</summary><pre>{{ formatValue(proposal.effectObservation) }}</pre></details>
      <a-button v-if="approvalPending" type="link" @click="openApproval">前往原审批/执行记录</a-button>
      <p class="muted">刷新、关闭页面或 SSE 断连不会撤销已提交的业务执行。效果复查只查询真实数据，不自动声称改善。</p>
    </section>
    <div class="proposal-actions">
      <a-button :disabled="Boolean(busy) || disabled || !fresh" @click="emit('reanalyze', proposal)">复制原目标重新分析</a-button>
      <a-button :loading="busy === 'load'" :disabled="Boolean(busy) || disabled" @click="loadLatest">重新加载提案</a-button>
      <a-button v-hasPermi="['ont:action:edit']" :loading="busy === 'prepare'" :disabled="!canPrepare" @click="prepare">准备执行预览</a-button>
      <a-button v-hasPermi="['ont:action:edit']" type="primary" :loading="busy === 'confirm'" :disabled="!canConfirm" @click="confirm">明确确认并提交</a-button>
      <a-button v-if="resumeAvailable" v-hasPermi="['ont:action:edit']" :loading="busy === 'resume'" :disabled="!canResume" @click="resume">明确确认续跑工作流</a-button>
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
import DecisionEffectPlan from './DecisionEffectPlan.vue'
import { useAnalysisNavigation } from '@/utils/analysisNavigation'
import { disposeDecision, amendDecision, confirmDecision, getDecision, getDecisionStatus, prepareDecision, resumeDecision, reviewDecision } from '@/api/ai/decision'

const props = defineProps({
  decisionId: { type: String, required: true },
  snapshot: { type: Object, default: () => ({}) },
  contextKey: { type: String, required: true },
  ontologyId: { type: [String, Number], default: null },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['updated', 'reanalyze'])
const { openApproval: navigateApproval } = useAnalysisNavigation()
const dispositionReason = ref('')
const reviewAfter = ref('')
const proposal = ref({})
const selectedId = ref('')
const busy = ref('')
const fresh = ref(false)
const errorText = ref('')
const reviewNotice = ref('')
const reviewUnknown = ref(false)
const draft = ref({ candidateId: '', revision: null, type: 'ACTION', targetId: '', objectKey: '', inputParams: '{}' })
const draftInitial = ref('')
const draftDirty = computed(() => Boolean(draftInitial.value) && JSON.stringify(draft.value) !== draftInitial.value)
let generation = 0
let disposed = false
let statusTimer = null
let statusChecks = 0
function scheduleStatusRead() {
  clearTimeout(statusTimer)
  if (disposed || props.disabled || document.hidden || statusChecks >= 6 ||
      !['SUBMITTED', 'RUNNING', 'WAITING_APPROVAL'].includes(proposal.value.status)) return
  statusTimer = setTimeout(async () => {
    if (disposed || props.disabled || busy.value || document.hidden) return
    ++statusChecks
    await refreshStatus()
  }, 15000)
}
const canExecute = computed(() => checkPermi(['ont:action:edit']))
const approvalPending = computed(() => [proposal.value.status, proposal.value.executionStatus].some(value => ['WAITING_APPROVAL', 'PENDING_APPROVAL'].includes(value)))
const reconciliationRequired = computed(() => [proposal.value.status, proposal.value.executionStatus].includes('RECONCILIATION_REQUIRED'))
const locked = computed(() => Boolean(proposal.value.executionId || proposal.value.runId || proposal.value.executionStatus ||
  !['PROPOSED', 'PREVIEW_READY'].includes(proposal.value.status)))
const selected = computed(() => (proposal.value.candidates || []).find(item => item.id === selectedId.value))
const canEditBinding = computed(() => !props.disabled && fresh.value && canExecute.value && Boolean(props.ontologyId) &&
  !locked.value && Boolean(selected.value) && !selected.value.baseline && draft.value.candidateId === selectedId.value && draft.value.revision === proposal.value.revision)
const canAmend = computed(() => canEditBinding.value && !busy.value)
const resumeAvailable = computed(() => isResumable(proposal.value))
const canResume = computed(() => resumeAvailable.value && fresh.value && canExecute.value && !props.disabled && !busy.value && !draftDirty.value)
const canPrepare = computed(() => !busy.value && !draftDirty.value && !props.disabled && fresh.value && canExecute.value && Boolean(props.ontologyId) &&
  !locked.value && Boolean(selected.value) && !selected.value.baseline && !hasFailedConstraint(selected.value) &&
  !(selected.value.calculationSpec && !selected.value.calculation))
const canConfirm = computed(() => canPrepare.value && proposal.value.status === 'PREVIEW_READY' && Boolean(proposal.value.binding && proposal.value.previewDigest && proposal.value.impactPreview) && proposal.value.selectedCandidateId === selectedId.value)

const canDispose = computed(() => fresh.value && !proposal.value.executionStatus && ['PROPOSED', 'PREVIEW_READY', 'NEED_INPUT', 'DEFERRED'].includes(proposal.value.status))
const canReopen = computed(() => fresh.value && !proposal.value.executionStatus && ['DEFERRED', 'ABANDONED', 'ACCEPTED_NO_ACTION'].includes(proposal.value.status))
function openApproval() {
  return navigateApproval({ ontologyId: proposal.value.ontologyId, decisionId: props.decisionId, executionId: proposal.value.executionId, runId: proposal.value.runId })
}
async function dispose(disposition) {
  if (props.disabled || busy.value || !(canDispose.value || canReopen.value)) return
  if (!dispositionReason.value.trim()) { errorText.value = '请填写处置原因'; return }
  const time = reviewAfter.value ? Date.parse(reviewAfter.value) : null
  if (reviewAfter.value && !Number.isFinite(time)) { errorText.value = '复查时间须为有效ISO时间'; return }
  const token = context(); const revision = proposal.value.revision
  busy.value = 'disposition'
  try {
    await ElMessageBox.confirm('此操作只记录人工决策，不提交或取消任何业务动作。是否确认？', '记录决策处置', { type: 'warning' })
    if (!isCurrent(token) || props.disabled) return
    const data = (await disposeDecision({ decisionId: token.id, revision, disposition, reason: dispositionReason.value.trim(), reviewAfter: time == null ? null : new Date(time).toISOString() })).data
    if (!isCurrent(token)) return
    dispositionReason.value = data.dispositionReason || ''
    reviewAfter.value = data.reviewAfter || ''
    draftInitial.value = ''
    applyProposal(data, token)
    if (isCurrent(token)) discardDraft()
  } catch (failure) { if (isCurrent(token) && failure !== 'cancel' && failure !== 'close') errorText.value = failure?.message || '处置失败，请刷新' }
  finally { if (isCurrent(token)) busy.value = '' }
}
function discardDraft() {
  const candidate = selected.value
  // 只允许同一候选复用已确认绑定；绝不把其他候选的 proposal.binding 带入草稿。
  const binding = candidate?.suggestedBinding || (candidate && proposal.value.selectedCandidateId === candidate.id ? proposal.value.binding : null) || {}
  draft.value = {
    candidateId: candidate?.id || '',
    revision: proposal.value.revision,
    type: binding.type === 'WORKFLOW' ? 'WORKFLOW' : 'ACTION',
    targetId: binding.targetId == null ? '' : String(binding.targetId),
    objectKey: String(binding.objectKey ?? proposal.value.objectKey ?? ''),
    inputParams: binding.inputParams == null ? '{}' : formatValue(binding.inputParams)
  }
  draftInitial.value = JSON.stringify(draft.value)
}
function isResumable(data) {
  return data?.resumeAllowed === true && data.binding?.type === 'WORKFLOW' &&
    typeof data.runId === 'string' && Boolean(data.runId.trim()) &&
    typeof data.resumeToken === 'string' && Boolean(data.resumeToken.trim()) &&
    ![data.status, data.executionStatus].includes('FAILED')
}
function resumeAllowedNow() {
  return fresh.value && !props.disabled && canExecute.value && !draftDirty.value && isResumable(proposal.value)
}
async function amend() {
  if (!canAmend.value || isExpired()) return
  const token = context()
  const draftSnapshot = JSON.stringify(draft.value)
  busy.value = 'amend'
  try {
    const params = JSON.parse(draft.value.inputParams)
    if (!params || typeof params !== 'object' || Array.isArray(params)) throw new Error('输入参数必须是 JSON 对象，不能为数组、null 或其他类型。')
    const targetId = draft.value.targetId.trim()
    const objectKey = draft.value.objectKey.trim()
    if (!['ACTION', 'WORKFLOW'].includes(draft.value.type) || !targetId) throw new Error('请选择绑定类型并明确填写目标 ID；更新/删除类动作还必须填写对象主键。')
    const request = { decisionId: token.id, revision: draft.value.revision, candidateId: draft.value.candidateId, type: draft.value.type, targetId, objectKey, inputParams: JSON.stringify(params) }
    await ElMessageBox.confirm('确认保存当前候选的补充信息？旧测算结果、排名和执行预览会作废，输入条件保留供重新核验；之后需重新测算（如需要）、准备预览并明确确认，此步骤不会提交执行。', '确认保存决策补充', { type: 'warning', confirmButtonText: '保存补充', cancelButtonText: '取消', closeOnClickModal: false })
    if (!isCurrent(token) || !canEditBinding.value || isExpired() || JSON.stringify(draft.value) !== draftSnapshot) return
    const applied = applyProposal((await amendDecision(request)).data, token)
    if (applied) {
      // 保存成功才丢弃本地草稿；失败或后台刷新均不得悄悄覆盖用户输入。
      selectedId.value = request.candidateId
      discardDraft()
      reviewNotice.value = ''
      reviewUnknown.value = false
    }
  } catch (error) {
    if (isCurrent(token) && error !== 'cancel' && error !== 'close') {
      fresh.value = false
      errorText.value = error instanceof SyntaxError ? '输入参数不是有效的 JSON 对象，请重新加载提案后修正草稿。' : (error?.message || '保存失败，请重新加载最新提案并核对；旧测算和预览不能作为执行依据。')
    }
  } finally {
    if (isCurrent(token)) busy.value = ''
  }
}
async function resume() {
  if (!canResume.value || !resumeAllowedNow()) return
  const token = context()
  const request = { decisionId: token.id, revision: proposal.value.revision, runId: proposal.value.runId, resumeToken: proposal.value.resumeToken }
  busy.value = 'resume'
  let requested = false
  try {
    await ElMessageBox.confirm('确认继续当前工作流？服务端仅推进审批节点已 EXECUTED 的运行，不重试 FAILED 节点。可能继续进入下一审批或执行步骤，续跑不代表成功或实际改善。', '明确确认续跑工作流', { type: 'warning', confirmButtonText: '确认续跑', cancelButtonText: '取消', closeOnClickModal: false })
    if (!isCurrent(token) || !resumeAllowedNow()) return
    const latest = (await getDecision(token.id)).data
    if (!isCurrent(token)) return
    const unchanged = latest?.revision === request.revision && latest?.runId === request.runId && latest?.resumeToken === request.resumeToken
    applyProposal(latest, token)
    if (!unchanged || !resumeAllowedNow()) {
      errorText.value = '工作流版本、运行或审批节点已变化，未续跑。请核对最新状态后重新明确确认。'
      return
    }
    requested = true
    applyProposal((await resumeDecision(request)).data, token)
  } catch (error) {
    if (isCurrent(token) && error !== 'cancel' && error !== 'close') {
      fresh.value = false
      errorText.value = requested ? '续跑结果未知，请刷新运行状态核对，勿重复续跑；断连不表示撤销执行。' : (error?.message || '续跑前校验失败，未发送续跑，请重新加载。')
    }
  } finally {
    if (isCurrent(token)) busy.value = ''
  }
}
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
  const preserveDraft = draftDirty.value
  proposal.value = data
  if (!preserveDraft) {
    selectedId.value = data.selectedCandidateId || ((data.candidates || []).some(item => item.id === selectedId.value) ? selectedId.value : '')
    discardDraft()
  }
  fresh.value = true
  errorText.value = ''
  emit('updated', data)
  scheduleStatusRead()
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
  if (!fresh.value || draftDirty.value || props.disabled || !canExecute.value || !props.ontologyId || locked.value || !selected.value || selected.value.baseline || hasFailedConstraint(selected.value)) return false
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
    // Preparation has no business side effect; reserve the confirmation dialog for actual submission.
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
  const request = { decisionId: props.decisionId, revision: proposal.value.revision, candidateId: selectedId.value, previewDigest: proposal.value.previewDigest, acknowledgeUnknownImpact: false }
  busy.value = 'confirm'
  let requested = false
  try {
    await ElMessageBox.confirm('确认按上方服务端预览提交业务操作？提交后可能进入审批或执行；提交成功不等于执行成功，关闭页面不能撤销。请核对对象、只读参数和风险。', '明确确认提交决策', { type: 'warning', confirmButtonText: '确认并提交', cancelButtonText: '取消', closeOnClickModal: false })
    if (!isCurrent(token) || !mutationAllowed()) return
    if (proposal.value.impactPreview?.status !== 'AVAILABLE' || proposal.value.impactPreview?.truncated === true) {
      await ElMessageBox.confirm('此类型或来源无法安全预测实际副作用，或匹配数超过上限而仅能显示下界。服务端不会试跑函数、回调或工作流；你是否明确知悉这些影响边界，仍按既有审批策略提交？', '单独知悉影响边界', { type: 'warning', confirmButtonText: '知悉未知影响并继续', cancelButtonText: '取消', closeOnClickModal: false })
      if (!isCurrent(token) || !mutationAllowed()) return
      request.acknowledgeUnknownImpact = true
    }
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
watch(selectedId, () => {
  if (!draftDirty.value) discardDraft()
}, { flush: 'sync' })
watch(() => [props.decisionId, props.contextKey], () => {
  ++generation
  clearTimeout(statusTimer); statusChecks = 0
  draftInitial.value = ''
  busy.value = ''
  fresh.value = false
  errorText.value = ''
  reviewNotice.value = ''
  reviewUnknown.value = false
  proposal.value = props.snapshot || {}
  selectedId.value = ''
  discardDraft()
  loadLatest()
}, { immediate: true, flush: 'sync' })
watch(() => props.disabled, (disabled) => {
  if (disabled) clearTimeout(statusTimer)
  if (!disabled && !fresh.value && !busy.value) loadLatest()
  else if (!disabled) scheduleStatusRead()
})
onBeforeUnmount(() => { disposed = true; ++generation; clearTimeout(statusTimer) })
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
.amend-fields { display: grid; gap: 12px; margin-top: 12px; padding: 0; border: 0; min-width: 0; }
.amend-fields label { display: grid; gap: 6px; }
.amend-fields input, .amend-fields select, .amend-fields textarea { width: 100%; box-sizing: border-box; padding: 8px; border: 1px solid #d9dfe8; border-radius: 4px; font: inherit; }
.amend-fields textarea { resize: vertical; font-family: monospace; }
.digest { font-size: 12px; word-break: break-all; }
summary { cursor: pointer; }
</style>
