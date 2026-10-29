import request from '@/utils/request'

export function listMetric(params) {
  return request({ url: '/ont/metric/page', method: 'get', params })
}
export function getMetric(id) {
  return request({ url: `/ont/metric/${id}`, method: 'get' })
}
export function addMetric(data) {
  return request({ url: '/ont/metric', method: 'post', data })
}
export function updateMetric(data) {
  return request({ url: '/ont/metric', method: 'put', data })
}
export function delMetric(id) {
  return request({ url: `/ont/metric/${id}`, method: 'delete' })
}
export function validateMetric(id, revision) {
  return request({ url: `/ont/metric/${id}/validate`, method: 'post', params: { revision } })
}
export function publishMetric(id, revision) {
  return request({ url: `/ont/metric/${id}/publish`, method: 'post', params: { revision } })
}
export function disableMetric(id, revision) {
  return request({ url: `/ont/metric/${id}/disable`, method: 'post', params: { revision } })
}
export function listMetricVersions(id) {
  return request({ url: `/ont/metric/${id}/versions`, method: 'get' })
}
export function getMetricVersion(id, version) {
  return request({ url: `/ont/metric/${id}/versions/${version}`, method: 'get' })
}
