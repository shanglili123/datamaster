import request from '@/utils/request'

// 查询关系列表
export function listRelation(query) {
  return request({
    url: '/ont/relation/page',
    method: 'get',
    params: query
  })
}

// 查询关系详情
export function getRelation(id) {
  return request({
    url: '/ont/relation/' + id,
    method: 'get'
  })
}

// 新增关系
export function addRelation(data) {
  return request({
    url: '/ont/relation',
    method: 'post',
    data: data
  })
}

// 修改关系
export function updateRelation(data) {
  return request({
    url: '/ont/relation',
    method: 'put',
    data: data
  })
}

// 删除关系
export function delRelation(id) {
  return request({
    url: '/ont/relation/' + id,
    method: 'delete'
  })
}
