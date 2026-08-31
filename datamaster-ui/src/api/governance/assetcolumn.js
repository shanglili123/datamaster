import request from '@/utils/request.js';

// 查询字段脱敏绑定列表
export function listAssetcolumnBinding(query) {
    return request({
        url: '/cat/standardsDesensitizeList/list',
        method: 'get',
        params: query
    });
}

// 查询字段脱敏绑定详情
export function getAssetcolumnBinding(id) {
    return request({
        url: '/cat/standardsDesensitizeList/' + id,
        method: 'get'
    });
}

// 新增字段脱敏绑定
export function addAssetcolumnBinding(data) {
    return request({
        url: '/cat/standardsDesensitizeList',
        method: 'post',
        data: data
    });
}

// 修改字段脱敏绑定
export function updateAssetcolumnBinding(data) {
    return request({
        url: '/cat/standardsDesensitizeList',
        method: 'put',
        data: data
    });
}

// 删除字段脱敏绑定
export function delAssetcolumnBinding(ids) {
    return request({
        url: '/cat/standardsDesensitizeList/' + ids,
        method: 'delete'
    });
}

// 导出字段脱敏绑定
export function exportAssetcolumnBinding(query) {
    return request({
        url: '/cat/standardsDesensitizeList/export',
        method: 'post',
        params: query,
        responseType: 'blob'
    });
}

// ===== 下拉数据接口 =====

// 查询数据分类下拉列表
export function listDataCategoryAll(query) {
    return request({
        url: '/cat/dataCategory/listAll',
        method: 'get',
        params: query
    });
}

// 查询资产列表
export function listAsset(query) {
    return request({
        url: '/ast/asset/list',
        method: 'get',
        params: query
    });
}

// 查询资产字段列表
export function listAssetColumn(query) {
    return request({
        url: '/ast/assetColumn/list',
        method: 'get',
        params: query
    });
}
