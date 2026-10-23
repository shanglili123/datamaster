
import request from '@/utils/request'

// 查询数据资产操作申请列表
export function listDaAssetOperateApply(query) {
  return request({
    url: '/ast/assetOperateApply/list',
    method: 'get',
    params: query
  })
}

// 查询数据资产操作申请详细
export function getDaAssetOperateApply(id) {
  return request({
    url: '/ast/assetOperateApply/' + id,
    method: 'get'
  })
}

// 新增数据资产操作申请
export function addDaAssetOperateApply(data) {
  return request({
    url: '/ast/assetOperateApply',
    method: 'post',
    data: data
  })
}

// 修改数据资产操作申请
export function updateDaAssetOperateApply(data) {
  return request({
    url: '/ast/assetOperateApply',
    method: 'put',
    data: data
  })
}

// 删除数据资产操作申请
export function delDaAssetOperateApply(id) {
  return request({
    url: '/ast/assetOperateApply/' + id,
    method: 'delete'
  })
}

// 提交申请
export function submitDaAssetOperateApply(id) {
  return request({
    url: `/ast/assetOperateApply/${id}/submit`,
    method: 'post'
  })
}

// 审批通过
export function approveDaAssetOperateApply(id, data = {}) {
  return request({
    url: `/ast/assetOperateApply/${id}/approve`,
    method: 'post',
    data: { id, ...data }
  })
}

// 驳回申请
export function rejectDaAssetOperateApply(id, data) {
  return request({
    url: `/ast/assetOperateApply/${id}/reject`,
    method: 'post',
    data: { id, ...(data || {}) }
  })
}

// 执行已审批申请
export function executeDaAssetOperateApply(id) {
  return request({
    url: `/ast/assetOperateApply/${id}/execute`,
    method: 'post'
  })
}

