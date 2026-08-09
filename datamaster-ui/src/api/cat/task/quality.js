import request from '@/utils/request'

// 查询探查任务绑定的质量探查任务列表
export function listTaskQuality(catTaskId) {
  return request({
    url: `/cat/taskQuality/listByTask/${catTaskId}`,
    method: 'get'
  })
}

// 绑定质量探查任务
export function bindTaskQuality(data) {
  return request({
    url: '/cat/taskQuality/bind',
    method: 'post',
    data: data
  })
}

// 解绑质量探查任务
export function unbindTaskQuality(id) {
  return request({
    url: `/cat/taskQuality/unbind/${id}`,
    method: 'delete'
  })
}

// 批量查询数据源表的最近一次质量探查结果摘要
export function batchQualitySummary(data) {
  return request({
    url: '/cat/task/batchQualitySummary',
    method: 'post',
    data: data
  })
}

// 查询单个数据源表的最近一次质量探查结果摘要
export function latestQualitySummary(datasourceId, tableName) {
  return request({
    url: '/cat/task/latestQualitySummary',
    method: 'get',
    params: { datasourceId, tableName }
  })
}

// 验证质量规则数据格式
export function verifyQualityRule(data) {
  return request({
    url: '/cat/task/verifyQualityRule',
    method: 'post',
    data: data
  })
}

// 生成错误数据SQL
export function generateErrorDataSql(data) {
  return request({
    url: '/cat/task/generateErrorDataSql',
    method: 'post',
    data: data
  })
}

// 生成正确数据SQL
export function generateValidDataSql(data) {
  return request({
    url: '/cat/task/generateValidDataSql',
    method: 'post',
    data: data
  })
}
