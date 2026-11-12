import { inject } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { v4 as uuidv4 } from 'uuid'
import useUserStore from '@/store/system/user'

export const ASK_PATH = '/ai/ask'
export const METRIC_ANALYSIS_PATH = '/ont/metric-analysis'
const DRAFT_PREFIX = 'datamaster:metric-decision-draft:'
const DRAFT_TTL = 15 * 60 * 1000

export function routeText(value) {
  return typeof value === 'string' && value.length <= 200 ? value : ''
}
export function positiveRouteId(value) {
  const text = typeof value === 'number' ? String(value) : routeText(value)
  return /^[1-9][0-9]{0,18}$/.test(text) ? text : ''
}
export function decisionRouteId(value) {
  const text = routeText(value)
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(text) ? text : ''
}

// A short-lived query-intent draft only. Never stores rows, SQL, physical mappings or execution authority.
export function saveMetricDecisionDraft(user, ontologyId, query, metricName) {
  if (!positiveRouteId(ontologyId) || !positiveRouteId(user.spaceId) || !user.id) throw new Error('请先选择当前用户空间及本体')
  const id = uuidv4()
  const draft = {
    owner: String(user.id), spaceId: String(user.spaceId), spaceCode: String(user.spaceCode || ''),
    ontologyId: String(ontologyId), createdAt: Date.now(), metricName: String(metricName || '').slice(0, 200),
    query: {
      metricId: String(query.metricId), version: query.version,
      dimensionPropertyIds: (query.dimensionPropertyIds || []).map(String),
      filters: (query.filters || []).map(f => ({ propertyId: String(f.propertyId), operator: f.operator, value: f.value })),
      startTime: query.startTime || null, endTime: query.endTime || null, limit: query.limit, pageNum: query.pageNum || 1, requireComplete: Boolean(query.requireComplete),
      timeBucket: query.timeBucket || 'NONE', sortBy: query.sortBy || null,
      sortDirection: query.sortBy ? (query.sortDirection || 'ASC') : null
    }
  }
  // Reap expired drafts rather than accumulating sensitive filter values indefinitely.
  for (const key of Object.keys(sessionStorage)) {
    if (!key.startsWith(DRAFT_PREFIX)) continue
    try { const old = JSON.parse(sessionStorage.getItem(key)); if (!old?.createdAt || Date.now() - old.createdAt > DRAFT_TTL) sessionStorage.removeItem(key) }
    catch { sessionStorage.removeItem(key) }
  }
  const text = JSON.stringify(draft)
  if (text.length > 20000) throw new Error('分析条件过长，请缩小范围后进入决策')
  sessionStorage.setItem(DRAFT_PREFIX + id, text)
  return id
}
export function takeMetricDecisionDraft(user, token) {
  if (!decisionRouteId(token)) return null
  const key = DRAFT_PREFIX + token
  const text = sessionStorage.getItem(key)
  if (!text) return null
  let draft
  try { draft = JSON.parse(text) } catch { sessionStorage.removeItem(key); throw new Error('分析跳转草稿内容不可用') }
  if (draft.owner !== String(user.id) || draft.spaceId !== String(user.spaceId) || draft.spaceCode !== String(user.spaceCode || '')) throw new Error('分析草稿不属于当前用户或空间，请返回原空间')
  if (!draft.createdAt || Date.now() - draft.createdAt > DRAFT_TTL) { sessionStorage.removeItem(key); throw new Error('分析跳转草稿已过期，请回到指标分析重新进入') }
  if (!positiveRouteId(draft.ontologyId) || !positiveRouteId(draft.query?.metricId) || !Number.isInteger(draft.query?.version) || draft.query.version < 1) throw new Error('分析草稿不可用')
  sessionStorage.removeItem(key)
  return draft
}

export function useAnalysisNavigation() {
  const router = useRouter()
  const station = inject('spaceWorkstationNavigation', null)
  const user = useUserStore()
  async function open(path, query = {}) {
    // Both normal menu routing and /explore's virtual route use this protocol.
    if (await station?.openPage?.({ path, query })) return true
    await router.push({ path, query })
    return true
  }
  const spaceQuery = () => ({ spaceId: String(user.spaceId || '') })
  async function openMetricAnalysis({ ontologyId, metricId, version }) {
    if (!positiveRouteId(ontologyId) || !positiveRouteId(metricId) || !Number.isInteger(Number(version)) || Number(version) < 1) {
      message.warning('缺少已发布指标及明确版本'); return false
    }
    return open(METRIC_ANALYSIS_PATH, { ...spaceQuery(), ontologyId: String(ontologyId), metricId: String(metricId), version: String(version) })
  }
  async function openDecisionTask({ decisionId, ontologyId }) {
    if (!decisionRouteId(decisionId) || !positiveRouteId(ontologyId)) { message.warning('决策任务标识不可用'); return false }
    return open(ASK_PATH, { ...spaceQuery(), ontologyId: String(ontologyId), decisionId })
  }
  async function openApproval({ ontologyId, decisionId, executionId, runId }) {
    if (!positiveRouteId(ontologyId) || !decisionRouteId(decisionId)) return false
    return open(`/ont/workspace/${ontologyId}`, {
      ...spaceQuery(), tab: 'action', returnDecisionId: decisionId,
      ...(positiveRouteId(executionId) ? { executionId: String(executionId) } : {}),
      ...(positiveRouteId(runId) ? { runId: String(runId) } : {})
    })
  }
  async function openManualDecision({ ontologyId, query, metricName }) {
    const token = saveMetricDecisionDraft(user, ontologyId, query, metricName)
    return open(ASK_PATH, { ...spaceQuery(), ontologyId: String(ontologyId), decisionDraft: token })
  }
  return { open, openMetricAnalysis, openDecisionTask, openApproval, openManualDecision }
}
