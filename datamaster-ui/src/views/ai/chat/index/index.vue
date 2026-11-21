<template>
  <div class="app-container ask-data-page">
    <aside class="conversation-panel">
      <div class="ai-local-nav">
        <button type="button" class="ai-local-nav-item is-active">
          <span class="ai-local-nav-dot"></span>
          数据分析
        </button>
        <button type="button" class="ai-local-nav-item" @click="openSkillManagement">
          <span class="ai-local-nav-dot skill"></span>
          Skill 管理
        </button>
      </div>
      <div class="panel-title">
        <strong>工作配置</strong>
        <span>问答与报告参数</span>
      </div>
      <div class="chat-options">
        <div class="chat-option-group ontology-option">
          <span class="chat-option-label">业务本体</span>
          <a-select v-model:value="form.ontologyId" :options="ontologyOptions" :loading="ontologyLoading" show-search option-filter-prop="label" placeholder="请选择业务本体" :disabled="sending || ontologyLoading || syncingSession" />
          <span class="chat-option-hint">选择本体后，自动关联指标、绑定数据源及可用 Skill。</span>
        </div>
        <div class="ontology-context" :class="{ 'has-error': ontologyContextError }">
          <a-spin v-if="ontologyContextLoading" size="small" tip="正在解析本体绑定…" />
          <span v-else-if="ontologyContextError" class="chat-option-hint">{{ ontologyContextError }}</span>
          <template v-else-if="form.ontologyId">
            <div class="ontology-context-title">本体关联指标</div>
            <strong>{{ selectedOntologyName }}</strong>
            <span class="chat-option-hint">{{ ontologyMetrics.length ? `已关联 ${ontologyMetrics.length} 个已发布指标，问数时自动按问题匹配。` : '暂无已发布指标；可使用绑定数据源开展探索分析。' }}</span>
            <details v-if="ontologyMetrics.length" class="ontology-metric-list">
              <summary>查看关联指标</summary>
              <ul><li v-for="metric in ontologyMetrics" :key="metric.id">{{ metric.name }} · V{{ metric.publishedVersion }}</li></ul>
            </details>
          </template>
          <span v-else class="chat-option-hint">请先选择本体，加载对应的分析上下文。</span>
        </div>
        <div v-if="datasourceList.length > 1" class="chat-option-group">
          <span class="chat-option-label">本体绑定数据源</span>
          <a-select v-model:value="form.datasourceId" :disabled="sending || ontologyContextLoading" @change="handleDatasourceChange">
            <a-select-option v-for="item in datasourceList" :key="item.id" :value="item.id">{{ item.datasourceName }}</a-select-option>
          </a-select>
          <span class="chat-option-hint">已默认选中第一个绑定项；探索查询、Skill 和模板使用此项，正式指标仍按发布来源查询。</span>
        </div>
        <div v-else-if="selectedDatasource" class="bound-datasource-summary">
          <span>自动解析的数据源</span>
          <strong>{{ selectedDatasource.datasourceName }}</strong>
        </div>
        <span v-else-if="form.ontologyId && !ontologyContextLoading && !ontologyContextError" class="chat-option-hint">本体尚未绑定可用数据源；正式指标按其已批准的发布来源查询。</span>
        <div class="chat-option-group">
          <span class="chat-option-label">Skill（可选）</span>
          <a-select
            v-model:value="form.skillId"
            :disabled="sending || ontologyContextLoading || !form.datasourceId"
            :loading="skillLoading"
            :not-found-content="skillLoading ? '加载中…' : (skillLoadError || '没有已发布的关联 Skill')"
            placeholder="自动匹配 Skill（可切换）"
            show-search
            allow-clear
            @change="handleSkillChange"
          >
            <a-select-option v-for="item in availableSkillList" :key="item.id" :value="item.id">
              {{ item.skillName }}
            </a-select-option>
          </a-select>
        </div>
        <div class="chat-option-group">
          <a-checkbox v-model:checked="form.returnSql" :disabled="sending">返回 SQL</a-checkbox>
        </div>
        <div class="chat-option-group">
          <span class="chat-option-label">对象主键（可选）</span>
          <a-input v-model:value="form.objectKey" :disabled="sending" allow-clear placeholder="请明确填写业务对象主键（不从问题自动提取）" />
          <span class="chat-option-hint">用于绑定具体业务对象，提交前以服务端预览为准；未选本体时请先选择本体。</span>
        </div>
        <div class="chat-option-group">
          <span class="chat-option-label">报告模板（可选）</span>
          <a-select v-model:value="form.templateId" placeholder="自动匹配报告模板" :disabled="sending || ontologyContextLoading || !form.skillId" :loading="templateLoading" show-search allow-clear>
            <a-select-option v-for="item in templateList" :key="item.id" :value="item.id">
              {{ templateOptionLabel(item) }}
            </a-select-option>
          </a-select>
          <a-button block :icon="h(FileTextOutlined)" @click="openTemplateFormat">模板格式</a-button>
        </div>
      </div>
    </aside>

    <main class="chat-shell">
      <AnalysisFlowNavigation stage="decision" :ontology-id="form.ontologyId" :disabled="sending" />
      <a-alert v-if="navigationError" type="warning" show-icon :message="navigationError" />
      <a-alert v-if="manualDecisionNotice" type="info" show-icon :message="manualDecisionNotice">
        <template #action><a-button type="link" @click="returnToMetricAnalysis">返回指标分析</a-button></template>
      </a-alert>
      <section v-if="linkedDecision" class="linked-decision">
        <strong>已返回原决策任务</strong>
        <a-button type="link" @click="linkedDecision = null">收起任务</a-button>
        <DecisionProposal :key="`${decisionContextKey}:linked:${linkedDecision.id}`" :decision-id="linkedDecision.id" :snapshot="linkedDecision" :context-key="`${decisionContextKey}:linked`" :ontology-id="linkedDecision.ontologyId" :disabled="sending" @updated="linkedDecision = $event" @reanalyze="reanalyzeDecision" />
      </section>
      <DecisionTaskList :space-id="userStore.spaceId" :space-code="userStore.spaceCode" :context-key="decisionContextKey" :disabled="sending" @reanalyze="reanalyzeDecision" />
      <section ref="messageScrollRef" class="message-list" @scroll="handleMessageScroll">
        <div v-if="messageWindow.hasBefore" class="history-loader">
          <a-button type="text" :loading="messageWindow.loadingBefore" @click="loadMoreMessages('before')">
            加载更早5条
          </a-button>
        </div>
        <div v-if="activeMessages.length === 0" class="empty-state">
          <h1>想查什么，直接问</h1>
          <p>选择业务本体即可开始；适用指标、Skill 与普通问数会自动组合，报告和方案只在需要时生成。影响估算不等于实际改善，动作仍需原有预览与审批。</p>
          <div class="examples">
            <button v-for="item in examples" :key="item" @click="prompt = item">
              {{ item }}
            </button>
          </div>
        </div>

        <div
          v-for="message in activeMessages"
          :key="message.id"
          class="message-row"
          :class="[message.role, { 'printing-report': printingMessageId === message.id }]"
        >
          <div class="message-bubble" :class="{ 'html-report-bubble': isFullHtmlMessage(message) }">
            <div class="message-actions">
              <a-tooltip v-if="isExportableReportMessage(message)" title="导出PDF">
                <a-button type="text" :icon="h(FileTextOutlined)" @click.stop="exportMessagePdf(message)" />
              </a-tooltip>
              <a-tooltip v-if="message.role === 'user'" title="编辑">
                <a-button type="text" :icon="h(EditOutlined)" @click.stop="editMessage(message)" />
              </a-tooltip>
              <a-tooltip title="复制">
                <a-button type="text" :icon="h(CopyOutlined)" @click.stop="copyMessage(message)" />
              </a-tooltip>
              <a-tooltip title="删除">
                <a-button type="text" danger :icon="h(DeleteOutlined)" @click.stop="removeMessage(message)" />
              </a-tooltip>
            </div>
            <div v-if="message.agentSteps" class="agent-fold">
              <details :open="sending && message === activeAssistantMessage">
                <summary>执行进度</summary>
                <pre>{{ message.agentSteps }}</pre>
              </details>
            </div>
            <div v-if="message.returnedSql || message.sqlUnavailable" class="sql-fold">
              <details :open="Boolean(message.returnedSql)">
                <summary>SQL</summary>
                <pre v-if="message.returnedSql">{{ message.returnedSql }}</pre>
                <p v-else>本次未返回可展示 SQL。</p>
              </details>
            </div>
            <div v-if="message.steps?.length" class="step-list">
              <div
                v-for="(step, index) in message.steps"
                :key="step.key"
                class="step-item"
                :class="step.status"
              >
                <span class="step-dot">
                  <template v-if="step.status === 'done'">✓</template>
                  <template v-else>{{ index + 1 }}</template>
                </span>
                <span>{{ step.label }}</span>
              </div>
            </div>
            <div v-if="message.decisionId" class="decision-analysis-banner">
              <strong>决策分析</strong>
              <span>默认只分析，预览确认后按已有审批执行</span>
            </div>
            <DecisionProposal
              v-if="message.decisionId"
              :key="`${decisionContextKey}:${message.id}:${message.decisionId}`"
              :decision-id="message.decisionId"
              :snapshot="message.decision"
              :context-key="decisionContextKey"
              :ontology-id="form.ontologyId"
              :disabled="sending"
              @updated="updateDecisionMessage(message, $event)"
              @reanalyze="reanalyzeDecision"
            />
            <a-button v-if="!message.decisionId && ['SUCCEEDED', 'PARTIAL'].includes(message.agentOperationStatus) && message.queryExecuted && message.inputBinding" :disabled="sending" size="small" @click="decisionFromAnalysis(message)">基于本次正式分析制定方案</a-button>
            <a-alert v-if="message.inputContinuation?.operationId" type="info" show-icon message="当前任务需要补充条件；可沿用原目标补充，不会沿用任何执行确认。">
              <template #action><a-button :disabled="sending" size="small" @click="selectInputContinuation(message)">补充本次条件</a-button></template>
            </a-alert>
            <MarkdownView
              v-if="(message.unifiedResult || (!message.reportTemplate && !message.reportData)) && (message.displayContent || message.content)"
              class="message-content"
              safe-markdown-only
              :content="message.displayContent || message.content"
            />
            <a-alert v-if="message.analysisNotice" type="info" show-icon :message="message.analysisNotice" style="margin-bottom: 12px" />
            <a-alert v-if="message.agentOperationStatus === 'NEED_CONFIRMATION'" type="warning" show-icon message="待确认保存分析条件" description="确认的是本条消息对应的服务端名称、指标版本、维度、过滤和时间条件，不保存查询结果。">
              <template #action><a-button type="primary" size="small" :loading="message.agentConfirming" :disabled="sending || !message.agentOperationId" @click="confirmMetricAnalysis(message)">确认保存</a-button></template>
            </a-alert>
            <details v-if="message.semanticAnalysisResults?.length" class="agent-fold">
              <summary>查看后端确定性分析结果</summary>
              <pre>{{ JSON.stringify(message.semanticAnalysisResults, null, 2) }}</pre>
            </details>
            <details v-if="message.agentOperationStatus === 'NEED_CONFIRMATION' && message.agentConfirmation?.arguments" class="agent-fold">
              <summary>查看待保存名称和完整查询条件</summary>
              <pre>{{ JSON.stringify(message.agentConfirmation.arguments, null, 2) }}</pre>
            </details>
            <ReportTemplateRenderer
              v-if="message.reportTemplate && message.reportData"
              :template="message.reportTemplate"
              :data="message.reportData"
            />
            <div v-for="(table, index) in message.queryTables || []" :key="index" class="data-table-wrap">
              <p>{{ table.name }}<template v-if="table.truncated">（结果已截断）</template></p>
              <a-table v-if="table.rows.length" :data-source="table.rows" :columns="buildTableColumns(table.columns)" :pagination="false" size="small" :scroll="{ y: 320 }" />
              <a-empty v-else description="该查询没有结果" />
            </div>
            <div v-if="!message.queryTables?.length && message.tableRows?.length" class="data-table-wrap">
              <a-table :data-source="message.tableRows" :columns="buildTableColumns(message.tableColumns)" :pagination="false" size="small" :scroll="{ y: 320 }" />
            </div>
            <div v-else-if="!message.queryTables?.length && message.queryExecuted" class="data-empty-wrap">
              <a-empty description="暂无查询结果" />
            </div>
            <div
              v-if="!message.decisionId && !message.reportTemplate && !message.reportData && !message.tableRows?.length && !message.displayContent && !message.content"
              class="typing"
            >
              <span></span>
              <span></span>
              <span></span>
              <em>正在分析...</em>
            </div>
          </div>
        </div>
        <div v-if="messageWindow.hasAfter" class="history-loader">
          <a-button type="text" :loading="messageWindow.loadingAfter" @click="loadMoreMessages('after')">
            加载更新5条
          </a-button>
        </div>
      </section>

      <footer class="composer-wrap">
        <a-alert v-if="inputContinuation" type="info" show-icon message="正在补充原任务条件；本体、数据源、对象或会话变化后请重新选择。">
          <template #action><a-button type="link" @click="inputContinuation = null">改为新问题</a-button></template>
        </a-alert>
        <div class="composer">
          <a-input
            ref="promptInputRef"
            v-model:value="prompt"
            type="textarea"
            :auto-size="{ minRows: 1, maxRows: 6 }"
            placeholder="输入你的数据问题"
            @keydown="handlePromptKeydown"
          />
          <div class="composer-actions">
            <a-button
              v-if="sending"
              danger
              :icon="h(StopOutlined)"
              class="send-btn stop-btn"
              @click="stopGeneration"
            >
              停止
            </a-button>
            <a-button
              v-else
              type="primary"
              :icon="h(SendOutlined)"
              class="send-btn"
              @click="sendMessage"
            />
            <a-button class="clear-btn" :disabled="!activeMessages.length" @click="clearMessages">
              清空
            </a-button>
          </div>
        </div>
        <div class="composer-tip">Enter 发送，Shift+Enter 换行</div>
      </footer>
    </main>

    <aside class="conversation-history-panel">
      <div class="history-panel-title">
        <div>
          <strong>聊天记录</strong>
          <span>切换或管理历史对话</span>
        </div>
        <a-button type="default" :icon="h(PlusOutlined)" class="new-chat" @click="createConversation">
          新建对话
        </a-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="item in conversations"
          :key="item.id"
          class="conversation-row"
          :class="{ active: item.id === activeConversationId }"
        >
          <button class="conversation-item" @click="selectConversation(item.id)">
            <span>{{ item.title }}</span>
            <small>{{ item.datasourceName || '未选择数据源' }}</small>
          </button>
          <a-button type="text" class="conversation-delete" @click.stop="removeConversation(item.id)">删除</a-button>
        </div>
      </div>
    </aside>

    <a-modal v-model:open="templateFormatOpen" title="报告模板格式" width="960px" destroy-on-close>
      <div class="template-format-header">
        <div>
          <strong>{{ selectedSkill?.skillName || '通用报告模板' }}</strong>
          <p>复制后按报告业务修改模板编码、字段、布局、样式和提示语，再到 Skill 模板中上传。</p>
        </div>
        <a-button type="primary" :icon="h(CopyOutlined)" @click="copyTemplateFormat">复制模板</a-button>
      </div>
      <a-input
        v-model:value="templateFormatText"
        type="textarea"
        :rows="26"
        readonly
      />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, h, inject, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CopyOutlined, DeleteOutlined, EditOutlined, FileTextOutlined, PlusOutlined, SendOutlined, StopOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import MarkdownView from '@/components/MarkdownView/index.vue'
