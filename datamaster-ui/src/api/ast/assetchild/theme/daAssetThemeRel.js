
import request from '@/utils/request'

// 查询数据资产-主题关联关系列表
export function listDaAssetThemeRel(query) {
  return request({
    url: '/ast/daAssetThemeRel/list',
    method: 'get',
    params: query
  })
}

// 查询数据资产-主题关联关系详细
export function getDaAssetThemeRel(id) {
  return request({
    url: '/ast/daAssetThemeRel/' + id,
    method: 'get'
  })
}

// 新增数据资产-主题关联关系
export function addDaAssetThemeRel(data) {
  return request({
    url: '/ast/daAssetThemeRel',
    method: 'post',
    data: data
  })
}

// 修改数据资产-主题关联关系
export function updateDaAssetThemeRel(data) {
  return request({
    url: '/ast/daAssetThemeRel',
    method: 'put',
    data: data
  })
}

// 删除数据资产-主题关联关系
export function delDaAssetThemeRel(id) {
  return request({
    url: '/ast/daAssetThemeRel/' + id,
    method: 'delete'
  })
}

