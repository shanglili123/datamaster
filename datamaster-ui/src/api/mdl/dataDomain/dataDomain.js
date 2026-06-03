
import request from '@/utils/request'

// 查询数据域管理列表
export function listDataDomain(query) {
  return request({
    url: '/mdl/dataDomain/list',
    method: 'get',
    params: query
  })
}

// 查询数据域管理详细
export function getDataDomain(id) {
  return request({
    url: '/mdl/dataDomain/' + id,
    method: 'get'
  })
}

// 新增数据域管理
export function addDataDomain(data) {
  return request({
    url: '/mdl/dataDomain',
    method: 'post',
    data: data
  })
}

// 修改数据域管理
export function updateDataDomain(data) {
  return request({
    url: '/mdl/dataDomain',
    method: 'put',
    data: data
  })
}

// 删除数据域管理
export function delDataDomain(id) {
  return request({
    url: '/mdl/dataDomain/' + id,
    method: 'delete'
  })
}

