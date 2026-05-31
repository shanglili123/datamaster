
import request from '@/utils/request'

// 查询数仓分层管理列表
export function listDataLayer(query) {
  return request({
    url: '/mdl/dataLayer/list',
    method: 'get',
    params: query
  })
}

// 查询数仓分层管理详细
export function getDataLayer(id) {
  return request({
    url: '/mdl/dataLayer/' + id,
    method: 'get'
  })
}

// 查询数仓分层管理树
export function treeDataLayer() {
  return request({
    url: '/mdl/dataLayer/tree',
    method: 'get'
  })
}

// 新增数仓分层管理
export function addDataLayer(data) {
  return request({
    url: '/mdl/dataLayer',
    method: 'post',
    data: data
  })
}

// 修改数仓分层管理
export function updateDataLayer(data) {
  return request({
    url: '/mdl/dataLayer',
    method: 'put',
    data: data
  })
}

// 删除数仓分层管理
export function delDataLayer(id) {
  return request({
    url: '/mdl/dataLayer/' + id,
    method: 'delete'
  })
}

