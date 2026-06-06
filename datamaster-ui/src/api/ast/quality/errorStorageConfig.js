import request from '@/utils/request'

export function getErrorStorageConfig() {
  return request({
    url: '/quality/errorStorageConfig/getConfig',
    method: 'get'
  })
}

export function setErrorStorageConfig(datasourceId, tableName) {
  return request({
    url: '/quality/errorStorageConfig/setConfig',
    method: 'post',
    params: { datasourceId, tableName }
  })
}

export function initErrorStorageTable() {
  return request({
    url: '/quality/errorStorageConfig/initTable',
    method: 'post'
  })
}

export function listDaDatasource(query) {
  return request({
    url: '/ast/dataSource/list',
    method: 'get',
    params: query
  })
}
