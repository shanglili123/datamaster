<template>
  <div class="ai-ask-chat-page">
    <aside class="conversation-pane">
      <div class="pane-title">AI问数</div>
      <el-button type="primary" :icon="Plus" class="new-btn" @click="createConversation">新对话</el-button>
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

    <main class="chat-workspace">
      <header class="chat-header">
        <div>
          <h2>{{ activeConversation?.title || '新对话' }}</h2>
          <p>选择数据源，自动引用已同步的问数 Skill 进行分析</p>
        </div>
        <div class="header-actions">
        </div>
      </header>

      <section class="config-bar">
        <el-select v-model="form.datasourceId" placeholder="选择数据源" filterable clearable class="config-select">
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
        <el-select v-model="form.chatMode" placeholder="问数模式" class="mode-select">
          <el-option label="数据库问答" value="chat_with_db_qa" />
          <el-option label="知识库问答" value="chat_knowledge" />
          <el-option label="通用对话" value="chat_normal" />
        </el-select>
      </section>

      <section ref="messageScrollRef" class="message-stream">
        <div v-if="activeMessages.length === 0" class="empty-state">
          <h3>开始问你的数据</h3>
          <p>先同步数据源和 Skill，然后选择数据源发起问题。</p>
          <div class="examples">
            <button v-for="item in examples" :key="item" @click="prompt = item">{{ item }}</button>
          </div>
        </div>
        <div v-for="message in activeMessages" :key="message.id" class="message-row" :class="message.role">
          <div class="avatar">{{ message.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-card">
            <div class="message-content">{{ message.content }}</div>
            <SqlPreviewCard
              v-if="message.sql"
              :sql="message.sql"
              :executed="message.executeSuccess"
              :execute-error="message.executeError"
              :result-count="message.rowCount"
            />
            <el-table
              v-if="message.executeResult?.length"
              :data="message.executeResult"
              border
              size="small"
              class="result-table"
            >
              <el-table-column
                v-for="column in resultColumns(message.executeResult)"
                :key="column"
                :prop="column"
                :label="column"
                min-width="140"
                show-overflow-tooltip
              />
            </el-table>
            <SkillReferencePanel
              v-if="message.skills?.length"
              :skills="message.skills"
              class="skill-panel"
            />
          </div>
        </div>
      </section>

      <footer class="prompt-box">
        <el-input
          v-model="prompt"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 8 }"
          placeholder="输入自然语言问题，按 Enter 发送，Shift+Enter 换行"
          @keydown.enter.prevent="handleEnter"
          @keydown.shift.enter.stop
        />
        <div class="prompt-actions">
          <span>AI问数将结合已同步的数据源与 Skill 生成回答。</span>
          <el-button type="primary" :loading="sending" :icon="Promotion" @click="sendMessage">发送</el-button>
        </div>
      </footer>
    </main>

  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Collection, Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { askDataDbgptChat } from '@/api/ai/askData'
import SqlPreviewCard from './components/SqlPreviewCard.vue'
import SkillReferencePanel from './components/SkillReferencePanel.vue'



const datasourceList = ref([])
const conversations = ref([])
const activeConversationId = ref(null)
const prompt = ref('')
const sending = ref(false)
const messageScrollRef = ref()

const form = reactive({
  datasourceId: null,
  chatMode: 'chat_with_db_qa'
})

const examples = [
  '最近7天订单数量趋势',
  '按部门统计本月数据质量异常数量',
  '这张表有哪些字段适合做指标统计？'
]

const activeConversation = computed(() =>
  conversations.value.find((item) => item.id === activeConversationId.value)
)

const activeMessages = computed(() => activeConversation.value?.messages || [])

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
  const question = prompt.value.trim()
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }
  if (!activeConversation.value) {
    createConversation()
  }
  const conversation = activeConversation.value
  conversation.title = conversation.messages.length === 0 ? question.slice(0, 18) : conversation.title
  conversation.datasourceName = selectedDatasourceName()
  conversation.messages.push({
    id: Date.now(),
    role: 'user',
    content: question
  })
  prompt.value = ''
  sending.value = true
  await scrollToBottom()
  try {
    const res = await askDataDbgptChat({
      question,
      datasourceId: form.datasourceId,
      chatMode: form.chatMode
    })
    const data = res.data || {}
    conversation.messages.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: answerText(data),
      sql: data.sql,
      executeResult: data.executeResult || [],
      executeSuccess: data.executeSuccess,
      executeError: data.executeError,
      rowCount: data.rowCount,
      skills: data.referencedSkills || []
    })
  } catch (error) {
    conversation.messages.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: error?.message || 'AI问数调用失败'
    })
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function selectedDatasourceName() {
  return datasourceList.value.find((item) => item.id === form.datasourceId)?.datasourceName || ''
}

