<template>
  <div class="app-container ask-data-page" :class="{ fullscreen: isFullscreen }">
    <aside class="conversation-panel">
      <div class="panel-title">
        <strong>对话</strong>
        <span>AI问数</span>
      </div>
      <a-button type="primary" :icon="h(PlusOutlined)" class="new-chat" @click="createConversation">
        新对话
      </a-button>
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

    <main class="chat-shell">
      <header class="chat-header">
        <div>
          <h2>{{ activeConversation?.title || '新问数对话' }}</h2>
          <p>{{ selectedDatasourceName() || '请选择数据源' }}</p>
        </div>
        <div class="datasource-box">
          <a-segmented
            v-model:value="form.mode"
            :options="modeOptions"
            class="mode-switch"
          />
          <a-select
            v-model:value="form.datasourceId"
            placeholder="选择数据源"
            show-search
            allow-clear
            @change="handleDatasourceChange"
          >
            <a-select-option
              v-for="item in datasourceList"
              :key="item.id"
              :value="item.id"
            >
              <span>{{ item.datasourceName }}</span>
              <span class="option-meta">{{ item.datasourceType }} / {{ syncText(item.dbgptSyncStatus) }}</span>
            </a-select-option>
          </a-select>
          <a-tag :color="syncTag(selectedDatasource?.dbgptSyncStatus)">
            {{ syncText(selectedDatasource?.dbgptSyncStatus) }}
          </a-tag>
          <a-select
            v-model:value="form.skillId"
            placeholder="选择知识库"
            show-search
            allow-clear
            @change="handleSkillChange"
          >
            <a-select-option
              v-for="item in skillList"
              :key="item.id"
              :value="item.id"
            >
              {{ item.skillName }}
            </a-select-option>
          </a-select>
          <template v-if="form.mode === 'report'">
            <a-select v-model:value="form.templateId" placeholder="选择报告模板" show-search allow-clear>
              <a-select-option
                v-for="item in templateList"
                :key="item.id"
                :value="item.id"
              >
                {{ templateOptionLabel(item) }}
              </a-select-option>
            </a-select>
            <a-button :icon="h(FileTextOutlined)" @click="openTemplateFormat">
              模板格式
            </a-button>
          </template>
          <a-switch
            v-model:checked="form.returnSql"
            checked-children="返回SQL"
            un-checked-children=""
            class="sql-switch"
          />
          <a-button :icon="h(isFullscreen ? AimOutlined : FullscreenOutlined)" @click="toggleFullscreen">
            {{ isFullscreen ? '退出全屏' : '全屏' }}
          </a-button>
        </div>
      </header>

      <section ref="messageScrollRef" class="message-list" @scroll="handleMessageScroll">
        <div v-if="messageWindow.hasBefore" class="history-loader">
          <a-button type="text" :loading="messageWindow.loadingBefore" @click="loadMoreMessages('before')">
            加载更早5条
          </a-button>
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
          :class="[message.role, { 'printing-report': printingMessageId === message.id }]"
        >
          <div class="avatar">{{ message.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-bubble" :class="{ 'html-report-bubble': isFullHtmlMessage(message) }">
            <a-button
              v-if="isExportableReportMessage(message)"
              type="text"
              class="message-export"
              @click.stop="exportMessagePdf(message)"
            >
              导出PDF
            </a-button>
            <a-button
              type="text"
              class="message-delete"
              @click="removeMessage(message)"
            >
              删除
            </a-button>
            <div v-if="message.agentSteps" class="agent-fold">
              <details>
                <summary>执行步骤</summary>
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
            <ReportTemplateRenderer
              v-if="message.reportTemplate && message.reportData"
              :template="message.reportTemplate"
              :data="message.reportData"
            />
            <div v-else-if="message.tableRows?.length" class="data-table-wrap">
              <a-table :data-source="message.tableRows" :columns="buildTableColumns(message.tableColumns)" :pagination="false" size="small" :scroll="{ y: 320 }" />
            </div>
            <div v-else-if="message.queryExecuted" class="data-empty-wrap">
              <a-empty description="暂无查询结果" />
            </div>
            <MarkdownView
              v-if="!message.reportTemplate && !message.reportData && !message.tableRows?.length && (message.displayContent || message.content)"
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
          <a-button type="text" :loading="messageWindow.loadingAfter" @click="loadMoreMessages('after')">
            加载更新5条
          </a-button>
        </div>
      </section>

      <footer class="composer-wrap">
        <div class="composer">
          <a-input
            v-model:value="prompt"
            type="textarea"
            :auto-size="{ minRows: 1, maxRows: 6 }"
            placeholder="输入你的数据问题"
            @keydown.enter.prevent="handleEnter"
            @keydown.shift.enter.stop
          />
          <a-button
            type="primary"
            :icon="h(SendOutlined)"
            class="send-btn"
            :loading="sending"
            @click="sendMessage"
          />
          <a-button class="clear-btn" :disabled="!activeMessages.length" @click="clearMessages">
            清空
          </a-button>
        </div>
        <div class="composer-tip">Enter 发送，Shift+Enter 换行</div>
      </footer>
    </main>

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
import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { AimOutlined, CopyOutlined, FileTextOutlined, FullscreenOutlined, PlusOutlined, SendOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
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
const isFullscreen = ref(false)
const printingMessageId = ref(null)
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

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  nextTick(scrollToBottom)
}

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

watch(() => userStore.spaceId, async () => {
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
    message.success('模板格式已复制')
  } catch (error) {
    message.error('复制失败，请手动选择复制')
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
  await deleteAskMessage(session.id, message.id, currentSpaceParams())
  session.messages = session.messages.filter((item) => item.id !== message.id)
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
    title: row?.title || '新问数对话',
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
        content: payload.content || row.content || '',
        displayContent: payload.displayContent || row.displayContent || ''
      }
      normalizeReportPayload(message)
      rebuildDisplayContentFromContent(message)
      return message
    } catch {}
  }
  message = {
    id: row.id,
    role: row.role,
    content: row.content || '',
    displayContent: row.displayContent || '',
    agentSteps: '',
    queryExecuted: false,
    tableRows: [],
    tableColumns: [],
    reportTemplate: null,
    reportData: null,
    returnedSql: '',
    sqlUnavailable: false,
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
  if (!message || message.role !== 'assistant' || !message.content) return
  if (message.displayContent) {
    message.displayContent = removeValidationSqlSection(message.displayContent).trim()
  }
  const mixed = stripNativeJsonBlocks(message.content)
  if (!mixed.text || mixed.text === message.content) return
  if (!message.displayContent || isSqlOnlyDisplay(message.displayContent)) {
    message.displayContent = mixed.text
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
  if (!message || message.role === 'user') return false
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
  const raw = activeConversation.value?.title || message?.title || 'AI问数报告'
  const safe = raw.replace(/[\\/:*?"<>|]/g, '_').slice(0, 60) || 'AI问数报告'
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
    spaceId: userStore.spaceId || null,
    spaceCode: userStore.spaceCode || '',
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
    message.warning('请输入问题')
    return
  }
  if (!form.datasourceId) {
    message.warning('请先选择数据源')
    return
  }
  if (form.mode === 'report' && !form.skillId) {
    message.warning('请先选择知识库')
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
        spaceId: userStore.spaceId || null,
        spaceCode: userStore.spaceCode || ''
      }, {
        onMessage(chunk) {
          if (!assistantMessage.content) {
            setActiveStep(assistantMessage, 'answer')
          }
          assistantMessage.content = mergeStreamContent(assistantMessage.content, chunk)
          scrollToBottom()
        },
        onSql(sql) {
          appendSqlToMessage(assistantMessage, sql)
          scrollToBottom()
        },
        onSteps(steps) {
          appendAgentStepsToMessage(assistantMessage, steps)
          scrollToBottom()
        },
        onError(message) {
          assistantMessage.content = message || 'AI问数调用失败'
          finishSteps(assistantMessage)
        }
      })
      hydrateStructuredData(assistantMessage)
      hydrateNativeExecutionData(assistantMessage)
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
    spaceId: userStore.spaceId || null,
    spaceCode: userStore.spaceCode || '',
    skillId: form.skillId,
    templateId: form.templateId,
    returnSql: form.returnSql
  })
  const data = res.data || {}
  if (data.qualityWarning) {
    assistantMessage.content = data.qualityWarning
  }
  if (data.templateContent && data.reportData) {
    const reportTemplate = parseMaybeJson(data.templateContent) || data.templateContent
    const reportData = parseMaybeJson(data.reportData) || data.reportData
    const sql = data.sql || reportData?.sql
    assistantMessage.returnedSql = sql || ''
    assistantMessage.displayContent = ''
    if (reportTemplate && reportData) {
      assistantMessage.reportTemplate = reportTemplate
      assistantMessage.reportData = reportData
      return
    }
    assistantMessage.content = '报告生成失败，模板或报告数据格式不正确'
    assistantMessage.displayContent = assistantMessage.content
    return
  }
  assistantMessage.content = data.rawReply || data.qualityWarning || '报告生成失败，未返回结构化数据'
  assistantMessage.displayContent = assistantMessage.content
}

