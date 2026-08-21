
import request from '@/utils/request'

// 查询探查任务实例列表
export function listProbeTaskInstance(query) {
  return request({
    url: '/metadata/probeTaskInstance/list',
    method: 'get',
    params: query
  })
}

// 按表查询探查历史记录（多次探查结果）
export function listProbeHistoryByTable(query) {
  return request({
    url: '/metadata/probeTaskInstance/listByTable',
    method: 'get',
    params: query
  })
}

// 查询探查任务实例详细
export function getProbeTaskInstance(id) {
  return request({
    url: '/metadata/probeTaskInstance/' + id,
    method: 'get'
  })
}

// 新增探查任务实例
export function addProbeTaskInstance(data) {
  return request({
    url: '/metadata/qualityTask',
    method: 'post',
    data: data
  })
}

// 修改探查任务实例
export function updateProbeTaskInstance(data) {
  return request({
    url: '/metadata/probeTaskInstance',
    method: 'put',
    data: data
  })
}

// 删除探查任务实例
export function delProbeTaskInstance(id) {
  return request({
    url: '/metadata/probeTaskInstance/' + id,
    method: 'delete'
  })
}
//发送消息
export function doSendMessage(id) {
  return request({
    url: '/metadata/probeTaskInstance/sendMessage',
    method: 'POST',
    params: {id}
  })
}
// 探查任务实例维度统计
export function statisticsEvaluateOne(id) {
  return request({
    url: '/metadata/evaluateLog/statisticsEvaluateOne/' + id,
    method: 'get',
  })
}
// 探查任务实例详情：治理数据量变化趋势
export function statisticsEvaluateTow(query) {
  console.log("🚀 ~ statisticsEvaluateTow ~ query:", query)
  return request({
    url: '/metadata/evaluateLog/statisticsEvaluateTow',
    method: 'get',
    params: query
  })
}
// 探查任务实例规则列表
export function statisticsEvaluateTable(id) {
  return request({
    url: '/metadata/evaluateLog/statisticsEvaluateTable/' + id,
    method: 'get',
  })
}
//  错误数据
export function pageErrorData(query) {
  return request({
    url: '/metadata/evaluateLog/pageErrorData',
    method: 'get',
    params: query
  })
}
// 修改接口 数据、状态，都是这个接口
export function updateErrorData(data) {
  return request({
    url: '/metadata/evaluateLog/updateErrorData',
    method: 'post',
    data: data
  })
}


