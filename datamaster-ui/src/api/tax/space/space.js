
import request from '@/utils/request';

// 查询空间列表
export function listSpace(query) {
    return request({
        url: '/tax/space/list',
        method: 'get',
        params: query
    });
}

// 查询当前用户所属的空间列表
export function currentUser() {
    return request({
        url: '/tax/space/currentUser/list',
        method: 'get'
    });
}

// 查询当前用户所属的空间列表
export function noSpaceUser(query) {
    return request({
        url: '/tax/space/noSpaceUser/list',
        method: 'post',
        params: query
    });
}

// 查询空间详细
export function getSpace(id) {
    return request({
        url: '/tax/space/' + id,
        method: 'get'
    });
}

// 获取当前用户是非具备用户添加和空间管理员
export function checkSpaceManagePermission(id) {
    return request({
        url: '/tax/space/managePermission/' + id,
        method: 'get'
    });
}

// 修改状态
export function editSpaceStatus(id, status) {
    return request({
        url: `/tax/space/editSpaceStatus/${id}/${status}`,
        method: 'get'
    });
}

// 新增空间
export function addSpace(data) {
    return request({
        url: '/tax/space',
        method: 'post',
        data: data
    });
}

// 修改空间
export function updateSpace(data) {
    return request({
        url: '/tax/space',
        method: 'put',
        data: data
    });
}

// 删除空间
export function delSpace(id) {
    return request({
        url: '/tax/space/' + id,
        method: 'delete'
    });
}

