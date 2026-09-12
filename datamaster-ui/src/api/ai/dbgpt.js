import request from '@/utils/request'

// 底层适配器同步接口（页面统一称为“决策智能体数据源同步”）
export function syncDatasourceToDbgpt(id) {
  return request({
    url: '/ast/dataSource/syncToDbgpt/' + id,
    method: 'post'
  })
}

export function syncAllDatasourceToDbgpt() {
  return request({
    url: '/ast/dataSource/syncAllToDbgpt',
    method: 'post'
  })
}
