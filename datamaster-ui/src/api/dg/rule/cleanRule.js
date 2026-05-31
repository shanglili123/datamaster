import request from '@/utils/request'

// 查询清洗规则列表
export function listDgRule(query) {
    return request({
        url: '/cat/cleanRule/list',
        method: 'get',
        params: query
    })
}

// 查询清洗规则详细
export function getDgRule(id) {
    return request({
        url: '/cat/cleanRule/' + id,
        method: 'get'
    })
}

// 新增清洗规则
export function addDgRule(data) {
    return request({
        url: '/cat/cleanRule',
        method: 'post',
        data: data
    })
}

// 修改清洗规则
export function updateDgRule(data) {
    return request({
        url: '/cat/cleanRule',
        method: 'put',
        data: data
    })
}

// 删除清洗规则
export function delDgRule(id) {
    return request({
        url: '/cat/cleanRule/' + id,
        method: 'delete'
    })
}

// tree
export function treeDgRule(params) {
    return request({
        url: '/cat/cleanRule/tree',
        method: 'get',
        params
    })
}

// 数据集成用到的 清洗规则
export function listAll(params) {
    return request({
        url: '/cat/cleanRule/listAll',
        method: 'get',
        params
    })
}
