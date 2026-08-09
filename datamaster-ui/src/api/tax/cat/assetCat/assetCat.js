
import request from '@/utils/request'

// 查询数据资产目录管理列表
export function listAttAssetCat(query) {
  return request({
    url: '/tax/category/list/ASSET',
    method: 'get',
    params: query
  })
}

// 查询数据资产目录管理详细
export function getAttAssetCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增数据资产目录管理
export function addAttAssetCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'ASSET' }
  })
}

// 修改数据资产目录管理
export function updateAttAssetCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'ASSET' }
  })
}

// 删除数据资产目录管理
export function delAttAssetCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'ASSET' }
  })
}
