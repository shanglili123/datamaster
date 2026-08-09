<template>
  <div class="app-container">
    <a-row :gutter="10">
      <a-col :span="8">
        <a-card style="height: calc(100vh - 125px)" :bordered="false">
          <template #title>
            <FolderOutlined style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">缓存列表</span>
            <a-button
              style="float: right; padding: 3px 0"
              type="link"
              :icon="h(ReloadOutlined)"
              @click="refreshCacheNames()"
            ></a-button>
          </template>
          <a-table
            striped
            :loading="loading"
            :data-source="cacheNames"
            :columns="cacheNameColumns"
            :height="tableHeight"
            :row-key="record => record.cacheName"
            @row-click="getCacheKeys"
            style="width: 100%"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'actions'">
                <a-button
                  type="link"
                  danger
                  :icon="h(DeleteOutlined)"
                  @click="handleClearCacheName(record)"
                ></a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card style="height: calc(100vh - 125px)" :bordered="false">
          <template #title>
            <KeyOutlined style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">键名列表</span>
            <a-button
              style="float: right; padding: 3px 0"
              type="link"
              :icon="h(ReloadOutlined)"
              @click="refreshCacheKeys()"
            ></a-button>
          </template>
          <a-table
            striped
            :loading="subLoading"
            :data-source="cacheKeys"
            :columns="cacheKeyColumns"
            :height="tableHeight"
            @row-click="handleCacheValue"
            style="width: 100%"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'actions'">
                <a-button
                  type="link"
                  danger
                  :icon="h(DeleteOutlined)"
                  @click="handleClearCacheKey(record)"
                ></a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card :bordered="false" style="height: calc(100vh - 125px)">
          <template #title>
            <FileTextOutlined style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">缓存内容</span>
            <a-button
              style="float: right; padding: 3px 0"
              type="link"
              :icon="h(ReloadOutlined)"
              @click="handleClearCacheAll()"
              >清理全部</a-button
            >
          </template>
          <a-form :model="cacheForm">
            <a-row :gutter="32">
              <a-col :offset="1" :span="22">
                <a-form-item label="缓存名称:" name="cacheName">
                  <a-input v-model:value="cacheForm.cacheName" readonly />
                </a-form-item>
              </a-col>
              <a-col :offset="1" :span="22">
                <a-form-item label="缓存键名:" name="cacheKey">
                  <a-input v-model:value="cacheForm.cacheKey" readonly />
                </a-form-item>
              </a-col>
              <a-col :offset="1" :span="22">
                <a-form-item label="缓存内容:" name="cacheValue">
                  <a-textarea
                    v-model:value="cacheForm.cacheValue"
                    :rows="8"
                    readonly
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup name="CacheList">
import { listCacheName, listCacheKey, getCacheValue, clearCacheName, clearCacheKey, clearCacheAll } from "@/api/system/monitor/cache.js";
import { h } from 'vue';
import { DeleteOutlined, FileTextOutlined, FolderOutlined, KeyOutlined, ReloadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();

const cacheNameColumns = [
  { title: "序号", key: "index", width: 80, align: "center", customRender: ({ index }) => index + 1 },
  { title: "缓存名称", dataIndex: "cacheName", key: "cacheName", align: "center", ellipsis: true, customRender: ({ record }) => nameFormatter(record) },
  { title: "备注", dataIndex: "remark", key: "remark", align: "center", ellipsis: true },
  { title: "操作", key: "actions", width: 60, align: "center", className: "small-padding fixed-width" },
];

const cacheKeyColumns = [
  { title: "序号", key: "index", width: 80, align: "center", customRender: ({ index }) => index + 1 },
  { title: "缓存键名", key: "cacheKey", align: "center", ellipsis: true, customRender: ({ record }) => keyFormatter(record) },
  { title: "操作", key: "actions", width: 60, align: "center", className: "small-padding fixed-width" },
];

const cacheNames = ref([]);
const cacheKeys = ref([]);
const cacheForm = ref({});
const loading = ref(true);
const subLoading = ref(false);
const nowCacheName = ref("");
const tableHeight = ref(window.innerHeight - 200);

/** 查询缓存名称列表 */
function getCacheNames() {
  loading.value = true;
  listCacheName().then(response => {
    cacheNames.value = response.data;
    loading.value = false;
  });
}

/** 刷新缓存名称列表 */
function refreshCacheNames() {
  getCacheNames();
  proxy.$modal.msgSuccess("刷新缓存列表成功");
}

/** 清理指定名称缓存 */
function handleClearCacheName(row) {
  clearCacheName(row.cacheName).then(response => {
    proxy.$modal.msgSuccess("清理缓存名称[" + row.cacheName + "]成功");
    getCacheKeys();
  });
}

/** 查询缓存键名列表 */
function getCacheKeys(row) {
  const cacheName = row !== undefined ? row.cacheName : nowCacheName.value;
  if (cacheName === "") {
    return;
  }
  subLoading.value = true;
  listCacheKey(cacheName).then(response => {
    cacheKeys.value = response.data;
    subLoading.value = false;
    nowCacheName.value = cacheName;
  });
}

/** 刷新缓存键名列表 */
function refreshCacheKeys() {
  getCacheKeys();
  proxy.$modal.msgSuccess("刷新键名列表成功");
}

/** 清理指定键名缓存 */
function handleClearCacheKey(cacheKey) {
  clearCacheKey(cacheKey).then(response => {
    proxy.$modal.msgSuccess("清理缓存键名[" + cacheKey + "]成功");
    getCacheKeys();
  });
}

/** 列表前缀去除 */
function nameFormatter(row) {
  return row.cacheName.replace(":", "");
}

/** 键名前缀去除 */
function keyFormatter(cacheKey) {
  return cacheKey.replace(nowCacheName.value, "");
}

/** 查询缓存内容详细 */
function handleCacheValue(cacheKey) {
  getCacheValue(nowCacheName.value, cacheKey).then(response => {
    cacheForm.value = response.data;
  });
}

/** 清理全部缓存 */
function handleClearCacheAll() {
  clearCacheAll().then(response => {
    proxy.$modal.msgSuccess("清理全部缓存成功");
  });
}

getCacheNames();
</script>

