<template>
  <div class="ask-data-page">
    <aside class="chat-sidebar">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <div>
          <strong>AI问数</strong>
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
          <span>{{ item.title }}</span>
          <small>{{ item.datasourceName || '未选择数据源' }}</small>
        </button>
      </div>
    </aside>

    <main class="chat-main">
      <header class="chat-topbar">
        <div class="title-block">
          <h2>{{ activeConversation?.title || '新问数对话' }}</h2>
          <span>{{ selectedDatasourceName() || '请选择数据源' }}</span>
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
            {{ syncText(selectedDatasource?.dbgptSyncStatus) }}
          </span>
        </div>
      </header>

      <section ref="messageScrollRef" class="message-area">
        <div v-if="activeMessages.length === 0" class="empty-state">
          <h1>想查什么，直接问</h1>
          <p>选择数据源后，AI问数会使用已同步的数据源和自动生成的 Skill 进行回答。</p>
          <div class="examples">
            <button v-for="item in examples" :key="item" @click="prompt = item">{{ item }}</button>
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
            <div v-if="message.content" class="message-content">{{ message.content }}</div>
            <div v-else class="typing">
              <span></span>
              <span></span>
              <span></span>
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
            circle
            :loading="sending"
            :icon="Promotion"
            class="send-btn"
            @click="sendMessage"
          />
        </div>
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
  if (sending.value) {
    return
  }
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
  conversation.title = conversation.messages.length === 0 ? question.slice(0, 20) : conversation.title
  conversation.datasourceName = selectedDatasourceName()
  conversation.messages.push({
    id: Date.now(),
    role: 'user',
    content: question
  })

  const assistantMessage = {
    id: Date.now() + 1,
    role: 'assistant',
    content: ''
  }
  conversation.messages.push(assistantMessage)

  prompt.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    await streamAskData({
      question,
      datasourceId: form.datasourceId,
      chatMode: 'chat_with_db_qa'
    }, (chunk) => {
      assistantMessage.content += chunk
      scrollToBottom()
    }, (message) => {
      assistantMessage.content = message || 'AI问数调用失败'
    })
  } catch (error) {
    assistantMessage.content = error?.message || 'AI问数调用失败'
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function streamAskData(payload, onMessage, onError) {
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
    events.forEach((raw) => handleSseEvent(raw, onMessage, onError))
  }
  if (buffer.trim()) {
    handleSseEvent(buffer, onMessage, onError)
  }
}

function handleSseEvent(raw, onMessage, onError) {
  const lines = raw.split(/\r?\n/)
  let eventName = 'message'
  const data = []
  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.substring(6).trim()
    } else if (line.startsWith('data:')) {
      data.push(line.substring(5).replace(/^ /, ''))
    } else if (line.trim() && !line.startsWith(':')) {
      if (data.length > 0) {
        data[data.length - 1] += '\n' + line
      }
    }
  })
  const text = data.join('\n')
  if (eventName === 'message') {
    onMessage(text)
  } else if (eventName === 'error') {
    onError(text)
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

async function scrollToBottom() {
  await nextTick()
  const el = messageScrollRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}
</script>

<style lang="scss" scoped>
.ask-data-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: calc(100vh - 84px);
  min-height: 640px;
  background: #f4f6f8;
  color: #1f2933;
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  background: #ffffff;
  border-right: 1px solid #dfe5ec;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2264d1;
  color: #ffffff;
  font-weight: 700;
}

.brand strong,
.brand span {
  display: block;
}

.brand strong {
  font-size: 17px;
}

.brand span {
  margin-top: 2px;
  color: #7b8794;
  font-size: 12px;
}

.new-chat {
  width: 100%;
  border-radius: 6px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #f7f9fb;
  color: #303133;
  cursor: pointer;
  text-align: left;
}

.conversation-item.active {
  background: #eef5ff;
  border-color: #8fb9f6;
}

.conversation-item span,
.conversation-item small {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-item small {
  color: #7b8794;
}

.chat-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
}

.chat-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 16px 24px;
  background: #ffffff;
  border-bottom: 1px solid #dfe5ec;
}

.title-block h2 {
  margin: 0 0 4px;
  font-size: 18px;
  line-height: 24px;
}

.title-block span {
  color: #7b8794;
  font-size: 13px;
}

.datasource-picker {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 380px;
}

.datasource-picker :deep(.el-select) {
  flex: 1;
}

.option-meta {
  float: right;
  margin-left: 24px;
  color: #8a94a6;
  font-size: 12px;
}

.sync-state {
  flex: 0 0 auto;
  padding: 5px 9px;
  border: 1px solid #ccd6e0;
  border-radius: 6px;
  color: #667085;
  background: #f8fafc;
  font-size: 12px;
}

.sync-state.SYNCED {
  color: #0f766e;
  border-color: #99d7d0;
  background: #ecfdf9;
}

.sync-state.FAILED {
  color: #b42318;
  border-color: #f5b5ae;
  background: #fff5f3;
}

.message-area {
  overflow-y: auto;
  padding: 28px 24px;
}

.empty-state {
  max-width: 760px;
  margin: 86px auto 0;
  text-align: center;
}

.empty-state h1 {
  margin: 0 0 12px;
  font-size: 30px;
  line-height: 38px;
}

.empty-state p {
  margin: 0;
  color: #667085;
}

.examples {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.examples button {
  min-height: 34px;
  padding: 7px 12px;
  border: 1px solid #d4dce6;
  border-radius: 6px;
  background: #ffffff;
  color: #344054;
  cursor: pointer;
}

.examples button:hover {
  border-color: #2264d1;
  color: #2264d1;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 960px;
  margin: 0 auto 20px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2264d1;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.message-row.user .avatar {
  background: #12a594;
}

.message-bubble {
  max-width: min(760px, 82%);
  min-height: 38px;
  padding: 12px 14px;
  border: 1px solid #dde5ee;
  border-radius: 8px;
  background: #ffffff;
}

.message-row.user .message-bubble {
  color: #ffffff;
  background: #2264d1;
  border-color: #2264d1;
}

.message-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.75;
  font-size: 14px;
}

.typing {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 20px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8a94a6;
  animation: typing 1s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
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
  padding: 14px 24px 20px;
  background: #ffffff;
  border-top: 1px solid #dfe5ec;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: 960px;
  margin: 0 auto;
  padding: 10px;
  border: 1px solid #ccd6e0;
  border-radius: 8px;
  background: #ffffff;
}

.composer :deep(.el-textarea__inner) {
  min-height: 42px !important;
  padding: 10px 0;
  border: none;
  box-shadow: none;
  line-height: 22px;
}

.send-btn {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
}

@media (max-width: 960px) {
  .ask-data-page {
    grid-template-columns: 1fr;
    height: calc(100vh - 64px);
  }

  .chat-sidebar {
    display: none;
  }

  .chat-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .datasource-picker {
    min-width: 0;
  }

  .message-bubble {
    max-width: 86%;
  }
}
</style>
