
import request from '@/utils/request'

// 查询数据分类列表
export function listDataCategory(query) {
  return request({
    url: '/cat/dataCategory/list',
    method: 'get',
    params: query
  })
}

// 查询数据分类详细
export function getDataCategory(id) {
  return request({
    url: '/cat/dataCategory/' + id,
    method: 'get'
  })
}

// 查询数据分类树
export function selectTreeDataCategory(query) {
  return request({
    url: '/cat/dataCategory/selectTree',
    method: 'get',
    params: query
  })
}

// 新增数据分类
export function addDataCategory(data) {
  return request({
    url: '/cat/dataCategory',
    method: 'post',
    data: data
  })
}

// 修改数据分类
export function updateDataCategory(data) {
  return request({
    url: '/cat/dataCategory',
    method: 'put',
    data: data
  })
}

// 删除数据分类
export function delDataCategory(id) {
  return request({
    url: '/cat/dataCategory/' + id,
    method: 'delete'
  })
}

// 批量定级
export function batchDataLevel(data) {
  return request({
    url: '/cat/dataCategory/batchDataLevel',
    method: 'PUT',
    data: data
  })
}

