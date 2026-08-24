import request from '@/utils/request'

// 查询概念表绑定列表
export function listConceptTable(query) {
  return request({
    url: '/ont/concept-table/list',
    method: 'get',
    params: query
  })
}

// 查询概念表绑定分页
export function pageConceptTable(query) {
  return request({
    url: '/ont/concept-table/page',
    method: 'get',
    params: query
  })
}

// 查询概念表绑定详情
export function getConceptTable(id) {
  return request({
    url: '/ont/concept-table/' + id,
    method: 'get'
  })
}

// 新增概念表绑定
export function addConceptTable(data) {
  return request({
    url: '/ont/concept-table',
    method: 'post',
    data: data
  })
}

// 修改概念表绑定
export function updateConceptTable(data) {
  return request({
    url: '/ont/concept-table',
    method: 'put',
    data: data
  })
}

// 删除概念表绑定
export function delConceptTable(id) {
  return request({
    url: '/ont/concept-table/' + id,
    method: 'delete'
  })
}
