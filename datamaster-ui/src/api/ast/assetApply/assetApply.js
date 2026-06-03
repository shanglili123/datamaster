
import request from '@/utils/request'

// 查询数据资产申请列表
export function listDaAssetApply(query) {
  return request({
    url: '/ast/assetApply/list',
    method: 'get',
    params: query
  })
}

// 查询数据资产申请详细
export function getDaAssetApply(id) {
  return request({
    url: '/ast/assetApply/' + id,
    method: 'get'
  })
}

// 新增数据资产申请
export function addDaAssetApply(data) {
  return request({
    url: '/ast/assetApply',
    method: 'post',
    data: data
  })
}

// 修改数据资产申请
export function updateDaAssetApply(data) {
  return request({
    url: '/ast/assetApply',
    method: 'put',
    data: data
  })
}

// 删除数据资产申请
export function delDaAssetApply(id) {
  return request({
    url: '/ast/assetApply/' + id,
    method: 'delete'
  })
}
// 請求
export function queryServiceForwarding(data) {
  return request({
    url: '/ast/api/queryServiceForwarding',
    method: 'post',
    data: data
  })
}

