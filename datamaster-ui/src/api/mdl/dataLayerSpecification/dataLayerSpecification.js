
import request from '@/utils/request'

// 查询数仓分层-规范管理列表
export function listDataLayerSpecification(query) {
  return request({
    url: '/mdl/dataLayerSpecification/list',
    method: 'get',
    params: query
  })
}

// 查询数仓分层-规范管理详细
export function getDataLayerSpecification(id) {
  return request({
    url: '/mdl/dataLayerSpecification/' + id,
    method: 'get'
  })
}

// 新增数仓分层-规范管理
export function addDataLayerSpecification(data) {
  return request({
    url: '/mdl/dataLayerSpecification',
    method: 'post',
    data: data
  })
}

// 修改数仓分层-规范管理
export function updateDataLayerSpecification(data) {
  return request({
    url: '/mdl/dataLayerSpecification',
    method: 'put',
    data: data
  })
}

// 删除数仓分层-规范管理
export function delDataLayerSpecification(id) {
  return request({
    url: '/mdl/dataLayerSpecification/' + id,
    method: 'delete'
  })
}

