<template>
  <div class="app-container" ref="app-container">

    <a-layout style="90%">
      <DeptTree :deptOptions="deptOptions" :leftWidth="leftWidth" :placeholder="'请输入质量探查目录名称'" ref="DeptTreeRef"
        @node-click="handleNodeClick" />
      <a-layout-content>
        <div class="pagecont-top" v-show="showSearch">
          <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }"
            v-show="showSearch" @submit.prevent>
             <a-form-item label="名称" name="taskName">
              <a-input v-model:value="queryParams.taskName" placeholder="请输入任务名称" allow-clear
                @pressEnter="handleQuery" style="width: 160px;" />
            </a-form-item>
            <a-form-item label="状态" name="status">
              <a-select v-model:value="queryParams.status" placeholder="请选择任务状态" allow-clear
                style="width: 160px;">
                <a-select-option v-for="dict in ast_discovery_task_status" :key="dict.value" :value="dict.value">{{
                  dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="创建人" name="contact">
              <a-input v-model:value="queryParams.contact" placeholder="请输入创建人" allow-clear
                style="width: 160px;" @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
              </a-button>
              <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
              </a-button>
            </a-form-item>
          </a-form>
          <div class="data-action-btns">
            <a-button type="primary" @click="routeTo('/ast/quality/qualityTask/add', { row: null, })"
              v-hasPermi="['ast:qualityTask:add']" @mousedown="(e) => e.preventDefault()">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
            <a-button @click="routeTo('/ast/quality/errorStorageConfig', {})"
              @mousedown="(e) => e.preventDefault()">
              <i class="iconfont-mini icon-shezhi mr5"></i>存储配置
            </a-button>
          </div>
          <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
          </div>
        </div>
        <div>
          <a-spin :spinning="loading">
            <a-table
              :data-source="DppQualityTaskEvaluateList"
              :columns="tableColumns"
              :pagination="false"
              striped
              :scroll="{ y: '60vh', x: 1800 }"
              row-key="id"
              :locale="{ emptyText: emptyContent }"
              @change="handleSortChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'taskName'">
                  <span class="link-text">{{ record.taskName || '-' }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'strategy'">
                  <dict-tag :options="col_etl_task_execution_type" :value="record.strategy" />
                </template>
                <template v-else-if="column.dataIndex === 'cycle'">
                  <span v-if="record.cycle">{{ cronToZh(record.cycle) }}</span>
                  <a-tag v-else size="small">未设置</a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'lastExecuteTime'">
                  <span v-if="record.lastExecuteTime">{{ parseTime(record.lastExecuteTime, '{y}-{m}-{d} {h}:{i}') }}</span>
                  <a-tag v-else color="default" size="small">未执行</a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                  {{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') || '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'status'">
                  <a-tag v-if="record.status == 0" color="success" size="small">上</a-tag>
                  <a-tag v-else color="default" size="small">下</a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <div class="task-actions-col">
                    <div class="action-row">
                      <a-button type="link" size="small" @click="routeTo('/ast/quality/qualityTask/edit', { ...record })" v-hasPermi="['ast:qualityTask:edit']" :disabled="record.status == 0">配置</a-button>
                      <a-button type="link" size="small" @click="routeTo('/ast/quality/qualityTask/detail', { ...record, info: true })" v-hasPermi="['ast:qualityTask:info']">详情</a-button>
                      <a-button type="link" danger size="small" :disabled="record.status == 0" @click="handleDelete(record)" v-hasPermi="['ast:qualityTask:remove']">删除</a-button>
                    </div>
                    <div class="action-row">
                      <a-button type="link" size="small" :disabled="record.status == 0" :loading="publishingId === record.id" @click="handlePublishClick(record)">发布</a-button>
                      <a-button type="link" size="small" :disabled="record.status != 0" :loading="unpublishingId === record.id" @click="handleUnpublishClick(record)">卸载</a-button>
                      <a-button type="link" size="small" @click="handleExecuteOnce(record)" v-hasPermi="['ast:qualityTask:once']" :disabled="record.status != 0">执行一次</a-button>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <span>{{ record[column.dataIndex] || '-' }}</span>
                </template>
              </template>
            </a-table>
          </a-spin>
          <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
      </a-layout-content>
    </a-layout>
    <DataViewDialog :visible="DataView" :taskType="3" @update:visible="DataView = $event" :data="form" title="执行记录" />

  </div>
</template>

<script setup name="QualityTask">

import { treeData } from "./data.js";

import {
  createEtlTaskFront
} from "@/api/col/task/index.js";

import { cronToZh } from "@/utils/cronUtils";

import { h } from 'vue';

const tableColumns = [
  { title: '编号', dataIndex: 'id', align: 'center', width: 105 },
  { title: '任务名称', dataIndex: 'taskName', align: 'left', width: 200 },
  { title: '所属目录', dataIndex: 'catName', align: 'left', width: 130 },
  { title: '描述', dataIndex: 'description', align: 'left', width: 160, ellipsis: true },
  { title: '稽查对象数', dataIndex: 'taskObjNum', align: 'center', width: 90 },
  { title: '稽查规则数', dataIndex: 'taskEvaluateNum', align: 'center', width: 90 },
  { title: '执行策略', dataIndex: 'strategy', align: 'left', width: 110 },
  { title: '调度周期', dataIndex: 'cycle', align: 'left', width: 160 },
  { title: '上次执行', dataIndex: 'lastExecuteTime', align: 'left', width: 160 },
  { title: '创建人', dataIndex: 'createBy', align: 'left', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', align: 'left', width: 150, sorter: true },
  { title: '状态', dataIndex: 'status', align: 'left', width: 70 },
  { title: '备注', dataIndex: 'remark', align: 'left', width: 120, ellipsis: true },
  { title: '操作', key: 'actions', align: 'left', fixed: 'right', width: 260 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

import DataViewDialog from "./components/instance.vue";
const userStore = useUserStore();

import { useRoute, useRouter } from "vue-router";

import useUserStore from "@/store/system/user";

import DeptTree from "@/components/DeptTree";

import { deptUserTree } from "@/api/system/system/user.js";

import { ref, nextTick } from "vue";

import { listAttQualityCat } from "@/api/tax/cat/qualityCat/qualityCat.js";
const defaultSort = ref({ columnKey: 'create_time', order: 'desc' });

import {
  listDppQualityTask,
  delDppQualityTask,
  updateDppQualityTaskStatus,
  startDppQualityTask,
} from "@/api/ast/quality/qualityTask";;

const { proxy } = getCurrentInstance();
const { ast_discovery_task_status, col_etl_task_execution_type, datasource_type, col_etl_task_process_type } =
  proxy.useDict(
    "ast_discovery_task_status",
    "col_etl_task_execution_type",
    "datasource_type",
    "col_etl_task_process_type"
  );
const typaOptions = treeData.map((item) => {
  return {
    ...item,
    label: item.label,
    value: item.label
  }
})

/** 排序触发事件 */
function handleSortChange(pag, filters, sorter) {
  const prop = sorter.field || sorter.column?.dataIndex;
  const order = sorter.order === 'ascend' ? 'ascending' : sorter.order === 'descend' ? 'descending' : null;
  queryParams.value.orderByColumn = prop === 'createTime' ? 'create_time' : prop;
  queryParams.value.isAsc = order;
  getList();
}

const getExecutionType = (executionType) => {
  console.log(executionType);

  return typaOptions.find((item) => item.value == executionType)?.label
}
const getStatus = (status) => {
  if (status == '-1') {
    return '-1'
  } else {
    return '0'
  }
}
// 任务配置
const taskConfigDialogVisible = ref(false);
let userList = ref([]);
let taskForm = ref({});
const handleAdd = () => {
  taskConfigDialogVisible.value = true;
}
// 保存并关闭
const handleSave = (form) => {
  const parms = {
    ...form,

    spaceCode: userStore.spaceCode,
    type: "3",//数据开发新增标识
  }
  createEtlTaskFront(parms).then((res) => {
    if (res.code == 200) {
      proxy.$modal.msgSuccess("操作成功");
      getList();
    }
  })
}
const deptOptions = ref([]);
const leftWidth = ref(240); // 初始左侧宽度
/** 下拉树结构 */
function getDeptTree() {
  listAttQualityCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "质量探查目录",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
  deptUserTree().then((res) => {
    userList.value = res.data;
  });
}
function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  queryParams.value.pageNum = 1;
  handleQuery();
}
const route = useRoute();
const tableRef = ref(null);
const DppQualityTaskEvaluateList = ref([]);
const handleExecuteOnce = async (row) => {
  if (!row?.id) {
    proxy.$modal.msgWarning("无效的任务id，请刷新后重试");
    return;
  }
  loading.value = true;
  try {
    const res = await startDppQualityTask(row.id);

    if (Number(res?.code) === 200) {
      proxy.$modal.msgSuccess("执行成功");
    } else {
      proxy.$modal.msgWarning(res?.msg || "执行失败");
    }
  } finally {
    loading.value = false;
  }
};
const publishingId = ref(null);
const unpublishingId = ref(null);
function handlePublishClick(row) {
  proxy.$modal.confirm('确认发布"' + row.taskName + '"质量任务吗？').then(function () {
    publishingId.value = row.id;
    updateDppQualityTaskStatus({ id: row.id, status: 0 })
      .then(() => {
        proxy.$modal.msgSuccess("发布成功");
        getList();
      })
      .finally(() => { publishingId.value = null; });
  }).catch(() => {});
}
function handleUnpublishClick(row) {
  proxy.$modal.confirm('确认卸载"' + row.taskName + '"质量任务吗？').then(function () {
    unpublishingId.value = row.id;
    updateDppQualityTaskStatus({ id: row.id, status: 1 })
      .then(() => {
        proxy.$modal.msgSuccess("卸载成功");
        getList();
      })
      .finally(() => { unpublishingId.value = null; });
  }).catch(() => {});
}
let DataView = ref(false);
/** 运行实例接口 */
function handleDataView(row) {
  form.value = row;
  DataView.value = true;
}
// 列显隐信息
const columns = ref([
  { key: 1, label: "编号", visible: true },
  { key: 2, label: "任务名称", visible: true },
  { key: 3, label: "所属目录", visible: true },
  { key: 4, label: "描述", visible: true },
  { key: 5, label: "稽查对象数", visible: true },
  { key: 6, label: "稽查规则数", visible: true },
  { key: 7, label: "执行策略", visible: true },
  { key: 8, label: "调度周期", visible: true },
  { key: 9, label: "上次执行时间", visible: true },
  { key: 10, label: "创建人", visible: true },
  { key: 11, label: "创建时间", visible: true },
  { key: 12, label: "状态", visible: true },
  { key: 13, label: "备注", visible: true },
  { key: 14, label: "操作", visible: true },
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  if (!column) return true;
  return column.visible;
};

const open = ref(false);
const loading = ref(false);
const showSearch = ref(true);
const ids = ref([]);
const total = ref(0);
const router = useRouter();
const data = reactive({
  form: {

  },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    type: null,
    taskName: null,
    status: null,
    contact: null,
  },
  rules: {},
});

const { queryParams, form, rules } = toRefs(data);

function normalizePageData(response) {
  const data = response?.data ?? response ?? {};
  if (Array.isArray(data)) {
    return { rows: data, total: data.length };
  }
  const rows = Array.isArray(data.rows)
    ? data.rows
    : Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(response?.rows)
          ? response.rows
          : [];
  const total = Number(data.total ?? data.totalCount ?? response?.total ?? rows.length);
  return { rows, total: Number.isNaN(total) ? rows.length : total };
}


function getList() {
  loading.value = true;
  queryParams.value.spaceCode = userStore.spaceCode;
  queryParams.value.spaceId = userStore.spaceId;
  listDppQualityTask(queryParams.value).then((response) => {
    const pageData = normalizePageData(response);
    DppQualityTaskEvaluateList.value = pageData.rows;
    total.value = pageData.total;
    loading.value = false;
    nextTick(() => { tableRef.value?.doLayout(); });
  });
}


/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
const DeptTreeRef = ref(null);
/** 重置按钮操作 */
function resetQuery() {
  if (DeptTreeRef.value?.resetTree) {
    DeptTreeRef.value.resetTree();
  }
  queryParams.value.catCode = "";
  queryParams.value.pageNum = 1;
  proxy.resetForm("queryRef");
  handleQuery();
}
/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除质量探查任务编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delDppQualityTask(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => { });
}

function routeTo(link, row) {
  if (link !== "" && link.indexOf("http") !== -1) {
    window.location.href = link;
    return;
  }
  if (link !== "") {
    if (link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      router.push({
        path: link,
        query: {
          id: row?.id,
          info: row?.info,
        },
      });
    }
  }
}

// onActivated(() => {
// });
onActivated(() => {
  getList();
});
getList();
getDeptTree();

</script>
<style lang="scss" src="@/assets/system/styles/table-style-optimized.scss"></style>
<style scoped lang="scss">
::v-deep {
  .selectlist .ant-tag.ant-tag-info {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.pagecont-top {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 8px;

  .ant-form {
    display: flex !important;
    flex-wrap: nowrap !important;
    flex: 0 1 auto !important;

    .ant-form-item {
      display: inline-flex !important;
      flex-shrink: 0 !important;
      margin-bottom: 0 !important;
    }
  }

  .data-action-btns {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .top-right-btn {
    flex-shrink: 0;
  }
}

.app-container {
  margin: 13px 15px;
}

.ant-layout-content {
  padding: 2px 0px;
}

.task-actions-col {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  .action-row {
    display: flex;
    justify-content: flex-start;
    gap: 0;
    .ant-btn {
      font-size: 12px;
      padding: 0 2px;
    }
  }
}

.link-text {
  color: var(--el-color-primary);
  cursor: pointer;
  &:hover {
    text-decoration: underline;
  }
}
</style>