import ReportTemplateRenderer from './components/ReportTemplateRenderer.vue'
import DecisionProposal from './components/DecisionProposal.vue'
import AnalysisFlowNavigation from '@/components/AnalysisFlowNavigation.vue'
import { getDecision } from '@/api/ai/decision'
import { positiveRouteId, decisionRouteId, routeText, takeMetricDecisionDraft, useAnalysisNavigation } from '@/utils/analysisNavigation'
import DecisionTaskList from './components/DecisionTaskList.vue'
import { listOntology } from '@/api/ont/ontology'
import { loadPagedOptions } from '@/api/ont/metricOptions'
import { normalizeAgentText } from '@/utils/agentText'
import { listSkill, listSkillReportTemplates } from '@/api/ai/skill'
import {
  appendAskMessage,
  clearAskMessages,
  createAskSession,
  deleteAskMessage,
  deleteAskSession,
  listAskMessages,
  listAskSessions,
  updateAskSession
} from '@/api/ai/askSession'
import useUserStore from '@/store/system/user'
import { buildReportTemplateFormatText } from './reportTemplateFormat'

const datasourceList = ref([])
const ontologyOptions = ref([])
const ontologyLoading = ref(false)
const ontologyContextLoading = ref(false)
const ontologyContextError = ref('')
const ontologyMetrics = ref([])
const skillLoading = ref(false)
const skillLoadError = ref('')
const templateLoading = ref(false)
let ontologyContextVersion = 0
let askContextVersion = 0
let ontologyLoadVersion = 0
const skillList = ref([])
const templateList = ref([])
const conversations = ref([])
const activeConversationId = ref(null)
const prompt = ref('')
const promptInputRef = ref()
const promptHistoryIndex = ref(-1)
const promptHistoryDraft = ref('')
const sending = ref(false)
const autoScroll = ref(true)
const activeAbortController = ref(null)
const activeAssistantMessage = ref(null)
const messageScrollRef = ref()
const templateFormatOpen = ref(false)
const templateFormatText = ref('')
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const stationNavigation = inject('spaceWorkstationNavigation', null)
const analysisNavigation = useAnalysisNavigation()
const navigationError = ref('')
const manualDecisionNotice = ref('')
const manualDecisionReturn = ref(null)
const linkedDecision = ref(null)
let navigationReady = false
let navigationSequence = 0
let lastNavigationKey = ''
function navigationKey() { return JSON.stringify([userStore.spaceId, userStore.spaceCode, route.query.spaceId, route.query.ontologyId, route.query.decisionId, route.query.decisionDraft]) }
async function returnToMetricAnalysis() {
  const target = manualDecisionReturn.value
  if (target) await analysisNavigation.openMetricAnalysis(target)
}
async function applyNavigationTarget() {
  if (!navigationReady) return
  const key = navigationKey()
  if (key === lastNavigationKey) return
  const sequence = ++navigationSequence
  const space = String(userStore.spaceId)
  navigationError.value = ''; linkedDecision.value = null
  manualDecisionNotice.value = ''; manualDecisionReturn.value = null
  const requestedSpace = routeText(route.query.spaceId)
  if (requestedSpace && requestedSpace !== space) { navigationError.value = '此入口属于其他空间，请先切回原空间；不会自动切换空间或执行任务'; return }
  const requestedOntology = positiveRouteId(route.query.ontologyId)
  if (requestedOntology && !ontologyOptions.value.some(item => String(item.value) === requestedOntology)) {
    navigationError.value = '指定本体不属于当前可访问空间'; return
  }
  const taskId = decisionRouteId(route.query.decisionId)
  if (route.query.decisionId && !taskId) { navigationError.value = '决策任务链接格式无效'; return }
  if (taskId) {
    try {
      const task = (await getDecision(taskId)).data
      if (sequence !== navigationSequence || key !== navigationKey() || space !== String(userStore.spaceId)) return
      if (!task?.id || String(task.id) !== taskId || (requestedOntology && String(task.ontologyId) !== requestedOntology) ||
          !ontologyOptions.value.some(item => String(item.value) === String(task.ontologyId))) throw new Error('任务本体与链接或当前空间不一致')
      linkedDecision.value = task
      lastNavigationKey = key
    } catch (failure) { if (sequence === navigationSequence) navigationError.value = failure?.message || '原任务加载失败，请确认权限和原会话仍有效' }
    return
  }
  if (route.query.decisionDraft) {
    try {
      if (sending.value) throw new Error('当前会话正在生成，请停止后重新进入指标分析')
      const draft = takeMetricDecisionDraft(userStore, routeText(route.query.decisionDraft))
      if (!draft) throw new Error('分析跳转草稿已使用或不存在，请回到指标分析重新进入')
      if (String(draft.ontologyId) !== requestedOntology) throw new Error('分析草稿本体与链接不一致')
      lastNavigationKey = key
      // Use a fresh conversation; handoff is editable intent, never a tool-result or confirmation replay.
      await createConversation()
      if (sequence !== navigationSequence || key !== navigationKey() || space !== String(userStore.spaceId)) return
      form.ontologyId = requestedOntology; form.objectKey = ''
      manualDecisionReturn.value = { ontologyId: requestedOntology, metricId: draft.query.metricId, version: draft.query.version }
      manualDecisionNotice.value = `已带入「${draft.metricName || draft.query.metricId}」V${draft.query.version}的分析条件。请补充业务目标并发送；服务端将重新查询核验，未沿用页面结果或执行确认。`
      prompt.value = `请严格按以下正式指标版本和结构化条件重新查询，再根据结果提出处置候选、约束和风险；条件无法支持时先询问，不改用其他版本或探索查询。\n指标：${draft.metricName}\n分析条件：${JSON.stringify(draft.query)}\n业务目标：请补充需要解决的问题。`
      await syncActiveSessionSelection()
    } catch (failure) { if (sequence === navigationSequence) navigationError.value = failure?.message || '分析跳转草稿加载失败' }
  } else {
    if (requestedOntology && String(form.ontologyId || '') !== requestedOntology) {
      form.ontologyId = requestedOntology
      await syncActiveSessionSelection()
    }
    lastNavigationKey = key
  }
}
watch(() => navigationKey(), () => { if (navigationReady) applyNavigationTarget() })
const sessionReady = ref(false)
const syncingSession = ref(false)
let sessionSaveVersion = 0
let sessionSaveQueue = Promise.resolve()
let sessionRestoreVersion = 0
let conversationLoadVersion = 0
let skillLoadVersion = 0
let templateLoadVersion = 0
function invalidateAskContext() {
  ++askContextVersion; ++sessionSaveVersion
  activeAbortController.value?.abort(); activeAbortController.value = null
  sending.value = false; activeAssistantMessage.value = null
  inputContinuation.value = null; decisionAnalysisReference.value = null
}
const printingMessageId = ref(null)
const messageWindow = reactive({
  hasBefore: false,
  hasAfter: false,
  loadingBefore: false,
  loadingAfter: false
})

const form = reactive({
  ontologyId: null,
  objectKey: '',
  datasourceId: null,
  skillId: null,
  templateId: null,
  returnSql: false
})

const examples = computed(() => [
  '查询下前5条订单数据',
  '按城市统计订单金额',
  '最近7天订单数量趋势',
  '分析哪些货物比较好卖，并给出库存分配比例建议'
])

const activeConversation = computed(() =>
  conversations.value.find((item) => item.id === activeConversationId.value)
)

const activeMessages = computed(() => activeConversation.value?.messages || [])
const decisionRequestVersion = ref(0)
const inputContinuation = ref(null)
const decisionAnalysisReference = ref(null)
function reanalyzeDecision(proposal) {
  if (sending.value || !proposal?.ontologyId || !proposal?.goal) return
  form.ontologyId = String(proposal.ontologyId); form.objectKey = proposal.objectKey || ''
  inputContinuation.value = null; decisionAnalysisReference.value = null
  prompt.value = proposal.goal
  message.info('已载入原目标。请核对日期、对象与约束后发送；将重新查询并生成新提案，不沿用旧确认')
}
function decisionFromAnalysis(target) {
  if (sending.value || !target.inputBinding || String(target.inputBinding.sessionId) !== String(activeConversationId.value) ||
      String(target.inputBinding.ontologyId) !== String(form.ontologyId) || String(target.inputBinding.spaceId) !== String(userStore.spaceId)) {
    message.warning('请回到原会话、本体和空间后制定方案'); return
  }
  decisionAnalysisReference.value = { operationId: target.agentOperationId, ontologyId: form.ontologyId, sessionId: activeConversationId.value, spaceId: userStore.spaceId }
  prompt.value = '请基于该分析结果提出可执行的处置候选，说明约束与风险；重新核验正式指标，不直接执行。'
}

function continuationMatches(binding) {
  const sameId = (a, b) => String(a ?? '') === String(b ?? '')
  return binding && sameId(binding.sessionId, activeConversationId.value) &&
    sameId(binding.spaceId, userStore.spaceId) && sameId(binding.spaceCode, userStore.spaceCode) && sameId(binding.ontologyId, form.ontologyId) &&
    sameId(binding.datasourceId, form.datasourceId) && String(binding.objectKey || '') === form.objectKey.trim()
}
function selectInputContinuation(target) {
  if (sending.value || !continuationMatches(target.inputContinuation)) {
    message.warning('请回到原会话、本体、数据源和工作模式后补充；改变对象范围请发起新问题')
    return
  }
  inputContinuation.value = { ...target.inputContinuation }
  message.info('请在输入框填写缺少的日期、时区或业务约束，再发送')
}
watch(() => [userStore.spaceId, userStore.spaceCode, activeConversationId.value, form.ontologyId, form.datasourceId, form.objectKey], () => {
  if (inputContinuation.value && !continuationMatches(inputContinuation.value)) inputContinuation.value = null
}, { flush: 'sync' })
const decisionContextKey = computed(() => JSON.stringify([
  userStore.spaceId, userStore.spaceCode, activeConversationId.value, form.ontologyId, decisionRequestVersion.value
]))

