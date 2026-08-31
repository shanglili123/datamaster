import request from '@/utils/request.js';

// 查询脱敏白名单列表
export function listDesensitizeWhitelist(query) {
    return request({
        url: '/cat/desensitizeWhitelist/list',
        method: 'get',
        params: query
    });
}

// 查询脱敏白名单详情
export function getDesensitizeWhitelist(id) {
    return request({
        url: '/cat/desensitizeWhitelist/' + id,
        method: 'get'
    });
}

// 新增脱敏白名单
export function addDesensitizeWhitelist(data) {
    return request({
        url: '/cat/desensitizeWhitelist',
        method: 'post',
        data: data
    });
}

// 修改脱敏白名单
export function updateDesensitizeWhitelist(data) {
    return request({
        url: '/cat/desensitizeWhitelist',
        method: 'put',
        data: data
    });
}

// 删除脱敏白名单
export function delDesensitizeWhitelist(ids) {
    return request({
        url: '/cat/desensitizeWhitelist/' + ids,
        method: 'delete'
    });
}

// 导出脱敏白名单
export function exportDesensitizeWhitelist(query) {
    return request({
        url: '/cat/desensitizeWhitelist/export',
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

// 查询用户列表
export function listUser(query) {
    return request({
        url: '/system/user/list',
        method: 'get',
        params: query
    });
}
