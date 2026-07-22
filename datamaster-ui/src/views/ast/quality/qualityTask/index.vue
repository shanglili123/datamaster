<template>
  <div class="app-container" ref="app-container">

    <el-container style="90%">
      <DeptTree :deptOptions="deptOptions" :leftWidth="leftWidth" :placeholder="'请输入数据质量类目名称'" ref="DeptTreeRef"
        @node-click="handleNodeClick" />
      <el-main>
        <div class="pagecont-top" v-show="showSearch">
          <el-form class="btn-style" :model="queryParams" ref="queryRef" :inline="true" label-width="75px"
            v-show="showSearch" @submit.prevent>
            <el-form-item label="任务名称" prop="taskName">
              <el-input class="el-form-input-width" v-model="queryParams.taskName" placeholder="请输入任务名称" clearable
                @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="任务状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择任务状态" clearable class="el-form-input-width">
                <el-option v-for="dict in ast_discovery_task_status" :key="dict.value" :label="dict.label"
                  :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="创建人" prop="contact">
              <el-input v-model="queryParams.contact" placeholder="请输入创建人" clearable
                class="el-form-input-width" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button plain type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
              </el-button>
              <el-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="pagecont-bottom">
          <div class="justify-between mb15">
            <el-row :gutter="15" class="btn-style">
              <el-col :span="1.5">
                <el-button type="primary" plain @click="routeTo('/ast/quality/qualityTask/add', { row: null, })"
                  v-hasPermi="['da:qualityTask:add']" @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                </el-button>
              </el-col>
              <el-col :span="1.5">
                <el-button plain @click="routeTo('/ast/quality/errorStorageConfig', {})"
                  @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-shezhi mr5"></i>存储配置
                </el-button>
              </el-col>
            </el-row>
            <div class="justify-end top-right-btn">
              <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
            </div>
          </div>
          <el-table ref="tableRef" stripe v-loading="loading" :data="DppQualityTaskEvaluateList" :default-sort="defaultSort"
            @sort-change="handleSortChange">
            <el-table-column v-if="getColumnVisibility(1)" label="编号" align="center" prop="id" width="105" />
            <el-table-column v-if="getColumnVisibility(2)" label="任务名称" align="left" prop="taskName" width="200">
              <template #default="scope">
                <span class="link-text">{{ scope.row.taskName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(3)" label="所属类目" align="left" prop="catName" width="130">
              <template #default="scope">
                {{ scope.row.catName || '-' }}
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(4)" label="描述" width="160" align="left" prop="description">
              <template #default="scope">
                {{ scope.row.description || '-' }}
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(5)" label="稽查对象数" align="center" prop="taskObjNum" width="90">
              <template #default="scope">
                {{ scope.row.taskObjNum || '-' }}
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(6)" label="稽查规则数" align="center" prop="taskEvaluateNum"
              width="90">
              <template #default="scope">
                {{ scope.row.taskEvaluateNum || '-' }}
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(7)" label="执行策略" align="left" prop="strategy" width="110">
              <template #default="scope">
                <dict-tag :options="col_etl_task_execution_type" :value="scope.row.strategy" />
              </template>
            </el-table-column>

            <el-table-column v-if="getColumnVisibility(8)" label="调度周期" align="left" prop="cycle" width="160">
              <template #default="scope">
                <span v-if="scope.row.cycle">
                  <el-icon style="margin-right:4px"><Clock /></el-icon>
                  {{ cronToZh(scope.row.cycle) }}
                </span>
                <el-tag v-else size="small">未设置</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(9)" label="上次执行" align="left" prop="lastExecuteTime"
              width="160">
              <template #default="scope">
                <span v-if="scope.row.lastExecuteTime">{{
                  parseTime(scope.row.lastExecuteTime, '{y}-{m}-{d} {h}:{i}')
                }}</span>
                <el-tag v-else size="small" type="info">未执行</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(10)" width="120" label="创建人" align="left" prop="createBy">
              <template #default="scope">
                {{ scope.row.createBy || '-' }}
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(11)" label="创建时间" align="left" prop="createTime" width="150"
              sortable="custom" column-key="create_time" :sort-orders="['descending', 'ascending']">
              <template #default="scope">
                {{
                  parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}") || "-"
                }}
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(12)" align="left" prop="status" width="110">
              <template #header>
                <span>发布状态</span>
              </template>
              <template #default="scope">
                <el-tag v-if="scope.row.status == 0" type="success" size="small">已发布</el-tag>
                <el-tag v-else type="info" size="small">未发布</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(13)" label="备注" width="120" align="left" prop="remark"
              :show-overflow-tooltip="{ effect: 'light' }">
              <template #default="scope">
                {{ scope.row.remark || '-' }}
              </template>
            </el-table-column>

            <el-table-column v-if="getColumnVisibility(14)" label="操作" align="left" fixed="right" width="260">
              <template #default="scope">
                <div class="task-actions-col">
                  <div class="action-row">
                    <el-button link type="primary" icon="Edit" @click="routeTo('/ast/quality/qualityTask/edit', {
                      ...scope.row,
                    })" v-hasPermi="['da:qualityTask:edit']" :disabled="scope.row.status == 0">
                      配置</el-button>
                    <el-button link type="primary" icon="view" @click="
                      routeTo('/ast/quality/qualityTask/detail', {
                        ...scope.row,
                        info: true,
                      })
                      " v-hasPermi="['da:qualityTask:info']">详情</el-button>
                    <el-button link type="danger" icon="Delete" :disabled="scope.row.status == 0"
                      @click="handleDelete(scope.row)" v-hasPermi="['da:qualityTask:remove']">删除</el-button>
                  </div>
                  <div class="action-row">
                    <el-button link type="success" icon="Upload" :disabled="scope.row.status == 0"
                      :loading="publishingId === scope.row.id"
                      @click="handlePublishClick(scope.row)">发布</el-button>
                    <el-button link type="warning" icon="Download" :disabled="scope.row.status != 0"
                      :loading="unpublishingId === scope.row.id"
                      @click="handleUnpublishClick(scope.row)">卸载</el-button>
                    <el-button link type="primary" icon="VideoPlay"
                      @click="handleExecuteOnce(scope.row)" v-hasPermi="['da:qualityTask:once']"
                      :disabled="scope.row.status != 0">执行一次</el-button>
                  </div>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <div class="emptyBg">
                <img src="@/assets/system/images/no_data/noData.png" alt="" />
                <p>暂无记录</p>
              </div>
            </template>
          </el-table>
          <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
      </el-main>
    </el-container>
    <DataViewDialog :visible="DataView" :taskType="3" @update:visible="DataView = $event" :data="form" title="执行记录" />

  </div>