const selectedOntologyName = computed(() =>
  ontologyOptions.value.find(item => String(item.value) === String(form.ontologyId))?.label || '当前本体'
)

const selectedDatasource = computed(() =>
  datasourceList.value.find((item) => String(item.id) === String(form.datasourceId))
)

const selectedSkill = computed(() =>
  skillList.value.find((item) => item.id === form.skillId)
)

// 问答、报告、决策统一使用当前数据源下的全部已发布 Skill；模式只影响调用协议，
// 不再按 Skill 类型做二次过滤。
const availableSkillList = computed(() => skillList.value)

onMounted(async () => {
  const initialSpace = JSON.stringify([userStore.spaceId, userStore.spaceCode])
  await loadOntologies()
  if (initialSpace !== JSON.stringify([userStore.spaceId, userStore.spaceCode])) return
  await loadConversations()
  if (initialSpace !== JSON.stringify([userStore.spaceId, userStore.spaceCode])) return
  navigationReady = true
  await applyNavigationTarget()
  const routePrompt = route.query?.prompt
  if (routePrompt && !route.query.decisionDraft && !route.query.decisionId) {
    prompt.value = Array.isArray(routePrompt) ? routePrompt[0] : String(routePrompt)
  }
})

watch(() => [form.datasourceId, form.skillId, form.templateId, form.returnSql], () => {
  syncActiveSessionSelection()
})

watch(() => form.datasourceId, () => {
  if (!syncingSession.value) invalidateAskContext()
}, { flush: 'sync' })
watch(() => form.ontologyId, () => {
  if (syncingSession.value) return
  invalidateAskContext()
  form.objectKey = ''
  loadOntologyContext().then(() => syncActiveSessionSelection())
}, { flush: 'sync' })

watch(() => [userStore.spaceId, userStore.spaceCode], async () => {
  navigationReady = false; ++navigationSequence; lastNavigationKey = ''
  linkedDecision.value = null; manualDecisionNotice.value = ''; manualDecisionReturn.value = null
  sessionReady.value = false; ++sessionRestoreVersion; ++conversationLoadVersion
  invalidateAskContext(); syncingSession.value = false
  activeConversationId.value = null; conversations.value = []
  form.ontologyId = null; form.objectKey = ''; form.datasourceId = null
  ++ontologyContextVersion; datasourceList.value = []; ontologyMetrics.value = []
  skillList.value = []; templateList.value = []; form.skillId = null; form.templateId = null
  ontologyContextLoading.value = false; ontologyContextError.value = ''
  const context = askContextVersion
  const space = JSON.stringify([userStore.spaceId, userStore.spaceCode])
  await loadOntologies(); if (context !== askContextVersion) return
  await loadConversations()
  if (space !== JSON.stringify([userStore.spaceId, userStore.spaceCode])) return
  navigationReady = true
  await applyNavigationTarget()
}, { flush: 'sync' })

onBeforeUnmount(() => { navigationReady = false; ++navigationSequence; ++askContextVersion; ++ontologyLoadVersion; ++ontologyContextVersion; activeAbortController.value?.abort() })

async function loadOntologies() {
  const version = ++ontologyLoadVersion
  ontologyOptions.value = []
  ontologyLoading.value = true
  try {
    const rows = await loadPagedOptions(listOntology, { spaceId: userStore.spaceId, spaceCode: userStore.spaceCode })
    if (version !== ontologyLoadVersion) return
    ontologyOptions.value = rows.map(item => ({ value: String(item.id), label: item.name }))
  } catch (error) {
    if (version === ontologyLoadVersion) ontologyOptions.value = []
  } finally { if (version === ontologyLoadVersion) ontologyLoading.value = false }
}

async function loadOntologyContext(preferred = {}) {
  const version = ++ontologyContextVersion
  ++skillLoadVersion; ++templateLoadVersion
  skillLoading.value = false; templateLoading.value = false
  const ontologyId = form.ontologyId == null ? null : String(form.ontologyId)
  const space = JSON.stringify(currentSpaceParams())
  const isCurrent = () => version === ontologyContextVersion && ontologyId === String(form.ontologyId ?? '') && space === JSON.stringify(currentSpaceParams())
  datasourceList.value = []; ontologyMetrics.value = []
  skillList.value = []; templateList.value = []
  form.datasourceId = null; form.skillId = null; form.templateId = null
  ontologyContextError.value = ''
  ontologyContextLoading.value = Boolean(ontologyId)
  if (!ontologyId) return
  try {
    const res = await request({
      url: '/ai/ask-data/ontology-context', method: 'get',
      params: { ontologyId, ...currentSpaceParams() }
    })
    if (!isCurrent()) return
    datasourceList.value = (res.data?.datasources || []).map(item => ({ ...item, id: String(item.id) }))
    ontologyMetrics.value = res.data?.metrics || []
    form.datasourceId = datasourceList.value.find(item => item.id === String(preferred.datasourceId))?.id || datasourceList.value[0]?.id || null
    try {
      await loadSkills(preferred.skillId)
      if (!isCurrent()) return
      form.templateId = preferred.templateId == null ? null : String(preferred.templateId)
      await refreshReportTemplates()
    } catch (error) {
      if (isCurrent()) message.warning(error?.message || 'Skill 或模板加载失败，仍可在当前数据源问数')
    }
  } catch (error) {
    if (isCurrent()) {
      datasourceList.value = []; ontologyMetrics.value = []; skillList.value = []; templateList.value = []
      form.datasourceId = null; form.skillId = null; form.templateId = null
      ontologyContextError.value = error?.message || '本体绑定解析失败，请重新选择本体后重试'
    }
  } finally {
    if (version === ontologyContextVersion) ontologyContextLoading.value = false
  }
}

async function loadSkills(preferredSkillId = form.skillId) {
  const context = askContextVersion, sequence = ++skillLoadVersion
  const datasourceId = form.datasourceId == null ? null : String(form.datasourceId)
  skillList.value = []
  form.skillId = null
  skillLoadError.value = ''
  if (!datasourceId) { skillLoading.value = false; return }
  skillLoading.value = true
  const isCurrent = () => context === askContextVersion && sequence === skillLoadVersion && String(form.datasourceId) === datasourceId
  try {
    const rows = await loadPagedOptions(listSkill, {
      status: 'PUBLISHED', datasourceId,
      spaceId: userStore.spaceId, spaceCode: userStore.spaceCode
    }, { isCurrent })
    if (!isCurrent()) return
    skillList.value = rows.map(item => ({ ...item, id: String(item.id) }))
    form.skillId = skillList.value.find(item => item.id === String(preferredSkillId))?.id || skillList.value[0]?.id || null
  } catch (error) {
    if (!isCurrent()) return
    skillLoadError.value = error?.message || 'Skill 加载失败，请重试'
    throw error
  } finally {
    if (sequence === skillLoadVersion) skillLoading.value = false
  }
}

async function handleDatasourceChange() {
  form.skillId = null; form.templateId = null; templateList.value = []
  try {
    await loadSkills()
    await refreshReportTemplates()
  } catch (error) { message.warning(error?.message || 'Skill 加载失败，请重试') }
}

async function handleSkillChange() {
  await refreshReportTemplates()
}

async function refreshReportTemplates() {
  const context = askContextVersion, sequence = ++templateLoadVersion, skillId = form.skillId
  const currentTemplateId = form.templateId
  templateList.value = []
  templateLoading.value = false
  if (!form.datasourceId || !form.skillId) {
    form.templateId = null
    return
  }
  templateLoading.value = true
  try {
    const res = await listSkillReportTemplates(skillId)
    if (context !== askContextVersion || sequence !== templateLoadVersion || skillId !== form.skillId) return
    templateList.value = (res.data || []).filter(item => item.status === 'PUBLISHED').map(item => ({ ...item, id: String(item.id) }))
    if (currentTemplateId && templateList.value.some(item => item.id === String(currentTemplateId))) {
      form.templateId = String(currentTemplateId)
      return
    }
    form.templateId = null
  } finally {
    if (sequence === templateLoadVersion) templateLoading.value = false
  }
}

function latestReportTemplate(templates) {
  const rows = Array.isArray(templates) ? templates.filter(Boolean) : []
  if (!rows.length) return null
  return [...rows].sort((a, b) => {
    const timeA = parseTemplateTime(a.updateTime || a.createTime)
    const timeB = parseTemplateTime(b.updateTime || b.createTime)
    if (timeA !== timeB) return timeB - timeA
    return Number(b.id || 0) - Number(a.id || 0)
  })[0]
}

function parseTemplateTime(value) {
  if (!value) return 0
  return Date.parse(String(value).replace(' ', 'T')) || 0
}

function templateOptionLabel(item) {
  const latest = latestReportTemplate(templateList.value)
  const suffixes = []
  if (latest && item.id === latest.id) {
    suffixes.push('最新')
  }
  if (item.defaultFlag) {
    suffixes.push('默认')
  }
  return suffixes.length ? `${item.templateName}（${suffixes.join(' / ')}）` : item.templateName
}

function openTemplateFormat() {
  templateFormatText.value = buildReportTemplateFormatText(selectedSkill.value)
  templateFormatOpen.value = true
}

async function copyTemplateFormat() {
  try {
    await navigator.clipboard.writeText(templateFormatText.value)
    message.success('模板格式已复制')
  } catch (error) {
    message.error('复制失败，请手动选择复制')
  }
}

async function loadConversations() {
  const version = ++conversationLoadVersion, space = JSON.stringify(currentSpaceParams())
  sessionReady.value = false
  const res = await listAskSessions({ ...currentSpaceParams(), category: 'ASK_DATA', limit: 10 })
  if (version !== conversationLoadVersion || space !== JSON.stringify(currentSpaceParams())) return
  conversations.value = (res.data || []).map(normalizeSession)
  if (conversations.value.length) await selectConversation(conversations.value[0].id)
  else await createConversation()
  if (version === conversationLoadVersion && space === JSON.stringify(currentSpaceParams())) sessionReady.value = true
}

async function createConversation() {
  const context = askContextVersion, space = JSON.stringify(currentSpaceParams())
  const res = await createAskSession(buildSessionPayload({ title: '新对话' }))
  if (context !== askContextVersion || space !== JSON.stringify(currentSpaceParams())) return
  const session = normalizeSession(res.data)
  conversations.value = [session, ...conversations.value.filter((item) => item.id !== session.id)].slice(0, 10)
  await selectConversation(session.id)
}

async function selectConversation(id) {
  if (id !== activeConversationId.value) {
    form.objectKey = ''
    invalidateAskContext()
  }
  activeConversationId.value = id
  resetPromptHistory()
  const session = activeConversation.value
  if (!session) return
  const context = askContextVersion
  await applySessionToForm(session)
  if (context !== askContextVersion || id !== activeConversationId.value) return
  await loadSessionMessages(id)
}

