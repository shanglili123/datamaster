
import request from '@/utils/request'

// 查询质量探查目录列表
export function listAttQualityCat(query) {
  return request({
    url: '/tax/category/list/QUALITY',
    method: 'get',
    params: query
  })
}

// 查询质量探查目录详细
export function getAttQualityCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'get'
  })
}

// 新增质量探查目录
export function addAttQualityCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'QUALITY' }
  })
}

// 修改质量探查目录
export function updateAttQualityCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'QUALITY' }
  })
}

// 删除质量探查目录
export function delAttQualityCat(id) {
  return request({
    url: '/tax/category/' + id,
    method: 'delete',
    params: { catType: 'QUALITY' }
  })
}
