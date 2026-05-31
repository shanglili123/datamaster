
import request from '@/utils/request'

// 查询来源系统列表
export function listSourceSystem(query) {
  return request({
    url: '/tax/sourceSystem/list',
    method: 'get',
    params: query
  })
}

// 查询所有来源系统列表
export function listValidSourceSystem(query) {
  return request({
    url: '/tax/sourceSystem/listValid',
    method: 'get',
    params: query
  })
}

// 查询来源系统详细
export function getSourceSystem(id) {
  return request({
    url: '/tax/sourceSystem/' + id,
    method: 'get'
  })
}

// 新增来源系统
export function addSourceSystem(data) {
  return request({
    url: '/tax/sourceSystem',
    method: 'post',
    data: data
  })
}

// 修改来源系统
export function updateSourceSystem(data) {
  return request({
    url: '/tax/sourceSystem',
    method: 'put',
    data: data
  })
}

// 删除来源系统
export function delSourceSystem(id) {
  return request({
    url: '/tax/sourceSystem/' + id,
    method: 'delete'
  })
}

