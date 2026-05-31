
import request from '@/utils/request';

// 查询主题列表
export function listAttTheme(query) {
    return request({
        url: '/tax/theme/list',
        method: 'get',
        params: query
    });
}

// 查询主题详细
export function getAttTheme(id) {
    return request({
        url: '/tax/theme/' + id,
        method: 'get'
    });
}

// 新增主题
export function addAttTheme(data) {
    return request({
        url: '/tax/theme',
        method: 'post',
        data: data
    });
}

// 修改主题
export function updateAttTheme(data) {
    return request({
        url: '/tax/theme',
        method: 'put',
        data: data
    });
}

// 删除主题
export function delAttTheme(id) {
    return request({
        url: '/tax/theme/' + id,
        method: 'delete'
    });
}
// 获取主题的查询接口
export function getThemeList(query) {
    return request({
        url: '/tax/theme/getAttThemeListByReqVO',
        method: 'get',
        params: query
    });
}

