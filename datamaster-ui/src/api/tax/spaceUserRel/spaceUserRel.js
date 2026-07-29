
import request from '@/utils/request';
import useUserStore from '@/store/system/user';

// 查询空间与用户关联关系列表
export function listSpaceUserRel(query) {
    return request({
        url: '/tax/spaceUserRel/list',
        method: 'get',
        params: query
    });
}

// 查询空间与用户关联关系详细
export function getSpaceUserRel(id) {
    return request({
        url: '/tax/spaceUserRel/' + id,
        method: 'get'
    });
}

// 查询空间与用户关联关系详细
export function getSpaceUserRoleDetail(id) {
    return request({
        url: '/tax/spaceUserRel/roleUser/' + id,
        method: 'get'
    });
}

// 新增空间与用户关联关系
export function addSpaceUserRel(data) {
    return request({
        url: '/tax/spaceUserRel',
        method: 'post',
        data: data
    });
}

// 新增空间与用户关联关系
export function addUserListAndRoleList(data) {
    return request({
        url: '/tax/spaceUserRel/addUserListAndRoleList',
        method: 'post',
        data: data
    });
}

// 修改空间与用户关联关系
export function updateSpaceUserRel(data) {
    return request({
        url: '/tax/spaceUserRel',
        method: 'put',
        data: data
    });
}

// 修改空间与用户关联关系
export function editUserListAndRoleList(data) {
    return request({
        url: '/tax/spaceUserRel/editUserListAndRoleList',
        method: 'put',
        data: data
    });
}

// 删除空间与用户关联关系
export function delSpaceUserRel(id) {
    return request({
        url: '/tax/spaceUserRel/' + id,
        method: 'delete'
    });
}

// 查询角色列表
export function listRole(query) {
    return request({
        url: '/tax/spaceUserRel/role/list',
        method: 'get',
        params: query
    });
}

// 查询角色详细
export function getRole(roleId) {
    return request({
        url: '/tax/spaceUserRel/role/' + roleId,
        method: 'get'
    });
}

// 新增角色
export function addRole(data) {
    return request({
        url: '/tax/spaceUserRel/role',
        method: 'post',
        data: data
    });
}

// 修改角色
export function updateRole(data) {
    return request({
        url: '/tax/spaceUserRel/role',
        method: 'put',
        data: data
    });
}

// 角色数据权限
export function dataScope(data) {
    return request({
        url: '/tax/spaceUserRel/role/dataScope',
        method: 'put',
        data: data
    });
}

// 角色状态修改
export function changeRoleStatus(roleId, status) {
    const userStore = useUserStore();
    const data = {
        roleId,
        status,
        spaceId: userStore.spaceId,
        spaceCode: userStore.spaceCode
    };
    return request({
        url: '/tax/spaceUserRel/role/changeStatus',
        method: 'put',
        data: data
    });
}

// 删除角色
export function delRole(roleId) {
    return request({
        url: '/tax/spaceUserRel/role/' + roleId,
        method: 'delete'
    });
}

// 查询角色已授权用户列表
export function allocatedUserList(query) {
    return request({
        url: '/tax/spaceUserRel/role/authUser/allocatedList',
        method: 'get',
        params: query
    });
}

// 查询角色未授权用户列表
export function unallocatedUserList(query) {
    return request({
        url: '/tax/spaceUserRel/role/authUser/unallocatedList',
        method: 'get',
        params: query
    });
}

// 取消用户授权角色
export function authUserCancel(data) {
    return request({
        url: '/tax/spaceUserRel/role/authUser/cancel',
        method: 'put',
        data: data
    });
}

// 批量取消用户授权角色
export function authUserCancelAll(data) {
    return request({
        url: '/tax/spaceUserRel/role/authUser/cancelAll',
        method: 'put',
        params: data
    });
}

// 授权用户选择
export function authUserSelectAll(data) {
    return request({
        url: '/tax/spaceUserRel/role/authUser/selectAll',
        method: 'put',
        params: data
    });
}

// 根据角色ID查询部门树结构
export function deptTreeSelect(roleId) {
    return request({
        url: '/tax/spaceUserRel/role/deptTree/' + roleId,
        method: 'get'
    });
}

