import request from '@/utils/request'

// 查询属性列表
export function listProperty(query) {
  return request({
    url: '/ont/property/page',
    method: 'get',
    params: query
  })
}

// 按真实概念 ID 汇总属性，保留服务端逐概念的归属与权限校验。
export async function listPropertiesForConcepts(conceptIds = []) {
  const ids = [...new Set(conceptIds.filter(id => id != null && id !== ''))]
  const groups = await Promise.all(ids.map(async conceptId => {
    const rows = []
    const pageSize = 200
    let pageNum = 1
    while (true) {
      const res = await listProperty({ conceptId, pageNum, pageSize })
      const data = res.data
      const page = Array.isArray(data) ? data : (data?.rows || [])
      rows.push(...page)
      if (!page.length || Array.isArray(data) || rows.length >= Number(data?.total ?? rows.length)) break
      pageNum += 1
    }
    return rows
  }))
  return groups.flat()
}

// 查询属性详情
export function getProperty(id) {
  return request({
    url: '/ont/property/' + id,
    method: 'get'
  })
}

// 新增属性
export function addProperty(data) {
  return request({
    url: '/ont/property',
    method: 'post',
    data: data
  })
}

// 修改属性
export function updateProperty(data) {
  return request({
    url: '/ont/property',
    method: 'put',
    data: data
  })
}

// 设置概念主属性（联合主键可传多个属性ID）
export function setPrimaryProperties(data) {
  return request({
    url: '/ont/property/primary',
    method: 'put',
    data
  })
}

// 删除属性
export function delProperty(id) {
  return request({
    url: '/ont/property/' + id,
    method: 'delete'
  })
}
