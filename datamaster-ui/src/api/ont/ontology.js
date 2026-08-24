import request from '@/utils/request'

// 查询本体列表
export function listOntology(query) {
  return request({
    url: '/ont/ontology/page',
    method: 'get',
    params: query
  })
}

// 查询本体详情
export function getOntology(id) {
  return request({
    url: '/ont/ontology/' + id,
    method: 'get'
  })
}

// 新增本体
export function addOntology(data) {
  return request({
    url: '/ont/ontology',
    method: 'post',
    data: data
  })
}

// 修改本体
export function updateOntology(data) {
  return request({
    url: '/ont/ontology',
    method: 'put',
    data: data
  })
}

// 删除本体
export function delOntology(id) {
  return request({
    url: '/ont/ontology/' + id,
    method: 'delete'
  })
}
