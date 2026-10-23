import { inject } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import usePermissionStore from '@/store/system/permission';
import { checkPermi } from '@/utils/permission';

export const DATA_QUERY_PATH = '/ast/dataQuery';

const targetValue = (value) => {
  if (typeof value !== 'string' && typeof value !== 'number') return undefined;
  const text = String(value);
  return text.trim() && text !== '-1' ? text : undefined;
};

// Only these non-sensitive identifiers may cross a route/workstation boundary.
export function normalizeDataQueryTarget(target = {}) {
  const query = {};
  for (const key of ['assetId', 'datasourceId', 'tableName']) {
    const value = targetValue(target[key]);
    if (value !== undefined) query[key] = value;
  }
  return query;
}

export function isQueryableAsset(asset) {
  return Boolean(asset && String(asset.type) === '1'
    && ![false, 0, '0'].includes(asset.validFlag)
    && ![true, 1, '1', '2'].includes(asset.delFlag)
    && targetValue(asset.datasourceId) && targetValue(asset.tableName));
}

function collectQueryPaths(routes, parent = '') {
  return (routes || []).flatMap((record) => {
    const path = record.path?.startsWith('/')
      ? record.path : `${parent}/${record.path || ''}`.replace(/\/+/g, '/');
    const component = typeof record.component === 'function' ? String(record.component) : '';
    const isQuery = /\/dataQuery(?:\/index)?\/?$/i.test(path)
      || /views\/ast\/dataQuery\/index\.vue/.test(component);
    return [
      ...(isQuery && record.component ? [path] : []),
      ...collectQueryPaths(record.children, path),
    ];
  });
}

export function resolveDataQueryPath(router, permissionStore) {
  // A catch-all match is not a query route. Use only current authorized dynamic menus.
  const paths = [...new Set(collectQueryPaths([
    ...(permissionStore.topbarRouters || []),
    ...(permissionStore.addRoutes || []),
  ]))];
  return paths.find((candidate) => {
    const resolved = router.resolve({ path: candidate });
    const leaf = resolved.matched[resolved.matched.length - 1];
    return leaf && leaf.path === candidate && !leaf.redirect
      && !resolved.matched.some((record) => /404|pathMatch|catchAll/i.test(`${record.name || ''} ${record.path}`));
  });
}

export function useDataQueryNavigation() {
  const router = useRouter();
  const stationNavigation = inject('spaceWorkstationNavigation', null);
  const permissionStore = usePermissionStore();

  async function openDataQuery(target = {}) {
    if (!checkPermi(['ast:dataQuery:list'])) {
      message.warning('暂无数据查询权限');
      return false;
    }
    const query = normalizeDataQueryTarget(target);
    if (!query.assetId && !(query.datasourceId && query.tableName)) {
      message.warning('缺少可查询的数据连接或表名称');
      return false;
    }
    if (await stationNavigation?.openPage?.({ path: DATA_QUERY_PATH, query })) return true;

    const path = resolveDataQueryPath(router, permissionStore);
    if (!path) {
      message.warning('当前空间暂无可用的数据查询入口');
      return false;
    }
    await router.push({ path, query });
    return true;
  }

  return { openDataQuery };
}
