
import request from "@/utils/request";

// 查询在线单数据列表
export function listdata(query) {
  return request({
    url: "/col/data/list",
    method: "get",
    params: query,
  });
}

// 查询在线单数据详细
export function getdata(id) {
  return request({
    url: "/col/data/" + id,
    method: "get",
  });
}

// 新增在线单数据
export function adddata(data) {
  return request({
    url: "/col/data",
    method: "post",
    data: data,
  });
}

// 修改在线单数据
export function updatedata(data) {
  return request({
    url: "/col/data",
    method: "put",
    data: data,
  });
}

// 删除在线单数据
export function deldata(data) {
  return request({
    url: "/col/data/deleted",
    method: "delete",
    data: data,
  });
}

