import request from '@/utils/request'

const base = '/ont/metric-source-approval'
export const listMetricSourceApprovals = params => request({ url: `${base}/page`, method: 'get', params })
export const getMetricSourceSettings = params => request({ url: `${base}/settings`, method: 'get', params })
export const listMetricSourceHistory = params => request({ url: `${base}/history`, method: 'get', params })
export const approveMetricSource = data => request({ url: `${base}/approve`, method: 'post', data })
export const revokeMetricSource = data => request({ url: `${base}/revoke`, method: 'post', data })
export const getMetricSourceStatus = params => request({ url: `${base}/status`, method: 'get', params })
