
import request from '@/utils/request.js'

// 查询数据资产文档目录管理列表
export function listAttDocCat(query) {
  return request({
    url: '/tax/category/list/DOCUMENT',
    method: 'get',
    params: query
  })
}

// 查询数据资产文档目录管理详细
export function getAttDocCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增数据资产文档目录管理
export function addAttDocCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'DOCUMENT' }
  })
}

// 修改数据资产文档目录管理
export function updateAttDocCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'DOCUMENT' }
  })
}

// 删除数据资产文档目录管理
export function delAttDocCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'DOCUMENT' }
  })
}
