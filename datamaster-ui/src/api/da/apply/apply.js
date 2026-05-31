
import request from '@/utils/request.js'

// 查询API服务-申请列表
export function listApply(query) {
  return request({
    url: '/ast/apply/list',
    method: 'get',
    params: query
  })
}

// 查询API服务-申请详细
export function getApply(id) {
  return request({
    url: '/ast/apply/' + id,
    method: 'get'
  })
}

// 新增API服务-申请
export function addApply(data) {
  return request({
    url: '/ast/apply',
    method: 'post',
    data: data
  })
}

// 修改API服务-申请
export function updateApply(data) {
  return request({
    url: '/ast/apply',
    method: 'put',
    data: data
  })
}

// 删除API服务-申请
export function delApply(id) {
  return request({
    url: '/ast/apply/' + id,
    method: 'delete'
  })
}

