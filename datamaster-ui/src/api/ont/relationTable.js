import request from '@/utils/request'

// 按关系ID查询关联表绑定列表
export function listRelationTable(relationId) {
  return request({
    url: '/ont/relation-table/list',
    method: 'get',
    params: { relationId }
  })
}

// 查询关联表绑定分页
export function pageRelationTable(query) {
  return request({
    url: '/ont/relation-table/page',
    method: 'get',
    params: query
  })
}

// 查询关联表绑定详情
export function getRelationTable(id) {
  return request({
    url: '/ont/relation-table/' + id,
    method: 'get'
  })
}

// 新增关联表绑定
export function addRelationTable(data) {
  return request({
    url: '/ont/relation-table',
    method: 'post',
    data: data
  })
}

// 修改关联表绑定（保存所选字段）
export function updateRelationTable(data) {
  return request({
    url: '/ont/relation-table',
    method: 'put',
    data: data
  })
}

// 删除关联表绑定
export function delRelationTable(id) {
  return request({
    url: '/ont/relation-table/' + id,
    method: 'delete'
  })
}
