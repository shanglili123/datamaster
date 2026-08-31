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

// 预览概念表绑定物理表数据（filters: {物理列: 过滤值}，可选，服务端 WHERE 过滤）
export function previewConceptTable(id, limit = 20, filters = {}) {
  const params = { limit }
  const filterKeys = Object.keys(filters)
  if (filterKeys.length) {
    // 仅传非空过滤值
    const valid = {}
    filterKeys.forEach(k => {
      const v = String(filters[k] == null ? '' : filters[k]).trim()
      if (v) valid[k] = v
    })
    if (Object.keys(valid).length) {
      params.filters = JSON.stringify(valid)
    }
  }
  return request({
    url: '/ont/concept-table/preview/' + id,
    method: 'get',
    params: params
  })
}
