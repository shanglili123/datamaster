
import request from '@/utils/request';

// 查询逻辑模型列表
export function listDpModel(query) {
    return request({
        url: '/std/model/list',
        method: 'get',
        params: query
    });
}

// 查询逻辑模型详细
export function getDpModel(ID) {
    return request({
        url: '/std/model/' + ID,
        method: 'get'
    });
}

// 新增逻辑模型
export function addDpModel(data) {
    return request({
        url: '/std/model',
        method: 'post',
        data: data
    });
}
// 新增逻辑模型
export function dpModelColumn(data) {
    return request({
        url: '/std/modelColumn/addList',
        method: 'post',
        data: data
    });
}
// 新增逻辑模型
export function updateDpModel(data) {
    return request({
        url: '/std/model',
        method: 'put',
        data: data
    });
}
// 修改逻辑模型
// export function updateDpModel(data) {
//   return request({
//     url: '/std/dpModelColumn/editList',
//     method: 'put',
//     data: data
//   })
// }

// 删除逻辑模型
export function delDpModel(ID) {
    return request({
        url: '/std/model/' + ID,
        method: 'delete'
    });
}

// 删除逻辑模型字段
export function delDpModelColumn(ID) {
    return request({
        url: '/std/model/columnAll/' + ID,
        method: 'delete'
    });
}
// 查询逻辑模型类目管理列表
export function listAttModelCat(query) {
    return request({
        url: '/tax/modelCat/list',
        method: 'get',
        params: query
    });
}
// 获取 表信息
export function getDpModelColumnList(query) {
    return request({
        url: '/std/modelColumn/getDpModelColumnList',
        method: 'get',
        params: query
    });
}
// 修改逻辑模型
export function updateDpModelColumn(data) {
    return request({
        url: '/std/modelColumn/editList',
        method: 'put',
        data: data
    });
}
// 物化
export function createMaterializedTable(data) {
    return request({
        url: '/std/modelMaterialized/createMaterializedTable',
        method: 'post',
        data: data
    });
}
// 数据库连接
export function getDaDatasourceList(query) {
    return request({
        url: '/ast/dataSource/getDaDatasourceList',
        method: 'get',
        params: query
    });
}
// 修改状态
export function updateStatusDpDataModel(id, status) {
    return request({
        url: `/dp/model/updateStatus/${id}/${status}`,
        method: 'post'
    });
}
//表
export function tableList(ID) {
    console.log("🚀 ~ tableList ~ ID:", ID)
    return request({
        url: '/ast/dataSource/tableList/' + ID,
        method: 'get'
    });
}
// 表字段
export function columnsList(data) {
    return request({
        url: `/da/dataSource/columnsList`,
        method: 'post',
        data: data
    });
}

// 查询逻辑模型属性信息列表
export function listDpModelColumn(query) {
    return request({
        url: '/std/modelColumn/list',
        method: 'get',
        params: query
    })
}

// 查询逻辑模型属性信息详细
export function getDpModelColumn(id) {
    return request({
        url: '/std/modelColumn/' + id,
        method: 'get'
    })
}

// 新增逻辑模型属性信息
export function addDpModelColumn(data) {
    return request({
        url: '/std/modelColumn',
        method: 'post',
        data: data
    })
}

// 修改逻辑模型属性信息
export function updateDpModelColumns(data) {
    return request({
        url: '/std/modelColumn',
        method: 'put',
        data: data
    })
}

// 删除逻辑模型属性信息
export function delDpModelColumns(id) {
    return request({
        url: '/std/modelColumn/' + id,
        method: 'delete'
    })
}
// 查询物化模型记录列表
export function listDpModelMaterialized(query) {
    return request({
        url: '/std/modelMaterialized/list',
        method: 'get',
        params: query
    })
}

// 查询物化模型记录详细
export function getDpModelMaterialized(id) {
    return request({
        url: '/std/modelMaterialized/' + id,
        method: 'get'
    })
}

// 新增物化模型记录
export function addDpModelMaterialized(data) {
    return request({
        url: '/std/modelMaterialized',
        method: 'post',
        data: data
    })
}

// 修改物化模型记录
export function updateDpModelMaterialized(data) {
    return request({
        url: '/std/modelMaterialized',
        method: 'put',
        data: data
    })
}

// 删除物化模型记录
export function delDpModelMaterialized(id) {
    return request({
        url: '/std/modelMaterialized/' + id,
        method: 'delete'
    })
}

