
import request from '@/utils/request'

// 查询数据分类-类目列表
export function listDataCategoryCat(query) {
  return request({
    url: '/cat/dataCategoryCat/list',
    method: 'get',
    params: query
  })
}

// 查询数据分类-类目详细
export function getDataCategoryCat(id) {
  return request({
    url: '/cat/dataCategoryCat/' + id,
    method: 'get'
  })
}

// 新增数据分类-类目
export function addDataCategoryCat(data) {
  return request({
    url: '/cat/dataCategoryCat',
    method: 'post',
    data: data
  })
}

// 修改数据分类-类目
export function updateDataCategoryCat(data) {
  return request({
    url: '/cat/dataCategoryCat',
    method: 'put',
    data: data
  })
}

// 删除数据分类-类目
export function delDataCategoryCat(id) {
  return request({
    url: '/cat/dataCategoryCat/' + id,
    method: 'delete'
  })
}

