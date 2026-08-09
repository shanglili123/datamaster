import request from '@/utils/request'

// 查询采集任务实例列表
export function listTaskInstance(query) {
  return request({
    url: '/cat/taskInstance/list',
    method: 'get',
    params: query
  })
}

// 查询采集任务实例详细
export function getTaskInstance(id) {
  return request({
    url: '/cat/taskInstance/' + id,
    method: 'get'
  })
}

// 获取采集任务实例日志内容
export function getTaskInstanceLog(id) {
  return request({
    url: '/cat/taskInstance/log/' + id,
    method: 'get'
  })
}
