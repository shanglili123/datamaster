
import request from '@/utils/request'

// 查询脱敏白名单列表
export function listDesensitizeWhitelist(query) {
  return request({
    url: '/cat/desensitizeWhitelist/list',
    method: 'get',
    params: query
  })
}

// 查询脱敏白名单详细
export function getDesensitizeWhitelist(id) {
  return request({
    url: '/cat/desensitizeWhitelist/' + id,
    method: 'get'
  })
}

// 新增脱敏白名单
export function addDesensitizeWhitelist(data) {
  return request({
    url: '/cat/desensitizeWhitelist',
    method: 'post',
    data: data
  })
}

// 修改脱敏白名单
export function updateDesensitizeWhitelist(data) {
  return request({
    url: '/cat/desensitizeWhitelist',
    method: 'put',
    data: data
  })
}

// 删除脱敏白名单
export function delDesensitizeWhitelist(id) {
  return request({
    url: '/cat/desensitizeWhitelist/' + id,
    method: 'delete'
  })
}

