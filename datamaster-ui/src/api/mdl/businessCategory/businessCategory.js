
import request from '@/utils/request';

export function listBusinessCategory(query) {
    return request({
        url: '/mdl/businessCategory/list',
        method: 'get',
        params: query
    });
}

export function getBusinessCategory(id) {
    return request({
        url: '/mdl/businessCategory/' + id,
        method: 'get'
    });
}

export function addBusinessCategory(data) {
    return request({
        url: '/mdl/businessCategory',
        method: 'post',
        data: data
    });
}

export function updateBusinessCategory(data) {
    return request({
        url: '/mdl/businessCategory',
        method: 'put',
        data: data
    });
}

export function delBusinessCategory(id) {
    return request({
        url: '/mdl/businessCategory/' + id,
        method: 'delete'
    });
}
