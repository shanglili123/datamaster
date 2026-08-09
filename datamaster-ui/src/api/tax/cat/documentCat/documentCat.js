
import request from '@/utils/request'

// 查询标准信息分类管理列表
export function listAttDocumentCat(query) {
  return request({
    url: '/tax/category/list/DOCUMENT',
    method: 'get',
    params: query
  })
}

// 查询标准信息分类管理详细
export function getAttDocumentCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增标准信息分类管理
export function addAttDocumentCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'DOCUMENT' }
  })
}

// 修改标准信息分类管理
export function updateAttDocumentCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'DOCUMENT' }
  })
}

// 删除标准信息分类管理
export function delAttDocumentCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'DOCUMENT' }
  })
}
