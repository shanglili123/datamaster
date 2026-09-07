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

// 元数据登记缺失时，直接读取绑定物理表字段
export function listConceptTablePhysicalColumns(id) {
  return request({
    url: '/ont/concept-table/columns/' + id,
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

// 预览概念表绑定物理表数据（filters: 类型化查询 spec 对象，可选，JSON.stringify 后服务端白名单编译 WHERE）
// spec 形如：{"groups":[{"connector":"AND","negate":false,"filters":[{"field":"<物理列>","op":"eq","value":x}]}],
//            "orderBy":[{"field":"<物理列>","dir":"asc"}],"columns":["<物理列>"],"keyword":"模糊"}
export function previewConceptTable(id, limit = 20, filters) {
  const params = { limit }
  if (filters && Object.keys(filters).length) {
    params.filters = JSON.stringify(filters)
  }
  return request({
    url: '/ont/concept-table/preview/' + id,
    method: 'get',
    params: params
  })
}
