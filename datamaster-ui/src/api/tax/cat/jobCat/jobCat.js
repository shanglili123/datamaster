
import request from '@/utils/request';

// 查询作业类目管理列表
export function listAttJobCat(query) {
    return request({
        url: '/tax/jobCat/list',
        method: 'get',
        params: query
    });
}

// 查询作业类目管理详细
export function getAttJobCat(id) {
    return request({
        url: '/tax/jobCat/' + id,
        method: 'get'
    });
}

// 新增作业类目管理
export function addAttJobCat(data) {
    return request({
        url: '/tax/jobCat',
        method: 'post',
        data: data
    });
}

// 修改作业类目管理
export function updateAttJobCat(data) {
    return request({
        url: '/tax/jobCat',
        method: 'put',
        data: data
    });
}

// 删除作业类目管理
export function delAttJobCat(id) {
    return request({
        url: '/tax/jobCat/' + id,
        method: 'delete'
    });
}

