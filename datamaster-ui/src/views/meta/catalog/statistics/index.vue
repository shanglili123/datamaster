<template>
  <div class="app-container catalog-statistics">
    <div class="catalog-statistics__header">
      <div>
        <h1>数据统计</h1>
      </div>
      <a-button :loading="loading" :icon="h(ReloadOutlined)" @click="loadStatistics">
        刷新统计
      </a-button>
    </div>

    <div class="catalog-statistics__summary">
      <div class="catalog-statistics__metric">
        <span class="catalog-statistics__metric-icon catalog-statistics__metric-icon--blue"><DatabaseOutlined /></span>
        <div>
          <span>数据库</span>
          <strong>{{ databaseCount }}</strong>
        </div>
      </div>
      <div class="catalog-statistics__metric">
        <span class="catalog-statistics__metric-icon catalog-statistics__metric-icon--teal"><TableOutlined /></span>
        <div>
          <span>数据表</span>
          <strong>{{ tableCount }}</strong>
        </div>
      </div>
      <div class="catalog-statistics__metric">
        <span class="catalog-statistics__metric-icon catalog-statistics__metric-icon--violet"><BarChartOutlined /></span>
        <div>
          <span>已采集行数</span>
          <strong>{{ formatNumber(totalRowCount) }}</strong>
        </div>
      </div>
      <div class="catalog-statistics__metric catalog-statistics__metric--muted">
        <span class="catalog-statistics__metric-icon catalog-statistics__metric-icon--orange"><WarningOutlined /></span>
        <div>
          <span>未返回行数</span>
          <strong>{{ missingRowCount }}</strong>
        </div>
      </div>
    </div>

    <div class="catalog-statistics__panel">
      <div class="catalog-statistics__toolbar">
        <div>
          <span class="catalog-statistics__panel-eyebrow">TABLE INVENTORY</span>
          <h2>表数据明细</h2>
        </div>
        <a-select
          v-model:value="selectedDbId"
          allow-clear
          show-search
          option-filter-prop="label"
          placeholder="筛选数据库"
          class="catalog-statistics__database-select"
        >
          <a-select-option
            v-for="database in databases"
            :key="database.id"
            :value="database.id"
            :label="database.dbName"
          >
            {{ database.dbName }}
          </a-select-option>
        </a-select>
      </div>

      <a-spin :spinning="loading">
        <a-table
          v-if="visibleTables.length"
          :columns="columns"
          :data-source="visibleTables"
          :pagination="{ pageSize: 12, showSizeChanger: true, showTotal: (total) => `共 ${total} 张表` }"
          :row-key="(record) => record.id || `${record.dbId}-${record.tableName}`"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'dbName'">
              <span class="catalog-statistics__database-name">{{ getDatabaseName(record) }}</span>
            </template>
            <template v-else-if="column.key === 'rowCount'">
              <span :class="{ 'is-missing': record.rowCount === null || record.rowCount === undefined }">
                {{ record.rowCount === null || record.rowCount === undefined ? "未采集" : formatNumber(record.rowCount) }}
              </span>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="String(record.status) === '1' ? 'success' : 'default'">
                {{ String(record.status) === '1' ? "已发布" : "未发布" }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <a-empty
          v-else
          description="暂无表数据，请先完成元数据采集"
          :image-style="{ height: '86px' }"
        />
      </a-spin>
    </div>
  </div>
</template>

<script setup name="CatalogStatistics">
import { computed, h, onMounted, ref } from "vue";
import {
  BarChartOutlined,
  DatabaseOutlined,
  ReloadOutlined,
  TableOutlined,
  WarningOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { listDb } from "@/api/cat/catalog/db";
import { listTable } from "@/api/cat/catalog/table";

const loading = ref(false);
const databases = ref([]);
const tables = ref([]);
const selectedDbId = ref(undefined);

const columns = [
  { title: "数据库", key: "dbName", width: 220 },
  { title: "Schema", dataIndex: "schemaName", key: "schemaName", width: 180 },
  { title: "表名", dataIndex: "tableName", key: "tableName", width: 260 },
  { title: "表说明", dataIndex: "tableComment", key: "tableComment", ellipsis: true },
  { title: "数据行数", key: "rowCount", width: 150, align: "right" },
  { title: "状态", key: "status", width: 100 },
  { title: "更新时间", dataIndex: "updateTime", key: "updateTime", width: 180 },
];

const visibleTables = computed(() => {
  if (!selectedDbId.value) return tables.value;
  return tables.value.filter((table) => String(table.dbId) === String(selectedDbId.value));
});

const databaseCount = computed(() => databases.value.length);
const tableCount = computed(() => visibleTables.value.length);
const missingRowCount = computed(
  () => visibleTables.value.filter((table) => table.rowCount === null || table.rowCount === undefined).length
);
const totalRowCount = computed(() =>
  visibleTables.value.reduce((total, table) => {
    const value = Number(table.rowCount);
    return Number.isFinite(value) ? total + value : total;
  }, 0)
);

function getRows(response) {
  return response?.data?.rows || response?.data?.list || response?.data || [];
}

function databaseKey(database) {
  return [database.datasourceId, database.dbName, database.schemaName].map((value) => value ?? "").join("|");
}

function tableKey(table) {
  return [table.datasourceId, table.dbName, table.schemaName, table.tableName]
    .map((value) => value ?? "")
    .join("|");
}

function preferRecord(current, candidate) {
  const currentHasRowCount = current?.rowCount !== null && current?.rowCount !== undefined;
  const candidateHasRowCount = candidate?.rowCount !== null && candidate?.rowCount !== undefined;
  if (!currentHasRowCount && candidateHasRowCount) return candidate;
  if (currentHasRowCount && !candidateHasRowCount) return current;
  const currentTime = current?.updateTime ? new Date(current.updateTime).getTime() : 0;
  const candidateTime = candidate?.updateTime ? new Date(candidate.updateTime).getTime() : 0;
  return candidateTime >= currentTime ? candidate : current;
}

function uniqueRecords(records, keyOf) {
  const recordMap = new Map();
  records.forEach((record) => {
    const key = keyOf(record);
    recordMap.set(key, preferRecord(recordMap.get(key), record));
  });
  return Array.from(recordMap.values());
}

function getDatabaseName(table) {
  if (table.dbName || table.databaseName) return table.dbName || table.databaseName;
  const database = databases.value.find((item) => String(item.id) === String(table.dbId));
  return database?.dbName || "未命名数据库";
}

function formatNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number.toLocaleString("zh-CN") : "--";
}

async function loadStatistics() {
  loading.value = true;
  try {
    const [databaseResponse, tableResponse] = await Promise.all([
      listDb({ pageNum: 1, pageSize: 2000 }),
      listTable({ pageNum: 1, pageSize: 10000 }),
    ]);
    databases.value = uniqueRecords(getRows(databaseResponse), databaseKey);
    tables.value = uniqueRecords(getRows(tableResponse), tableKey);
  } catch (error) {
    databases.value = [];
    tables.value = [];
    message.error("库表统计加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadStatistics);
</script>

<style lang="scss" scoped>
.catalog-statistics {
  min-height: 100%;
  padding: 4px 0 24px;
  background: transparent;
}

.catalog-statistics__header,
.catalog-statistics__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.catalog-statistics__header {
  margin-bottom: 20px;

  h1,
  h2 {
    margin: 0;
    color: var(--dm-text-main);
    font-weight: 650;
    letter-spacing: -0.02em;
  }

  h1 {
    margin-top: 4px;
    font-size: 24px;
  }

  p {
    margin: 7px 0 0;
    color: var(--dm-text-secondary);
    font-size: 13px;
  }
}

.catalog-statistics__eyebrow,
.catalog-statistics__panel-eyebrow {
  color: #71809b;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.catalog-statistics__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.catalog-statistics__metric {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 92px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--dm-border-light);
  border-radius: 14px;
  box-shadow: 0 6px 18px rgba(25, 43, 80, 0.05);

  > div {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  > div > span {
    color: var(--dm-text-secondary);
    font-size: 12px;
  }

  strong {
    margin-top: 4px;
    color: var(--dm-text-main);
    font-size: 24px;
    line-height: 30px;
  }
}

.catalog-statistics__metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  font-size: 20px;
  border-radius: 12px;
}

.catalog-statistics__metric-icon--blue { color: #566ce4; background: #edf0ff; }
.catalog-statistics__metric-icon--teal { color: #159f95; background: #e8faf7; }
.catalog-statistics__metric-icon--violet { color: #8b5bd6; background: #f2ecff; }
.catalog-statistics__metric-icon--orange { color: #d28a28; background: #fff5e4; }

.catalog-statistics__panel {
  padding: 18px;
  background: #fff;
  border: 1px solid var(--dm-border-light);
  border-radius: 15px;
  box-shadow: 0 8px 24px rgba(25, 43, 80, 0.05);
}

.catalog-statistics__toolbar {
  margin-bottom: 16px;

  h2 {
    margin-top: 3px;
    color: var(--dm-text-main);
    font-size: 17px;
  }
}

.catalog-statistics__database-select {
  width: 240px;
}

.catalog-statistics__database-name {
  color: #425bd2;
  font-weight: 600;
}

.is-missing {
  color: #c3914c;
}

@media screen and (max-width: 900px) {
  .catalog-statistics__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 640px) {
  .catalog-statistics__header,
  .catalog-statistics__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .catalog-statistics__summary {
    grid-template-columns: 1fr;
  }

  .catalog-statistics__database-select {
    width: 100%;
  }
}
</style>
