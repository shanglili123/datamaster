
import request from '@/utils/request'

// 查询标签管理列表
export function listAttTag(query) {
  return request({
    url: '/tax/category/list/TAG',
    method: 'get',
    params: query
  })
}
export function listDict(query) {
  return request({
    url: '/tax/tag/listDict',
    method: 'get',
    params: query
  })
}

// 查询标签管理详细
export function getAttTag(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增标签管理
export function addAttTag(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'TAG' }
  })
}

// 修改标签管理
export function updateAttTag(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'TAG' }
  })
}

// 删除标签管理
export function delAttTag(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'TAG' }
  })
}
