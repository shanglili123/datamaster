import request from '@/utils/request.js';

// 查询字段元数据列表
export function listColumn(query) {
    return request({
        url: '/cat/column/list',
        method: 'get',
        params: query
    });
}

// 查询字段元数据详细
export function getColumn(id) {
    return request({
        url: '/cat/column/' + id,
        method: 'get'
    });
}

// 新增字段元数据
export function addColumn(data) {
    return request({
        url: '/cat/column',
        method: 'post',
        data: data
    });
}

// 修改字段元数据
export function updateColumn(data) {
    return request({
        url: '/cat/column',
        method: 'put',
        data: data
    });
}

// 删除字段元数据
export function delColumn(id) {
    return request({
        url: '/cat/column/' + id,
        method: 'delete'
    });
}

// 修改字段元数据状态
export function updateColumnStatus(data) {
    return request({
        url: '/cat/column/toggle',
        method: 'post',
        data
    });
}

// 暂存字段元数据
export function draftColumn(data) {
    return request({
        url: '/cat/column/draft',
        method: 'post',
        data: data
    });
}

// 获取库元数据可删除的列
export function batchDeleteCheck(id) {
    return request({
        url: '/cat/column/batchDeleteCheck/' + id,
        method: 'get'
    });
}
