import request from '@/utils/request'

export function listWorkflow(query) {
  return request({ url: '/ont/workflow/page', method: 'get', params: query })
}
export function getWorkflow(id) {
  return request({ url: '/ont/workflow/' + id, method: 'get' })
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
