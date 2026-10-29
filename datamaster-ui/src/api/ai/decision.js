import request from '@/utils/request'

const decisionUrl = (id) => '/ai/decision/' + encodeURIComponent(String(id))

export function getDecision(id) {
  return request({ url: decisionUrl(id), method: 'get' })
}

export function calculateDecision(data) {
  return request({ url: '/ai/decision/calculate', method: 'post', data })
}

export function prepareDecision({ decisionId, revision, candidateId }) {
  return request({ url: '/ai/decision/prepare', method: 'post', data: { decisionId, revision, candidateId } })
}

export function confirmDecision({ decisionId, revision, candidateId, previewDigest }) {
  return request({ url: '/ai/decision/confirm', method: 'post', data: { decisionId, revision, candidateId, previewDigest } })
}

export function getDecisionStatus(id) {
  return request({ url: decisionUrl(id) + '/status', method: 'get' })
}

export function reviewDecision(id) {
  return request({ url: decisionUrl(id) + '/review', method: 'post', data: {} })
}
