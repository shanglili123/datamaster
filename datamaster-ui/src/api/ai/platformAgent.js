import request from '@/utils/request'

// 启动平台数据智能体自动运营流程
export function startPlatformAgent(data) {
  return request({
    url: '/ai/agent/operation/start',
    method: 'post',
    data
  })
}

// 查询平台数据智能体流程实例
export function getPlatformAgentOperation(id) {
  return request({
    url: '/ai/agent/operation/' + id,
    method: 'get'
  })
}

// 查询当前空间最近的未完成流程，用于离开页面或刷新后的自动恢复
export function getPlatformAgentOperationPage(params) {
  return request({
    url: '/ai/agent/operation/page',
    method: 'get',
    params
  })
}

// 在当前对话中补充连接信息并继续
export function resumePlatformAgent(id, data) {
  return request({
    url: '/ai/agent/operation/' + id + '/resume',
    method: 'post',
    data
  })
}

// 从失败步骤继续执行，不重复数据源、探查和资产步骤
export function retryPlatformAgent(id) {
  return request({
    url: '/ai/agent/operation/' + id + '/retry',
    method: 'post'
  })
}

// 人工确认发布并执行元数据探查任务
export function confirmPlatformAgentMetadata(id) {
  return request({
    url: '/ai/agent/operation/' + id + '/confirm-metadata',
    method: 'post'
  })
}

export function cancelPlatformAgent(id) {
  return request({
    url: '/ai/agent/operation/' + id + '/cancel',
    method: 'post'
  })
}
