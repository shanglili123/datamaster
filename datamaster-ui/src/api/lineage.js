import request from '@/utils/request';

// The v2 graph contract uses opaque string node/edge IDs, not asset/metadata IDs.
export function getAssetLineageGraph(id, params) {
  return request({
    url: `/ast/asset/dataLineage/v2/${encodeURIComponent(id)}`,
    method: 'get',
    params,
  });
}

export function getTableLineageGraph(id, params) {
  return request({
    url: `/cat/table/lineage/v2/${encodeURIComponent(id)}`,
    method: 'get',
    params,
  });
}
