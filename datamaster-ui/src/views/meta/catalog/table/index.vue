<template>
  <div class="app-container metadata-result-page">
    <div class="metadata-workspace">
      <a-layout class="metadata-layout">
      <SourceSystemTree
        ref="sourceSystemTreeRef"
        tree-type="dbTable"
        @node-click="handleNodeClick"
        @data-loaded="handleTreeDataLoaded"
      />
      <a-layout-content class="main-content">
        <dm-wrap :columns="tableStroe.columns" :tableRef="tableRef">
          <template #search>
            <dm-search-bar
              v-bind="searchStore"
              :params="tableStroe.params"
              @query="handleQueryClick"
              @reset="handleResetQueryClick"
              :config="{ permi: ['cat:table:query'] }"
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
          <dm-table v-bind="tableStroe" ref="tableRef">
            <template #domain-name="scope">
              {{ getDomainPath(scope.row.domainId) }}
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
                  @click="handleDeleteClick(row)"
                >
                  删除
                </a-button>
              </a-popover>
            </template>
          </dm-table>
        </dm-wrap>
      </a-layout-content>
      </a-layout>
    </div>
  </div>
</template>

<script setup name="CatalogTable">
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, EyeOutlined, EditOutlined, DownOutlined } from '@ant-design/icons-vue'
import { reactive, ref, getCurrentInstance, computed, h } from "vue";

import { getParentLabelPath } from "@/utils/anivia.js";

import {
  listTable,
  delTable,
  batchDeleteCheck,
} from "@/api/cat/catalog/table";

import { useRouter } from "vue-router";

import { listDb } from "@/api/cat/catalog/db";

import { batchQualitySummary } from "@/api/cat/task/quality";

import { listProbeHistoryByTable } from "@/api/ast/quality/probeTaskInstance";

import SourceSystemTree from "@/views/meta/task/structured/components/SourceSystemTree.vue";

const { proxy } = getCurrentInstance();

const router = useRouter();
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
  // 质量结果标签点击：直接跳转该表最近一次探查报告
  handleProbeReportClick(row);
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
      width: 310,
      fixed: "right",
      slot: "handle",
    },
  ],
  func: listTable,
  params: {},
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
  tableStroe.params.dbName = undefined;
  tableStroe.params.tableName = undefined;

  if (data.type === "DATABASE") {
    // 库节点按数据源和库名筛选，兼容历史采集产生的重复库记录。
    tableStroe.params.datasourceId = data.datasourceId;
    tableStroe.params.dbName = data.name;
    tableRef.value.getList();
  } else if (data.type === "TABLE") {
    // 表节点只加载对应的元数据结果列表；由列表中的“详情”再进入详情页。
    tableStroe.params.datasourceId = data.datasourceId;
    tableStroe.params.tableName = data.name;
    tableRef.value.getList();
  }
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
  tableStroe.params.dbName = null;
  tableStroe.params.tableName = null;
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
    path: "/meta/catalog/table/edit",
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
    path: "/meta/catalog/table/detail",
    query: {
      id: row.id,
      tab: typeof tab === "string" ? tab : undefined,
    },
  });
}

// 查看质量报告：直接跳转该表最近一次探查报告
function handleProbeReportClick(row) {
  const { datasourceId, tableName } = row || {};
  if (!datasourceId || !tableName) {
    message.warning("该表暂无质量报告，请先执行带质量规则的探查任务");
    return;
  }
  listProbeHistoryByTable({ datasourceId, tableName })
    .then((res) => {
      const records = res.data || [];
      if (!records.length) {
        message.warning("该表暂无质量报告，请先执行带质量规则的探查任务");
        return;
      }
      // 后端按开始时间倒序，第一条即最近一次探查结果
      const latest = records[0];
      router.push({
        path: "/ast/quality/probeTaskInstance/detail",
        query: { id: latest.id, score: latest.score },
      });
    })
    .catch(() => {
      message.error("质量报告加载失败，请稍后重试");
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

:deep(.dm-wrap) {
  gap: 0;
}

:deep(.dm-wrap--search),
:deep(.dm-wrap--content) {
  border: none;
  box-shadow: none;
}

:deep(.dm-wrap--search) {
  padding: 0 0 14px;
  border-bottom: 1px solid #edf1f7;
  border-radius: 0;
}

:deep(.dm-wrap--content.full) {
  min-height: calc(100vh - 230px);
  padding: 14px 0 0;
}
</style>

