
import request from '@/utils/request'

// 查询数据服务目录管理列表
export function listAttApiCat(query) {
  return request({
    url: '/tax/category/list/API',
    method: 'get',
    params: query
  })
}

// 查询数据服务目录管理详细
export function getAttApiCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增数据服务目录管理
export function addAttApiCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'API' }
  })
}

// 修改数据服务目录管理
export function updateAttApiCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'API' }
  })
}

// 删除数据服务目录管理
export function delAttApiCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'API' }
  })
}
