
import request from "@/utils/request";

// 查询在线单设计器列表
export function listdesform(query) {
  return request({
    url: "/col/desForm/list",
    method: "get",
    params: query,
  });
}

// 查询在线单设计器详细
export function getdesform(id) {
  return request({
    url: "/col/desForm/" + id,
    method: "get",
  });
}

// 新增在线单设计器
export function add(data) {
  return request({
    url: "/col/desForm",
    method: "post",
    data: data,
  });
}

// 修改在线单设计器
export function edit(data) {
  return request({
    url: "/col/desForm",
    method: "put",
    data: data,
  });
}

// 删除在线单设计器
export function deldesform(id) {
  return request({
    url: "/col/desForm/deleted?id=" + id,
    method: "delete",
  });
}
// 表单编码是否重复校验
export function duplicateCheck(query) {
  return request({
    url: "/col/desForm/duplicateCheck",
    method: "get",
    params: query,
  });
}
// 通过表单编码查询
export function getByDesformCode(desformCode) {
  return request({
    url: "/col/desForm/" + desformCode,
    method: "get",
  });
}

