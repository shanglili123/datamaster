
import request from '@/utils/request'

// 查询数据分级列表
export function listDataLevel(query) {
  return request({
    url: '/cat/dataLevel/list',
    method: 'get',
    params: query
  })
}

// 查询所有数据分级下拉列表
export function listAllDataLevel(query) {
  return request({
    url: '/cat/dataLevel/listAll',
    method: 'get',
    params: query
  })
}

// 查询数据分级详细
export function getDataLevel(id) {
  return request({
    url: '/cat/dataLevel/' + id,
    method: 'get'
  })
}

// 新增数据分级
export function addDataLevel(data) {
  return request({
    url: '/cat/dataLevel',
    method: 'post',
    data: data
  })
}

// 修改数据分级
export function updateDataLevel(data) {
  return request({
    url: '/cat/dataLevel',
    method: 'put',
    data: data
  })
}

// 删除数据分级
export function delDataLevel(id) {
  return request({
    url: '/cat/dataLevel/' + id,
    method: 'delete'
  })
}

