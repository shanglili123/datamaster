import request from '@/utils/request.js';

// 查询脱敏规则列表
export function listDesensitizeRule(query) {
    return request({
        url: '/cat/desensitizeRules/list',
        method: 'get',
        params: query
    });
}

// 查询脱敏规则详情
export function getDesensitizeRule(id) {
    return request({
        url: '/cat/desensitizeRules/' + id,
        method: 'get'
    });
}

// 新增脱敏规则
export function addDesensitizeRule(data) {
    return request({
        url: '/cat/desensitizeRules',
        method: 'post',
        data: data
    });
}

// 修改脱敏规则
export function updateDesensitizeRule(data) {
    return request({
        url: '/cat/desensitizeRules',
        method: 'put',
        data: data
    });
}

// 删除脱敏规则
export function delDesensitizeRule(ids) {
    return request({
        url: '/cat/desensitizeRules/' + ids,
        method: 'delete'
    });
}

// 导出脱敏规则
export function exportDesensitizeRule(query) {
    return request({
        url: '/cat/desensitizeRules/export',
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
