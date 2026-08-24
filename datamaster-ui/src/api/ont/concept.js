import request from '@/utils/request'

// 查询概念列表
export function listConcept(query) {
  return request({
    url: '/ont/concept/page',
    method: 'get',
    params: query
  })
}

// 查询概念详情
export function getConcept(id) {
  return request({
    url: '/ont/concept/' + id,
    method: 'get'
  })
}

// 新增概念
export function addConcept(data) {
  return request({
    url: '/ont/concept',
    method: 'post',
    data: data
  })
}

// 修改概念
export function updateConcept(data) {
  return request({
    url: '/ont/concept',
    method: 'put',
    data: data
  })
}

// 删除概念
export function delConcept(id) {
  return request({
    url: '/ont/concept/' + id,
    method: 'delete'
  })
}
