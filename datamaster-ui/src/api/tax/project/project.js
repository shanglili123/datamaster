
import request from '@/utils/request';

// 查询空间列表
export function listAttProject(query) {
    return request({
        url: '/tax/project/list',
        method: 'get',
        params: query
    });
}

// 查询当前用户所属的空间列表
export function currentUser() {
    return request({
        url: '/tax/project/currentUser/list',
        method: 'get'
    });
}

// 查询当前用户所属的空间列表
export function noProjectUser(query) {
    return request({
        url: '/tax/project/noProjectUser/list',
        method: 'post',
        params: query
    });
}

// 查询空间详细
export function getAttProject(id) {
    return request({
        url: '/tax/project/' + id,
        method: 'get'
    });
}

// 获取当前用户是非具备用户添加和空间管理员
export function addUserAndProject(id) {
    return request({
        url: '/tax/project/addUserAndProject/' + id,
        method: 'get'
    });
}

// 修改状态
export function editProjectStatus(id, status) {
    return request({
        url: `/tax/project/editProjectStatus/${id}/${status}`,
        method: 'get'
    });
}

// 新增空间
export function addAttProject(data) {
    return request({
        url: '/tax/project',
        method: 'post',
        data: data
    });
}

// 修改空间
export function updateAttProject(data) {
    return request({
        url: '/tax/project',
        method: 'put',
        data: data
    });
}

// 删除空间
export function delAttProject(id) {
    return request({
        url: '/tax/project/' + id,
        method: 'delete'
    });
}