async function removeConversation(id) {
  const confirmed = await new Promise((resolve) => {
    Modal.confirm({
      title: '删除会话',
      content: '确认删除该会话及全部聊天记录？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
  if (!confirmed) return
  await deleteAskSession(id, currentSpaceParams())
  conversations.value = conversations.value.filter((item) => item.id !== id)
  if (activeConversationId.value === id) {
    if (conversations.value.length) {
      await selectConversation(conversations.value[0].id)
    } else {
      await createConversation()
    }
  }
}

async function loadSessionMessages(sessionId) {
  const context = askContextVersion
  const res = await listAskMessages(sessionId, { limit: 10 })
  if (context !== askContextVersion || sessionId !== activeConversationId.value) return
  const data = res.data || {}
  const session = conversations.value.find((item) => item.id === sessionId)
  if (!session) return
  session.messages = (data.rows || []).map(normalizeMessage)
  messageWindow.hasBefore = Boolean(data.hasBefore)
  messageWindow.hasAfter = Boolean(data.hasAfter)
  autoScroll.value = true
  await scrollToBottom(true)
}

function openSkillManagement() {
  if (stationNavigation?.openFlowSubFlow) {
    stationNavigation.openFlowSubFlow('ai', 'ai-skill')
    return
  }
  router.push('/ai/skill')
}

async function loadMoreMessages(direction) {
  const session = activeConversation.value
  if (!session || !session.messages.length) return
  const context = askContextVersion
  const isBefore = direction === 'before'
  if (isBefore && messageWindow.loadingBefore) return
  if (!isBefore && messageWindow.loadingAfter) return
  if (isBefore) messageWindow.loadingBefore = true
  else messageWindow.loadingAfter = true
  const messageList = messageScrollRef.value
  const previousScrollHeight = messageList?.scrollHeight || 0
  const previousScrollTop = messageList?.scrollTop || 0
  try {
    const params = { limit: 5 }
    if (isBefore) params.beforeId = session.messages[0].id
    else params.afterId = session.messages[session.messages.length - 1].id
    const res = await listAskMessages(session.id, params)
    if (context !== askContextVersion || session.id !== activeConversationId.value) return
    const data = res.data || {}
    const rows = (data.rows || []).map(normalizeMessage)
    if (isBefore) {
      session.messages = [...rows, ...session.messages].slice(0, 10)
      messageWindow.hasBefore = Boolean(data.hasBefore)
      messageWindow.hasAfter = true
      await nextTick()
      if (messageList) {
        messageList.scrollTop = previousScrollTop + messageList.scrollHeight - previousScrollHeight
      }
    } else {
      session.messages = [...session.messages, ...rows].slice(-10)
      messageWindow.hasAfter = Boolean(data.hasAfter)
      messageWindow.hasBefore = true
    }
  } finally {
    messageWindow.loadingBefore = false
    messageWindow.loadingAfter = false
  }
}

function handleMessageScroll() {
  const el = messageScrollRef.value
  if (!el) return
  const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  autoScroll.value = distanceFromBottom <= 40
  if (el.scrollTop <= 8 && distanceFromBottom > 8 && messageWindow.hasBefore) {
    loadMoreMessages('before')
  } else if (distanceFromBottom <= 8 && messageWindow.hasAfter) {
    loadMoreMessages('after')
  }
}

async function removeMessage(message) {
  const session = activeConversation.value
  if (!session || !message?.id) return
  await deleteAskMessage(session.id, message.id, currentSpaceParams())
  session.messages = session.messages.filter((item) => item.id !== message.id)
}

async function copyMessage(item) {
  const content = String(item?.displayContent || item?.content || '').trim()
  if (!content) {
    message.warning('当前消息没有可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(content)
    message.success('消息已复制')
  } catch (error) {
    message.error('复制失败，请手动选择复制')
  }
}

function editMessage(message) {
  if (!message || String(message.role || '').toLowerCase() !== 'user') return
  prompt.value = String(message.content || '')
  resetPromptHistory()
  nextTick(() => {
    const input = promptInputRef.value
    const textarea = input?.$el?.querySelector?.('textarea') || input?.$el || input
    textarea?.focus?.()
    movePromptCaretToEnd(textarea)
  })
}

async function clearMessages() {
  const session = activeConversation.value
  if (!session) return
  const confirmed = await new Promise((resolve) => {
    Modal.confirm({
      title: '清空聊天记录',
      content: '确认清空当前会话的聊天记录？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
  if (!confirmed) return
  await clearAskMessages(session.id, currentSpaceParams())
  session.messages = []
  messageWindow.hasBefore = false
  messageWindow.hasAfter = false
}

function normalizeSession(row) {
  return {
    ...row,
    ontologyId: row?.ontologyId == null ? null : String(row.ontologyId),
    title: normalizeAgentText(row?.title) || '新对话',
    messages: row?.messages || []
  }
}

function normalizeMessage(row) {
  let message
  if (row?.payloadJson) {
    try {
      const payload = JSON.parse(row.payloadJson)
      message = {
        ...payload,
        id: row.id,
        role: row.role,
        content: normalizeAgentText(payload.content || row.content || ''),
        displayContent: normalizeVisibleLineBreaks(normalizeAgentText(payload.displayContent || row.displayContent || '')),
        agentSteps: normalizeAgentText(payload.agentSteps || ''),
        agentConfirmation: payload.agentConfirmation || null,
        decisionId: payload.decisionId ? String(payload.decisionId) : (payload.decision?.id ? String(payload.decision.id) : ''),
        decision: payload.decision || null,
        agentConfirming: false
      }
      normalizeReportPayload(message)
      rebuildDisplayContentFromContent(message)
      return message
    } catch {}
  }
  message = {
    id: row.id,
    role: row.role,
    content: normalizeAgentText(row.content || ''),
    displayContent: normalizeVisibleLineBreaks(normalizeAgentText(row.displayContent || '')),
    agentSteps: normalizeAgentText(row.agentSteps || ''),
    queryExecuted: false,
    responseMode: 'qa',
    tableRows: [],
    tableColumns: [],
    reportTemplate: null,
    reportData: null,
    returnedSql: '',
    sqlUnavailable: false,
    agentConfirmation: null,
    agentConfirming: false,
    steps: []
  }
  normalizeReportPayload(message)
  rebuildDisplayContentFromContent(message)
  return message
}

function normalizeReportPayload(message) {
  if (!message) return
  if (typeof message.reportTemplate === 'string') {
    message.reportTemplate = parseMaybeJson(message.reportTemplate) || null
  }
  if (typeof message.reportData === 'string') {
    message.reportData = parseMaybeJson(message.reportData) || null
  }
}

function rebuildDisplayContentFromContent(message) {
  if (!message || message.unifiedResult || message.role !== 'assistant' || !message.content) return
  if (message.displayContent) {
    message.displayContent = normalizeVisibleLineBreaks(removeValidationSqlSection(message.displayContent).trim())
  }
  const mixed = stripNativeJsonBlocks(message.content)
  if (!mixed.text || mixed.text === message.content) return
  if (!message.displayContent || isSqlOnlyDisplay(message.displayContent)) {
    message.displayContent = normalizeVisibleLineBreaks(mixed.text)
  }
  if (mixed.steps && !message.agentSteps) {
    message.agentSteps = mixed.steps
  }
  const sql = extractLastSqlBlock(message.displayContent)?.sql || extractLastSqlBlock(message.content)?.sql
  if (sql && !message.returnedSql) {
    message.returnedSql = sql
  }
}

function isFullHtmlMessage(message) {
  if (!message || message.role === 'user' || message.unifiedResult) return false
  const content = message.displayContent || message.content || ''
  return /<!doctype\s+html|<html[\s>]/i.test(content)
}

function isExportableReportMessage(message) {
  return Boolean(message && message.role !== 'user' && (
    isFullHtmlMessage(message) || (message.reportTemplate && message.reportData)
  ))
}

async function exportMessagePdf(message) {
  if (!message) return
  try {
    if (isFullHtmlMessage(message)) {
      await exportHtmlMessagePdf(message)
      return
    }
    printingMessageId.value = message.id
    await nextTick()
    const element = document.querySelector('.message-row.printing-report .message-bubble')
    await downloadElementAsPdf(element, reportFileName(message))
  } catch (error) {
    message.error(error?.message || 'PDF导出失败')
  } finally {
    clearPrintingState()
  }
}

async function exportHtmlMessagePdf(message) {
  const html = buildPrintableHtml(message.displayContent || message.content || '')
  if (!html) {
    throw new Error('当前报告内容为空，无法导出')
  }
  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.left = '-10000px'
  iframe.style.top = '0'
  iframe.style.width = '1280px'
  iframe.style.height = '900px'
  iframe.style.border = '0'
  document.body.appendChild(iframe)
  try {
    await new Promise((resolve, reject) => {
      iframe.onload = resolve
      iframe.onerror = reject
      iframe.srcdoc = html
    })
    const doc = iframe.contentDocument
    const element = doc?.body
    if (!element) {
      throw new Error('HTML报告渲染失败')
    }
    await waitForImages(doc)
    await downloadElementAsPdf(element, reportFileName(message), iframe.contentWindow)
  } finally {
    document.body.removeChild(iframe)
  }
}

async function downloadElementAsPdf(element, filename, targetWindow = window) {
  if (!element) {
    throw new Error('未找到可导出的报告内容')
  }
  const canvas = await html2canvas(element, {
    backgroundColor: '#ffffff',
    scale: Math.min(2, window.devicePixelRatio || 1.5),
    useCORS: true,
    windowWidth: Math.max(1280, targetWindow?.document?.documentElement?.scrollWidth || element.scrollWidth),
    windowHeight: Math.max(900, targetWindow?.document?.documentElement?.scrollHeight || element.scrollHeight)
  })
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const margin = 8
  const imgWidth = pageWidth - margin * 2
  const imgHeight = (canvas.height * imgWidth) / canvas.width
  const pageCanvasHeight = Math.floor((canvas.width * (pageHeight - margin * 2)) / imgWidth)
  let renderedHeight = 0
  let pageIndex = 0
  while (renderedHeight < canvas.height) {
    const sliceHeight = Math.min(pageCanvasHeight, canvas.height - renderedHeight)
    const pageCanvas = document.createElement('canvas')
    pageCanvas.width = canvas.width
    pageCanvas.height = sliceHeight
    const ctx = pageCanvas.getContext('2d')
    ctx.drawImage(canvas, 0, renderedHeight, canvas.width, sliceHeight, 0, 0, canvas.width, sliceHeight)
    const pageImgHeight = (sliceHeight * imgWidth) / canvas.width
    if (pageIndex > 0) pdf.addPage()
    pdf.addImage(pageCanvas.toDataURL('image/jpeg', 0.92), 'JPEG', margin, margin, imgWidth, pageImgHeight)
    renderedHeight += sliceHeight
    pageIndex += 1
  }
  if (imgHeight <= pageHeight - margin * 2 && pageIndex === 0) {
    pdf.addImage(canvas.toDataURL('image/jpeg', 0.92), 'JPEG', margin, margin, imgWidth, imgHeight)
  }
  pdf.save(filename)
}

function waitForImages(doc) {
  const images = Array.from(doc.images || [])
  if (!images.length) {
    return Promise.resolve()
  }
  return Promise.all(images.map((img) => {
    if (img.complete) return Promise.resolve()
    return new Promise((resolve) => {
      img.onload = resolve
      img.onerror = resolve
    })
  }))
}

function reportFileName(message) {
  const raw = activeConversation.value?.title || message?.title || '数据分析报告'
  const safe = raw.replace(/[\\/:*?"<>|]/g, '_').slice(0, 60) || '数据分析报告'
  return `${safe}.pdf`
}

function buildPrintableHtml(content) {
  const html = extractFullHtmlDocument(content)
  if (!html) return ''
  const printStyle = '<style>@page{size:auto;margin:12mm;} html,body{max-width:none!important;background:#fff!important;} .container{max-width:none!important;width:auto!important;}</style>'
  return /<\/head>/i.test(html)
    ? html.replace(/<\/head>/i, `${printStyle}</head>`)
    : `${printStyle}${html}`
}

function extractFullHtmlDocument(content = '') {
  const htmlStart = content.search(/<!doctype\s+html|<html[\s>]/i)
  if (htmlStart < 0) return ''
  const htmlContent = content.slice(htmlStart)
  const endMatch = htmlContent.match(/<\/html\s*>/i)
  if (!endMatch) {
    return removeHtmlInterpreterSummary(htmlContent)
  }
  const end = endMatch.index + endMatch[0].length
  return removeHtmlInterpreterSummary(htmlContent.slice(0, end))
}

function removeHtmlInterpreterSummary(content = '') {
  return content.replace(
    /✅?\s*[^<\n]*?报告已生成并(?:成功)?渲染。?[\s\S]*?$/i,
    ''
  )
}

function clearPrintingState() {
  printingMessageId.value = null
}

function isSqlOnlyDisplay(content) {
  if (!content || typeof content !== 'string') return false
  const text = content.trim()
  if (!text) return false
  const withoutSql = text
    .replace(/校验SQL：?/g, '')
    .replace(/```sql\s*[\s\S]*?```/gi, '')
    .trim()
  return !withoutSql
}

async function applySessionToForm(session) {
  const context = askContextVersion, restore = ++sessionRestoreVersion
  syncingSession.value = true
  try {
    const storedOntology = session.ontologyId == null ? null : String(session.ontologyId)
    form.ontologyId = ontologyOptions.value.some(item => item.value === storedOntology) ? storedOntology : null
    form.objectKey = ''
    form.returnSql = Boolean(session.returnSql)
    await loadOntologyContext({ datasourceId: session.datasourceId, skillId: session.skillId, templateId: session.templateId })
    if (context !== askContextVersion || session.id !== activeConversationId.value || restore !== sessionRestoreVersion) return
  } finally {
    await nextTick()
    if (restore === sessionRestoreVersion) syncingSession.value = false
  }
}

function buildSessionPayload(extra = {}) {
  return {
    title: activeConversation.value?.title || extra.title || '新对话',
    mode: 'qa',
    ontologyId: form.ontologyId == null ? null : String(form.ontologyId),
    datasourceId: form.datasourceId,
    datasourceName: selectedDatasourceName(),
    skillId: form.skillId,
    templateId: form.templateId,
    returnSql: form.returnSql,
    category: 'ASK_DATA',
    spaceId: userStore.spaceId || null,
    spaceCode: userStore.spaceCode || '',
    ...extra
  }
}

async function syncActiveSessionSelection() {
  if (syncingSession.value || ontologyContextLoading.value || !sessionReady.value || !activeConversationId.value) return
  const context = askContextVersion, id = activeConversationId.value
  const payload = buildSessionPayload(), version = ++sessionSaveVersion
  const valid = () => context === askContextVersion && id === activeConversationId.value && String(payload.spaceId) === String(userStore.spaceId) && payload.spaceCode === (userStore.spaceCode || '')
  const job = sessionSaveQueue.catch(() => {}).then(async () => {
    if (!valid() || version !== sessionSaveVersion) return
    const res = await updateAskSession(id, payload)
    if (!valid() || version !== sessionSaveVersion) return
    const updated = normalizeSession(res.data)
    const index = conversations.value.findIndex(item => item.id === id)
    if (index >= 0) conversations.value[index] = { ...conversations.value[index], ...updated, messages: conversations.value[index].messages }
  })
  sessionSaveQueue = job
  try { await job } catch (failure) { if (valid()) message.warning('会话配置保存失败，请重试后再刷新页面') }
}

async function persistMessage(message) {
  const session = activeConversation.value
  if (!session) return
  const payload = serializeMessage(message)
  const res = await appendAskMessage(session.id, {
    role: payload.role,
    content: payload.content,
    displayContent: payload.displayContent,
    payloadJson: JSON.stringify(payload)
  }, currentSpaceParams())
  if (res.data?.id) {
    message.id = res.data.id
  }
  session.messageCount = (session.messageCount || 0) + 1
}

function serializeMessage(message) {
  return {
    id: message.id,
    role: message.role,
    content: message.content || '',
    displayContent: message.displayContent || '',
    agentSteps: message.agentSteps || '',
    unifiedResult: Boolean(message.unifiedResult),
    queryExecuted: Boolean(message.queryExecuted),
    responseMode: message.responseMode || 'qa',
    decisionId: message.decisionId || '',
    decision: message.decision || null,
    agentOperationId: message.agentOperationId || '',
    agentOperationStatus: message.agentOperationStatus || '',
    askResultStatus: message.askResultStatus || '',
    agentConfirmation: message.agentConfirmation || null,
    inputBinding: message.inputBinding || null,
    inputContinuation: message.inputContinuation || null,
    analysisNotice: message.analysisNotice || '',
    semanticAnalysisResults: message.semanticAnalysisResults || [],
    tableRows: message.tableRows || [],
    tableColumns: message.tableColumns || [],
    queryTables: message.queryTables || [],
    reportTemplate: message.reportTemplate || null,
    reportData: message.reportData || null,
    returnedSql: message.returnedSql || '',
    sqlUnavailable: Boolean(message.sqlUnavailable),
    steps: message.steps || []
  }
}

function currentSpaceParams() {
  return {
    spaceId: userStore.spaceId || null,
    spaceCode: userStore.spaceCode || ''
  }
}

function trimActiveMessagesToLatest() {
  const session = activeConversation.value
  if (session && session.messages.length > 10) {
    session.messages = session.messages.slice(-10)
    messageWindow.hasBefore = true
  }
}

function handlePromptKeydown(event) {
  if (event?.isComposing) return
  if (event.key === 'ArrowUp' && !event.altKey && !event.ctrlKey && !event.metaKey) {
    handlePromptHistoryUp(event)
    return
  }
  if (event.key === 'ArrowDown' && !event.altKey && !event.ctrlKey && !event.metaKey) {
    handlePromptHistoryDown(event)
    return
  }
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

function normalizeVisibleLineBreaks(value) {
  if (value === undefined || value === null) return value
  const text = String(value)
  // 结构化消息中的 \n 可能属于 JSON 字符串转义，不能破坏 JSON；普通展示文本则转换为真实换行。
  try {
    JSON.parse(text)
    return text
  } catch (error) {
    return text
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n/g, '\n')
      .replace(/\\r/g, '\n')
      // 部分模型/适配器会把换行错误返回成单独的字母 n，按 Markdown 行首标记兜底恢复。
      .replace(/(^|[\r\n\s])n(?=\s*(?:#{1,6}\s|\||[-*+]\s))/g, '$1\n')
  }
}

function userPromptHistory() {
  return (activeConversation.value?.messages || [])
    .filter((item) => String(item?.role || '').toLowerCase() === 'user' && String(item.content || '').trim())
    .map((item) => String(item.content).trim())
}

function handlePromptHistoryUp(event) {
  const target = event?.target
  const history = userPromptHistory()
  if (!history.length) return
  event.preventDefault()
  if (promptHistoryIndex.value < 0) {
    promptHistoryDraft.value = prompt.value
    promptHistoryIndex.value = history.length - 1
  } else if (promptHistoryIndex.value > 0) {
    promptHistoryIndex.value -= 1
  }
  prompt.value = history[promptHistoryIndex.value] || ''
  nextTick(() => movePromptCaretToEnd(target))
}

function handlePromptHistoryDown(event) {
  if (promptHistoryIndex.value < 0) return
  const target = event?.target
  event.preventDefault()
  const history = userPromptHistory()
  if (promptHistoryIndex.value < history.length - 1) {
    promptHistoryIndex.value += 1
    prompt.value = history[promptHistoryIndex.value] || ''
  } else {
    prompt.value = promptHistoryDraft.value
    resetPromptHistory(false)
  }
  nextTick(() => movePromptCaretToEnd(target))
}

function movePromptCaretToEnd(target) {
  const length = String(prompt.value || '').length
  target?.setSelectionRange?.(length, length)
}

function resetPromptHistory(clearDraft = true) {
  promptHistoryIndex.value = -1
  if (clearDraft) promptHistoryDraft.value = ''
}

let submittingMessage = false
async function sendMessage() {
  if (submittingMessage || sending.value) return
  submittingMessage = true
  try {
    await submitMessage()
  } finally {
    submittingMessage = false
  }
}

async function submitMessage() {
  if (ontologyContextLoading.value || skillLoading.value || templateLoading.value || syncingSession.value) {
    message.info('正在加载本体分析上下文，请稍候')
    return
  }
  if (!form.ontologyId) {
    message.warning('请先选择业务本体')
    return
  }
  if (ontologyContextError.value) {
    message.warning(ontologyContextError.value)
    return
  }

  const question = prompt.value.trim()
  if (!question) {
    message.warning('请输入问题')
    return
  }
  if (inputContinuation.value && !continuationMatches(inputContinuation.value)) {
    inputContinuation.value = null
    message.warning('原任务上下文已变化，未发送；请重新选择待补充任务或作为新问题发送')
    return
  }
  const continuation = inputContinuation.value ? { ...inputContinuation.value } : null
  const analysisReference = decisionAnalysisReference.value
  if (analysisReference && (String(analysisReference.sessionId) !== String(activeConversationId.value) || String(analysisReference.ontologyId) !== String(form.ontologyId) || String(analysisReference.spaceId) !== String(userStore.spaceId))) {
    decisionAnalysisReference.value = null; message.warning('原分析上下文已变化，请重新选择'); return
  }
  ++decisionRequestVersion.value
  if (!activeConversation.value) {
    await createConversation()
  }

  const conversation = activeConversation.value
  if (!conversation) return
  const context = askContextVersion
  if (conversation.messages.length === 0) {
    conversation.title = question.slice(0, 20)
    await updateAskSession(conversation.id, buildSessionPayload({ title: conversation.title }))
    if (context !== askContextVersion) return
  }
  conversation.datasourceName = selectedDatasourceName()
  const userMessage = {
    id: Date.now(),
    role: 'user',
    content: question
  }
  conversation.messages.push(userMessage)
  trimActiveMessagesToLatest()
  await persistMessage(userMessage)
  if (context !== askContextVersion) return

  const assistantMessage = reactive({
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    displayContent: '',
    agentSteps: '',
    queryExecuted: false,
    tableRows: [],
    tableColumns: [],
    responseMode: 'qa',
    reportTemplate: null,
    reportData: null,
    agentOperationId: '',
    agentOperationStatus: '',
    agentConfirmation: null,
    agentConfirming: false,
    returnedSql: '',
    sqlUnavailable: false,
    inputBinding: {
      sessionId: conversation.id, spaceId: userStore.spaceId, spaceCode: userStore.spaceCode, ontologyId: form.ontologyId,
      datasourceId: form.datasourceId, objectKey: form.objectKey.trim()
    },
    inputContinuation: null,
    steps: createAskSteps()
  })
  conversation.messages.push(assistantMessage)
  trimActiveMessagesToLatest()
  activeAssistantMessage.value = assistantMessage
  const controller = new AbortController()
  activeAbortController.value = controller
  autoScroll.value = true

  prompt.value = ''
  inputContinuation.value = null
  decisionAnalysisReference.value = null
  resetPromptHistory()
  sending.value = true
  await scrollToBottom(true)

  let receivedResult = false
  try {
    setActiveStep(assistantMessage, 'connect')
    await streamAskData({
        question,
        ...(continuation ? { continuationOperationId: continuation.operationId } : {}),
        ...(analysisReference ? { analysisOperationId: analysisReference.operationId } : {}),
        datasourceId: form.datasourceId,
        ontologyId: form.ontologyId,
        ...(form.objectKey.trim() ? { objectKey: form.objectKey.trim() } : {}),
        sessionId: conversation.id,
        reportTemplateId: form.templateId,
        chatMode: 'chat_with_db_qa',
        conversationId: String(conversation.id),
        skillIds: form.skillId ? [form.skillId] : [],
        returnSql: form.returnSql,
        // Only explicit decision navigation supplies this legacy hint; no client intent routing.
        ...(analysisReference ? { responseMode: 'decision' } : {}),
        spaceId: userStore.spaceId || null,
        spaceCode: userStore.spaceCode || ''
      }, {
        onMessage(chunk) {
          if (context !== askContextVersion) return
          if (!assistantMessage.content) {
            setActiveStep(assistantMessage, 'answer')
          }
          assistantMessage.content = mergeStreamContent(assistantMessage.content, chunk)
          scrollToBottom()
        },
        onAnswerDelta(chunk) {
          if (context !== askContextVersion || typeof chunk !== 'string') return
          assistantMessage.content += chunk
          assistantMessage.displayContent = assistantMessage.content
          scrollToBottom()
        },
        onSql(sql) {
          if (context !== askContextVersion) return
          appendSqlToMessage(assistantMessage, sql)
          scrollToBottom()
        },
        onSteps(steps) {
          if (context !== askContextVersion) return
          appendAgentStepsToMessage(assistantMessage, steps)
          scrollToBottom()
        },
        onResult(data) {
          if (context !== askContextVersion) return
          receivedResult = true
          applyAskAgentResult(assistantMessage, data)
          scrollToBottom()
        },
        onReport(data) {
          if (context !== askContextVersion) return
          applyAskReport(assistantMessage, data)
          scrollToBottom()
        },
        onError(message) {
          if (context !== askContextVersion || receivedResult) return
          assistantMessage.content = normalizeAgentText(message) || '问数调用失败'
          assistantMessage.displayContent = assistantMessage.content
          assistantMessage.askResultStatus = 'FAILED'
        }
      }, controller.signal)
    if (context !== askContextVersion) return
    if (!receivedResult && assistantMessage.askResultStatus !== 'FAILED') {
      throw new Error('连接已结束，但未收到问数结果；生成片段不能作为查询结论')
    }
    finishSteps(assistantMessage)
    await persistMessage(assistantMessage)
  } catch (error) {
    if (context !== askContextVersion) return
    if (!receivedResult) {
      const aborted = isAbortError(error)
      assistantMessage.content = aborted ? '已停止生成，未完成的回答不作为查询结论。'
        : normalizeAgentText(error?.message) || '数据分析调用失败'
      assistantMessage.displayContent = assistantMessage.content
      assistantMessage.askResultStatus = aborted ? 'CANCELLED' : 'FAILED'
      finishSteps(assistantMessage)
      await persistMessage(assistantMessage)
    } else if (!isAbortError(error)) {
      message.warning('已收到问数结果；连接收尾或会话保存未完成，请保留当前结果。')
    }
  } finally {
    if (context === askContextVersion) {
      sending.value = false
      activeAbortController.value = null
      activeAssistantMessage.value = null
      await scrollToBottom()
    }
  }
}

function stopGeneration() {
  if (!sending.value) return
  message.info('仅停止接收生成内容，不会撤销已提交的业务执行；请通过提案刷新运行状态。')
  activeAbortController.value?.abort()
}

function isAbortError(error) {
  return error?.name === 'AbortError' || error?.code === 'ERR_CANCELED' || /canceled|aborted/i.test(error?.message || '')
}

function applyAskReport(assistantMessage, data) {
  if (data.qualityWarning) assistantMessage.content = normalizeAgentText(data.qualityWarning)
  if (data.templateContent && data.reportData) {
    assistantMessage.reportTemplate = parseMaybeJson(data.templateContent) || data.templateContent
    assistantMessage.reportData = parseMaybeJson(data.reportData) || data.reportData
    assistantMessage.returnedSql = data.sql || assistantMessage.reportData?.sql || ''
    assistantMessage.displayContent = ''
    return
  }
  assistantMessage.content = normalizeVisibleLineBreaks(normalizeAgentText(data.rawReply || data.qualityWarning)) || '报告生成失败，未返回结构化数据'
  assistantMessage.displayContent = assistantMessage.content
}

function applyAskAgentResult(target, data) {
  target.unifiedResult = data.currentStep === '统一问数'
  target.askResultStatus = data.status || (data.success === false ? 'FAILED' : '')
  target.inputContinuation = data.status === 'NEED_INPUT' && target.inputBinding &&
    /^ASK_(METRIC|DECISION|UNIFIED)_/.test(data.operationId || '')
    ? { ...target.inputBinding, operationId: data.operationId } : null
  if (data.decision) {
    target.responseMode = 'decision'
    target.decision = data.decision
    target.decisionId = String(data.decision.id || '')
    target.content = normalizeAgentText(data.text || data.message || data.decision.message || '')
    target.displayContent = target.content
    // 提案确认独立于问数操作；同一消息仍可保留查询和报告。
  }
  target.agentOperationId = data.operationId || ''
  target.agentOperationStatus = data.status || ''
  const tools = data.toolResults || []
  const pending = [...tools].reverse().find(item => item.toolName === 'save_analysis' && item.status === 'NEED_CONFIRMATION')
  target.agentConfirmation = data.status === 'NEED_CONFIRMATION' && pending
    ? { ontologyId: form.ontologyId, sessionId: activeConversation.value?.id, arguments: pending.metadata?.arguments }
    : null
  target.content = normalizeAgentText(data.text || data.message || '')
  target.displayContent = normalizeVisibleLineBreaks(target.content)
  if (data.output?.includes('NATIVE_REPLY_DISPLAYED')) {
    target.returnedSql = data.returnedSql || ''
  }
  if (data.output?.includes('UNVERIFIED_EXPLORATORY')) {
    target.analysisNotice = data.output.includes('NATIVE_REPLY_DISPLAYED')
      ? '以上为AI分析，仅供参考'
      : '远端答复未提供可核验的执行结果，未计入本轮查询结论。'
  }
  if (data.output?.includes('REQUESTED_TEMPLATE_NOT_APPLIED')) {
    target.analysisNotice = [target.analysisNotice, '指定模板未应用：模板不可用，或本轮证据不足以填充必需字段。'].filter(Boolean).join(' ')
  }
  if (!target.unifiedResult || !target.agentSteps) {
    target.agentSteps = tools.map(item => `${item.toolName || '工具'}：${item.message || item.status || ''}`).join('\n')
  }
  const queries = tools.filter(item => item.toolName === 'execute_metric_analysis' && item.status === 'SUCCEEDED' && Array.isArray(item.data?.rows))
  target.queryTables = queries.map(({ data: result }) => ({
    name: result.metricName || '正式指标查询',
    rows: result.rows,
    columns: (result.columns || []).map(column => ({ prop: column.key, label: column.label || column.key })),
    truncated: result.truncated,
    notice: result.notice
  }))
  target.queryExecuted = queries.length > 0 || tools.some(item => item.toolName === 'compare_periods' && item.status === 'SUCCEEDED')
  const notices = target.queryTables.map(table => table.notice).filter(Boolean)
  target.analysisNotice = [target.analysisNotice, ...new Set(notices)].filter(Boolean).join(' ')
  target.semanticAnalysisResults = tools.filter(item => ['compare_periods', 'analyze_distribution', 'analyze_trend'].includes(item.toolName) && item.status === 'SUCCEEDED')
    .map(item => ({ tool: item.toolName, data: item.data }))
  const chart = [...tools].reverse().find(item => item.toolName === 'build_chart_spec' && item.status === 'SUCCEEDED')
  if (chart?.data) target.analysisNotice = [target.analysisNotice, '已生成声明式展示配置；此页面暂不渲染新图表。'].filter(Boolean).join(' ')
}

function updateDecisionMessage(target, proposal) {
  // 组件只在原空间/会话/请求仍有效时发出更新。历史保存的是展示快照，不作为提交参数来源。
  if (!activeMessages.value.includes(target) || String(target.decisionId) !== String(proposal.id)) return
  target.decision = proposal
}

async function confirmMetricAnalysis(target) {
  if (sending.value || target.decisionId || target.agentConfirming || !target.agentOperationId) return
  const selection = target.agentConfirmation
  if (!selection?.ontologyId || String(selection.sessionId) !== String(activeConversationId.value)) {
    message.warning('请回到原会话及指标本体后确认')
    return
  }
  const context = askContextVersion
  const session = activeConversation.value
  sending.value = true
  target.agentConfirming = true
  const controller = new AbortController()
  activeAbortController.value = controller
  try {
    await streamAskData({
      question: '确认保存分析条件', confirmed: true, operationId: target.agentOperationId,
      ontologyId: selection.ontologyId, sessionId: selection.sessionId,
      conversationId: String(selection.sessionId), spaceId: userStore.spaceId, spaceCode: userStore.spaceCode
    }, {
      onResult(data) { if (context === askContextVersion) applyAskAgentResult(target, data) },
      onError(text) { throw new Error(text || '保存确认失败') }
    }, controller.signal)
    if (context === askContextVersion && session?.id === activeConversationId.value) await persistMessage(target)
  } catch (error) {
    if (context === askContextVersion && !isAbortError(error)) message.error(error?.message || '保存失败，请读取状态后重试')
  } finally {
    target.agentConfirming = false
    if (context === askContextVersion) { sending.value = false; activeAbortController.value = null }
  }
}

function hydrateStructuredData(message) {
  hydrateMixedNativeContent(message)
  const parsed = parseStructuredContent(message.content)
  if (!parsed) {
    message.displayContent = message.displayContent || message.content
    extractAgentSteps(message)
    return
  }

  message.displayContent = message.displayContent || normalizeVisibleLineBreaks(parsed.text || '')
  message.tableRows = parsed.rows || []
  message.tableColumns = parsed.columns?.length ? parsed.columns : buildColumns(parsed.rows)
  if (parsed.sql) {
    message.returnedSql = parsed.sql
  }
  if (parsed.agentSteps) {
    message.agentSteps = parsed.agentSteps
  }
  appendSqlMarkdownFromContent(message)
  extractAgentSteps(message)
}

function hydrateMixedNativeContent(message) {
  if (!message?.content || typeof message.content !== 'string') return
  const normalized = stripNativeJsonBlocks(message.content)
  if (normalized.steps && !message.agentSteps) {
    message.agentSteps = normalized.steps
  }
  if (normalized.text && normalized.text !== message.content) {
    message.displayContent = normalizeVisibleLineBreaks(normalized.text)
  }
}

function mergeStreamContent(current, chunk) {
  if (!chunk) return current || ''
  const before = current || ''
  if (looksLikeCompleteJson(chunk) && looksLikeCompleteJson(before)) {
    return normalizeAgentText(chunk)
  }
  if (looksLikeCompleteJson(chunk) && extractJsonObjects(before).length) {
    return normalizeAgentText(chunk)
  }
  return normalizeVisibleLineBreaks(normalizeAgentText(before + chunk))
}

function looksLikeCompleteJson(value) {
  if (!value || typeof value !== 'string') return false
  const text = value.trim()
  if (!text.startsWith('{') || !text.endsWith('}')) return false
  try {
    JSON.parse(text)
    return true
  } catch {
    return false
  }
}

function hydrateNativeExecutionData(message) {
  const raw = parseNativePayload(message.content)
  if (!raw) return
  const sql = extractSqlFromRaw(raw)
  const rows = extractRowsFromRaw(raw)
  const columns = extractColumnsFromRaw(raw, rows)
  const steps = extractStepsFromRaw(raw)
  const text = extractTextFromRaw(raw)

  if (rows.length) {
    message.tableRows = rows
    message.tableColumns = columns.length ? columns : buildColumns(rows)
    message.queryExecuted = true
  }
  if (sql) {
    message.returnedSql = sql
  }
  if (steps) {
    message.agentSteps = steps
  }
  if (text && (!message.displayContent || message.displayContent === message.content)) {
    message.displayContent = text
  }
  appendSqlMarkdownFromContent(message)
}

function appendSqlToMessage(message, sql) {
  const value = (sql || '').trim()
  message.returnedSql = value
  message.sqlUnavailable = !value
}

function appendAgentStepsToMessage(message, steps) {
  const value = (steps || '').trim()
  if (value) {
    message.agentSteps = [...new Set([...(message.agentSteps || '').split('\n').filter(Boolean), value])].slice(-12).join('\n')
  }
}

function appendSqlMarkdownFromContent(message) {
  if (message.returnedSql) return
  const sqlBlock = extractLastSqlBlock(message.content)
  if (sqlBlock?.sql) {
    message.returnedSql = sqlBlock.sql
  }
}

function extractLastSqlBlock(content) {
  if (!content || typeof content !== 'string') return null
  const matches = Array.from(content.matchAll(/```sql\s*([\s\S]*?)```/gi))
  if (!matches.length) return null
  const last = matches[matches.length - 1]
  const sql = (last[1] || '').trim()
  if (!sql) return null
  return {
    sql,
    markdown: `\`\`\`sql\n${sql}\n\`\`\``
  }
}

function extractAgentSteps(message) {
  const content = (message.displayContent || message.content)
    .replace(/^No correct response found\..*?system prompt\.\s*/i, '')
  if (!content) return

  const agentPatterns = [
    /^\s*with\s+open\s*\(/m,
    /^\s*f\.write\(/m,
    /^\s*f\.close\(\)/m,
    /encoding=['"]utf-8['"]/,
    /\.write\(html_content\)/,
    /html_content\s*=\s*['"]/,
    /\(no output/,
  ]

  const lines = content.split('\n')
  let stepStart = -1
  let lastCodeLine = -1

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const isAgent = agentPatterns.some((p) => p.test(line))
    if (isAgent) {
      if (stepStart === -1) stepStart = i
      lastCodeLine = i
    }
  }

  if (stepStart === -1) return

  let stepEnd = Math.min(lastCodeLine + 3, lines.length)
  while (stepEnd < lines.length && lines[stepEnd].trim() === '') {
    stepEnd++ // skip trailing blanks after steps
  }

  const stepsText = lines.slice(stepStart, stepEnd).join('\n')
  const cleanLines = []
  let inStep = false
  for (let i = 0; i < lines.length; i++) {
    if (i === stepStart) { inStep = true }
    if (i === stepEnd) { inStep = false }
    if (!inStep) cleanLines.push(lines[i])
  }
  const cleanText = cleanLines.join('\n').replace(/\n{3,}/g, '\n\n').trim()
  if (!cleanText) return
  message.agentSteps = stepsText
  message.displayContent = cleanText
}

function parseStructuredContent(content) {
  if (!content || typeof content !== 'string') return null
  const mixed = stripNativeJsonBlocks(content)
  if (mixed.text && mixed.text !== content) {
    const sql = extractLastSqlBlock(mixed.text)?.sql || ''
    return {
      text: mixed.text,
      rows: [],
      columns: [],
      sql,
      agentSteps: mixed.steps
    }
  }
  const parsed = tryParseJson(content.trim())
  if (!parsed) return null
  const raw = normalizeStructuredPayload(parsed)

  let text = raw.msg || raw.message || raw.content || raw.summary || raw.explanation || ''
  let rows = []
  let columns = []
  let sql = extractSqlFromRaw(raw)
  let agentSteps = extractStepsFromRaw(raw)

  if (Number(raw.code) === 500 && !text) {
    text = '对话异常'
  }
  if (raw.qualityWarning) {
    text = `${text ? `${text}\n\n` : ''}${raw.qualityWarning}`
  }

  rows = extractRowsFromRaw(raw)
  columns = extractColumnsFromRaw(raw, rows)

  if (sql && !text.includes(sql)) {
    text = `${text ? `${text}

` : ''}\`\`\`sql
${sql}
\`\`\``
  }

  if (!rows.length && !text && !sql && !agentSteps) return null
  return { text, rows, columns, sql, agentSteps }
}

function stripNativeJsonBlocks(content) {
  const jsonObjects = extractJsonObjectsWithRange(content)
  if (!jsonObjects.length) return { text: content, steps: '' }
  const stepTexts = []
  let text = ''
  let cursor = 0

  jsonObjects.forEach((item) => {
    text += content.slice(cursor, item.start)
    cursor = item.end
    try {
      const raw = normalizeStructuredPayload(JSON.parse(item.text))
      const step = extractStepsFromRaw(raw)
      if (step) {
        stepTexts.push(step)
      } else {
        text += item.text
      }
    } catch {
      text += item.text
    }
  })
  text += content.slice(cursor)

  return {
    text: removeValidationSqlSection(text).replace(/\n{3,}/g, '\n\n').trim(),
    steps: stepTexts.join('\n\n').trim()
  }
}

function removeValidationSqlSection(content) {
  if (!content || typeof content !== 'string') return ''
  return content
    .replace(/\n*校验SQL：?\s*```sql\s*[\s\S]*?```\s*/gi, '\n')
    .replace(/\n*>?\s*未能生成可校验的 SELECT SQL。\s*/gi, '\n')
}

function normalizeStructuredPayload(raw) {
  if (!raw || Array.isArray(raw) || typeof raw !== 'object') return raw
  const vis = parseMaybeJson(raw.vis || raw.view || raw.render)
  if (vis && typeof vis === 'object') return normalizeStructuredPayload(vis)
  const resource = parseMaybeJson(raw.resource_value || raw.resourceValue)
  if (resource && typeof resource === 'object') {
    return normalizeStructuredPayload({
      ...resource,
      msg: raw.msg || raw.message || raw.content || resource.msg || resource.message,
      steps: raw.steps || raw.thoughts || raw.tool_calls || raw.action || resource.steps
    })
  }
  if (typeof raw.data === 'string') {
    const parsedData = parseMaybeJson(raw.data)
    if (parsedData && typeof parsedData === 'object') {
      return normalizeStructuredPayload({
        ...parsedData,
        msg: raw.msg || raw.message || parsedData.msg || parsedData.message
      })
    }
  }
  if (raw.detailData || raw.chatData || raw.sql || raw.msg || raw.steps || raw.result) return raw
  if (raw.data && typeof raw.data === 'object' && !Array.isArray(raw.data)) {
    return {
      ...raw.data,
      msg: raw.msg || raw.message || raw.data.msg || raw.data.message,
      steps: raw.steps || raw.thoughts || raw.tool_calls || raw.action || raw.data.steps,
      code: raw.code ?? raw.data.code
    }
  }
  return raw
}

function tryParseJson(content) {
  try {
    return JSON.parse(content)
  } catch {
    const candidates = extractJsonObjects(content)
    for (let i = candidates.length - 1; i >= 0; i--) {
      try {
        return JSON.parse(candidates[i])
      } catch {
        // Try the previous complete object.
      }
    }
    return null
  }
}

function extractJsonObjects(content) {
  return extractJsonObjectsWithRange(content).map((item) => item.text)
}

function extractJsonObjectsWithRange(content) {
  if (!content || typeof content !== 'string') return []
  const objects = []
  let start = -1
  let depth = 0
  let inString = false
  let escaped = false

  for (let i = 0; i < content.length; i++) {
    const char = content[i]
    if (escaped) {
      escaped = false
      continue
    }
    if (char === '\\') {
      escaped = inString
      continue
    }
    if (char === '"') {
      inString = !inString
      continue
    }
    if (inString) continue
    if (char === '{') {
      if (depth === 0) start = i
      depth++
    } else if (char === '}' && depth > 0) {
      depth--
      if (depth === 0 && start >= 0) {
        objects.push({
          text: content.slice(start, i + 1),
          start,
          end: i + 1
        })
        start = -1
      }
    }
  }
  return objects
}

function parseNativePayload(content) {
  if (!content || typeof content !== 'string') return null
  const parsed = tryParseJson(content.trim())
  if (!parsed) return null
  return normalizeStructuredPayload(parsed)
}

function parseMaybeJson(value) {
  if (!value || typeof value !== 'string') return null
  return tryParseJson(value.trim())
}

function extractTextFromRaw(raw) {
  if (!raw || typeof raw !== 'object') return ''
  return raw.msg || raw.message || raw.content || raw.summary || raw.explanation || raw.answer || raw.incremental || raw.data?.incremental || ''
}

function extractSqlFromRaw(raw) {
  if (!raw || typeof raw !== 'object') return ''
  const candidates = [
    raw.sql,
    raw.SQL,
    raw.text2sql,
    raw.sqlText,
    raw.query,
    raw.data?.sql,
    raw.data?.SQL,
    raw.result?.sql,
    raw.view?.sql,
    raw.detailData?.sql,
    raw.resource_value?.sql,
    raw.resourceValue?.sql
  ]
  for (const item of candidates) {
    if (typeof item === 'string' && item.trim()) return item.trim()
  }
  const content = extractTextFromRaw(raw)
  return extractLastSqlBlock(content)?.sql || ''
}

function extractRowsFromRaw(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  const candidates = [
    raw.rows,
    raw.list,
    raw.data,
    raw.executeResult,
    raw.result,
    raw.result?.rows,
    raw.result?.data,
    raw.view?.rows,
    raw.view?.data,
    raw.resource_value?.rows,
    raw.resource_value?.data,
    raw.resourceValue?.rows,
    raw.resourceValue?.data,
    raw.detailData?.list,
    raw.detailData?.rows,
    raw.data?.rows,
    raw.data?.list,
    raw.data?.detailData?.list
  ]
  for (const item of candidates) {
    if (Array.isArray(item) && item.length) return item
  }
  return []
}

function extractColumnsFromRaw(raw, rows = []) {
  if (!raw || typeof raw !== 'object') return []
  const labels = Array.isArray(raw.detailData?.label) && raw.detailData.label.length
    ? raw.detailData.label
    : Array.isArray(raw.selectColumnDescription)
      ? raw.selectColumnDescription
      : Array.isArray(raw.data?.selectColumnDescription)
        ? raw.data.selectColumnDescription
        : []
  if (Array.isArray(raw.selectColumn) && raw.selectColumn.length) {
    return raw.selectColumn.map((key, index) => ({ prop: key, label: labels[index] || key }))
  }
  if (Array.isArray(raw.data?.selectColumn) && raw.data.selectColumn.length) {
    return raw.data.selectColumn.map((key, index) => ({ prop: key, label: labels[index] || key }))
  }
  return rows.length ? buildColumns(rows, labels) : []
}

function extractStepsFromRaw(raw) {
  if (!raw || typeof raw !== 'object') return ''
  if (raw.type === 'step.meta' || raw.type === 'step') {
    return formatStepItem(raw, 0)
  }
  const candidates = [
    raw.agentSteps,
    raw.steps,
    raw.thoughts,
    raw.toolCalls,
    raw.tool_calls,
    raw.actions,
    raw.observation,
    raw.thought,
    raw.data?.steps,
    raw.data?.thoughts,
    raw.data?.tool_calls,
    raw.data?.action,
    raw.action,
    raw.tool_name,
    raw.tool_input
  ]
  for (const item of candidates) {
    if (typeof item === 'string' && item.trim()) return item.trim()
    if (Array.isArray(item) && item.length) return item.map(formatStepItem).join('\n\n')
    if (item && typeof item === 'object') return JSON.stringify(item, null, 2)
  }
  return ''
}

function formatStepItem(item, index) {
  if (typeof item === 'string') return item
  if (!item || typeof item !== 'object') return String(item)
  const name = item.title || item.name || item.tool || item.tool_name || item.action || item.type || `步骤${index + 1}`
  const thought = item.thought || item.observation || ''
  const input = item.action_input || item.tool_input || item.input || item.args || item.sql || item.query || item.content || ''
  const output = item.output || item.result || item.observation || ''
  return [`【${name}】`, thought, input, output].filter(Boolean).join('\n')
}

function buildColumns(rows, labels = []) {
  if (!rows || !rows.length) return []
  return Object.keys(rows[0] || {}).map((key, index) => ({
    prop: key,
    label: labels[index] || key
  }))
}

function createAskSteps() {
  return [
    { key: 'understand', label: '理解问题', status: 'active' },
    { key: 'connect', label: '调度可用能力', status: 'pending' },
    { key: 'answer', label: '生成回答', status: 'pending' }
  ]
}

function setActiveStep(message, key) {
  if (!message.steps?.length) return
  let found = false
  message.steps.forEach((step) => {
    if (step.key === key) {
      step.status = 'active'
      found = true
      return
    }
    step.status = found ? 'pending' : 'done'
  })
}

function finishSteps(message) {
  if (!message.steps?.length) return
  const status = message.askResultStatus || message.agentOperationStatus
  const unsuccessful = status !== 'SUCCEEDED'
  message.steps.forEach((step) => {
    // A stream ending does not prove that a source was connected or a query succeeded.
    if (unsuccessful) {
      step.status = 'pending'
    } else {
      step.status = 'done'
    }
  })
}

async function streamAskData(payload, callbacks, signal) {
  const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
  const response = await fetch(baseUrl + '/ai/ask-data/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + getToken()
    },
    credentials: 'include',
    signal,
    body: JSON.stringify(payload)
  })

  const contentType = response.headers.get('content-type') || ''
  if (!response.ok || !contentType.includes('text/event-stream')) {
    const error = contentType.includes('json') ? await response.json().catch(() => null) : null
    const code = Number(error?.code || response.status)
    if (code === 401) throw new Error('登录已失效，请重新登录后重试')
    if (code === 403) throw new Error('当前账号无权发起问数或访问所选资源')
    if (code === 429) throw new Error('问数请求过多，请稍后重试')
    throw new Error('问数请求未能建立，请检查当前会话和空间后重试')
  }
  if (!response.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''
    events.forEach((raw) => handleSseEvent(raw, callbacks))
  }
  if (buffer.trim()) {
    handleSseEvent(buffer, callbacks)
  }
}

function handleSseEvent(raw, callbacks) {
  const lines = raw.split(/\r?\n/)
  let eventName = 'message'
  const data = []
  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.substring(6).trim()
    } else if (line.startsWith('data:')) {
      data.push(line.substring(5).replace(/^ /, ''))
    } else if (data.length > 0) {
      const last = data.length - 1
      data[last] = data[last] + '\n' + line
    }
  })
  const text = data.join('\n')
  if (eventName === 'message') {
    callbacks.onMessage?.(normalizeAgentText(text))
  } else if (eventName === 'sql') {
    callbacks.onSql?.(text)
  } else if (eventName === 'steps') {
    callbacks.onSteps?.(normalizeAgentText(text))
  } else if (eventName === 'answer_delta') {
    callbacks.onAnswerDelta?.(JSON.parse(text))
  } else if (eventName === 'result') {
    callbacks.onResult?.(JSON.parse(text))
  } else if (eventName === 'report') {
    callbacks.onReport?.(JSON.parse(text))
  } else if (eventName === 'error') {
    callbacks.onError?.(normalizeAgentText(text))
  }
}

function selectedDatasourceName() {
  return selectedDatasource.value?.datasourceName || ''
}

function syncText(status) {
  if (status === 'SYNCED') return '已同步'
  if (status === 'FAILED') return '同步失败'
  if (status === 'SYNCING') return '同步中'
  return '未同步'
}

function buildTableColumns(columns) {
  return (columns || []).map(c => ({
    title: c.label,
    dataIndex: c.prop,
    key: c.prop,
    width: 130,
    ellipsis: true,
  }))
}

function syncTag(status) {
  if (status === 'SYNCED') return 'success'
  if (status === 'FAILED') return 'error'
  if (status === 'SYNCING') return 'warning'
  return 'default'
}

async function scrollToBottom(force = false) {
  if (!force && !autoScroll.value) return
  const context = askContextVersion
  await nextTick()
  if (context !== askContextVersion || (!force && !autoScroll.value)) return
  const el = messageScrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}
</script>

<style lang="scss" scoped>
.ask-data-page {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 280px;
  grid-template-rows: minmax(0, 1fr);
  height: calc(100dvh - 96px);
  min-height: 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e6eaf0;
  border-radius: 14px;
  background: #ffffff;
  color: #1d2129;
  box-shadow: 0 8px 30px rgba(31, 45, 61, .08);
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px 16px 16px;
  overflow-y: auto;
  min-height: 0;
  min-width: 0;
  background: linear-gradient(180deg, #f8fbff 0%, #f5f7fb 100%);
  border-right: 1px solid #e8edf4;
}

.conversation-history-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  gap: 16px;
  padding: 22px 16px 16px;
  background: #fbfcfe;
  border-left: 1px solid #e8edf4;
}

.history-panel-title {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-panel-title strong,
.history-panel-title span {
  display: block;
}

.history-panel-title strong {
  color: #1d2129;
  font-size: 17px;
  line-height: 24px;
}

.history-panel-title span {
  margin-top: 3px;
  color: #86909c;
  font-size: 12px;
}

.ai-local-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 4px;
  border: 1px solid #e1eaf6;
  border-radius: 12px;
  background: rgba(235, 243, 255, .72);
}

.ai-local-nav-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  height: 34px;
  padding: 0 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #4e5969;
  font-size: 12px;
  cursor: pointer;
  transition: background .2s, color .2s, box-shadow .2s;
}

.ai-local-nav-item:hover {
  color: #165dff;
  background: rgba(255, 255, 255, .78);
}

.ai-local-nav-item.is-active {
  color: #165dff;
  background: #ffffff;
  box-shadow: 0 3px 10px rgba(22, 93, 255, .12);
  font-weight: 600;
}

.ai-local-nav-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  border-radius: 50%;
  background: #165dff;
  box-shadow: 0 0 0 3px rgba(22, 93, 255, .12);
}

.ai-local-nav-dot.skill {
  background: #ff7d00;
  box-shadow: 0 0 0 3px rgba(255, 125, 0, .14);
}

.panel-title strong,
.panel-title span {
  display: block;
}

.panel-title strong {
  font-size: 18px;
  line-height: 26px;
  letter-spacing: .01em;
}

.panel-title span {
  margin-top: 2px;
  color: #86909c;
  font-size: 12px;
}

.new-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 36px;
  border: 0;
  border-radius: 9px;
  background: #ffffff;
  color: #1d2129;
  box-shadow: 0 2px 8px rgba(31, 45, 61, .04);
  font-size: 13px;
  white-space: nowrap;
  word-break: keep-all;
}

.new-chat :deep(.anticon) {
  flex: 0 0 auto;
  margin-inline-end: 0;
}

.new-chat:hover,
.new-chat:focus {
  border-color: transparent;
  background: #f5f8ff;
  color: #165dff;
  box-shadow: 0 3px 10px rgba(22, 93, 255, .1);
}

.chat-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e2eaf4;
  border-radius: 12px;
  background: rgba(255, 255, 255, .82);
  box-shadow: 0 4px 14px rgba(31, 45, 61, .04);
}

.chat-option-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.ontology-option .chat-option-label {
  color: #1d2129;
  font-size: 13px;
  font-weight: 600;
}

.ontology-context {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  border: 1px solid #dce8fb;
  border-radius: 8px;
  background: #f4f8ff;
}

.ontology-context-title,
.bound-datasource-summary > span {
  color: #6b7fa3;
  font-size: 11px;
}

.ontology-context strong,
.bound-datasource-summary strong {
  color: #1d2129;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.ontology-metric-list {
  color: #4e5969;
  font-size: 11px;
}

.ontology-metric-list summary {
  cursor: pointer;
}

.ontology-metric-list ul {
  max-height: 140px;
  margin: 6px 0 0;
  padding-left: 16px;
  overflow-y: auto;
}

.ontology-context.has-error {
  border-color: #ffe2b5;
  background: #fffaf0;
}

.bound-datasource-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 0 10px;
  border-bottom: 1px solid #edf0f5;
}

.chat-option-label {
  color: #86909c;
  font-size: 12px;
  line-height: 18px;
}

.chat-option-hint {
  color: #86909c;
  font-size: 11px;
  line-height: 16px;
}

.chat-option-group :deep(.ant-select),
.chat-option-group :deep(.ant-segmented) {
  width: 100%;
}

.chat-option-group :deep(.ant-select-selector) {
  border-radius: 8px !important;
}

.chat-option-group :deep(.ant-segmented) {
  padding: 3px;
  border-radius: 8px;
  background: #f2f6fc;
}

.chat-option-group :deep(.ant-btn) {
  height: 32px;
  border-radius: 8px;
}

.chat-option-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 2px;
  color: #4e5969;
  font-size: 12px;
}

.chat-option-footer .sql-switch {
  flex: 0 0 auto;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow-y: auto;
}

.conversation-history-panel .conversation-list {
  flex: 1;
  padding-right: 2px;
}

.conversation-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: stretch;
  gap: 4px;
}

.conversation-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-height: 62px;
  padding: 11px 12px;
  border: 1px solid #e7ecf3;
  border-radius: 9px;
  background: #ffffff;
  color: #1d2129;
  cursor: pointer;
  text-align: left;
  transition: border-color .2s, background .2s, box-shadow .2s;
}

.conversation-row:hover .conversation-item,
.conversation-row.active .conversation-item {
  border-color: #94bfff;
  background: #eef6ff;
  box-shadow: 0 4px 12px rgba(22, 93, 255, .09);
}

.conversation-delete {
  align-self: center;
  min-width: 36px;
  padding: 0 4px;
  color: #a9aeb8;
}

.conversation-item span,
.conversation-item small {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-item small {
  color: #86909c;
}

.chat-shell {
  --chat-content-width: 800px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.chat-shell > :not(.message-list) {
  flex: 0 0 auto;
}

.chat-shell > .message-list {
  flex: 1 1 0;
  min-height: 0;
  height: auto;
}

.mode-switch {
  flex: 0 0 auto;
}

.sql-switch {
  flex: 0 0 auto;
}

.template-format-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.template-format-header strong {
  display: block;
  font-size: 16px;
  line-height: 24px;
  color: #303133;
}

.template-format-header p {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

:deep(.template-format-header + .ant-input) {
  font-family: Consolas, Monaco, monospace;
  line-height: 1.55;
}

.option-meta {
  float: right;
  margin-left: 24px;
  color: #a9aeb8;
  font-size: 12px;
}

.message-list {
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior-y: contain;
  scrollbar-gutter: stable both-edges;
  height: 100%;
  padding: 32px 28px 48px;
  background: #ffffff;
  // 流式回复需要立即贴底，避免动画中间位置被判定为离开底部。
  scroll-behavior: auto;
}

.history-loader {
  display: flex;
  justify-content: center;
  max-width: var(--chat-content-width);
  margin: 0 auto 12px;
}

.empty-state {
  max-width: 680px;
  margin: 90px auto 0;
  text-align: center;
}

.empty-state h1 {
  margin: 0 0 10px;
  font-size: 24px;
  line-height: 32px;
}

.empty-state p {
  margin: 0;
  color: #6b7280;
}

.examples {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.examples button {
  min-height: 34px;
  padding: 7px 12px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #ffffff;
  color: #4e5969;
  cursor: pointer;
  transition: border-color .2s, color .2s, box-shadow .2s;
}

.examples button:hover {
  border-color: #165dff;
  color: #165dff;
  box-shadow: 0 1px 4px rgba(22,93,255,.12);
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 0;
  width: 100%;
  max-width: var(--chat-content-width);
  margin: 0 auto 60px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-bubble {
  position: relative;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 32px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.message-bubble.html-report-bubble {
  width: 100%;
  max-width: none;
}

.decision-analysis-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  max-width: 100%;
  margin-bottom: 12px;
  padding: 7px 11px;
  border: 1px solid #cfe0ff;
  border-radius: 8px;
  background: #f2f7ff;
  color: #165dff;
  font-size: 12px;
}

.decision-analysis-banner strong {
  font-weight: 600;
}

.decision-analysis-banner span {
  color: #6b7fa3;
}

.message-actions {
  position: absolute;
  left: -4px;
  bottom: -38px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
}

.message-row.user .message-actions {
  left: auto;
  right: 0;
  opacity: 0;
  pointer-events: none;
  transition: opacity .18s ease;
}

.message-row.user:hover .message-actions,
.message-row.user:focus-within .message-actions {
  opacity: 1;
  pointer-events: auto;
}

.message-actions :deep(.ant-btn) {
  width: 26px;
  height: 26px;
  padding: 0;
  border-radius: 6px;
  color: #86909c;
}

.message-actions :deep(.ant-btn:hover) {
  background: #f0f5ff;
  color: #165dff;
}

.message-actions :deep(.ant-btn-dangerous:hover) {
  background: #fff1f0;
  color: #ff4d4f;
}

.message-row.printing-report .message-actions {
  display: none !important;
}

.message-row:not(.user) .message-bubble {
  width: 100%;
}

.message-row.user .message-bubble {
  width: auto;
  max-width: 80%;
  min-height: 0;
  padding: 11px 16px;
  border: 0;
  border-radius: 18px;
  background: #eaf4ff;
  color: #1f2937;
  box-shadow: none;
}

.message-row.user .message-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.message-row.user .message-content :deep(p),
.message-row.user .message-content :deep(ul),
.message-row.user .message-content :deep(ol) {
  margin-top: 0;
  margin-bottom: 0;
}

.message-content {
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 1.8;
  font-size: 15px;
  color: #1f2937 !important;
}

.message-content :deep(p) {
  margin: 0 0 16px;
}

.message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(pre) {
  white-space: pre;
  overflow-x: auto;
  max-width: 100%;
}

.message-content :deep(.markdown-table-scroll) {
  max-width: 100%;
  margin: 16px 0 20px;
}

.data-table-wrap {
  max-width: 100%;
  margin: 2px 0 10px;
  overflow-x: auto;
}

.data-table-wrap :deep(.ant-table) {
  min-width: 520px;
}

.agent-fold {
  margin-bottom: 8px;
}
.agent-fold details,
.sql-fold details {
  cursor: pointer;
}
.agent-fold summary,
.sql-fold summary {
  font-size: 13px;
  color: #86909c;
  user-select: none;
  padding: 2px 0;
}
.agent-fold pre,
.sql-fold pre {
  font-size: 12px;
  color: #666;
  background: #f7f8fa;
  padding: 8px;
  border-radius: 4px;
  margin: 4px 0 0;
  overflow-x: auto;
  white-space: pre-wrap;
  line-height: 1.5;
}

.sql-fold {
  margin-bottom: 8px;
}

.sql-fold pre {
  color: #1f2937;
}

.sql-fold p {
  margin: 4px 0 0;
  color: #86909c;
  font-size: 12px;
}

.data-empty-wrap {
  margin: 2px 0 10px;
  padding: 8px 0;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
}

.step-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #edf0f5;
}

.step-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 26px;
  padding: 3px 9px 3px 5px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #f7f8fa;
  color: #86909c;
  font-size: 12px;
}

.step-item.active {
  border-color: #94bfff;
  background: #eef6ff;
  color: #165dff;
}

.step-item.done {
  border-color: #a5e0b7;
  background: #eefbf3;
  color: #00a870;
}

.step-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  background: #c9cdd4;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
}

.step-item.active .step-dot {
  background: #165dff;
}

.step-item.done .step-dot {
  background: #00a870;
}

.typing {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 20px;
  color: #86909c;
  font-size: 13px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a9aeb8;
  animation: typing 1.1s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

.typing em {
  margin-left: 6px;
  font-style: normal;
}

@keyframes typing {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.composer-wrap {
  padding: 8px 28px 16px;
  background: #ffffff;
}

.composer {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  width: 100%;
  max-width: var(--chat-content-width);
  margin: 0 auto;
  min-height: 100px;
  padding: 14px 16px 12px;
  border: 1px solid #e0e3e8;
  border-radius: 24px;
  box-shadow: 0 2px 8px rgba(31, 45, 61, .03);
  background: #ffffff;
}

.composer:focus-within {
  border-color: #165dff;
}

.composer :deep(.ant-input) {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
  min-height: 28px !important;
  padding: 2px 4px;
  border: none !important;
  box-shadow: none !important;
  line-height: 24px;
  font-size: 15px;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex: 0 0 auto;
  white-space: nowrap;
}

.send-btn {
  flex: 0 0 auto;
  order: 2;
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 50%;
}

.stop-btn {
  width: 76px;
  border-radius: 18px;
}

.clear-btn {
  flex: 0 0 auto;
  order: 1;
  height: 40px;
  border-radius: 18px;
}

.composer-tip {
  max-width: var(--chat-content-width);
  margin: 6px auto 0;
  color: #a9aeb8;
  font-size: 11px;
  text-align: center;
}

@media (max-width: 960px) {
  .ask-data-page {
    grid-template-columns: 1fr;
    height: calc(100vh - 72px);
    min-height: 0;
    border-radius: 10px;
  }

  .conversation-panel {
    display: none;
  }

  .conversation-history-panel {
    display: none;
  }

  .message-list {
    padding: 20px 14px 30px;
  }

  .composer-wrap {
    padding: 12px 14px 14px;
  }
}
</style>
