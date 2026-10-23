<template>
  <div class="app-container lineage-analysis">
    <div class="center-selector">
      <label for="lineage-metadata-table">中心元数据表</label>
      <a-select
        id="lineage-metadata-table"
        :value="selected?.id"
        :options="options"
        :loading="selectLoading || centerLoading"
        :filter-option="false"
        show-search
        allow-clear
        placeholder="搜索并选择当前空间元数据表"
        @search="searchTables"
        @dropdown-visible-change="openSelector"
        @change="selectTable"
      />
      <a-button :disabled="!hasContext" :loading="selectLoading" @click="searchTables(searchText)">刷新表列表</a-button>
    </div>
    <p class="selector-help">选择中心表后请求真实血缘；可通过 metadataTableId 路由参数定位。候选列表复用元数据接口，血缘访问由 v2 接口重新校验空间和数据权限。</p>
    <p v-if="selectorError" role="alert">{{ selectorError }}</p>
    <a-button v-if="moreTables" :loading="selectLoading" @click="loadTables(true)">加载更多表</a-button>
    <LineageGraph source="metadata" :center-id="selected?.id" :center-name="selected?.tableName || ''" />
  </div>
</template>

<script setup name="MetaAnalysesLineage">
import { computed, inject, onActivated, onDeactivated, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getTable, listTable } from '@/api/cat/catalog/table';
import LineageGraph from '@/components/LineageGraph/index.vue';
import useUserStore from '@/store/system/user';

const route = useRoute();
const router = useRouter();
const stationNavigation = inject('spaceWorkstationNavigation', null);
const userStore = useUserStore();
const selected = ref(null);
const tables = ref([]);
const selectLoading = ref(false);
const centerLoading = ref(false);
const selectorError = ref('');
const searchText = ref('');
const page = ref(1);
const moreTables = ref(false);
const hasContext = computed(() => Boolean(userStore.spaceId && userStore.token));
const options = computed(() => {
  const records = new Map(tables.value.map((table) => [String(table.id), table]));
  if (selected.value) records.set(selected.value.id, selected.value);
  return Array.from(records.values()).map((table) => ({ value: String(table.id), label: tableLabel(table) }));
});
let listSequence = 0;
let centerSequence = 0;
let disposed = false;
let active = true;

function tableLabel(table) {
  return [table.dbName, table.schemaName, table.tableName || String(table.id)].filter(Boolean).join(' / ');
}
function readTable(table) {
  if (!table || table.id === undefined || table.id === null) throw new Error('Invalid metadata table');
  if (table.spaceId !== undefined && table.spaceId !== null && String(table.spaceId) !== String(userStore.spaceId)) throw new Error('Space mismatch');
  return { id: String(table.id), tableName: table.tableName || '', dbName: table.dbName || '', schemaName: table.schemaName || '' };
}
async function loadTables(append = false) {
  const sequence = ++listSequence;
  selectLoading.value = false;
  if (!hasContext.value || disposed || !active) return;
  selectLoading.value = true;
  selectorError.value = '';
  const nextPage = append ? page.value + 1 : 1;
  if (!append) { tables.value = []; moreTables.value = false; }
  try {
    const response = await listTable({
      pageNum: nextPage, pageSize: 50, tableName: searchText.value || undefined,
      spaceId: userStore.spaceId, spaceCode: userStore.spaceCode,
    });
    if (sequence !== listSequence || disposed) return;
    const rows = response?.data?.rows;
    if (!Array.isArray(rows)) throw new Error('Invalid table list');
    const records = rows.map(readTable);
    const merged = new Map((append ? tables.value : []).map((table) => [table.id, table]));
    records.forEach((table) => merged.set(table.id, table));
    tables.value = Array.from(merged.values());
    page.value = nextPage;
    moreTables.value = Number(response.data.total) > tables.value.length && records.length > 0;
  } catch {
    if (sequence !== listSequence || disposed) return;
    selectorError.value = '元数据表列表加载失败，请确认当前空间权限后重试。';
  } finally {
    if (sequence === listSequence) selectLoading.value = false;
  }
}
function searchTables(value = '') {
  searchText.value = value;
  loadTables();
}
function openSelector(open) {
  if (open && !tables.value.length && !selectLoading.value) loadTables();
}
async function selectTable(id) {
  const query = { ...route.query };
  if (id !== undefined && id !== null && id !== '') query.metadataTableId = String(id);
  else delete query.metadataTableId;
  try {
    if (await stationNavigation?.openPage?.({ path: route.path, query, title: '血缘分析', routeName: route.name, replace: true })) return;
    await router.replace({ path: route.path, query });
  } catch {
    selectorError.value = '无法切换中心表，请确认当前页面入口后重试。';
  }
}
async function loadRouteCenter() {
  const sequence = ++centerSequence;
  selected.value = null;
  centerLoading.value = false;
  selectorError.value = '';
  const id = route.query.metadataTableId;
  if (!id || Array.isArray(id) || !hasContext.value || disposed || !active) return;
  centerLoading.value = true;
  try {
    // Resolve metadata identity only. The v2 graph endpoint separately enforces lineage scope.
    const response = await getTable(id);
    if (sequence !== centerSequence || disposed) return;
    const table = readTable(response?.data);
    if (table.id !== String(id)) throw new Error('Center mismatch');
    selected.value = table;
  } catch {
    if (sequence !== centerSequence || disposed) return;
    selectorError.value = '中心元数据表不存在或当前空间无权访问，请重新选择。';
  } finally {
    if (sequence === centerSequence) centerLoading.value = false;
  }
}
watch(() => [userStore.spaceId, userStore.spaceCode, userStore.id, userStore.token, userStore.permissions.join('|')], () => {
  ++listSequence;
  tables.value = [];
  moreTables.value = false;
  selectLoading.value = false;
  searchText.value = '';
  loadRouteCenter();
}, { immediate: true, flush: 'sync' });
watch(() => route.query.metadataTableId, loadRouteCenter, { flush: 'sync' });
onDeactivated(() => {
  active = false;
  ++listSequence;
  ++centerSequence;
  selected.value = null;
  tables.value = [];
  moreTables.value = false;
  page.value = 1;
  selectLoading.value = false;
  centerLoading.value = false;
});
onActivated(() => { if (!active) { active = true; loadRouteCenter(); } });
onUnmounted(() => { disposed = true; ++listSequence; ++centerSequence; });
</script>

<style scoped lang="scss">
.lineage-analysis { min-width: 0; overflow: auto; }
.center-selector { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 10px; }
.center-selector :deep(.ant-select) { width: min(520px, 100%); }
.selector-help { color: var(--dm-text-secondary, #6b7280); margin: 8px 0 16px; }
</style>
