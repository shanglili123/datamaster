
import request from '@/utils/request.js'

// 查询数据元目录管理列表
export function listAttDataElemCat(query) {
  return request({
    url: '/tax/category/list/DATA_ELEM',
    method: 'get',
    params: query
  })
}

// 查询数据元目录管理详细
export function getAttDataElemCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增数据元目录管理
export function addAttDataElemCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'DATA_ELEM' }
  })
}

// 修改数据元目录管理
export function updateAttDataElemCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'DATA_ELEM' }
  })
}

// 删除数据元目录管理
export function delAttDataElemCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'DATA_ELEM' }
  })
}
