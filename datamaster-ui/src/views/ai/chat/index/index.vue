<template>
  <div class="app-container ask-data-page">
    <aside class="conversation-panel">
      <div class="panel-title">
        <strong>对话</strong>
        <span>AI问数</span>
      </div>
      <el-button type="primary" :icon="Plus" class="new-chat" @click="createConversation">
        新对话
      </el-button>
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
          <el-button text class="conversation-delete" @click.stop="removeConversation(item.id)">删除</el-button>
        </div>
      </div>
    </aside>

    <main class="chat-shell">
      <header class="chat-header">
        <div>
          <h2>{{ activeConversation?.title || '新问数对话' }}</h2>
          <p>{{ selectedDatasourceName() || '请选择数据源' }}</p>
        </div>
        <div class="datasource-box">
          <el-segmented
            v-model="form.mode"
            :options="modeOptions"
            class="mode-switch"
          />
          <el-select
            v-model="form.datasourceId"
            placeholder="选择数据源"
            filterable
            clearable
            @change="handleDatasourceChange"
          >
            <el-option
              v-for="item in datasourceList"
              :key="item.id"
              :label="item.datasourceName"
              :value="item.id"
            >
              <span>{{ item.datasourceName }}</span>
              <span class="option-meta">{{ item.datasourceType }} / {{ syncText(item.dbgptSyncStatus) }}</span>
            </el-option>
          </el-select>
          <el-tag :type="syncTag(selectedDatasource?.dbgptSyncStatus)" effect="plain">
            {{ syncText(selectedDatasource?.dbgptSyncStatus) }}
          </el-tag>
          <el-select
            v-model="form.skillId"
            placeholder="选择知识库"
            filterable
            clearable
            @change="handleSkillChange"
          >
            <el-option
              v-for="item in skillList"
              :key="item.id"
              :label="item.skillName"
              :value="item.id"
            />
          </el-select>
          <template v-if="form.mode === 'report'">
            <el-select v-model="form.templateId" placeholder="选择报告模板" filterable clearable>
              <el-option
                v-for="item in templateList"
                :key="item.id"
                :label="templateOptionLabel(item)"
                :value="item.id"
              />
            </el-select>
            <el-button icon="Document" @click="openTemplateFormat">
              模板格式
            </el-button>
          </template>
          <el-switch
            v-model="form.returnSql"
            active-text="返回SQL"
            inactive-text=""
            class="sql-switch"
          />
        </div>
      </header>

      <section ref="messageScrollRef" class="message-list" @scroll="handleMessageScroll">
        <div v-if="messageWindow.hasBefore" class="history-loader">
          <el-button text :loading="messageWindow.loadingBefore" @click="loadMoreMessages('before')">
            加载更早5条
          </el-button>
        </div>
        <div v-if="activeMessages.length === 0" class="empty-state">
          <h1>想查什么，直接问</h1>
          <p>选择数据源后，AI问数会使用已同步的数据源和问数 Skill 进行回答。</p>
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
          :class="message.role"
        >
          <div class="avatar">{{ message.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-bubble">
            <el-button
              text
              class="message-delete"
              @click="removeMessage(message)"
            >
              删除
            </el-button>
            <div v-if="message.agentSteps" class="agent-fold">
              <details>
                <summary>思考过程</summary>
                <pre>{{ message.agentSteps }}</pre>
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
            <ReportTemplateRenderer
              v-if="message.reportTemplate && message.reportData"
              :template="message.reportTemplate"
              :data="message.reportData"
            />
            <div v-else-if="message.tableRows?.length" class="data-table-wrap">
              <el-table :data="message.tableRows" border size="small" max-height="320">
                <el-table-column
                  v-for="column in message.tableColumns"
                  :key="column.prop"
                  :prop="column.prop"
                  :label="column.label"
                  min-width="130"
                  show-overflow-tooltip
                />
              </el-table>
            </div>
            <div v-else-if="message.queryExecuted" class="data-empty-wrap">
              <el-empty description="暂无查询结果" :image-size="56" />
            </div>
            <MarkdownView
              v-if="message.displayContent || message.content"
              class="message-content"
              :content="message.displayContent || message.content"
            />
            <div
              v-if="!message.reportTemplate && !message.reportData && !message.tableRows?.length && !message.displayContent && !message.content"
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
          <el-button text :loading="messageWindow.loadingAfter" @click="loadMoreMessages('after')">
            加载更新5条
          </el-button>
        </div>
      </section>

      <footer class="composer-wrap">
        <div class="composer">
          <el-input
            v-model="prompt"
            type="textarea"
            resize="none"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="输入你的数据问题"
            @keydown.enter.prevent="handleEnter"
            @keydown.shift.enter.stop
          />
          <el-button
            type="primary"
            :icon="Promotion"
            class="send-btn"
            :loading="sending"
            @click="sendMessage"
          />
          <el-button class="clear-btn" :disabled="!activeMessages.length" @click="clearMessages">
            清空
          </el-button>
        </div>
        <div class="composer-tip">Enter 发送，Shift+Enter 换行</div>
      </footer>
    </main>

    <el-dialog v-model="templateFormatOpen" title="报告模板格式" width="960px" append-to-body>
      <div class="template-format-header">
        <div>
          <strong>{{ selectedSkill?.skillName || '通用报告模板' }}</strong>
          <p>复制后按报告业务修改模板编码、字段、布局、样式和提示语，再到 Skill 模板中上传。</p>
        </div>
        <el-button type="primary" icon="CopyDocument" @click="copyTemplateFormat">复制模板</el-button>
      </div>
      <el-input
        v-model="templateFormatText"
        type="textarea"
        :rows="26"
        readonly
        resize="none"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import MarkdownView from '@/components/MarkdownView/index.vue'
import ReportTemplateRenderer from './components/ReportTemplateRenderer.vue'
import { askDataDbgptReport } from '@/api/ai/askData'
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
const skillList = ref([])
const templateList = ref([])
const conversations = ref([])
const activeConversationId = ref(null)
const prompt = ref('')
const sending = ref(false)
const messageScrollRef = ref()
const templateFormatOpen = ref(false)
const templateFormatText = ref('')
const userStore = useUserStore()
const sessionReady = ref(false)
const syncingSession = ref(false)
const messageWindow = reactive({
  hasBefore: false,
  hasAfter: false,
  loadingBefore: false,
  loadingAfter: false
})

const form = reactive({
  datasourceId: null,
  mode: 'qa',
  skillId: null,
  templateId: null,
  returnSql: false
})

const modeOptions = [
  { label: '问答', value: 'qa' },
  { label: '报告', value: 'report' }
]

const examples = [
  '查询下前5条订单数据',
  '按城市统计订单金额',
  '最近7天订单数量趋势'
]

const activeConversation = computed(() =>
  conversations.value.find((item) => item.id === activeConversationId.value)
)

const activeMessages = computed(() => activeConversation.value?.messages || [])

const selectedDatasource = computed(() =>
  datasourceList.value.find((item) => item.id === form.datasourceId)
)

const selectedSkill = computed(() =>
  skillList.value.find((item) => item.id === form.skillId)
)

onMounted(async () => {
  await loadDatasources()
  await loadSkills()
  await loadConversations()
})

watch(() => form.mode, () => {
  refreshReportTemplates()
  syncActiveSessionSelection()
})

watch(() => [form.datasourceId, form.skillId, form.templateId, form.returnSql], () => {
  syncActiveSessionSelection()
})

watch(() => userStore.projectId, async () => {
  await loadConversations()
})

async function loadDatasources() {
  const res = await request({
    url: '/ast/dataSource/getDatasourceList',
    method: 'get',
    params: { pageSize: 200 }
  })
  datasourceList.value = res.data || []
}

async function loadSkills() {
  const res = await listSkill({
    pageNum: 1,
    pageSize: 200,
    status: 'PUBLISHED'
  })
  skillList.value = res.data?.rows || []
}

async function handleDatasourceChange() {
  await refreshReportTemplates()
}

async function handleSkillChange() {
  await refreshReportTemplates()
}

async function refreshReportTemplates() {
  const currentTemplateId = form.templateId
  templateList.value = []
  if (form.mode !== 'report' || !form.datasourceId || !form.skillId) {
    form.templateId = null
    return
  }
  const res = await listSkillReportTemplates(form.skillId)
  templateList.value = res.data || []
  if (currentTemplateId && templateList.value.some((item) => item.id === currentTemplateId)) {
    form.templateId = currentTemplateId
    return
  }
  const latestTemplate = latestReportTemplate(templateList.value)
  form.templateId = latestTemplate ? latestTemplate.id : null
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
    ElMessage.success('模板格式已复制')
  } catch (error) {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

async function loadConversations() {
  sessionReady.value = false
  const res = await listAskSessions({ limit: 10 })
  conversations.value = (res.data || []).map(normalizeSession)
  if (conversations.value.length) {
    await selectConversation(conversations.value[0].id)
  } else {
    await createConversation()
  }
  sessionReady.value = true
}

async function createConversation() {
  const res = await createAskSession(buildSessionPayload({ title: '新问数对话' }))
  const session = normalizeSession(res.data)
  conversations.value = [session, ...conversations.value.filter((item) => item.id !== session.id)].slice(0, 10)
  await selectConversation(session.id)
}

async function selectConversation(id) {
  activeConversationId.value = id
  const session = activeConversation.value
  if (!session) return
  await applySessionToForm(session)
  await loadSessionMessages(id)
}

async function removeConversation(id) {
  await ElMessageBox.confirm('确认删除该会话及全部聊天记录？', '删除会话', { type: 'warning' })
  await deleteAskSession(id, currentProjectParams())
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
  const res = await listAskMessages(sessionId, { limit: 10 })
  const data = res.data || {}
  const session = conversations.value.find((item) => item.id === sessionId)
  if (!session) return
  session.messages = (data.rows || []).map(normalizeMessage)
  messageWindow.hasBefore = Boolean(data.hasBefore)
  messageWindow.hasAfter = Boolean(data.hasAfter)
  await scrollToBottom()
}

async function loadMoreMessages(direction) {
  const session = activeConversation.value
  if (!session || !session.messages.length) return
  const isBefore = direction === 'before'
  if (isBefore && messageWindow.loadingBefore) return
  if (!isBefore && messageWindow.loadingAfter) return
  if (isBefore) messageWindow.loadingBefore = true
  else messageWindow.loadingAfter = true
  try {
    const params = { limit: 5 }
    if (isBefore) params.beforeId = session.messages[0].id
    else params.afterId = session.messages[session.messages.length - 1].id
    const res = await listAskMessages(session.id, params)
    const data = res.data || {}
    const rows = (data.rows || []).map(normalizeMessage)
    if (isBefore) {
      session.messages = [...rows, ...session.messages].slice(0, 10)
      messageWindow.hasBefore = Boolean(data.hasBefore)
      messageWindow.hasAfter = true
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
  if (el.scrollTop <= 8 && messageWindow.hasBefore) {
    loadMoreMessages('before')
  } else if (el.scrollHeight - el.scrollTop - el.clientHeight <= 8 && messageWindow.hasAfter) {
    loadMoreMessages('after')
  }
}

async function removeMessage(message) {
  const session = activeConversation.value
  if (!session || !message?.id) return
  await deleteAskMessage(session.id, message.id, currentProjectParams())
  session.messages = session.messages.filter((item) => item.id !== message.id)
}

async function clearMessages() {
  const session = activeConversation.value
  if (!session) return
  await ElMessageBox.confirm('确认清空当前会话的聊天记录？', '清空聊天记录', { type: 'warning' })
  await clearAskMessages(session.id, currentProjectParams())
  session.messages = []
  messageWindow.hasBefore = false
  messageWindow.hasAfter = false
}

function normalizeSession(row) {
  return {
    ...row,
    title: row?.title || '新问数对话',
    messages: row?.messages || []
  }
}

function normalizeMessage(row) {
  if (row?.payloadJson) {
    try {
      return {
        ...JSON.parse(row.payloadJson),
        id: row.id,
        role: row.role,
        content: row.content || JSON.parse(row.payloadJson).content || '',
        displayContent: row.displayContent || JSON.parse(row.payloadJson).displayContent || ''
      }
    } catch {}
  }
  return {
    id: row.id,
    role: row.role,
    content: row.content || '',
    displayContent: row.displayContent || '',
    agentSteps: '',
    executeError: '',
    queryExecuted: false,
    tableRows: [],
    tableColumns: [],
    reportTemplate: null,
    reportData: null,
    returnedSql: '',
    sqlUnavailable: false,
    steps: []
  }
}

async function applySessionToForm(session) {
  syncingSession.value = true
  try {
    form.mode = session.mode || 'qa'
    form.datasourceId = session.datasourceId || null
    form.skillId = session.skillId || null
    form.templateId = session.templateId || null
    form.returnSql = Boolean(session.returnSql)
    await refreshReportTemplates()
  } finally {
    syncingSession.value = false
  }
}

function buildSessionPayload(extra = {}) {
  return {
    title: activeConversation.value?.title || extra.title || '新问数对话',
    mode: form.mode,
    datasourceId: form.datasourceId,
    datasourceName: selectedDatasourceName(),
    skillId: form.skillId,
    templateId: form.templateId,
    returnSql: form.returnSql,
    projectId: userStore.projectId || null,
    projectCode: userStore.projectCode || '',
    ...extra
  }
}

async function syncActiveSessionSelection() {
  if (syncingSession.value || !sessionReady.value || !activeConversationId.value) return
  const res = await updateAskSession(activeConversationId.value, buildSessionPayload())
  const updated = normalizeSession(res.data)
  const index = conversations.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) {
    conversations.value[index] = {
      ...conversations.value[index],
      ...updated,
      messages: conversations.value[index].messages
    }
  }
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
  }, currentProjectParams())
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
    executeError: message.executeError || '',
    queryExecuted: Boolean(message.queryExecuted),
    tableRows: message.tableRows || [],
    tableColumns: message.tableColumns || [],
    reportTemplate: message.reportTemplate || null,
    reportData: message.reportData || null,
    returnedSql: message.returnedSql || '',
    sqlUnavailable: Boolean(message.sqlUnavailable),
    steps: message.steps || []
  }
}

function currentProjectParams() {
  return {
    projectId: userStore.projectId || null,
    projectCode: userStore.projectCode || ''
  }
}

function trimActiveMessagesToLatest() {
  const session = activeConversation.value
  if (session && session.messages.length > 10) {
    session.messages = session.messages.slice(-10)
    messageWindow.hasBefore = true
  }
}

function handleEnter(event) {
  if (event.shiftKey) {
    prompt.value += '\n'
    return
  }
  sendMessage()
}

async function sendMessage() {
  if (sending.value) return

  const question = prompt.value.trim()
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }
  if (!form.datasourceId) {
    ElMessage.warning('请先选择数据源')
    return
  }
  if (form.mode === 'report' && !form.skillId) {
    ElMessage.warning('请先选择知识库')
    return
  }
  if (!activeConversation.value) {
    await createConversation()
  }

  const conversation = activeConversation.value
  if (conversation.messages.length === 0) {
    conversation.title = question.slice(0, 20)
    await updateAskSession(conversation.id, buildSessionPayload({ title: conversation.title }))
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

  const assistantMessage = reactive({
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    displayContent: '',
    agentSteps: '',
    executeError: '',
    queryExecuted: false,
    tableRows: [],
    tableColumns: [],
    reportTemplate: null,
    reportData: null,
    returnedSql: '',
    sqlUnavailable: false,
    steps: createAskSteps()
  })
  conversation.messages.push(assistantMessage)
  trimActiveMessagesToLatest()

  prompt.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    setActiveStep(assistantMessage, 'connect')
    if (form.mode === 'report') {
      setActiveStep(assistantMessage, 'answer')
      await generateReportMessage(question, assistantMessage)
    } else {
      await streamAskData({
        question,
        datasourceId: form.datasourceId,
        chatMode: 'chat_with_db_qa',
        skillIds: form.skillId ? [form.skillId] : [],
        returnSql: form.returnSql,
        projectId: userStore.projectId || null,
        projectCode: userStore.projectCode || ''
      }, {
        onMessage(chunk) {
          if (!assistantMessage.content) {
            setActiveStep(assistantMessage, 'answer')
          }
          assistantMessage.content += chunk
          scrollToBottom()
        },
        onSql(sql) {
          appendSqlToMessage(assistantMessage, sql)
          scrollToBottom()
        },
        onError(message) {
          assistantMessage.content = message || 'AI问数调用失败'
          finishSteps(assistantMessage)
        }
      })
      hydrateStructuredData(assistantMessage)
    }
    finishSteps(assistantMessage)
    await persistMessage(assistantMessage)
  } catch (error) {
    assistantMessage.content = error?.message || 'AI问数调用失败'
    finishSteps(assistantMessage)
    await persistMessage(assistantMessage)
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function generateReportMessage(question, assistantMessage) {
  const res = await askDataDbgptReport({
    question,
    datasourceId: form.datasourceId,
    skillId: form.skillId,
    templateId: form.templateId,
    returnSql: form.returnSql
  })
  const data = res.data || {}
  if (data.qualityWarning) {
    assistantMessage.content = data.qualityWarning
  }
  if (data.templateContent && data.reportData) {
    assistantMessage.reportTemplate = JSON.parse(data.templateContent)
    assistantMessage.reportData = data.reportData
    const sql = data.sql || data.reportData?.sql
    assistantMessage.displayContent = form.returnSql && sql
      ? `校验SQL：\n\`\`\`sql\n${sql}\n\`\`\``
      : ''
    return
  }
  assistantMessage.content = data.rawReply || data.qualityWarning || '报告生成失败，未返回结构化数据'
  hydrateStructuredData(assistantMessage)
}

function hydrateStructuredData(message) {
  const parsed = parseStructuredContent(message.content)
  if (!parsed) {
    message.displayContent = message.content
    extractAgentSteps(message)
    return
  }

  message.displayContent = parsed.text || ''
  message.tableRows = parsed.rows || []
  message.tableColumns = parsed.columns?.length ? parsed.columns : buildColumns(parsed.rows)
  appendSqlMarkdownFromContent(message)
  extractAgentSteps(message)
}

function appendSqlToMessage(message, sql) {
  const value = (sql || '').trim()
  message.returnedSql = value
  message.sqlUnavailable = !value
  const block = value
    ? `校验SQL：\n\`\`\`sql\n${value}\n\`\`\``
    : '> 未能生成可校验的 SELECT SQL。'
  const current = message.displayContent || message.content || ''
  if (value && current.includes(value)) return
  if (!value && current.includes('未能生成可校验的 SELECT SQL')) return
  message.displayContent = `${current ? `${current}\n\n` : ''}${block}`
}

function appendSqlMarkdownFromContent(message) {
  if (message.sqlUnavailable) {
    const current = message.displayContent || ''
    if (current.includes('未能生成可校验的 SELECT SQL')) return
    message.displayContent = `${current ? `${current}\n\n` : ''}> 未能生成可校验的 SELECT SQL。`
    return
  }
  const sqlBlock = message.returnedSql
    ? {
        sql: message.returnedSql,
        markdown: `\`\`\`sql\n${message.returnedSql}\n\`\`\``
      }
    : extractLastSqlBlock(message.content)
  if (!sqlBlock) return
  const current = message.displayContent || ''
  if (current.includes(sqlBlock.sql)) return
  message.displayContent = `${current ? `${current}\n\n` : ''}校验SQL：\n${sqlBlock.markdown}`
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
  const parsed = tryParseJson(content.trim())
  if (!parsed) return null
  const raw = normalizeStructuredPayload(parsed)

  let text = raw.msg || raw.message || raw.content || raw.summary || raw.explanation || ''
  let rows = []
  let columns = []

  if (Number(raw.code) === 500 && !text) {
    text = '对话异常'
  }
  if (raw.qualityWarning) {
    text = `${text ? `${text}\n\n` : ''}${raw.qualityWarning}`
  }

  if (Array.isArray(parsed)) {
    rows = parsed
  } else if (Array.isArray(raw.rows)) {
    rows = raw.rows
  } else if (Array.isArray(raw.list)) {
    rows = raw.list
  } else if (Array.isArray(raw.data)) {
    rows = raw.data
  } else if (Array.isArray(raw.executeResult)) {
    rows = raw.executeResult
  } else if (Array.isArray(raw.detailData?.list)) {
    rows = raw.detailData.list
    const labels = Array.isArray(raw.detailData.label) ? raw.detailData.label : []
    columns = buildColumns(rows, labels)
  }

  if (!columns.length && Array.isArray(raw.selectColumn) && raw.selectColumn.length) {
    const labels = Array.isArray(raw.detailData?.label) && raw.detailData.label.length
      ? raw.detailData.label
      : Array.isArray(raw.selectColumnDescription)
        ? raw.selectColumnDescription
        : []
    columns = raw.selectColumn.map((key, index) => ({
      prop: key,
      label: labels[index] || key
    }))
  }

  if (raw.sql && !text.includes(raw.sql)) {
    text = `${text ? `${text}\n\n` : ''}\`\`\`sql\n${raw.sql}\n\`\`\``
  }

  if (!rows.length && !text) return null
  return { text, rows, columns }
}

function normalizeStructuredPayload(raw) {
  if (!raw || Array.isArray(raw) || typeof raw !== 'object') return raw
  if (raw.detailData || raw.chatData || raw.sql || raw.msg) return raw
  if (raw.data && typeof raw.data === 'object' && !Array.isArray(raw.data)) {
    return {
      ...raw.data,
      msg: raw.msg || raw.message || raw.data.msg || raw.data.message,
      code: raw.code ?? raw.data.code
    }
  }
  return raw
}

function tryParseJson(content) {
  try {
    return JSON.parse(content)
  } catch {
    const match = content.match(/\{[\s\S]*\}/)
    if (!match) return null
    try {
      return JSON.parse(match[0])
    } catch {
      return null
    }
  }
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
    { key: 'connect', label: '连接数据源', status: 'pending' },
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
  message.steps.forEach((step) => {
    step.status = 'done'
  })
}

async function streamAskData(payload, callbacks) {
  const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
  const response = await fetch(baseUrl + '/ai/ask-data/dbgpt/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + getToken()
    },
    credentials: 'include',
    body: JSON.stringify(payload)
  })

  if (!response.ok) {
    throw new Error(await response.text() || 'AI问数请求失败')
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
    callbacks.onMessage?.(text)
  } else if (eventName === 'sql') {
    callbacks.onSql?.(text)
  } else if (eventName === 'error') {
    callbacks.onError?.(text)
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

function syncTag(status) {
  if (status === 'SYNCED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'SYNCING') return 'warning'
  return 'info'
}

async function scrollToBottom() {
  await nextTick()
  await new Promise((r) => requestAnimationFrame(r))
  const el = messageScrollRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}
</script>

<style lang="scss" scoped>
.ask-data-page {
  display: grid;
  grid-template-columns: 228px minmax(0, 1fr);
  height: calc(100vh - 124px);
  min-height: 560px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #ffffff;
  color: #1d2129;
  box-shadow: 0 1px 3px rgba(0,0,0,.06), 0 1px 2px rgba(0,0,0,.04);
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: #f7f8fa;
  border-right: 1px solid #e5e6eb;
}

.panel-title strong,
.panel-title span {
  display: block;
}

.panel-title strong {
  font-size: 16px;
  line-height: 22px;
}

.panel-title span {
  margin-top: 2px;
  color: #86909c;
  font-size: 12px;
}

.new-chat {
  width: 100%;
  border-radius: 6px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
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
  min-height: 54px;
  padding: 9px 10px;
  border: 1px solid #edf0f5;
  border-radius: 6px;
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
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
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
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid #e5e6eb;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(0,0,0,.03);
}

.chat-header h2 {
  margin: 0 0 3px;
  font-size: 16px;
  line-height: 22px;
}

.chat-header p {
  margin: 0;
  color: #86909c;
  font-size: 13px;
}

.datasource-box {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 760px;
  min-width: 430px;
}

.datasource-box :deep(.el-select) {
  flex: 1;
  min-width: 160px;
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

:deep(.template-format-header + .el-textarea .el-textarea__inner) {
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
  height: 100%;
  padding: 24px 20px 36px;
  background: #f5f7fa;
}

.history-loader {
  display: flex;
  justify-content: center;
  max-width: 960px;
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
  gap: 10px;
  max-width: 960px;
  margin: 0 auto 18px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #165dff;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.message-row.user .avatar {
  background: #00a870;
}

.message-bubble {
  position: relative;
  max-width: min(760px, 82%);
  min-height: 38px;
  padding: 11px 13px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(0,0,0,.04);
}

.message-delete {
  position: absolute;
  top: 2px;
  right: 6px;
  min-width: 32px;
  padding: 0;
  color: #a9aeb8;
  opacity: 0;
}

.message-bubble:hover .message-delete {
  opacity: 1;
}

.message-row:not(.user) .message-bubble {
  border-top-left-radius: 2px;
}

.message-row.user .message-bubble {
  border: none;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.message-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.72;
  font-size: 14px;
  color: #1f2937 !important;
}

.message-content :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  margin: 6px 0 10px;
  color: #1f2937;
}

.message-content :deep(th) {
  background: #f7f8fa;
}

.data-table-wrap {
  max-width: 100%;
  margin: 2px 0 10px;
  overflow-x: auto;
}

.data-table-wrap :deep(.el-table) {
  min-width: 520px;
}

.agent-fold {
  margin-bottom: 8px;
}
.agent-fold details {
  cursor: pointer;
}
.agent-fold summary {
  font-size: 13px;
  color: #86909c;
  user-select: none;
  padding: 2px 0;
}
.agent-fold pre {
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
  padding: 12px 20px 14px;
  border-top: 1px solid #e5e6eb;
  background: #ffffff;
  box-shadow: 0 -1px 2px rgba(0,0,0,.03);
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 960px;
  margin: 0 auto;
  padding: 8px 8px 8px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #ffffff;
}

.composer:focus-within {
  border-color: #165dff;
}

.composer :deep(.el-textarea__inner) {
  min-height: 24px !important;
  padding: 6px 0;
  border: none !important;
  box-shadow: none !important;
  line-height: 22px;
}

.send-btn {
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 6px;
}

.clear-btn {
  flex: 0 0 auto;
  height: 36px;
  border-radius: 6px;
}

.composer-tip {
  max-width: 960px;
  margin: 6px auto 0;
  color: #a9aeb8;
  font-size: 12px;
}

@media (max-width: 960px) {
  .ask-data-page {
    grid-template-columns: 1fr;
    height: calc(100vh - 104px);
  }

  .conversation-panel {
    display: none;
  }

  .chat-header {
    flex-direction: column;
    align-items: stretch;
  }

  .datasource-box {
    min-width: 0;
  }
}
</style>
