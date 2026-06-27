<template>
  <div class="ask-data-page">
    <aside class="chat-sidebar">
      <div class="brand">
        <div class="brand-mark">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
        </div>
        <div class="brand-text">
          <strong>AI 问数</strong>
          <span>数据问答工作台</span>
        </div>
      </div>
      <el-button type="primary" :icon="Plus" class="new-chat" @click="createConversation">新对话</el-button>
      <div class="conversation-list">
        <button
          v-for="item in conversations"
          :key="item.id"
          class="conversation-item"
          :class="{ active: item.id === activeConversationId }"
          @click="selectConversation(item.id)"
        >
          <span class="conv-title">{{ item.title }}</span>
          <small class="conv-meta">{{ item.datasourceName || '未选择数据源' }}</small>
        </button>
      </div>
    </aside>

    <main class="chat-main">
      <header class="chat-topbar">
        <div class="title-block">
          <h2>{{ activeConversation?.title || '新问数对话' }}</h2>
          <span class="title-sub">
            <el-icon><Connection /></el-icon>
            {{ selectedDatasourceName() || '请选择数据源' }}
          </span>
        </div>
        <div class="datasource-picker">
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
          <span class="sync-state" :class="selectedDatasource?.dbgptSyncStatus || 'NONE'">
            <span class="sync-dot"></span>
            {{ syncText(selectedDatasource?.dbgptSyncStatus) }}
          </span>
        </div>
      </header>

      <section ref="messageScrollRef" class="message-area">
        <div v-if="activeMessages.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#c0c8d4" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          </div>
          <h1>想查什么，直接问</h1>
          <p>选择数据源后，AI 会自动分析并回答你的数据问题</p>
          <div class="examples">
            <button v-for="item in examples" :key="item" @click="prompt = item">
              <el-icon><ChatDotSquare /></el-icon>
              {{ item }}
            </button>
          </div>
        </div>

        <div
          v-for="(message, msgIdx) in activeMessages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="message-col">
            <div class="message-header">
              <div class="avatar" :class="message.role">
                <template v-if="message.role === 'user'">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                </template>
                <template v-else>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                </template>
              </div>
              <span class="message-role">{{ message.role === 'user' ? '你' : 'AI 问数' }}</span>
              <span class="message-time">{{ message.createTime || '' }}</span>
            </div>
            <div class="message-body" :class="message.role">
              <template v-if="message.role === 'user'">
                <div class="user-bubble">{{ message.content }}</div>
              </template>
              <template v-else>
                <div class="assistant-card">
                  <div v-if="message.steps && message.steps.length > 0" class="steps-bar">
                    <div
                      v-for="(step, si) in message.steps"
                      :key="si"
                      class="step-chip"
                      :class="step.status"
                    >
                      <span class="step-num">
                        <template v-if="step.status === 'done'">✓</template>
                        <template v-else-if="step.status === 'active'">
                          <span class="step-spinner"></span>
                        </template>
                        <template v-else>{{ si + 1 }}</template>
                      </span>
                      <span>{{ step.label }}</span>
                    </div>
                    <div v-if="message.currentStep < message.steps.length" class="step-progress">
                      <span>步骤 {{ Math.min(message.currentStep + 1, message.steps.length) }}/{{ message.steps.length }}</span>
                    </div>
                  </div>
                  <div v-if="message.sql" class="sql-section">
                    <button class="sql-toggle" @click="message._showSql = !message._showSql">
                      <el-icon><Monitor /></el-icon>
                      <span>查看 SQL 语句</span>
                      <el-icon :class="{ rotated: message._showSql }"><ArrowDown /></el-icon>
                    </button>
                    <div v-show="message._showSql" class="sql-content">
                      <pre><code>{{ message.sql }}</code></pre>
                      <button class="sql-copy" @click="copySql(message.sql)">复制</button>
                    </div>
                  </div>
                  <AssistantReportCard
                    v-if="isReportCard(message)"
                    :data="toReportCard(message)"
                  />
                  <MarkdownView
                    v-else-if="message.content"
                    :content="getDisplayContent(message)"
                  />
                  <div v-else class="typing">
                    <span></span>
                    <span></span>
                    <span></span>
                    <span class="typing-text">正在分析...</span>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </section>

      <footer class="composer-wrap">
        <div class="composer-inner">
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
          <p class="composer-hint">按 Enter 发送，Shift+Enter 换行</p>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Plus, Promotion, Connection, ChatDotSquare, Monitor, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import MarkdownView from '@/components/MarkdownView/index.vue'
