import request from '@/utils/request'

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