</template>

<script setup name="QualityTask">
import { treeData } from "./data.js";
import {
  createEtlTaskFront
} from "@/api/col/task/index.js";
import { cronToZh } from "@/utils/cronUtils";
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
function handleSortChange({ column, prop, order }) {
  queryParams.value.orderByColumn = column?.columnKey || prop;
  queryParams.value.isAsc = column.order;
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

    projectCode: userStore.projectCode,
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
const leftWidth = ref(300); // 初始左侧宽度
/** 下拉树结构 */
function getDeptTree() {
  listAttQualityCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "数据质量类目",
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
  { key: 3, label: "所属类目", visible: true },
  { key: 4, label: "描述", visible: true },
  { key: 5, label: "稽查对象数", visible: true },
  { key: 6, label: "稽查规则数", visible: true },
  { key: 7, label: "执行策略", visible: true },
  { key: 8, label: "调度周期", visible: true },
  { key: 9, label: "上次执行时间", visible: true },
  { key: 10, label: "创建人", visible: true },
  { key: 11, label: "创建时间", visible: true },
  { key: 12, label: "发布状态", visible: true },
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
  queryParams.value.projectCode = userStore.projectCode;
  queryParams.value.projectId = userStore.projectId;
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
    .confirm('是否确认删除数据质量任务编号为"' + _ids + '"的数据项？')
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
  .selectlist .el-tag.el-tag--info {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.app-container {
  margin: 13px 15px;
}

.el-main {
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
    .el-button {
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


