
import request from '@/utils/request.js'

// 查询检索的资源列表
export function listRetriever(query) {
  return request({
    url: '/app/retriever/list',
    method: 'get',
    params: query
  })
}

export function listByMessage(query) {
  return request({
    url: '/app/retriever/listByMessage',
    method: 'get',
    params: query
  })
}

// 查询检索的资源详细
export function getRetriever(id) {
  return request({
    url: '/app/retriever/' + id,
    method: 'get'
  })
}

// 新增检索的资源
export function addRetriever(data) {
  return request({
    url: '/app/retriever',
    method: 'post',
    data: data
  })
}

// 修改检索的资源
export function updateRetriever(data) {
  return request({
    url: '/app/retriever',
    method: 'put',
    data: data
  })
}

// 删除检索的资源
export function delRetriever(id) {
  return request({
    url: '/app/retriever/' + id,
    method: 'delete'
  })
}

