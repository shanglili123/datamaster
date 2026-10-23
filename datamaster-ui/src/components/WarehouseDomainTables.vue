<template>
  <a-drawer :open="open" :title="`${domainType === 'theme' ? '主题域' : '数据域'}：${domainName || '-'} · 所属表`"
    width="min(1200px, 95vw)" @close="close">
    <a-alert type="info" show-icon message="仅当前域已登记且可访问的库表，不含下级域或未归属表。可通过详情中的预览页签查看数据，或进入 SQL 查询。" />
    <a-space class="domain-table-search" wrap>
      <a-input v-model:value="tableName" placeholder="按物理表名搜索" allow-clear style="width: 260px" @pressEnter="search" />
      <a-button type="primary" :disabled="!canList" @click="search">查询</a-button>
      <a-button :disabled="!canList" @click="resetSearch">重置</a-button>
      <a-button :loading="loading" :disabled="!canList" @click="loadTables">刷新</a-button>
    </a-space>
    <a-alert v-if="error" type="warning" show-icon :message="error" style="margin-bottom: 12px" />
    <a-table :columns="columns" :data-source="rows" row-key="id" :loading="loading" :pagination="false"
      :scroll="{ x: 1100 }" :locale="{ emptyText: error ? '暂时无法加载所属表' : '当前域暂无已登记且可访问的表' }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <a-tooltip :title="canDetail ? '进入资产详情，可切换到预览页签' : '暂无资产详情权限'">
            <span><a-button type="link" size="small" :disabled="!canDetail" @click="openDetail(record)">详情/预览</a-button></span>
          </a-tooltip>
          <a-tooltip :title="sqlHint(record)">
            <span><a-button type="link" size="small" :disabled="!canSql || !isQueryableAsset(record)" @click="queryTable(record)">SQL 查询</a-button></span>
          </a-tooltip>
        </template>
        <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
      </template>
    </a-table>
    <a-pagination class="domain-table-pagination" :current="pageNum" :page-size="pageSize" :total="total"
      show-size-changer :page-size-options="['10', '20', '50']" :show-total="value => `共 ${value} 张表`"
      @change="changePage" />
  </a-drawer>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onDeactivated, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import useUserStore from '@/store/system/user';
import { checkPermi } from '@/utils/permission';
import { normalizePage } from '@/utils/page';
import { listDppAsset } from '@/api/ast/asset/asset';
import { isQueryableAsset, useDataQueryNavigation } from '@/utils/dataQueryNavigation';

const props = defineProps({
  open: { type: Boolean, default: false },
  domainType: { type: String, required: true },
  domainId: { type: [String, Number], default: undefined },
  domainName: { type: String, default: '' },
});
const emit = defineEmits(['update:open']);
const userStore = useUserStore();
const router = useRouter();
const stationNavigation = inject('spaceWorkstationNavigation', null);
const { openDataQuery } = useDataQueryNavigation();
const canList = computed(() => checkPermi(['ast:asset:list']));
const canDetail = computed(() => checkPermi(['ast:asset:query']));
const canSql = computed(() => checkPermi(['ast:dataQuery:list']));
const rows = ref([]);
const total = ref(0);
const tableName = ref('');
const searchName = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const error = ref('');
let requestSequence = 0;
const spaceKey = computed(() => JSON.stringify([userStore.id, userStore.spaceId, userStore.spaceCode]));
const contextKey = computed(() => JSON.stringify([props.open, props.domainType, props.domainId, spaceKey.value, canList.value]));
const columns = [
  { title: '物理表名', dataIndex: 'tableName', width: 220, ellipsis: true },
  { title: '中文注释', dataIndex: 'tableComment', width: 180, ellipsis: true },
  { title: '数据连接', dataIndex: 'datasourceName', width: 160, ellipsis: true },
  { title: '数仓分层', dataIndex: 'dataLayerName', width: 120, ellipsis: true },
  { title: '业务分类', dataIndex: 'businessCategoryName', width: 140, ellipsis: true },
  { title: '字段数', dataIndex: 'fieldCount', width: 80 },
  { title: '操作', key: 'actions', width: 200, fixed: 'right' },
];

