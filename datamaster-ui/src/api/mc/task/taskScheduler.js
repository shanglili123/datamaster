import request from '@/utils/request'

// 查询数据集成调度信息列表
export function listTaskScheduler(query) {
  return request({
    url: '/cat/taskScheduler/list',
    method: 'get',
    params: query
  })
}

// 查询数据集成调度信息详细
export function getTaskScheduler(id) {
  return request({
    url: '/cat/taskScheduler/' + id,
    method: 'get'
  })
}

// 新增数据集成调度信息
export function addTaskScheduler(data) {
  return request({
    url: '/cat/taskScheduler',
    method: 'post',
    data: data
  })
}

// 修改数据集成调度信息
export function updateTaskScheduler(data) {
  return request({
    url: '/cat/taskScheduler',
    method: 'put',
    data: data
  })
}

// 删除数据集成调度信息
export function delTaskScheduler(id) {
  return request({
    url: '/cat/taskScheduler/' + id,
    method: 'delete'
  })
}
