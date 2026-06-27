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
        <button
          v-for="item in conversations"
          :key="item.id"
          class="conversation-item"
          :class="{ active: item.id === activeConversationId }"
          @click="selectConversation(item.id)"
        >
          <span>{{ item.title }}</span>
          <small>{{ item.datasourceName || '未选择数据源' }}</small>
        </button>
      </div>
    </aside>

    <main class="chat-shell">
      <header class="chat-header">
        <div>
          <h2>{{ activeConversation?.title || '新问数对话' }}</h2>
          <p>{{ selectedDatasourceName() || '请选择数据源' }}</p>
        </div>
        <div class="datasource-box">
          <el-select v-model="form.datasourceId" placeholder="选择数据源" filterable clearable>
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
        </div>
      </header>

      <section ref="messageScrollRef" class="message-list">
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
            <div v-if="message.tableRows?.length" class="data-table-wrap">
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
              v-if="!message.tableRows?.length && !message.displayContent && !message.content"
              class="typing"
            >
              <span></span>
              <span></span>
              <span></span>
              <em>正在分析...</em>
            </div>
          </div>
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
        </div>
        <div class="composer-tip">Enter 发送，Shift+Enter 换行</div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import MarkdownView from '@/components/MarkdownView/index.vue'

const datasourceList = ref([])
const conversations = ref([])
const activeConversationId = ref(null)
const prompt = ref('')
const sending = ref(false)
const messageScrollRef = ref()

const form = reactive({
  datasourceId: null
})

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

onMounted(async () => {
  await loadDatasources()
  createConversation()
})

async function loadDatasources() {
  const res = await request({
    url: '/ast/dataSource/getDatasourceList',
    method: 'get',
    params: { pageSize: 200 }
  })
  datasourceList.value = res.data || []
}

function createConversation() {
  const id = Date.now()
  conversations.value.unshift({
    id,
    title: '新问数对话',
    datasourceName: selectedDatasourceName(),
    messages: []
  })
  activeConversationId.value = id
}

function selectConversation(id) {
  activeConversationId.value = id
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
  if (!activeConversation.value) {
    createConversation()
  }

  const conversation = activeConversation.value
  if (conversation.messages.length === 0) {
    conversation.title = question.slice(0, 20)
  }
  conversation.datasourceName = selectedDatasourceName()
  conversation.messages.push({
    id: Date.now(),
    role: 'user',
    content: question
  })

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
    steps: createAskSteps()
  })
  conversation.messages.push(assistantMessage)

  prompt.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    setActiveStep(assistantMessage, 'connect')
    await streamAskData({
      question,
      datasourceId: form.datasourceId,
      chatMode: 'chat_with_db_qa'
    }, {
      onMessage(chunk) {
        if (!assistantMessage.content) {
          setActiveStep(assistantMessage, 'answer')
        }
        assistantMessage.content += chunk
        scrollToBottom()
      },
      onError(message) {
        assistantMessage.content = message || 'AI问数调用失败'
        finishSteps(assistantMessage)
      }
    })
    hydrateStructuredData(assistantMessage)
    await hydrateSqlResult(assistantMessage, form.datasourceId)
    finishSteps(assistantMessage)
  } catch (error) {
    assistantMessage.content = error?.message || 'AI问数调用失败'
    finishSteps(assistantMessage)
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function hydrateSqlResult(message, datasourceId) {
  if (message.tableRows?.length) return
  const sql = extractSql(message.content)
  if (!sql) return

  try {
    const res = await request({
      url: '/ai/ask-data/execute',
      method: 'post',
      params: {
        datasourceId,
        sql,
        maxRows: 1000
      }
    })
    const rows = Array.isArray(res.data) ? res.data : []
    message.queryExecuted = true
    message.tableRows = rows
    message.tableColumns = buildColumns(rows)
  } catch (error) {
    message.executeError = error?.message || 'SQL执行失败'
    if (!message.displayContent) {
      message.displayContent = message.content
    }
    message.displayContent = `${message.displayContent}\n\n> ${message.executeError}`
  }
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
  extractAgentSteps(message)
}

function extractAgentSteps(message) {
  const content = message.displayContent || message.content
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

function extractSql(content) {
  if (!content || typeof content !== 'string') return ''
  const sqlBlock = content.match(/```sql\s*([\s\S]*?)```/i)
  if (sqlBlock?.[1]) {
    return sqlBlock[1].trim()
  }
  const genericBlock = content.match(/```\s*([\s\S]*?)```/)
  if (genericBlock?.[1] && /^\s*select\b/i.test(genericBlock[1])) {
    return genericBlock[1].trim()
  }
  const selectMatch = content.match(/\bselect\b[\s\S]*?(?:;|$)/i)
  if (selectMatch) return selectMatch[0].replace(/;$/, '').trim()

  const lines = content.split('\n')
  const sqlLines = []
  let inSql = false
  for (const line of lines) {
    const trimmed = line.trim().toLowerCase()
    if (trimmed.startsWith('select')) {
      inSql = true
    }
    if (inSql) {
      sqlLines.push(line)
      if (trimmed.endsWith(';')) {
        break
      }
    }
  }
  if (sqlLines.length) {
    let sql = sqlLines.join(' ').trim()
    if (sql.endsWith(';')) sql = sql.slice(0, -1)
    return sql
  }
  return ''
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
  border-radius: 6px;
  background: #ffffff;
  color: #1d2129;
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
}

.conversation-item:hover,
.conversation-item.active {
  border-color: #94bfff;
  background: #eef6ff;
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
  min-width: 390px;
}

.datasource-box :deep(.el-select) {
  flex: 1;
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
}

.examples button:hover {
  border-color: #165dff;
  color: #165dff;
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
  max-width: min(760px, 82%);
  min-height: 38px;
  padding: 11px 13px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #ffffff;
}

.message-row.user .message-bubble {
  border-color: #c9cdd4;
  background: #f7f8fa;
  color: #1f2937;
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
