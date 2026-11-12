import request from '@/utils/request'

const decisionUrl = (id) => '/ai/decision/' + encodeURIComponent(String(id))

export function listDecisionTasks(params) {
  return request({ url: '/ai/decision/tasks', method: 'get', params })
}
export function disposeDecision(data) {
  return request({ url: '/ai/decision/disposition', method: 'post', data })
}
export function configureDecisionEffect(data) {
  return request({ url: '/ai/decision/effect-plan', method: 'post', data })
}

export function getDecision(id) {
  return request({ url: decisionUrl(id), method: 'get' })
}

export function calculateDecision(data) {
  return request({ url: '/ai/decision/calculate', method: 'post', data })
}

export function prepareDecision({ decisionId, revision, candidateId }) {
  return request({ url: '/ai/decision/prepare', method: 'post', data: { decisionId, revision, candidateId } })
}

export function confirmDecision({ decisionId, revision, candidateId, previewDigest, acknowledgeUnknownImpact }) {
  return request({ url: '/ai/decision/confirm', method: 'post', data: { decisionId, revision, candidateId, previewDigest, acknowledgeUnknownImpact } })
}

export function amendDecision({ decisionId, revision, candidateId, type, targetId, objectKey, inputParams }) {
  return request({ url: '/ai/decision/amend', method: 'post', data: { decisionId, revision, candidateId, type, targetId, objectKey, inputParams } })
}

export function resumeDecision({ decisionId, revision, runId, resumeToken }) {
  return request({ url: '/ai/decision/resume', method: 'post', data: { decisionId, revision, runId, resumeToken } })
}

export function getDecisionStatus(id) {
  return request({ url: decisionUrl(id) + '/status', method: 'get' })
}

export function reviewDecision(id) {
  return request({ url: decisionUrl(id) + '/review', method: 'post', data: {} })
}
