
import request from '@/utils/request'

// 查询清洗规则目录列表
export function listAttCleanCat(query) {
  return request({
    url: '/tax/category/list/CLEAN',
    method: 'get',
    params: query
  })
}

// 查询清洗规则目录详细
export function getAttCleanCat(ID) {
  return request({
    url: '/tax/category/' + ID,
    method: 'get'
  })
}

// 新增清洗规则目录
export function addAttCleanCat(data) {
  return request({
    url: '/tax/category',
    method: 'post',
    data: { ...data, catType: 'CLEAN' }
  })
}

// 修改清洗规则目录
export function updateAttCleanCat(data) {
  return request({
    url: '/tax/category',
    method: 'put',
    data: { ...data, catType: 'CLEAN' }
  })
}

// 删除清洗规则目录
export function delAttCleanCat(ID) {
  return request({
    url: '/tax/category/' + ID,
    method: 'delete',
    params: { catType: 'CLEAN' }
  })
}
