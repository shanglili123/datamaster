
import request from '@/utils/request.js'

// 查询数据资产字段列表
export function listDaAssetColumn(query) {
  return request({
    url: '/ast/assetColumn/list',
    method: 'get',
    params: query
  })
}

// 查询数据资产字段详细
export function getDaAssetColumn(id) {
  return request({
    url: '/ast/assetColumn/' + id,
    method: 'get'
  })
}

// 新增数据资产字段
export function addDaAssetColumn(data) {
  return request({
    url: '/ast/assetColumn',
    method: 'post',
    data: data
  })
}

// 修改数据资产字段
export function updateDaAssetColumn(data) {
  return request({
    url: '/ast/assetColumn',
    method: 'put',
    data: data
  })
}

// 删除数据资产字段
export function delDaAssetColumn(id) {
  return request({
    url: '/ast/assetColumn/' + id,
    method: 'delete'
  })
}

// 查询数据资产字段与空间关联关系列表
export function listAssetColumnSpaceRel(query) {
  return request({
    url: '/ast/assetColumnSpaceRel/list',
    method: 'get',
    params: query
  })
}

// 查询数据资产字段与空间关联关系详细
export function getAssetColumnSpaceRel(id) {
  return request({
    url: '/ast/assetColumnSpaceRel/' + id,
    method: 'get'
  })
}

// 新增数据资产字段与空间关联关系
export function addAssetColumnSpaceRel(data) {
  return request({
    url: '/ast/assetColumnSpaceRel',
    method: 'post',
    data: data
  })
}

// 修改数据资产字段与空间关联关系
export function updateAssetColumnSpaceRel(data) {
  return request({
    url: '/ast/assetColumnSpaceRel',
    method: 'put',
    data: data
  })
}

// 删除数据资产字段与空间关联关系
export function delAssetColumnSpaceRel(id) {
  return request({
    url: '/ast/assetColumnSpaceRel/' + id,
    method: 'delete'
  })
}
// 預覽

export function preview(data) {
  return request({
    url: '/ast/asset/preview',
    method: 'post',
    data: data
  })
}

