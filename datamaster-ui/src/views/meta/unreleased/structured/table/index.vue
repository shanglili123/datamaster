<template>
  <div class="app-container metadata-result-page">
    <div class="metadata-workspace">
      <a-layout class="metadata-layout">
      <SourceSystemTree
        ref="sourceSystemTreeRef"
        @node-click="handleNodeClick"
        @data-loaded="handleTreeDataLoaded"
      />
      <a-layout-content class="main-content">
        <qt-wrap :columns="tableStroe.columns" :tableRef="tableRef">
          <template #search>
            <qt-search-bar
              v-bind="searchStore"
              :params="tableStroe.params"
              @query="handleQueryClick"
              @reset="handleResetQueryClick"
              :config="{ permi: ['cat:unreleased:structured:table:query'] }"
            />
          </template>
          <template #actions-data>
            <a-button
              danger
              :icon="h(DeleteOutlined)"
              :disabled="!store.rows.length"
              @click="handleDeleteColumnClick"
            >
              删除
            </a-button>
          </template>
          <qt-table v-bind="tableStroe" ref="tableRef">
            <template #domain-name="scope">
              {{ getDomainPath(scope.row.domainId) }}
            </template>

            <template #status="scope">
              <a-switch
                v-if="scope.row.status != undefined"
                v-model:checked="scope.row.status"
                checked-value="1"
                un-checked-value="0"
                @change="handleStatusChange(scope.row, $event)"
              />
            </template>

            <template #qualitySummary="scope">
              <a-typography-link
                v-if="getQualitySummary(scope.row)"
                @click="handleQualitySummaryClick(scope.row)"
              >
                <a-tag
                  :color="getQualitySummary(scope.row).score == null ? 'default' : (getQualitySummary(scope.row).score >= 90 ? 'green' : (getQualitySummary(scope.row).score >= 60 ? 'orange' : 'red'))"
                >
                  {{ qualitySummaryText(scope.row) }}
                </a-tag>
              </a-typography-link>
              <span v-else class="quality-none">未探查</span>
            </template>

            <template #handle="{ row }">
              <a-button
                type="link"
                :icon="h(EyeOutlined)"
                @click="handleDetailClick(row)"
              >
                详情
              </a-button>
              <a-button
                type="link"
                :icon="h(EditOutlined)"
                :disabled="row.status == 1"
                @click="handleEditClick(row)"
              >
                修改
              </a-button>
              <a-popover
                placement="bottom"
                :overlay-style="{ width: '120px' }"
                overlayClassName="handle-popover"
                trigger="click"
              >
                <template #reference>
                  <a-button type="link" :icon="h(DownOutlined)">
                    更多
                  </a-button>
                </template>
                <a-button
                  type="link"
                  danger
                  :icon="h(DeleteOutlined)"
                  :disabled="row.status == 1"
                  @click="handleDeleteClick(row)"
                >
                  删除
                </a-button>
                <a-button
                  type="link"
                  @click="handleDetailClick(row, 'VersionManagement')"
                >
                  <svg-icon icon-class="meta-version" class="handle-svg-icon" />
                  版本与变更
                </a-button>
              </a-popover>
            </template>
          </qt-table>
        </qt-wrap>
      </a-layout-content>
      </a-layout>
    </div>
  </div>
</template>

<script setup name="UnreleasedStructuredTable">
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, EyeOutlined, EditOutlined, DownOutlined } from '@ant-design/icons-vue'
import { reactive, ref, getCurrentInstance, computed, h } from "vue";

import { getParentLabelPath } from "@/utils/anivia.js";

import {
  listTable,
  delTable,
  updateTableStatus,
  batchDeleteCheck,
} from "@/api/cat/unreleased/table";

import { useRoute, useRouter } from "vue-router";

import { listDb } from "@/api/cat/unreleased/db";

import { batchQualitySummary } from "@/api/cat/task/quality";

import SourceSystemTree from "@/views/meta/task/structured/components/SourceSystemTree.vue";

const { proxy } = getCurrentInstance();

const router = useRouter();
const route = useRoute();
const sourceSystemTreeRef = ref();
const store = reactive({
  domains: [],
  treeDomains: [],
  rows: [],
  metaDatabases: [],
  metaTables: [],
});

// 数据源表最近一次质量探查结果摘要，key 为 datasourceId:tableName
const qualitySummaries = reactive({});