function syncText(status) {
  if (status === 'SYNCED') return '已同步'
  if (status === 'FAILED') return '同步失败'
  return '未同步'
}

function answerText(data) {
  if (data.explanation) {
    return data.explanation
  }
  if (data.executeSuccess) {
    return `已查询到 ${data.rowCount || 0} 条记录。`
  }
  if (data.executeError) {
    return `SQL执行失败：${data.executeError}`
  }
  if (data.qualityWarning) {
    return data.qualityWarning
  }
  return data.explanation || 'AI问数未返回内容'
}

function resultColumns(rows) {
  if (!rows || !rows.length) {
    return []
  }
  const columns = new Set()
  rows.forEach((row) => {
    Object.keys(row || {}).forEach((key) => columns.add(key))
  })
  return Array.from(columns)
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
.ai-ask-chat-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: calc(100vh - 96px);
  background: #f5f7fb;
  color: #202936;
}

.conversation-pane {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border-right: 1px solid #e6eaf0;
}

.pane-title {
  font-size: 18px;
  font-weight: 700;
}

.new-btn {
  width: 100%;
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
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #f7f9fc;
  color: #303133;
  cursor: pointer;
  text-align: left;
}

.conversation-item.active {
  background: #eef5ff;
  border-color: #91caff;
}

.conversation-item span {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-item small {
  color: #909399;
}

.chat-workspace {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e6eaf0;
}

.chat-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
}

.chat-header p {
  margin: 0;
  color: #748094;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.config-bar {
  display: flex;
  gap: 12px;
  padding: 14px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e6eaf0;
}

.config-select {
  width: 260px;
}

.mode-select {
  width: 160px;
}

.option-meta {
  float: right;
  margin-left: 20px;
  color: #909399;
  font-size: 12px;
}

.message-stream {
  overflow-y: auto;
  padding: 24px;
}

.empty-state {
  max-width: 720px;
  margin: 80px auto 0;
  text-align: center;
}

.empty-state h3 {
  font-size: 24px;
  margin-bottom: 8px;
}

.empty-state p {
  color: #748094;
}

.examples {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.examples button {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #ffffff;
  cursor: pointer;
}

.message-row {
  display: flex;
  gap: 12px;
  max-width: 980px;
  margin: 0 auto 18px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1f6feb;
  color: #ffffff;
  font-size: 13px;
}

.message-row.user .avatar {
  background: #19a974;
}

.message-card {
  max-width: min(760px, 80%);
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.05);
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.7;
}

.result-table {
  margin-top: 12px;
}

.skill-panel {
  margin-top: 12px;
}

.prompt-box {
  padding: 14px 24px 18px;
  background: #ffffff;
  border-top: 1px solid #e6eaf0;
}

.prompt-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 900px) {
  .ai-ask-chat-page {
    grid-template-columns: 1fr;
  }

  .conversation-pane {
    display: none;
  }

  .chat-header,
  .config-bar,
  .prompt-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .config-select,
  .mode-select {
    width: 100%;
  }
}
</style>
