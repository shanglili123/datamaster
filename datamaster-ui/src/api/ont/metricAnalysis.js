import request from '@/utils/request'

export function queryMetric(data) {
  return request({ url: '/ont/metric-analysis/query', method: 'post', data, timeout: 70000 })
}
export function listMetricAnalysis(params) {
  return request({ url: '/ont/metric-analysis/page', method: 'get', params })
}
export function getMetricAnalysis(id) {
  return request({ url: `/ont/metric-analysis/${id}`, method: 'get' })
}
export function addMetricAnalysis(data) {
  return request({ url: '/ont/metric-analysis', method: 'post', data })
}
export function updateMetricAnalysis(data) {
  return request({ url: '/ont/metric-analysis', method: 'put', data })
}
export function delMetricAnalysis(id) {
  return request({ url: `/ont/metric-analysis/${id}`, method: 'delete' })
}
