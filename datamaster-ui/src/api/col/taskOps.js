import request from '@/utils/request.js'

export function getTaskOpsPolicy(taskId) {
  return request({
    url: '/col/etlTaskOps/policy',
    method: 'get',
    params: { taskId }
  })
}

export function saveTaskOpsPolicy(data) {
  return request({
    url: '/col/etlTaskOps/policy',
    method: 'post',
    data
  })
}

export function listTaskOpsEvents(params) {
  return request({
    url: '/col/etlTaskOps/events',
    method: 'get',
    params
  })
}
