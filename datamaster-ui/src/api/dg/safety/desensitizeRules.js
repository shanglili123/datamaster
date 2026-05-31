
import request from '@/utils/request'

// 查询脱敏规则列表
export function listDesensitizeRules(query) {
  return request({
    url: '/cat/desensitizeRules/list',
    method: 'get',
    params: query
  })
}

// 查询脱敏规则详细
export function getDesensitizeRules(id) {
  return request({
    url: '/cat/desensitizeRules/' + id,
    method: 'get'
  })
}

// 新增脱敏规则
export function addDesensitizeRules(data) {
  return request({
    url: '/cat/desensitizeRules',
    method: 'post',
    data: data
  })
}

// 修改脱敏规则
export function updateDesensitizeRules(data) {
  return request({
    url: '/cat/desensitizeRules',
    method: 'put',
    data: data
  })
}

// 删除脱敏规则
export function delDesensitizeRules(id) {
  return request({
    url: '/cat/desensitizeRules/' + id,
    method: 'delete'
  })
}

