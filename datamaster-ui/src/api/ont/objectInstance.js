import request from '@/utils/request'

// 查询本体下的对象集（概念 + 物理表绑定）
export function listObjectSets(ontologyId) {
  return request({
    url: '/ont/object-instance/object-sets',
    method: 'get',
    params: { ontologyId }
  })
}

// 分页查询对象实例
export function queryObjects(query) {
  return request({
    url: '/ont/object-instance/objects',
    method: 'get',
    params: query
  })
}

// 查询对象血缘（四维度聚合：数据/决策/版本/权限）
export function getObjectLineage(conceptId, spaceId, spaceCode) {
  return request({
    url: `/ont/object-instance/lineage/${conceptId}`,
    method: 'get',
    params: { spaceId, spaceCode }
  })
}

// 关系跳转：按源对象实例值查询目标概念对象（同页内嵌展开，d8 值过滤，不跨表 JOIN）
export function queryRelatedObjects(data) {
  return request({
    url: '/ont/object-instance/related',
    method: 'post',
    data
  })
}

// 对象行操作-提交预览（新增/修改/删除）：生成 SQL + dry-run，按需建审批链
export function rowPreview(data) {
  return request({
    url: '/ont/object-instance/row/preview',
    method: 'post',
    data
  })
}

// 对象行操作-确认执行（审批通过 + 执行；弹框确定 = 审批通过）
export function rowConfirm(data) {
  return request({
    url: '/ont/object-instance/row/confirm',
    method: 'post',
    data
  })
}
