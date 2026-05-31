
import request from '@/utils/request'

// 查询数据资产-矢量列表
export function listDaAssetGeo(query) {
  return request({
    url: '/ast/daAssetGeo/list',
    method: 'get',
    params: query
  })
}

// 查询数据资产-矢量详细
export function getDaAssetGeo(id) {
  return request({
    url: '/ast/daAssetGeo/' + id,
    method: 'get'
  })
}

// 新增数据资产-矢量
export function addDaAssetGeo(data) {
  return request({
    url: '/ast/daAssetGeo',
    method: 'post',
    data: data
  })
}

// 修改数据资产-矢量
export function updateDaAssetGeo(data) {
  return request({
    url: '/ast/daAssetGeo',
    method: 'put',
    data: data
  })
}

// 删除数据资产-矢量
export function delDaAssetGeo(id) {
  return request({
    url: '/ast/daAssetGeo/' + id,
    method: 'delete'
  })
}

