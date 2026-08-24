import request from '@/utils/request'

// 按概念表绑定ID查询列映射列表
export function listPropertyColumn(conceptTableId) {
  return request({
    url: '/ont/property-column/list',
    method: 'get',
    params: { conceptTableId }
  })
}

// 批量保存列映射
export function batchSavePropertyColumns(conceptTableId, data) {
  return request({
    url: '/ont/property-column/batch?conceptTableId=' + conceptTableId,
    method: 'post',
    data: data
  })
}

// 删除列映射
export function delPropertyColumn(id) {
  return request({
    url: '/ont/property-column/' + id,
    method: 'delete'
  })
}