import AssistantReportCard from './components/message/AssistantReportCard.vue'

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
  if (!question) { ElMessage.warning('请输入问题'); return }
  if (!form.datasourceId) { ElMessage.warning('请先选择数据源'); return }
  if (!activeConversation.value) createConversation()

  const conversation = activeConversation.value
  if (conversation.messages.length === 0) conversation.title = question.slice(0, 20)
  conversation.datasourceName = selectedDatasourceName()

  const now = new Date()
  const timeStr = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  conversation.messages.push({
    id: Date.now(), role: 'user', content: question, createTime: timeStr
  })

  // 预定义执行步骤
  const stepDefs = [
    { label: '理解问题', icon: '🤔' },
    { label: '查询数据', icon: '🔍' },
    { label: '生成结果', icon: '✨' },
  ]

  const assistantMessage = reactive({
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    sql: '',
    steps: stepDefs.map((s, i) => ({ label: s.label, status: i === 0 ? 'active' : 'pending' })),
    currentStep: 0,
    createTime: timeStr
  })
  conversation.messages.push(assistantMessage)

  prompt.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    // 模拟步骤推进：连接成功后进入第二步
    setTimeout(() => advanceStep(assistantMessage), 800)

    await streamAskData({
      question,
      datasourceId: form.datasourceId,
      chatMode: 'chat_with_db_qa'
    }, {
      onStep(step) {
        advanceStep(assistantMessage)
        scrollToBottom()
      },
      onSql(sql) {
        assistantMessage.sql += sql + '\n'
        scrollToBottom()
      },
      onMessage(chunk) {
        // 收到数据时自动推进到第三步
        if (assistantMessage.currentStep < 2) {
          assistantMessage.steps[1].status = 'done'
          assistantMessage.steps[2].status = 'active'
          assistantMessage.currentStep = 2
        }
        assistantMessage.content += chunk
        scrollToBottom()
      },
      onError(msg) {
        assistantMessage.content = msg || 'AI问数调用失败'
      },
      onDone() {
        // 标记所有步骤完成
        assistantMessage.steps.forEach(s => s.status = 'done')
        assistantMessage.currentStep = assistantMessage.steps.length
        // 从 markdown 中提取 SQL 代码块
        if (!assistantMessage.sql) {
          const sqlMatch = assistantMessage.content.match(/```sql\s*([\s\S]*?)```/)
          if (sqlMatch) {
            assistantMessage.sql = sqlMatch[1].trim()
            assistantMessage._showSql = true
          }
        }
      }
    })
  } catch (error) {
    assistantMessage.content = error?.message || 'AI问数调用失败'
    assistantMessage.steps.forEach(s => s.status = 'done')
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function advanceStep(msg) {
  if (msg.currentStep < msg.steps.length - 1) {
    msg.steps[msg.currentStep].status = 'done'
    msg.currentStep++
    msg.steps[msg.currentStep].status = 'active'
  }
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

  if (!response.ok) throw new Error(await response.text() || 'AI问数请求失败')
  if (!response.body) throw new Error('浏览器不支持流式响应')

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
  if (buffer.trim()) handleSseEvent(buffer, callbacks)
  callbacks.onDone?.()
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
    } else if (line.trim() && !line.startsWith(':')) {
      if (data.length > 0) data[data.length - 1] += '\n' + line
    }
  })
  const text = data.join('\n')
  if (eventName === 'message') {
    callbacks.onMessage?.(text)
  } else if (eventName === 'step') {
    callbacks.onStep?.(text)
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

function copySql(sql) {
  navigator.clipboard.writeText(sql).then(() => {
    ElMessage.success('SQL 已复制')
  })
}

async function scrollToBottom() {
  await nextTick()
  const el = messageScrollRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function toNumber(v) {
  if (v == null) return v
  if (typeof v === 'number') return v
  const n = Number(v)
  return Number.isNaN(n) ? v : n
}

function safeJsonParse(str, defVal = {}) {
  try {
    if (typeof str === 'object' && str !== null) return str
    return JSON.parse(str || '')
  } catch {
    return defVal
  }
}

function getDisplayContent(item) {
  let content = item?.content || ''
  while (true) {
    const raw = safeJsonParse(content, null)
    if (raw && typeof raw === 'object' && raw.msg) {
      content = raw.msg
    } else {
      break
    }
  }
  return content
}

function isReportCard(item) {
  const rt = item?.replyType
  const num = toNumber(rt)
  return num === 1 || num === 2 || item?.content === 'loading'
}

function toReportCard(item) {
  const content = item?.content || ''
  const rt = toNumber(item?.replyType)
  const isNewMessage = !item.id || item.id <= 0

  if (item.isError) {
    return {
      header: '智能洞察',
      summary: getDisplayContent(item) || '对话异常',
      tabs: [],
      isLoading: false,
      code: 500,
    }
  }

  if ((content === 'loading' || content === '') && isNewMessage) {
    const tabs = []
    if (rt === 2) {
      tabs.push({ key: 'viz', label: '可视化' })
      tabs.push({ key: 'detail', label: '明细数据' })
    }
    return {
      header: '智能洞察',
      summary: rt === 2 ? '正在分析数据并生成报表，请稍候...' : '正在思考中...',
      isLoading: true,
      tabs: tabs,
    }
  }

  let raw = safeJsonParse(content, null)
  if (!raw && content) {
    raw = { msg: content }
  }

  const header = '智能洞察'
  let summary = raw?.msg || (toNumber(raw?.code) === 500 ? '对话异常' : '')

  if (toNumber(raw?.code) === 500) {
    return { header, summary: summary || '对话异常', tabs: [], isLoading: false, code: 500 }
  }

  const hasStructuralData =
    (raw?.chatData?.xAxisData?.length > 0 &&
      (raw?.chatData?.yAxisData?.length > 0 || raw?.chatData?.yAxisDataArr?.length > 0)) ||
    raw?.detailData?.list?.length > 0 ||
    raw?.sql

  const tabs = []
  if (rt === 2 || hasStructuralData) {
    if (rt === 2 && !hasStructuralData && isNewMessage) {
      tabs.push({ key: 'viz', label: '可视化' })
      tabs.push({ key: 'detail', label: '明细数据' })
      return { header, summary, tabs, isLoading: true }
    }

    let xAxisData = raw?.chatData?.xAxisData || []
    let yAxisData = raw?.chatData?.yAxisData || []
    let yAxisDataArr = raw?.chatData?.yAxisDataArr || []
    const dataType = toNumber(raw?.dataType)

    const rows = Array.isArray(raw?.detailData?.list) ? raw.detailData.list : []
    let columns = []
    if (rows.length > 0) {
      const keys = Object.keys(rows[0] || {})
      const labels = Array.isArray(raw?.detailData?.label) ? raw.detailData.label : []
      columns = keys.map((k, i) => ({ prop: k, label: labels[i] || k }))
    } else if (Array.isArray(raw?.selectColumn) && raw.selectColumn.length > 0) {
      const labels = Array.isArray(raw?.detailData?.label) && raw.detailData.label.length > 0
        ? raw.detailData.label
        : Array.isArray(raw?.selectColumnDescription) ? raw.selectColumnDescription : []
      columns = raw.selectColumn.map((k, i) => ({ prop: k, label: labels[i] || k }))
    }

    const isChatDataValid = (data) => Array.isArray(data) && data.length > 0 && data.some((v) => v !== null)

    if (!isChatDataValid(xAxisData) && rows.length > 0) {
      const keys = Object.keys(rows[0])
      if (keys.length >= 2) {
        xAxisData = rows.map((row) => row[keys[1]] || row[keys[0]])
        yAxisData = rows.map((row) => row[keys[keys.length - 1]])
        yAxisDataArr = []
      }
    }

    const hasChartData = isChatDataValid(xAxisData) &&
      (isChatDataValid(yAxisData) || (yAxisDataArr.length > 0 && yAxisDataArr.some(isChatDataValid)))

    if (rt === 2 || hasChartData) {
      let chartType = 'bar'
      if (dataType === 2) chartType = 'line'
      if (dataType === 3) chartType = 'pie'

      const series = []
      if (yAxisDataArr.length > 0) {
        yAxisDataArr.forEach((data) => series.push({ name: `数据${series.length + 1}`, data }))
      } else {
        series.push({ name: '数据', data: yAxisData })
      }

      tabs.push({ key: 'viz', label: '可视化', chart: hasChartData ? { type: chartType, xAxis: xAxisData, series } : null })
    }

    if (rt === 2 || rows.length > 0) {
      tabs.push({ key: 'detail', label: '明细数据', table: { rows, columns } })
    }

    if (raw?.sql) {
      tabs.push({ key: 'sql', label: 'Text2SQL', code: raw.sql })
    }
  }

  return { header, summary, tabs }
}
</script>

<style lang="scss" scoped>
.ask-data-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: calc(100vh - 84px);
  min-height: 640px;
  background: #f0f2f5;
  color: #1d2129;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== Sidebar ===== */
.chat-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 16px;
  background: #ffffff;
  border-right: 1px solid #e5e6eb;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #165dff, #0e42d2);
  color: #ffffff;
}

.brand-text strong {
  display: block;
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
}

.brand-text span {
  display: block;
  margin-top: 1px;
  color: #86909c;
  font-size: 12px;
}

.new-chat {
  width: 100%;
  border-radius: 6px;
  height: 36px;
  font-size: 13px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  flex: 1;
}

.conversation-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #1d2129;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s;
}

