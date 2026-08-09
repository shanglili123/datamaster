
import request from '@/utils/request.js'

// 查询逻辑模型目录管理列表
export function listAttModelCat(query) {
  return request({
    url: '/tax/category/list/MODEL',
    method: 'get',
    params: query
  })
}

// 查询逻辑模型目录管理详细
export function getAttModelCat(ID) {
  return request({
    url: '/tax/category/' + ID,
    method: 'get'
  })
}

// 新增逻辑模型目录管理
export function addAttModelCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'MODEL' }
  })
}

// 修改逻辑模型目录管理
export function updateAttModelCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'MODEL' }
  })
}

// 删除逻辑模型目录管理
export function delAttModelCat(ID) {
  return request({
    url: '/tax/category/' + ID,
    method: 'delete',
    params: { catType: 'MODEL' }
  })
}
