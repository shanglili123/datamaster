import request from '@/utils/request'

// 查询属性列表
export function listProperty(query) {
  return request({
    url: '/ont/property/page',
    method: 'get',
    params: query
  })
}

// 查询属性详情
export function getProperty(id) {
  return request({
    url: '/ont/property/' + id,
    method: 'get'
  })
}

// 新增属性
export function addProperty(data) {
  return request({
    url: '/ont/property',
    method: 'post',
    data: data
  })
}

// 修改属性
export function updateProperty(data) {
  return request({
    url: '/ont/property',
    method: 'put',
    data: data
  })
}

// 设置概念主属性（联合主键可传多个属性ID）
export function setPrimaryProperties(data) {
  return request({
    url: '/ont/property/primary',
    method: 'put',
    data
  })
}

// 删除属性
export function delProperty(id) {
  return request({
    url: '/ont/property/' + id,
    method: 'delete'
  })
}