function clearRows() {
  requestSequence++;
  rows.value = [];
  total.value = 0;
  loading.value = false;
  error.value = '';
}

function validDomain() {
  return ['data', 'theme'].includes(props.domainType) && /^[1-9]\d*$/.test(String(props.domainId ?? ''));
}

async function loadTables() {
  clearRows();
  if (!props.open) return;
  if (!validDomain()) {
    error.value = '请选择有效的数据域或主题域，顶级节点不支持查询所属表';
    return;
  }
  if (!canList.value) {
    error.value = '暂无资产列表权限，无法查看所属表';
    return;
  }
  const sequence = requestSequence;
  const context = contextKey.value;
  loading.value = true;
  const params = {
    type: '1',
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    tableName: searchName.value || undefined,
    spaceId: userStore.spaceId || undefined,
    spaceCode: userStore.spaceCode || undefined,
    ...(props.domainType === 'data' ? { dataDomainId: props.domainId } : { themeDomainId: props.domainId }),
  };
  try {
    const response = await listDppAsset(params);
    if (sequence !== requestSequence || context !== contextKey.value) return;
    const page = normalizePage(response);
    rows.value = page.rows;
    total.value = page.total;
  } catch (failure) {
    if (sequence === requestSequence && context === contextKey.value) {
      error.value = failure?.response?.data?.msg || failure?.message || '所属表加载失败，请刷新重试';
    }
  } finally {
    if (sequence === requestSequence && context === contextKey.value) loading.value = false;
  }
}

function search() {
  searchName.value = String(tableName.value || '').trim();
  pageNum.value = 1;
  loadTables();
}
function resetSearch() {
  tableName.value = '';
  search();
}
function changePage(page, size) {
  pageNum.value = size !== pageSize.value ? 1 : page;
  pageSize.value = size;
  loadTables();
}
function close() {
  clearRows();
  emit('update:open', false);
}
function sqlHint(record) {
  if (!canSql.value) return '暂无数据查询权限';
  return isQueryableAsset(record) ? '定位连接并预填 SQL，不自动执行' : '该资产没有有效的库表查询目标';
}
async function queryTable(record) {
  if (!props.open || !canList.value || !canSql.value || !isQueryableAsset(record)) return;
  const context = contextKey.value;
  try {
    const opened = await openDataQuery({ assetId: record.id, datasourceId: record.datasourceId, tableName: record.tableName });
    if (opened && context === contextKey.value) close();
  } catch (failure) {
    if (context === contextKey.value) message.warning('无法打开 SQL 查询，请检查入口和访问权限');
  }
}
async function openDetail(record) {
  if (!props.open || !canList.value || !canDetail.value || !record.id) return;
  const context = contextKey.value;
  try {
    const target = { path: '/col/asset/detail', query: { id: String(record.id) } };
    const handled = await stationNavigation?.openPage?.(target);
    if (context !== contextKey.value) return;
    if (!handled) await router.push(target);
    if (context === contextKey.value) close();
  } catch (failure) {
    if (context === contextKey.value) message.warning('无法打开资产详情，请检查入口和访问权限');
  }
}

watch(() => [props.open, props.domainType, props.domainId, canList.value], () => {
  clearRows();
  tableName.value = '';
  searchName.value = '';
  pageNum.value = 1;
  if (props.open) loadTables();
}, { immediate: true, flush: 'sync' });
watch(spaceKey, () => {
  close();
  tableName.value = '';
  searchName.value = '';
  pageNum.value = 1;
}, { flush: 'sync' });
onDeactivated(close);
onBeforeUnmount(clearRows);
</script>

<style scoped>
.domain-table-search { margin: 16px 0; }
.domain-table-pagination { margin-top: 16px; text-align: right; }
</style>