function getQualitySummaryKey(row) {
  return `${row.datasourceId}:${row.tableName}`;
}

function getQualitySummary(row) {
  if (!row.datasourceId || !row.tableName) return null;
  return qualitySummaries[getQualitySummaryKey(row)] || null;
}

function qualitySummaryText(row) {
  const summary = getQualitySummary(row);
  if (!summary) return "未探查";
  if (summary.score != null) {
    return `得分 ${summary.score}`;
  }
  return summary.successFlag == "1" ? "通过" : "已探查";
}

function handleQualitySummaryClick(row) {
  const summary = getQualitySummary(row);
  if (!summary || !summary.taskId) return;
  router.push({
    path: "/ast/quality/qualityTask/detail",
    query: { id: summary.taskId, info: true },
  });
}

// 批量加载质量结果摘要
function loadQualitySummaries(rows) {
  const queryList = rows
    .filter((row) => row.datasourceId && row.tableName)
    .map((row) => ({
      datasourceId: row.datasourceId,
      tableName: row.tableName,
    }));
  if (!queryList.length) return;
  batchQualitySummary(queryList)
    .then((res) => {
      const map = res.data || {};
      Object.keys(map).forEach((key) => {
        qualitySummaries[key] = map[key];
      });
    })
    .catch((err) => {
      console.error("质量结果摘要加载失败", err);
    });
}

const tableRef = ref(null);
const tableStroe = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "createTime", order: "descending" },
      onSelectionChange: function (rows) {
        store.rows = rows;
      },
      onRowDblclick: handleDetailClick,
    },
  },
  columns: [
    {
      type: "selection",
      width: 55,
      // selectable: function (row) {
      //     return row.status === '0' ? true : false;
      // }
    },
    {
      label: "编号",
      prop: "id",
      sortable: true,
      width: 90,
    },
    {
      label: "所属库名",
      prop: "dbName",
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 230,
    },
    {
      label: "表名称",
      prop: "tableName",
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 240,
      link: {
        external: handleDetailClick,
      },
    },
    {
      label: "表注释",
      prop: "tableComment",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 240,
      align: "left",
    },
    {
      label: "描述",
      prop: "description",
      align: "left",
      width: 240,
      showOverflowTooltip: {
        effect: "light",
      },
    },

    {
      label: "质量结果",
      width: 120,
      slot: "qualitySummary",
      align: "center",
    },

    {
      label: "版本号",
      prop: "version",
      width: 90,
    },
    {
      label: "状态",
      prop: "status",
      width: 90,
      slot: "status",
    },
    {
      label: "更新人",
      prop: "updateBy",
      width: 120,
    },
    {
      label: "更新时间",
      prop: "updateTime",
      sortable: true,
      width: 160,
      date: true,
    },
    {
      label: "创建人",
      prop: "createBy",
      width: 120,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      width: 160,
      date: true,
    },
    {
      label: "操作",
      width: 220,
      fixed: "right",
      slot: "handle",
    },
  ],
  func: listTable,
  params: {
    dataType: 1,
  },
  events: {
    formatData: function (data) {
      data.forEach((item) => {
        item.version = proxy.formatVersion(item.version);
      });
      loadQualitySummaries(data);
      return data;
    },
  },
});

const searchStore = reactive({
  items: [
    {
      label: "表名称",
      prop: "tableName",
      component: {
        is: "input",
      },
    },
    {
      label: "表注释",
      prop: "tableComment",
      component: {
        is: "input",
      },
    },
    {
      label: "所属库名",
      prop: "dbId",
      component: {
        is: "select",
        options: store.metaDatabases,
      },
    },
  ],
});

// 获取来源系统路径
const getDomainPath = computed(() => {
  return function (id) {
    let domainName = getParentLabelPath(store.treeDomains, id, {
      idKey: "id",
      labelKey: "name",
      childrenKey: "children",
    });
    const idx = domainName.indexOf("/");
    return idx == -1 ? domainName : domainName.slice(idx + 1);
  };
});

function handleTreeDataLoaded({ treeData }) {
  store.treeDomains = treeData;
}

// 节点单击事件
function handleNodeClick(data) {
  // 清除之前的筛选
  tableStroe.params.sourceSystemId = undefined;
  tableStroe.params.datasourceId = undefined;
  tableStroe.params.taskId = undefined;
  tableStroe.params.dbId = undefined;

  if (data.type === "SOURCE") {
    tableStroe.params.sourceSystemId = data.id;
  } else if (data.type === "DATASOURCE") {
    tableStroe.params.datasourceId = data.id;
  } else if (data.type === "DATABASE") {
    tableStroe.params.taskId = data.taskId;
    tableStroe.params.dbId = data.id;
  }
  tableRef.value.getList();
}