.conversation-item:hover {
  background: #f2f3f5;
}

.conversation-item.active {
  background: #e8f3ff;
  border-color: #94bfff;
}

.conv-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
}

.conv-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #86909c;
  font-size: 12px;
}

/* ===== Main Layout ===== */
.chat-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
}

/* ===== Topbar ===== */
.chat-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 14px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e5e6eb;
}

.title-block h2 {
  margin: 0 0 2px;
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
}

.title-sub {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #86909c;
  font-size: 13px;
  .el-icon { font-size: 14px; }
}

.datasource-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 380px;
}

.datasource-picker :deep(.el-select) { flex: 1; }

.option-meta {
  float: right;
  margin-left: 20px;
  color: #a9aeb8;
  font-size: 12px;
}

.sync-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  color: #86909c;
  background: #f7f8fa;
  font-size: 12px;
  white-space: nowrap;
}

.sync-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c9cdd4;
}

.assistant-card :deep(.markdown-view),
.assistant-card :deep(.markdown-body) {
  color: #1d2129 !important;
}

.sync-state.SYNCED {
  color: #00b42a;
  border-color: #a5e0b7;
  background: #e8f9ee;
  .sync-dot { background: #00b42a; }
}

.sync-state.FAILED {
  color: #f53f3f;
  border-color: #fbb0b0;
  background: #fef0ef;
  .sync-dot { background: #f53f3f; }
}

/* ===== Message Area ===== */
.message-area {
  overflow-y: auto;
  padding: 20px 24px 40px;
  background: linear-gradient(180deg, #f7f8fa 0%, #f0f2f5 100%);
}

/* ===== Empty State ===== */
.empty-state {
  max-width: 600px;
  margin: 100px auto 0;
  text-align: center;
}

.empty-icon {
  margin-bottom: 20px;
}

.empty-state h1 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #1d2129;
}

.empty-state p {
  margin: 0;
  color: #86909c;
  font-size: 14px;
}

.examples {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.examples button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: 1px solid #e5e6eb;
  border-radius: 20px;
  background: #ffffff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  .el-icon { font-size: 14px; }
}

.examples button:hover {
  border-color: #165dff;
  color: #165dff;
  background: #f2f7ff;
}

/* ===== Message Row ===== */
.message-row {
  max-width: 960px;
  margin: 0 auto 24px;
}

.message-col {
  display: flex;
  flex-direction: column;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 0 4px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar.user {
  background: #165dff;
  color: #ffffff;
}

.avatar.assistant {
  background: linear-gradient(135deg, #722ed1, #531dab);
  color: #ffffff;
}

.message-role {
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
}

.message-time {
  margin-left: auto;
  font-size: 12px;
  color: #a9aeb8;
}

/* ===== Message Bubbles ===== */
.message-body.user {
  display: flex;
  justify-content: flex-end;
}

.user-bubble {
  display: inline-block;
  max-width: 640px;
  padding: 10px 16px;
  border-radius: 6px 6px 2px 6px;
  background: #165dff;
  color: #ffffff;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.assistant-card {
  background: #ffffff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ===== Steps Bar ===== */
.steps-bar {
  display: flex;
  gap: 4px;
  padding: 10px 16px;
  background: #f7f8fa;
  border-bottom: 1px solid #e5e6eb;
  overflow-x: auto;
}

.step-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px 4px 6px;
  border-radius: 4px;
  background: #ffffff;
  border: 1px solid #e5e6eb;
  font-size: 12px;
  color: #86909c;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.25s;
}

.step-chip.active {
  background: #e8f3ff;
  border-color: #94bfff;
  color: #165dff;
  .step-num { background: #165dff; color: #fff; }
}

.step-chip.done {
  color: #00b42a;
  border-color: #a5e0b7;
  .step-num { background: #00b42a; color: #fff; }
}

.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: #c9cdd4;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-spinner {
  display: inline-block;
  width: 10px;
  height: 10px;
  border: 2px solid rgba(22, 93, 255, 0.25);
  border-top-color: #165dff;
  border-radius: 50%;
  animation: step-spin 0.7s linear infinite;
}

@keyframes step-spin {
  to { transform: rotate(360deg); }
}

.step-progress {
  display: flex;
  align-items: center;
  margin-left: auto;
  padding-left: 8px;
  font-size: 11px;
  color: #a9aeb8;
  white-space: nowrap;
}

/* ===== SQL Section ===== */
.sql-section {
  border-bottom: 1px solid #e5e6eb;
}

.sql-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 8px 16px;
  border: none;
  background: #fafafa;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
  .el-icon { font-size: 15px; }
  .el-icon:last-child { margin-left: auto; transition: transform 0.2s; }
  .el-icon:last-child.rotated { transform: rotate(180deg); }
}

.sql-toggle:hover { background: #f0f2f5; }

.sql-content {
  position: relative;
  padding: 12px 16px;
  background: #1d2129;
}

.sql-content pre {
  margin: 0;
  overflow-x: auto;
}

.sql-content code {
  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #e6e6e6;
}

.sql-copy {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 3px 10px;
  border: 1px solid #4e5969;
  border-radius: 4px;
  background: transparent;
  color: #a9aeb8;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.sql-copy:hover {
  background: #4e5969;
  color: #ffffff;
}

/* ===== Typing Indicator ===== */
.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 16px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c9cdd4;
  animation: typing 1.2s infinite ease-in-out;
}

.typing span:nth-child(2) { animation-delay: 0.15s; }
.typing span:nth-child(3) { animation-delay: 0.3s; }

.typing-text {
  margin-left: 8px;
  font-size: 13px;
  color: #86909c;
}

@keyframes typing {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-4px); }
}

/* ===== Composer ===== */
.composer-wrap {
  padding: 12px 24px 16px;
  background: #ffffff;
  border-top: 1px solid #e5e6eb;
}

.composer-inner {
  max-width: 960px;
  margin: 0 auto;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 8px 8px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #ffffff;
  transition: border-color 0.15s;
}

.composer:focus-within {
  border-color: #165dff;
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.1);
}

.composer :deep(.el-textarea__inner) {
  min-height: 22px !important;
  padding: 6px 0;
  border: none !important;
  box-shadow: none !important;
  line-height: 22px;
  font-size: 14px;
}

.send-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 6px;
  font-size: 16px;
}

.composer-hint {
  margin: 6px 4px 0;
  font-size: 12px;
  color: #c9cdd4;
}

/* ===== Responsive ===== */
@media (max-width: 960px) {
  .ask-data-page {
    grid-template-columns: 1fr;
    height: calc(100vh - 56px);
  }
  .chat-sidebar { display: none; }
  .chat-topbar { flex-direction: column; align-items: stretch; }
  .datasource-picker { min-width: 0; }
}
</style>
