<template>
  <div class="data-agent-assistant">
    <button v-if="!open" ref="fabRef" class="data-agent-fab" type="button" title="打开 Data Agent" :style="fabStyle" @pointerdown="startDrag" @click="handleFabClick"><RobotOutlined /><span>Data Agent</span></button>
    <a-drawer v-model:open="open" title="Data Agent" placement="right" :width="430" :destroy-on-close="false" @afterOpenChange="handleDrawerAfterOpen">
      <div class="data-agent-context"><div><span>当前页面</span><a-button type="link" size="small" @click="resetConversation">新会话</a-button></div><strong>{{ pageContext.title }}</strong><small>{{ pageContext.path }}</small></div>
      <div class="data-agent-presets"><a-button v-for="item in presets" :key="item" size="small" @click="usePreset(item)">{{ item }}</a-button></div>
      <div ref="messageBox" class="data-agent-messages"><div v-if="!messages.length" class="data-agent-empty">告诉我想建设的数据流程、本体或指标草稿；日常数据查询与指标分析请进入 AI 问数。</div><div v-for="(item, index) in messages" :key="`${index}-${item.role}`" class="data-agent-message" :class="item.role"><div class="data-agent-message-role">{{ item.role === 'user' ? '你' : 'Agent' }}</div><div class="data-agent-message-content">{{ item.content }}</div></div><a-spin v-if="loading" size="small" tip="Agent 正在调用工具…" /></div>
      <template #footer><div class="data-agent-input"><div class="data-agent-input-box"><a-textarea v-model:value="input" :rows="3" :maxlength="1000" placeholder="例如：根据当前数据源生成本体，或为当前概念预览指标草稿；数据问答请进入AI问数" @keydown.enter.exact.prevent="send" /><a-button class="data-agent-send" type="primary" size="small" :loading="loading" :disabled="!input.trim()" @click="send">发送</a-button></div></div><a-button v-if="needsConfirmation" type="primary" block class="data-agent-confirm" @click="continueWithConfirmation">确认继续执行</a-button></template>
    </a-drawer>
  </div>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { RobotOutlined } from '@ant-design/icons-vue'