function hydrateStructuredData(message) {
  hydrateMixedNativeContent(message)
  const parsed = parseStructuredContent(message.content)
  if (!parsed) {
    message.displayContent = message.displayContent || message.content
    extractAgentSteps(message)
    return
  }

  message.displayContent = message.displayContent || parsed.text || ''
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
    message.displayContent = normalized.text
  }
}

function mergeStreamContent(current, chunk) {
  if (!chunk) return current || ''
  const before = current || ''
  if (looksLikeCompleteJson(chunk) && looksLikeCompleteJson(before)) {
    return chunk
  }
  if (looksLikeCompleteJson(chunk) && extractJsonObjects(before).length) {
    return chunk
  }
  return before + chunk
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
    message.agentSteps = value
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
  } else if (eventName === 'steps') {
    callbacks.onSteps?.(text)
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

.ask-data-page.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 2000;
  grid-template-columns: minmax(0, 1fr);
  height: 100vh;
  min-height: 0;
  border: none;
  border-radius: 0;
}

.ask-data-page.fullscreen .conversation-panel {
  display: none;
}

.ask-data-page.fullscreen .chat-header {
  padding: 12px 24px;
}

.ask-data-page.fullscreen .datasource-box {
  max-width: none;
}

.ask-data-page.fullscreen .message-row,
.ask-data-page.fullscreen .history-loader,
.ask-data-page.fullscreen .composer,
.ask-data-page.fullscreen .composer-tip {
  max-width: 1480px;
}

.ask-data-page.fullscreen .message-bubble.html-report-bubble {
  width: calc(100vw - 120px);
  max-width: none;
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

.datasource-box :deep(.ant-select) {
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

.message-bubble.html-report-bubble {
  width: calc(100vw - 360px);
  max-width: 1180px;
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

.message-export {
  position: absolute;
  top: 2px;
  right: 50px;
  min-width: 54px;
  padding: 0;
  color: #165dff;
  opacity: 0;
}

.message-bubble:hover .message-delete,
.message-bubble:hover .message-export {
  opacity: 1;
}

.message-row.printing-report .message-delete,
.message-row.printing-report .message-export {
  display: none !important;
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

.composer :deep(.ant-input) {
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
