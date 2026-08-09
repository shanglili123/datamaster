
import request from '@/utils/request'

// 查询数据开发目录管理列表
export function listAttDataDevCat(query) {
  return request({
    url: '/tax/category/list/DATA_DEV',
    method: 'get',
    params: query
  })
}

// 查询数据开发目录管理详细
export function getAttDataDevCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增数据开发目录管理
export function addAttDataDevCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'DATA_DEV' }
  })
}

// 修改数据开发目录管理
export function updateAttDataDevCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'DATA_DEV' }
  })
}

// 删除数据开发目录管理
export function delAttDataDevCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'DATA_DEV' }
  })
}
