import request from '@/utils/request'

export function getErrorStorageConfig() {
  return request({
    url: '/col/errorStorageConfig/getConfig',
    method: 'get'
  })
}

export function setErrorStorageConfig(datasourceId, tableName) {
  return request({
    url: '/col/errorStorageConfig/setConfig',
    method: 'post',
    params: { datasourceId, tableName }
  })
}

export function listDaDatasource(query) {
  return request({
    url: '/ast/dataSource/list',
    method: 'get',
    params: query
  })
}
