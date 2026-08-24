import request from '@/utils/request'

// 按关系ID查询列映射列表
export function listRelationColumn(relationId) {
  return request({
    url: '/ont/relation-column/list',
    method: 'get',
    params: { relationId }
  })
}

// 批量保存关系列映射
export function batchSaveRelationColumns(relationId, data) {
  return request({
    url: '/ont/relation-column/batch?relationId=' + relationId,
    method: 'post',
    data: data
  })
}

// 删除关系列映射
export function delRelationColumn(id) {
  return request({
    url: '/ont/relation-column/' + id,
    method: 'delete'
  })
}
