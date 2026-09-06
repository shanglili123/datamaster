import request from '@/utils/request'

// ========== Webhook CRUD ==========
export function listWebhook(query) {
  return request({ url: '/ont/webhook/page', method: 'get', params: query })
}
export function getWebhooksByOntology(ontologyId) {
  return request({ url: '/ont/webhook/list', method: 'get', params: { ontologyId } })
}
export function getWebhook(id) {
  return request({ url: '/ont/webhook/' + id, method: 'get' })
}
export function addWebhook(data) {
  return request({ url: '/ont/webhook', method: 'post', data })
}
export function updateWebhook(data) {
  return request({ url: '/ont/webhook', method: 'put', data })
}
export function delWebhook(id) {
  return request({ url: '/ont/webhook/' + id, method: 'delete' })
}

// ========== Webhook Callback Log ==========
export function listWebhookLog(query) {
  return request({ url: '/ont/webhook/log/page', method: 'get', params: query })
}
export function clearWebhookLogs(webhookId) {
  return request({ url: '/ont/webhook/log', method: 'delete', params: { webhookId } })
}