import { runAutonomousAgent, getAutonomousAgentOperation } from '@/api/ai/platformAgent'
import { listAskSessions, createAskSession, listAskMessages, appendAskMessage } from '@/api/ai/askSession'
import useUserStore from '@/store/system/user'
const route = useRoute(); const userStore = useUserStore(); const open = ref(false); const input = ref(''); const loading = ref(false); const messages = ref([]); const sessionId = ref(null); const operationId = ref(''); const conversationId = ref(''); const needsConfirmation = ref(false); const confirmationArgument = ref(''); const confirmationValue = ref(null); const renderedToolResultCount = ref(0); const messageBox = ref(null); const pageOverride = ref(null); const fabRef = ref(null); const fabPosition = ref({ left: 0, top: 0 }); const dragging = ref(false); let dragOffset = { x: 0, y: 0 }; let dragged = false
const AGENT_SESSION_CATEGORY = 'AUTONOMOUS_AGENT'
const conversationStorageKey = () => `dataMasterDataAgentConversation:${userStore.spaceId || userStore.spaceCode || 'default'}`
const fabStyle = computed(() => ({ left: `${fabPosition.value.left}px`, top: `${fabPosition.value.top}px` }))
const pageContext = computed(() => { const routeTitle = (route.matched || []).map((item) => item.meta?.title).filter(Boolean).slice(-1)[0] || route.meta?.title || '当前页面'; const override = pageOverride.value; const path = String(override?.path || route.fullPath || route.path); const ontologyId = override?.ontologyId || route.params?.ontologyId || path.match(/^\/ont\/workspace\/([^/?]+)/)?.[1]; const baseDetail = override?.detail || ''; const detail = ontologyId && !String(baseDetail).includes('ontologyId=') ? [baseDetail, `ontologyId=${ontologyId}`].filter(Boolean).join(' / ') : baseDetail; return { title: String(override?.title || routeTitle), path, detail, ontologyId } })
const presets = computed(() => { const path = pageContext.value.path.toLowerCase(); if (path.includes('/ont/')) return ['根据当前数据源生成本体', '查看当前本体有哪些动作', '为当前概念预览指标草稿']; if (path.includes('datasource')) return ['根据页面信息添加数据源', '检查当前数据源连接', '列出当前空间数据源']; return ['列出当前空间数据源', '根据当前页面信息生成本体', '同步当前数据源资产'] })
function usePreset(value) { input.value = value; send() }
function buildGoal(text) { const detail = pageContext.value.detail ? `；页面内容=${pageContext.value.detail}` : ''; return `当前页面上下文：页面名称=${pageContext.value.title}；路径=${pageContext.value.path}${detail}。\n用户目标：${text}` }
function sessionParams() { return { spaceId: userStore.spaceId, spaceCode: userStore.spaceCode } }
async function ensureAgentSession() {
  const saved = JSON.parse(localStorage.getItem(conversationStorageKey()) || '{}')
  const listed = await listAskSessions({ ...sessionParams(), category: AGENT_SESSION_CATEGORY, limit: 20 })
  const rows = listed?.data || []
  const currentSession = sessionId.value ? rows.find((item) => String(item.id) === String(sessionId.value)) : null
  const savedSession = saved.sessionId ? rows.find((item) => String(item.id) === String(saved.sessionId)) : null
  if (currentSession) sessionId.value = currentSession.id
  else if (savedSession) sessionId.value = savedSession.id
  else if (sessionId.value || saved.sessionId) {
    // 旧版本可能把 AI 问数会话 ID 写进了 Data Agent 快照，发现分类不匹配后必须整组丢弃。
    sessionId.value = null
    messages.value = []
    operationId.value = ''
    conversationId.value = ''
    renderedToolResultCount.value = 0
    localStorage.removeItem(conversationStorageKey())
  }
  if (!sessionId.value && rows.length) sessionId.value = rows[0].id
  if (!sessionId.value) {
    const created = await createAskSession({ ...sessionParams(), title: 'Data Agent', mode: 'agent', category: AGENT_SESSION_CATEGORY })
    sessionId.value = created?.data?.id
  }
  if (!sessionId.value) throw new Error('无法创建 Data Agent 会话')
  return sessionId.value
}
function normalizeSessionMessage(row) {
  let payload = {}
  try { payload = row?.payloadJson ? JSON.parse(row.payloadJson) : {} } catch (error) { payload = {} }
  return { role: payload.role || row.role, content: payload.displayContent || payload.content || row.displayContent || row.content || '' }
}
function scrollMessagesToBottom(behavior = 'smooth') {
  nextTick(() => {
    const scroll = () => {
      const element = messageBox.value
      if (!element) return
      element.scrollTo({ top: element.scrollHeight, behavior })
    }
    // Drawer 打开和历史消息渲染都可能跨越多个布局帧，第一次 nextTick
    // 时 scrollHeight 还没有最终值，因此在后续帧再定位一次。
    scroll()
    window.requestAnimationFrame(() => {
      scroll()
      window.requestAnimationFrame(scroll)
    })
    window.setTimeout(scroll, 180)
  })
}
function handleDrawerAfterOpen(visible) {
  if (visible) scrollMessagesToBottom('auto')
}
async function loadAgentSessionMessages() {
  if (!sessionId.value) return
  const res = await listAskMessages(sessionId.value, { ...sessionParams(), limit: 100 })
  const rows = res?.data?.rows || []
  if (rows.length) {
    messages.value = rows.map(normalizeSessionMessage)
    scrollMessagesToBottom('auto')
  }
}
async function persistAgentMessage(messageItem) {
  if (!sessionId.value || !messageItem?.content) return
  await appendAskMessage(sessionId.value, {
    role: messageItem.role,
    content: messageItem.content,
    displayContent: messageItem.content,
    payloadJson: JSON.stringify(messageItem)
  }, sessionParams())
}
function appendAgentResponse(data) {
  if (!data) return ''
  operationId.value = data.operationId || operationId.value
  conversationId.value = data.conversationId || conversationId.value
  if (conversationId.value || operationId.value) localStorage.setItem(conversationStorageKey(), JSON.stringify({ operationId: operationId.value, conversationId: conversationId.value }))
  const lines = []
  if (data.text) lines.push(data.text)
  if (data.message && data.message !== data.text) lines.push(data.message)
  const toolResults = data.toolResults || []
  // 查询接口返回的是当前操作的完整结果列表，而不是本次轮询新增的结果。
  // 只渲染新增部分，避免“已确认，正在执行”后再次把上一轮确认消息刷出来。
  if (toolResults.length < renderedToolResultCount.value) renderedToolResultCount.value = 0
  toolResults.slice(renderedToolResultCount.value).forEach((item) => {
    if (!item) return
    const label = item.toolName ? `[${item.toolName}] ` : ''
    const detail = item.message || item.status || ''
    if (detail && !lines.some((line) => line.includes(detail))) lines.push(`${label}${detail}`)
  })
  renderedToolResultCount.value = toolResults.length
  if (!lines.length && data.status) lines.push(`Agent 状态：${data.status}`)
  if (lines.length) {
    messages.value.push({ role: 'assistant', content: lines.join('\n') })
    scrollMessagesToBottom()
  }
  needsConfirmation.value = data.status === 'NEED_CONFIRMATION'
  const pending = [...toolResults].reverse().find((item) => item?.status === 'NEED_CONFIRMATION' && item?.metadata?.confirmationArgument)
  if (pending) {
    confirmationArgument.value = String(pending.metadata.confirmationArgument)
    confirmationValue.value = pending.metadata.arguments?.[confirmationArgument.value]
  } else if (!needsConfirmation.value) {
    confirmationArgument.value = ''
    confirmationValue.value = null
  }
  return lines.join('\n')
}
function isAutonomousOperation(data) {
  return String(data?.currentStep || '').trim() === '自主 Tool 调度'
}
async function send() { const text = input.value.trim(); if (!text || loading.value) return; input.value = ''; loading.value = true; needsConfirmation.value = false; confirmationArgument.value = ''; confirmationValue.value = null; try { await ensureAgentSession(); const userMessage = { role: 'user', content: text }; messages.value.push(userMessage); scrollMessagesToBottom(); await persistAgentMessage(userMessage); const res = await runAutonomousAgent({ goal: buildGoal(text), sessionId: sessionId.value, operationId: operationId.value || undefined, conversationId: conversationId.value || undefined, spaceId: userStore.spaceId, spaceCode: userStore.spaceCode, confirmed: false, maxTurns: 12 }); const data = res?.data || res || {}; const responseText = appendAgentResponse(data); if (responseText) await persistAgentMessage({ role: 'assistant', content: responseText }) } catch (error) { const errorMessage = { role: 'assistant', content: error?.message || 'Agent 调用失败，请稍后重试。' }; messages.value.push(errorMessage); scrollMessagesToBottom(); await persistAgentMessage(errorMessage) } finally { loading.value = false; scrollMessagesToBottom() } }
async function pollAgentCompletion(data) {
  if (data.status !== 'RUNNING' || !operationId.value) return data
  messages.value.push({ role: 'assistant', content: '已继续处理，完成后会在这里显示结果，请稍候。' }); scrollMessagesToBottom()
  for (let attempt = 0; attempt < 150; attempt++) {
    await new Promise((resolve) => window.setTimeout(resolve, 2000))
    const res = await getAutonomousAgentOperation(operationId.value, { spaceId: userStore.spaceId, spaceCode: userStore.spaceCode })
    data = res?.data || res || {}
    if (data.status !== 'RUNNING') return data
  }
  return { ...data, message: '任务仍在后台执行，尚未返回最终结果。请稍后查询状态，不要重复创建。' }
}
async function continueWithConfirmation() { if (!operationId.value || loading.value) return; loading.value = true; needsConfirmation.value = false; try { await ensureAgentSession(); const latestUserMessage = [...messages.value].reverse().find(item => item.role === 'user' && item.content?.trim()); const confirmationGoal = latestUserMessage?.content?.trim() || '用户已明确确认上一轮待确认操作，请继续执行上一轮任务，不要重新询问确认。'; const payload = { goal: confirmationGoal, operationId: operationId.value, sessionId: sessionId.value, conversationId: conversationId.value, spaceId: userStore.spaceId, spaceCode: userStore.spaceCode, confirmed: true, maxTurns: 12 }; if (confirmationArgument.value && confirmationValue.value !== null && confirmationValue.value !== undefined) { const key = `confirmed${confirmationArgument.value.charAt(0).toUpperCase()}${confirmationArgument.value.slice(1)}`; payload[key] = String(confirmationValue.value); } const res = await runAutonomousAgent(payload); const data = res?.data || res || {}; const responseText = appendAgentResponse(data); if (responseText) await persistAgentMessage({ role: 'assistant', content: responseText }) } catch (error) { const errorMessage = { role: 'assistant', content: error?.message || '继续执行或读取执行结果失败，请查询状态后再操作。' }; messages.value.push(errorMessage); scrollMessagesToBottom(); await persistAgentMessage(errorMessage) } finally { loading.value = false; scrollMessagesToBottom() } }
async function resetConversation() { if (loading.value) return; messages.value = []; operationId.value = ''; conversationId.value = ''; needsConfirmation.value = false; confirmationArgument.value = ''; confirmationValue.value = null; renderedToolResultCount.value = 0; sessionId.value = null; localStorage.removeItem(conversationStorageKey()); try { await ensureAgentSession() } catch (error) { message.error(error?.message || '创建新会话失败') } }
async function restoreConversation() {
  if (!operationId.value) return
  try {
    const res = await getAutonomousAgentOperation(operationId.value, { spaceId: userStore.spaceId, spaceCode: userStore.spaceCode })
    const data = res?.data || res || {}
    if (!data || !data.operationId) return
    if (!isAutonomousOperation(data)) {
      // 旧快照可能指向固定流程记录；自主 Agent 不得打开固定流程的结果。
      operationId.value = ''
      conversationId.value = ''
      messages.value = []
      localStorage.removeItem(conversationStorageKey())
      return
    }
    appendAgentResponse(data)
    if (data.status === 'RUNNING') {
      loading.value = true
      appendAgentResponse(await pollAgentCompletion(data))
      loading.value = false
    }
  } catch (error) {
    // 操作可能已被清理或当前用户无权访问，避免下次打开继续恢复失效会话。
    operationId.value = ''
    conversationId.value = ''
    localStorage.removeItem(conversationStorageKey())
  }
}
function handlePageContext(event) { pageOverride.value = event?.detail || null }
function saveConversationSnapshot() {
  if (!operationId.value && !conversationId.value && !messages.value.length) return
  localStorage.setItem(conversationStorageKey(), JSON.stringify({
    sessionId: sessionId.value,
    operationId: operationId.value,
    conversationId: conversationId.value,
    messages: messages.value,
    renderedToolResultCount: renderedToolResultCount.value
  }))
}
function clampFabPosition(left, top) { const width = fabRef.value?.offsetWidth || 142; const height = fabRef.value?.offsetHeight || 44; return { left: Math.max(8, Math.min(left, window.innerWidth - width - 8)), top: Math.max(8, Math.min(top, window.innerHeight - height - 8)) } }
function startDrag(event) { if (event.button !== undefined && event.button !== 0) return; const rect = fabRef.value?.getBoundingClientRect(); if (!rect) return; dragging.value = true; dragged = false; dragOffset = { x: event.clientX - rect.left, y: event.clientY - rect.top }; fabRef.value?.setPointerCapture?.(event.pointerId); window.addEventListener('pointermove', moveDrag); window.addEventListener('pointerup', stopDrag, { once: true }) }
function moveDrag(event) { if (!dragging.value) return; const next = clampFabPosition(event.clientX - dragOffset.x, event.clientY - dragOffset.y); if (Math.abs(next.left - fabPosition.value.left) > 2 || Math.abs(next.top - fabPosition.value.top) > 2) dragged = true; fabPosition.value = next }
function stopDrag() { dragging.value = false; window.removeEventListener('pointermove', moveDrag); window.setTimeout(() => { dragged = false }, 0) }
function handleFabClick(event) { if (dragged) { event.preventDefault(); return } open.value = true }
function resetFabPosition() { fabPosition.value = clampFabPosition(window.innerWidth - 166, window.innerHeight - 76) }
watch(() => route.fullPath, () => { if (route.path !== '/explore') pageOverride.value = null })
watch(() => messages.value.length, () => scrollMessagesToBottom(), { flush: 'post' })
watch(open, (visible) => { if (visible) scrollMessagesToBottom('auto') })
watch(() => [messages.value, operationId.value, conversationId.value, sessionId.value, renderedToolResultCount.value], saveConversationSnapshot, { deep: true })
onMounted(async () => { window.addEventListener('data-agent-context', handlePageContext); resetFabPosition(); window.addEventListener('resize', resetFabPosition); try { const saved = JSON.parse(localStorage.getItem(conversationStorageKey()) || '{}'); sessionId.value = saved.sessionId ? String(saved.sessionId) : null; operationId.value = saved.operationId || ''; conversationId.value = saved.conversationId || ''; messages.value = Array.isArray(saved.messages) ? saved.messages : []; renderedToolResultCount.value = Number(saved.renderedToolResultCount || 0); await ensureAgentSession(); await loadAgentSessionMessages(); await restoreConversation() } catch (error) { resetConversation() } })
onBeforeUnmount(() => { window.removeEventListener('data-agent-context', handlePageContext); window.removeEventListener('pointermove', moveDrag); window.removeEventListener('resize', resetFabPosition) })
</script>
<style lang="scss" scoped>
.data-agent-fab{position:fixed;z-index:1200;display:inline-flex;align-items:center;gap:7px;border:0;border-radius:22px;padding:10px 16px;color:#fff;background:linear-gradient(135deg,#6d5dfc,#3b82f6);box-shadow:0 8px 24px rgba(59,130,246,.32);cursor:grab;touch-action:none;user-select:none}.data-agent-fab:active{cursor:grabbing}.data-agent-fab:hover{transform:translateY(-2px)}.data-agent-context{display:flex;flex-direction:column;gap:3px;padding:10px 12px;border-radius:10px;background:#f5f7fb;margin-bottom:12px}.data-agent-context>div{display:flex;align-items:center;justify-content:space-between}.data-agent-context span,.data-agent-context small{color:#8b95a7;font-size:12px}.data-agent-presets{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:14px}.data-agent-messages{min-height:360px;max-height:calc(100vh - 330px);overflow-y:auto;padding:4px 0}.data-agent-empty{color:#8b95a7;line-height:1.7;padding:32px 12px;text-align:center}.data-agent-message{display:flex;flex-direction:column;gap:4px;margin-bottom:14px}.data-agent-message.user{align-items:flex-end}.data-agent-message-content{max-width:88%;padding:9px 12px;border-radius:10px;white-space:pre-wrap;line-height:1.6;background:#f3f5f8}.data-agent-message.user .data-agent-message-content{color:#fff;background:#4f46e5}.data-agent-message-role{color:#98a2b3;font-size:12px}.data-agent-input{display:block}.data-agent-input-box{position:relative;width:100%}.data-agent-input-box .ant-input{padding-right:62px;padding-bottom:38px}.data-agent-send{position:absolute;right:8px;bottom:8px;z-index:1}.data-agent-confirm{margin-top:8px}
</style>
