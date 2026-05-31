
import request from "@/utils/request";

// 查询数据元代码映射列表
export function dpDocumentList(query) {
  return request({
    url: "/std/document/search",
    method: "get",
    params: query,
  });
}

