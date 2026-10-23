import request from '@/utils/request'

export function listWorkflow(query) {
  return request({ url: '/ont/workflow/page', method: 'get', params: query })
}
export function getWorkflow(id) {
  return request({ url: '/ont/workflow/' + id, method: 'get' })
}
export function listWorkflowVersions(id) {
  return request({ url: `/ont/workflow/${id}/versions`, method: 'get' })
}
export function getWorkflowVersion(id, version) {
  return request({ url: `/ont/workflow/${id}/versions/${version}`, method: 'get' })
}
export function rollbackWorkflowVersion(id, version) {
  return request({ url: `/ont/workflow/${id}/versions/${version}/rollback`, method: 'post' })
}
export function addWorkflow(data) {
  return request({ url: '/ont/workflow', method: 'post', data })
}
export function updateWorkflow(data) {
  return request({ url: '/ont/workflow', method: 'put', data })
}
export function delWorkflow(id) {
  return request({ url: '/ont/workflow/' + id, method: 'delete' })
}
export function validateWorkflow(id) {
  return request({ url: `/ont/workflow/${id}/validate`, method: 'post' })
}
export function publishWorkflow(id) {
  return request({ url: `/ont/workflow/${id}/publish`, method: 'post' })
}
export function disableWorkflow(id) {
  return request({ url: `/ont/workflow/${id}/disable`, method: 'post' })
}
export function runWorkflow(id, data = {}) {
  return request({ url: `/ont/workflow/${id}/run`, method: 'post', data })
}
export function listWorkflowRuns(id, query = {}) {
  return request({ url: `/ont/workflow/${id}/runs/page`, method: 'get', params: query })
}
export function getWorkflowMonitor(limit = 20) {
  return request({ url: '/ont/workflow/monitor', method: 'get', params: { limit } })
}
export function getWorkflowRun(runId) {
  return request({ url: `/ont/workflow/runs/${runId}`, method: 'get' })
}
export function cancelWorkflowRun(runId) {
  return request({ url: `/ont/workflow/runs/${runId}/cancel`, method: 'post' })
}
export function resumeWorkflowRun(runId, data = {}) {
  return request({ url: `/ont/workflow/runs/${runId}/resume`, method: 'post', data })
}
export function compensateWorkflowRun(runId) {
  return request({ url: `/ont/workflow/runs/${runId}/compensate`, method: 'post' })
}

export function retryWorkflowCompensation(runId) {
  return request({ url: `/ont/workflow/runs/${runId}/compensate/retry`, method: 'post' })
}
