
import request from '@/utils/request'

/** 查询数据源列表 */
export function listDppDatasource(query) {
  return request({
    url: '/col/datasource/list',
    method: 'get',
    params: query
  })
}

/** 查询数据源详情 */
export function getDppDatasource(id) {
  return request({
    url: '/col/datasource/' + id,
    method: 'get'
  })
}

/** 新增数据源 */
export function addDppDatasource(data) {
  return request({
    url: '/col/datasource',
    method: 'post',
    data
  })
}

/** 修改数据源 */
export function updateDppDatasource(data) {
  return request({
    url: '/col/datasource',
    method: 'put',
    data
  })
}

/** 删除数据源（删除前后端校验是否被数据资产或元数据采集任务引用） */
export function delDppDatasource(id) {
  return request({
    url: '/col/datasource/' + id,
    method: 'delete'
  })
}

/** 连接测试 */
export function testDppDatasource(id) {
  return request({
    url: '/col/datasource/test/' + id,
    method: 'post'
  })
}

/** 连接测试（未保存时传表单参数） */
export function testDppDatasourceForm(data) {
  return request({
    url: '/col/datasource/test',
    method: 'post',
    data
  })
}

/** 启用/禁用数据源 */
export function toggleDppDatasourceStatus(id) {
  return request({
    url: '/col/datasource/status/' + id,
    method: 'put'
  })
}

