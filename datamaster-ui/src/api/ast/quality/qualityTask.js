
import request from '@/utils/request'

// 查询质量探查任务列表
export function listDppQualityTask(query) {
  return request({
    url: '/col/qualityTask/list',
    method: 'get',
    params: query
  })
}

// 查询质量探查任务详细
export function getDppQualityTask(id) {
  return request({
    url: '/col/qualityTask/' + id,
    method: 'get'
  })
}

// 新增质量探查任务
export function addDppQualityTask(data) {
  return request({
    url: '/col/qualityTask',
    method: 'post',
    data: data
  })
}

// 修改质量探查任务
export function updateDppQualityTask(data) {
  return request({
    url: '/col/qualityTask',
    method: 'put',
    data: data
  })
}

// 删除质量探查任务
export function delDppQualityTask(id) {
  return request({
    url: '/col/qualityTask/' + id,
    method: 'delete'
  })
}


//检验接口
export function verifyInterfaceValue(query) {
  return request({
    url: '/col/qualityTaskEvaluate/verifyInterfaceValue',
    method: 'get',
    params: query
  })
}
//错误抽查功能
export function validationErrorDataSql(data) {
  return request({
    url: '/col/qualityTaskEvaluate/validationErrorDataSql',
    method: 'post',
    data: data
  })
}
// 成功抽查功能
export function validationValidDataSql(data) {
  return request({
    url: '/col/qualityTaskEvaluate/validationValidDataSql',
    method: 'post',
    data: data
  })
}
//执行一次
export function startDppQualityTask(id) {
  return request({
    url: `/col/qualityTask/startCollectorQualityTask/${id}`,
    method: 'put',
  })
}
// 任务开关
export function updateDppQualityTaskStatus(query) {
  return request({
    url: '/col/qualityTask/updateCollectorQualityTaskStatus',
    method: 'post',
    data: query
  })
}

// 调度周期

export function updateDaDiscoveryTaskCronExpression(query) {
  return request({
    url: '/col/qualityTask/updateDaDiscoveryTaskCronExpression',
    method: 'post',
    data: query
  })
}// 质量探查 查询资产质量详情

export function getQualityTaskAsset(query) {
  return request({
    url: '/col/qualityTask/getQualityTaskAsset',
    method: 'get',
    params: query
  });
}
// 质量探查 日志质量探查维度统计

export function statisticsEvaluateAssetOne(query) {
  return request({
    url: '/col/evaluateLog/statisticsEvaluateAssetOne',
    method: 'get',
    params: query
  });
}
// 查看日志

export function probeTaskInstanceLogDetail(query) {
  return request({
    url: '/col/probeTaskInstance/logDetailCat',
    method: 'get',
    params: query
  });
}

