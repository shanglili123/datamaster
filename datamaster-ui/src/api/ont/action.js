import request from '@/utils/request'

// ========== Action CRUD ==========

export function listAction(query) {
  return request({ url: '/ont/action/page', method: 'get', params: query })
}

export function getActionsByOntology(ontologyId) {
  return request({ url: '/ont/action/list', method: 'get', params: { ontologyId } })
}

export function getAction(id) {
  return request({ url: '/ont/action/' + id, method: 'get' })
}

export function addAction(data) {
  return request({ url: '/ont/action', method: 'post', data })
}

export function updateAction(data) {
  return request({ url: '/ont/action', method: 'put', data })
}

export function delAction(id) {
  return request({ url: '/ont/action/' + id, method: 'delete' })
}

// ========== Execution ==========

export function submitExecution(data) {
  return request({ url: '/ont/action/execution/submit', method: 'post', data })
}

export function approveExecution(data) {
  return request({ url: '/ont/action/execution/approve', method: 'post', data })
}

export function rejectExecution(data) {
  return request({ url: '/ont/action/execution/reject', method: 'post', data })
}

export function runExecution(id) {
  return request({ url: '/ont/action/execution/run/' + id, method: 'post' })
}

export function rollbackExecution(id) {
  return request({ url: '/ont/action/execution/rollback/' + id, method: 'post' })
}

export function getExecution(id) {
  return request({ url: '/ont/action/execution/' + id, method: 'get' })
}

export function listExecution(query) {
  return request({ url: '/ont/action/execution/page', method: 'get', params: query })
}

export function getPendingApprovals(ontologyId) {
  return request({ url: '/ont/action/execution/pending', method: 'get', params: { ontologyId } })
}

export function getApprovalChain(executionId) {
  return request({ url: '/ont/action/execution/approval-chain/' + executionId, method: 'get' })
}

// ========== AI Generate ==========

export function aiGenerateActions(data) {
  return request({ url: '/ont/ai-generate/actions', method: 'post', data })
}