// 搜索按钮操作
function handleQueryClick() {
  tableRef.value?.getList();
}

// 重置按钮操作
function handleResetQueryClick() {
  if (sourceSystemTreeRef.value?.resetTree) {
    sourceSystemTreeRef.value.resetTree();
  }
  tableStroe.params.sourceSystemId = null;
  tableStroe.params.datasourceId = null;
  tableStroe.params.taskId = null;
  tableStroe.params.dbId = null;
  tableRef.value?.resetQuery();
}

// 获取库元素列表
function getMetaDatabases() {
  store.metaDatabases.splice(0, store.metaDatabases.length);
  return listDb({ pageSize: 1000 }).then((res) => {
    res.data.rows.forEach((item) => {
      store.metaDatabases.push({
        value: item.id,
        label: item.dbName,
      });
    });
    return res;
  });
}

// 修改
function handleEditClick(row) {
  router.push({
    path: route.path + "/edit",
    query: {
      id: row.id,
    },
  });
}

// 删除选中行
function handleDeleteColumnClick() {
  if (!store.rows.length) return;
  const ids = store.rows.map((item) => item.id);
  store.loading = true;
  batchDeleteCheck(ids).then((res) => {
    const { canDeleteCount, cannotDeleteCount, canDeleteIds } = res.data;
    store.loading = false;
    Modal.confirm({
      title: "系统提示",
      content: `可删除${canDeleteCount}个，不可删除${cannotDeleteCount}个，是否删除可删部分`,
      okText: "确定",
      cancelText: "取消",
      onOk: async () => {
        if (!canDeleteIds.length) {
          message.success("删除成功");
          return;
        }
        await delTable(canDeleteIds.toString());
        message.success("删除成功");
        tableRef.value.getList();
      },
    });
  });
}

// 删除
function handleDeleteClick(row) {
  Modal.confirm({
    title: "系统提示",
    content: `是否确认删除编号为${row.id}的数据项？`,
    okText: "确定",
    cancelText: "取消",
    onOk: async () => {
      await delTable(row.id);
      message.success("删除成功");
      tableRef.value.getList();
    },
  });
}

// 详情
function handleDetailClick(row, tab) {
  router.push({
    path: route.path + "/detail",
    query: {
      id: row.id,
      tab: typeof tab === "string" ? tab : undefined,
      table_status: 1,
    },
  });
}

// 切换状态
function handleStatusChange(row, status) {
  Modal.confirm({
    title: "系统提示",
    content: `是否确认${status == 1 ? "发布" : "取消发布"}数据编号为${
      row.id
    }的表元数据吗？`,
    okText: "确定",
    cancelText: "取消",
    onOk: async () => {
      try {
        await updateTableStatus({
          id: row.id,
          status,
        });
        message.success(
          `编号为${row.id}的表元数据${status == 1 ? "发布" : "取消发布"}成功!`
        );
        row.status = status;
      } catch (error) {
        row.status = status == "1" ? "0" : "1";
      }
    },
  });
}

getMetaDatabases();
</script>

<style lang="scss" scoped>
.metadata-result-page {
  height: 100%;
}

.quality-none {
  color: #a8b0bd;
  font-size: 13px;
}

.metadata-workspace {
  height: 100%;
  min-height: calc(100vh - 132px);
  padding: 14px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
  box-sizing: border-box;
}

.metadata-layout {
  height: 100%;
}

.main-content {
  padding: 0 0 0 14px;
}

:deep(.left-pane) {
  padding: 0;
}

:deep(.left-tree) {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

:deep(.resize-bar) {
  margin: 0 2px;
}

:deep(.qt-wrap) {
  gap: 0;
}

:deep(.qt-wrap--search),
:deep(.qt-wrap--content) {
  border: none;
  box-shadow: none;
}

:deep(.qt-wrap--search) {
  padding: 0 0 14px;
  border-bottom: 1px solid #edf1f7;
  border-radius: 0;
}

:deep(.qt-wrap--content.full) {
  min-height: calc(100vh - 230px);
  padding: 14px 0 0;
}
</style>

