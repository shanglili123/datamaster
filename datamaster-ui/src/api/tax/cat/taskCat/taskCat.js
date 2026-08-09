
import request from '@/utils/request'

// 查询数据集成任务目录管理列表
export function listAttTaskCat(query) {
  return request({
    url: '/tax/category/list/TASK',
    method: 'get',
    params: query
  })
}

// 查询数据集成任务目录管理详细
export function getAttTaskCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增数据集成任务目录管理
export function addAttTaskCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'TASK' }
  })
}

// 修改数据集成任务目录管理
export function updateAttTaskCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'TASK' }
  })
}

// 删除数据集成任务目录管理
export function delAttTaskCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'TASK' }
  })
}
