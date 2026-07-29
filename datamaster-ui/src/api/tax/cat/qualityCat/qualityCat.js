
import request from '@/utils/request'

// 查询质量探查类目列表
export function listAttQualityCat(query) {
  return request({
    url: '/tax/qualityCat/list',
    method: 'get',
    params: query
  })
}

// 查询质量探查类目详细
export function getAttQualityCat(id) {
  return request({
    url: '/tax/qualityCat/' + id,
    method: 'get'
  })
}

// 新增质量探查类目
export function addAttQualityCat(data) {
  return request({
    url: '/tax/qualityCat',
    method: 'post',
    data: data
  })
}

// 修改质量探查类目
export function updateAttQualityCat(data) {
  return request({
    url: '/tax/qualityCat',
    method: 'put',
    data: data
  })
}

// 删除质量探查类目
export function delAttQualityCat(id) {
  return request({
    url: '/tax/qualityCat/' + id,
    method: 'delete'
  })
}

