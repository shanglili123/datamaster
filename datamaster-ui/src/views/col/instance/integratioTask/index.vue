<template>
  <div class="app-container" ref="app-container">
    <el-container style="90%">
      <DeptTree
        ref="DeptTreeRef"
        :deptOptions="deptOptions"
        :leftWidth="leftWidth"
        :placeholder="'请输入数据集成类目名称'"
        @node-click="handleNodeClick"
      />
      <el-main>
        <div class="pagecont-top" v-show="showSearch">
          <el-form
            class="btn-style"
            :model="queryParams"
            ref="queryRef"
            :inline="true"
            label-width="100px"
            v-show="showSearch"
            @submit.prevent
          >
            <el-form-item label="任务实例名称" prop="name">
              <el-input
                class="el-form-input-width"
                v-model="queryParams.name"
                placeholder="请输入任务实例名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="执行状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择执行状态" clearable class="el-form-input-width">
                <el-option
                  v-for="item in taskInstanceStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="执行时间" prop="time">
              <el-date-picker
                class="el-form-input-width"
                v-model="queryParams.time"
                @change="handleTimeChange"
                value-format="YYYY-MM-DD"
                type="daterange"
                range-separator="-"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
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
            <div class="justify-end top-right-btn">
              <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns" />
            </div>
          </div>
          <el-table
            stripe
            v-loading="loading"
            :data="dppEtlTaskLogList"
            @selection-change="handleSelectionChange"
            :default-sort="defaultSort"
            @sort-change="handleSortChange"
          >
            <el-table-column v-if="getColumnVisibility(0)" width="225" label="编号" align="left" prop="id" />
            <el-table-column
              v-if="getColumnVisibility(1)"
              :show-overflow-tooltip="{ effect: 'light' }"
              label="任务实例名称"
              align="left"
              prop="name"
              width="200"
            >
              <template #default="scope">
                {{ scope.row.name || "-" }}
              </template>
            </el-table-column>

            <el-table-column
              v-if="getColumnVisibility(3)"
              label="执行类型"
              width="120"
              :show-overflow-tooltip="{ effect: 'light' }"
              align="left"
              prop="commandType"
            >
              <template #default="scope">
                {{ commandTypeLabel(scope.row.commandType) }}
              </template>
            </el-table-column>
            <el-table-column v-if="getColumnVisibility(4)" width="100" label="执行状态" align="left" prop="status">
              <template #default="scope">
                <el-tag
                  v-if="scope.row.status !== null && scope.row.status !== undefined && scope.row.status !== ''"
                  :type="taskInstanceStatusType(scope.row.status)"
                  size="small"
                >
                  {{ taskInstanceStatusLabel(scope.row.status) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(5)"
              width="160"
              label="开始时间"
              align="left"
              prop="startTime"
              :show-overflow-tooltip="{ effect: 'light' }"
            >
              <template #default="scope">
                {{ scope.row.startTime || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(6)"
              width="160"
              label="结束时间"
              align="left"
              prop="endTime"
              :show-overflow-tooltip="{ effect: 'light' }"
            >
              <template #default="scope">
                <span>{{ parseTime(scope.row.endTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
            </el-table-column>

            <el-table-column v-if="getColumnVisibility(9)" width="100" label="责任人" align="left" prop="createBy">
              <template #default="scope">
                {{ scope.row.personChargeName || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(10)"
              label="创建人"
              :show-overflow-tooltip="true"
              align="left"
              prop="createBy"
            >
              <template #default="scope">
                {{ scope.row.createBy || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(11)"
              label="创建时间"
              align="left"
              prop="create_time"
              width="150"
              sortable="custom"
              column-key="create_time"
              :sort-orders="['descending', 'ascending']"
            >
              <template #default="scope">
                <span>{{ parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="160">
              <template #default="scope">
                <el-button link type="primary" icon="View" @click="logDetailCatList(scope.row)">查看日志</el-button>
                <el-button
                  link
                  type="warning"
                  icon="Download"
                  @click="handleExport(scope.row)"
                  @mousedown="(e) => e.preventDefault()"
                >
                  下载日志
                </el-button>
              </template>
            </el-table-column>

            <template #empty>
              <div class="emptyBg">
                <img src="@/assets/system/images/no_data/noData.png" alt="" />
                <p>暂无记录</p>
              </div>
            </template>
          </el-table>

          <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </div>
      </el-main>
    </el-container>

    <TaskLogDialog ref="logDialogRef" />
  </div>
</template>

<script setup name="Integratio">
import TaskLogDialog from "@/views/col/components/taskLog.vue";
import { listDppEtlTaskInstance } from "@/api/col/instance/job";
import useUserStore from "@/store/system/user";
import { listAttTaskCat } from "@/api/tax/cat/taskCat/taskCat";
import DeptTree from "@/components/DeptTree/index.vue";

const { proxy } = getCurrentInstance();

const dppEtlTaskLogList = ref([]);
const taskInstanceStatusMap = {
  0: { label: "提交成功", type: "info" },
  1: { label: "运行中", type: "primary" },
  2: { label: "准备暂停", type: "warning" },
  3: { label: "暂停", type: "warning" },
  4: { label: "准备停止", type: "warning" },
  5: { label: "停止", type: "info" },
  6: { label: "失败", type: "danger" },
  7: { label: "成功", type: "success" },
  8: { label: "需要容错", type: "warning" },
  9: { label: "已杀死", type: "danger" },
  10: { label: "等待线程", type: "info" },
  11: { label: "等待依赖", type: "info" },
};
const taskInstanceStatusOptions = Object.entries(taskInstanceStatusMap).map(([value, item]) => ({
  value,
  label: item.label,
}));
const commandTypeMap = {
  0: "启动工作流",
  1: "从当前节点开始执行",
  2: "恢复容错工作流",
  3: "恢复暂停工作流",
  4: "从失败节点开始执行",
  5: "补数",
  6: "调度执行",
  7: "重跑",
  8: "暂停",
  9: "停止",
  10: "恢复等待线程",
  11: "恢复串行等待",
  12: "动态生成",
};

function normalizeCode(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function taskInstanceStatusLabel(status) {
  const code = normalizeCode(status);
  return taskInstanceStatusMap[code]?.label || code || "-";
}

function taskInstanceStatusType(status) {
  return taskInstanceStatusMap[normalizeCode(status)]?.type || "info";
}

function commandTypeLabel(commandType) {
  const code = normalizeCode(commandType);
  return commandTypeMap[code] || code || "-";
}

const columns = ref([
  { key: 0, label: "编号", visible: true },
  { key: 1, label: "任务实例名称", visible: true },
  { key: 3, label: "执行类型", visible: true },
  { key: 4, label: "执行状态", visible: true },
  { key: 5, label: "开始时间", visible: true },
  { key: 6, label: "结束时间", visible: true },
  { key: 9, label: "责任人", visible: true },
  { key: 10, label: "创建人", visible: true },
  { key: 11, label: "创建时间", visible: true },
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  return column ? column.visible : true;
};

const userStore = useUserStore();
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const defaultSort = ref({ prop: "createTime", order: "desc" });
const emit = defineEmits(["resetCat"]);
const leftWidth = ref(300);
const DeptTreeRef = ref(null);
const logDialogRef = ref(null);
const deptOptions = ref([]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    type: null,
    name: null,
    code: null,
    version: null,
    projectId: null,
    projectCode: null,
    personCharge: null,
    locations: null,
    description: null,
    timeout: null,
    extractionCount: null,
    writeCount: null,
    status: null,
    dsId: null,
    createTime: null,
    startTime: null,
    endTime: null,
    time: [],
    taskType: "1",
    catCode: null,
    orderByColumn: "start_time",
  },
});

const { queryParams } = toRefs(data);

function handleTimeChange(value) {
  if (!value) {
    handleTimeClear();
    return;
  }
  queryParams.value.startTime = value[0] + " 00:00:00";
  queryParams.value.endTime = value[1] + " 23:59:59";
}

function handleTimeClear() {
  queryParams.value.startTime = null;
  queryParams.value.endTime = null;
}

function getList() {
  loading.value = true;
  queryParams.value.projectCode = userStore.projectCode;
  listDppEtlTaskInstance(queryParams.value)
    .then((response) => {
      const page = response.data || {};
      dppEtlTaskLogList.value = page.rows || [];
      total.value = Number(page.total || dppEtlTaskLogList.value.length || 0);
    })
    .finally(() => {
      loading.value = false;
    });
}

const logDetailCatList = (row) => {
  logDialogRef.value?.open(row.id);
};

function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  queryParams.value.pageNum = 1;
  handleQuery();
}

async function handleExport(row) {
  proxy.download(
    "/col/etlTaskInstance/downloadLog",
    {
      taskInstanceId: row.id,
    },
    `${row.name}.log`
  );
}

function getDeptTree() {
  listAttTaskCat({
    projectId: userStore.projectId,
    projectCode: userStore.projectCode,
    validFlag: true,
  }).then((response) => {
    const children = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "数据集成类目",
        value: "",
        id: 0,
        children,
      },
    ];
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  if (DeptTreeRef.value?.resetTree) {
    DeptTreeRef.value.resetTree();
  }
  handleTimeClear();
  proxy.resetForm("queryRef");
  queryParams.value.catCode = null;
  emit("resetCat");
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleSortChange(column) {
  queryParams.value.orderByColumn = column.column?.columnKey || column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}

watch(
  () => userStore.projectCode,
  (projectCode) => {
    if (projectCode) {
      getList();
    }
  },
  { immediate: true }
);

getDeptTree();
</script>

<style scoped lang="scss">
::v-deep {
  .selectlist .el-tag.el-tag--info {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.el-main {
  padding: 2px 0px;
}

.pagecont-bottom {
  min-height: auto;
}

.ellipsis-container {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
