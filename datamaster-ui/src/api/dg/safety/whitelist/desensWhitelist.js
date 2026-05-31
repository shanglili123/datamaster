import request from "@/utils/request";

export function listDesensWhitelist(query) {
  return request({
    url: "/cat/desensWhitelist/list",
    method: "get",
    params: query,
  });
}

export function getDesensWhitelist(id) {
  return request({
    url: "/cat/desensWhitelist/" + id,
    method: "get",
  });
}

export function addDesensWhitelist(data) {
  return request({
    url: "/cat/desensWhitelist",
    method: "post",
    data,
  });
}

export function updateDesensWhitelist(data) {
  return request({
    url: "/cat/desensWhitelist",
    method: "put",
    data,
  });
}

export function delDesensWhitelist(id) {
  return request({
    url: "/cat/desensWhitelist/" + id,
    method: "delete",
  });
}
