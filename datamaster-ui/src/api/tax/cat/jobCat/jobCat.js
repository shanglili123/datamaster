
import request from '@/utils/request';

// 查询作业目录管理列表
export function listAttJobCat(query) {
    return request({
        url: '/tax/category/list/DATA_DEV',
        method: 'get',
        params: query
    });
}

// 查询作业目录管理详细
export function getAttJobCat(id) {
    return request({
        url: '/tax/category/' + id,
        method: 'get'
    });
}

// 新增作业目录管理
export function addAttJobCat(data) {
    return request({
        url: '/tax/category',
        method: 'post',
        data: { ...data, catType: 'DATA_DEV' }
    });
}

// 修改作业目录管理
export function updateAttJobCat(data) {
    return request({
        url: '/tax/category',
        method: 'put',
        data: { ...data, catType: 'DATA_DEV' }
    });
}

// 删除作业目录管理
export function delAttJobCat(id) {
    return request({
        url: '/tax/category/' + id,
        method: 'delete',
        params: { catType: 'DATA_DEV' }
    });
}
