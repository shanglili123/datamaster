
import request from '@/utils/request'

// 查询主题域管理列表
export function listThemeDomain(query) {
  return request({
    url: '/mdl/themeDomain/list',
    method: 'get',
    params: query
  })
}

// 查询主题域管理详细
export function getThemeDomain(id) {
  return request({
    url: '/mdl/themeDomain/' + id,
    method: 'get'
  })
}

// 新增主题域管理
export function addThemeDomain(data) {
  return request({
    url: '/mdl/themeDomain',
    method: 'post',
    data: data
  })
}

// 修改主题域管理
export function updateThemeDomain(data) {
  return request({
    url: '/mdl/themeDomain',
    method: 'put',
    data: data
  })
}

// 删除主题域管理
export function delThemeDomain(id) {
  return request({
    url: '/mdl/themeDomain/' + id,
    method: 'delete'
  })
}

