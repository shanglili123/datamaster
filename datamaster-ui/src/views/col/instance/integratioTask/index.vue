<template>
  <div class="app-container" ref="app-container">
    <a-layout style="90%; background: transparent">
      <DeptTree
        ref="DeptTreeRef"
        :deptOptions="deptOptions"
        :leftWidth="leftWidth"
        :placeholder="'请输入数据集成目录名称'"
        @node-click="handleNodeClick"
      />
      <a-layout-content>
        <div class="pagecont-top" v-show="showSearch">
          <a-form
            class="btn-style"
            :model="queryParams"
            ref="queryRef"
            layout="inline"
            :label-col="{ style: { width: '100px' } }"
            v-show="showSearch"
            @submit.prevent
          >
            <a-form-item label="任务实例名称" name="name">
              <a-input
                class="el-form-input-width"
                v-model:value="queryParams.name"
                placeholder="请输入任务实例名称"
                allow-clear
                @pressEnter="handleQuery"
              />
            </a-form-item>
            <a-form-item label="执行状态" name="status">
              <a-select v-model:value="queryParams.status" placeholder="请选择执行状态" allow-clear class="el-form-input-width">
                <a-select-option
                  v-for="item in taskInstanceStatusOptions"
                  :key="item.value"
                  :value="item.value"
                >{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="执行时间" name="time">
              <a-range-picker
                class="el-form-input-width"
                v-model:value="queryParams.time"
                @change="handleTimeChange"
                valueFormat="YYYY-MM-DD"
                :separator="'-'"
                :placeholder="['开始日期', '结束日期']"
              />
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
        </div>

        <div class="pagecont-bottom">
          <div class="justify-between mb15">
            <div class="justify-end top-right-btn">
              <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns" />
            </div>
          </div>
          <a-table
            striped
            :loading="loading"
            :data-source="dppEtlTaskLogList"
            :columns="tableColumns"
            :scroll="tableScroll"
            row-key="id"
            :default-sort-order="'descend'"
            @change="handleTableChange"
            :locale="{ emptyText: '' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'name'">
                {{ record.name || "-" }}
              </template>
              <template v-else-if="column.dataIndex === 'commandType'">
                {{ commandTypeLabel(record.commandType) }}
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <a-tag
                  v-if="record.status !== null && record.status !== undefined && record.status !== ''"
                  :color="taskInstanceStatusType(record.status)"
                >
                  {{ taskInstanceStatusLabel(record.status) }}
                </a-tag>
                <span v-else>-</span>
              </template>
              <template v-else-if="column.dataIndex === 'startTime'">
                {{ record.startTime || "-" }}
              </template>
              <template v-else-if="column.dataIndex === 'endTime'">
                <span>{{ parseTime(record.endTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
              <template v-else-if="column.key === 10">
                {{ record.createBy || "-" }}
              </template>
              <template v-else-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="logDetailCatList(record)">查看日志</a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleExport(record)"
                  @mousedown="(e) => e.preventDefault()"
                >
                  下载日志
                </a-button>
              </template>
            </template>

            <template #emptyText>
              <div class="emptyBg">
                <img src="@/assets/system/images/no_data/noData.png" alt="" />
                <p>暂无记录</p>
              </div>
            </template>
          </a-table>

          <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </div>
      </a-layout-content>
    </a-layout>

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
  0: { label: "提交成功", type: "default" },
  1: { label: "运行中", type: "processing" },
  2: { label: "准备暂停", type: "warning" },
  3: { label: "暂停", type: "warning" },
  4: { label: "准备停止", type: "warning" },
  5: { label: "停止", type: "default" },
  6: { label: "失败", type: "error" },
  7: { label: "成功", type: "success" },
  8: { label: "需要容错", type: "warning" },
  9: { label: "已杀死", type: "error" },
  10: { label: "等待线程", type: "default" },
  11: { label: "等待依赖", type: "default" },
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
  return taskInstanceStatusMap[normalizeCode(status)]?.type || "default";
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
  { key: 10, label: "创建人", visible: true },
  { key: 11, label: "创建时间", visible: true },
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  return column ? column.visible : true;
};

const tableColumns = computed(() => {
  const all = [
    { key: 0, title: "编号", dataIndex: "id", width: 225, align: "left" },
    { key: 1, title: "任务实例名称", dataIndex: "name", width: 200, align: "left", ellipsis: true },
    { key: 3, title: "执行类型", dataIndex: "commandType", width: 120, align: "left", ellipsis: true },
    { key: 4, title: "执行状态", dataIndex: "status", width: 100, align: "left" },
    { key: 5, title: "开始时间", dataIndex: "startTime", width: 160, align: "left", ellipsis: true },
    { key: 6, title: "结束时间", dataIndex: "endTime", width: 160, align: "left", ellipsis: true },
    { key: 10, title: "创建人", dataIndex: "createBy", align: "left", ellipsis: true },
    {
      key: 11,
      title: "创建时间",
      dataIndex: "createTime",
      width: 150,
      align: "left",
      sorter: true,
      columnKey: "create_time",
      defaultSortOrder: "descend",
      sortDirections: ["descend", "ascend"],
    },
    { key: "action", title: "操作", align: "center", width: 180, fixed: "right" },
  ];
  return all.filter((col) => col.key === "action" || getColumnVisibility(col.key));
});
// 列总宽超出容器时启用横向滚动，保证 fixed 列与内容完整展示
const tableScroll = computed(() => {
  const totalWidth = tableColumns.value.reduce(
    (sum, c) => sum + (typeof c.width === "number" ? c.width : 0),
    0
  );
  return totalWidth > 0 ? { x: totalWidth } : undefined;
});

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
    spaceId: null,
    spaceCode: null,
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
  queryParams.value.spaceCode = userStore.spaceCode;
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
    spaceId: userStore.spaceId,
    spaceCode: userStore.spaceCode,
    validFlag: true,
  }).then((response) => {
    const children = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "数据集成目录",
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

function handleTableChange(pagination, filters, sorter) {
  if (sorter && sorter.field) {
    queryParams.value.orderByColumn = sorter.columnKey || sorter.field;
    queryParams.value.isAsc =
      sorter.order === "ascend" ? "ascending" : sorter.order === "descend" ? "descending" : null;
    getList();
  }
}

watch(
  () => userStore.spaceCode,
  (spaceCode) => {
    if (spaceCode) {
      getList();
    }
  },
  { immediate: true }
);

getDeptTree();
</script>

<style scoped lang="scss">
::v-deep {
  .selectlist .ant-tag {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.ant-layout-content {
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
