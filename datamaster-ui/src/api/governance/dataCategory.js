import request from '@/utils/request.js';

// 查询数据分类列表
export function listDataCategory(query) {
    return request({
        url: '/cat/dataCategory/list',
        method: 'get',
        params: query
    });
}

// 查询数据分类列表（全部）
export function listDataCategoryAll(query) {
    return request({
        url: '/cat/dataCategory/listAll',
        method: 'get',
        params: query
    });
}

// 查询数据分类树
export function selectDataCategoryTree(query) {
    return request({
        url: '/cat/dataCategory/selectTree',
        method: 'get',
        params: query
    });
}

// 查询数据分类详情
export function getDataCategory(id) {
    return request({
        url: '/cat/dataCategory/' + id,
        method: 'get'
    });
}

// 新增数据分类
export function addDataCategory(data) {
    return request({
        url: '/cat/dataCategory',
        method: 'post',
        data: data
    });
}

// 修改数据分类
export function updateDataCategory(data) {
    return request({
        url: '/cat/dataCategory',
        method: 'put',
        data: data
    });
}

// 删除数据分类
export function delDataCategory(ids) {
    return request({
        url: '/cat/dataCategory/' + ids,
        method: 'delete'
    });
}

// 查询类目列表
export function listDataCategoryCat(query) {
    return request({
        url: '/cat/dataCategoryCat/list',
        method: 'get',
        params: query
    });
}

// 查询数据等级列表
export function listDataLevel(query) {
    return request({
        url: '/cat/dataLevel/listAll',
        method: 'get',
        params: query
    });
}
