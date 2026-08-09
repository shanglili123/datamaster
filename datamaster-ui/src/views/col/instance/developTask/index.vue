<template>
  <div class="app-container" ref="app-container">
    <a-layout>
      <DeptTree ref="DeptTreeRef" :deptOptions="deptOptions" :leftWidth="leftWidth" :placeholder="'请输入数据开发目录名称'"
        @node-click="handleNodeClick" />

      <a-layout-content>
        <div class="pagecont-top" v-show="showSearch">
          <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline"
            :label-col="{ style: { width: '100px' } }" v-show="showSearch" @submit.prevent>
            <a-form-item label="任务实例名称" name="name">
              <a-input class="el-form-input-width" v-model:value="queryParams.name" placeholder="请输入任务实例名称" allow-clear
                @keyup.enter="handleQuery" />
            </a-form-item>
            <a-form-item label="执行状态" name="status">
              <a-select v-model:value="queryParams.status" placeholder="请选择执行状态" allow-clear class="el-form-input-width">
                <a-select-option v-for="item in taskInstanceStatusOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="执行时间" name="time">
              <a-range-picker class="el-form-input-width" v-model:value="queryParams.time" @change="handleRangeChange"
                valueFormat="YYYY-MM-DD" :allow-clear="false" />
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
              <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
            </div>
          </div>
          <a-table
            striped
            :loading="loading"
            :data-source="dppEtlTaskLogList"
            :columns="tableColumns"
            :scroll="tableScroll"
            :pagination="false"
            :locale="{ emptyText: '暂无记录' }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'name'">
                {{ record.name || "-" }}
              </template>
              <template v-if="column.dataIndex === 'commandType'">
                {{ commandTypeLabel(record.commandType) }}
              </template>
              <template v-if="column.dataIndex === 'status'">
                <a-tag v-if="record.status !== null && record.status !== undefined && record.status !== ''"
                  :color="taskInstanceStatusColor(record.status)" size="small">
                  {{ taskInstanceStatusLabel(record.status) }}
                </a-tag>
                <span v-else>-</span>
              </template>
              <template v-if="column.dataIndex === 'startTime'">
                <span>{{ parseTime(record.startTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
              <template v-if="column.dataIndex === 'endTime'">
                <span>{{ parseTime(record.endTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
              <template v-if="column.dataIndex === 'createBy'">
                {{ record.createBy || "-" }}
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
              <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="logDetailCatList(record)">查看日志</a-button>
                <a-button type="link" size="small" @click="handleExport(record)"
                  @mousedown="(e) => e.preventDefault()">下载日志</a-button>
              </template>
            </template>
          </a-table>
          <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
      </a-layout-content>
    </a-layout>
    <TaskLogDialog ref="logDialogRef" />
  </div>
</template>

<script setup name="Develop">
import { defineEmits, defineProps, computed } from "vue";
import { listAttDataDevCat } from "@/api/tax/cat/dataDevCat/dataDevCat";
import TaskLogDialog from "@/views/col/components/taskLog.vue";
import { listDppEtlTaskInstance } from "@/api/col/instance/job";
import useUserStore from "@/store/system/user";
const { proxy } = getCurrentInstance();
import DeptTree from "@/components/DeptTree/index.vue";
let activeName = ref("first");
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

/** antd a-tag color 适配：将 el-tag type 映射为 antd 预设色 */
function taskInstanceStatusColor(status) {
  const type = taskInstanceStatusType(status);
  const colorMap = { info: "default", primary: "processing", warning: "warning", danger: "error", success: "success" };
  return colorMap[type] || "default";
}

function commandTypeLabel(commandType) {
  const code = normalizeCode(commandType);
  return commandTypeMap[code] || code || "-";
}

// 列显隐信息
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
  // 如果没有找到对应列配置，默认显示
  if (!column) return true;
  // 如果找到对应列配置，根据visible属性来控制显示
  return column.visible;
};

const tableColumns = computed(() => {
  const allCols = [
    { title: '编号', dataIndex: 'id', align: 'left', width: 225, colKey: 0 },
    { title: '任务实例名称', dataIndex: 'name', align: 'left', width: 200, ellipsis: true, colKey: 1 },
    { title: '执行类型', dataIndex: 'commandType', align: 'left', width: 120, ellipsis: true, colKey: 3 },
    { title: '执行状态', dataIndex: 'status', align: 'left', width: 100, colKey: 4 },
    { title: '开始时间', dataIndex: 'startTime', align: 'left', width: 160, ellipsis: true, colKey: 5 },
    { title: '结束时间', dataIndex: 'endTime', align: 'left', width: 160, ellipsis: true, colKey: 6 },
    { title: '创建人', dataIndex: 'createBy', align: 'left', ellipsis: true, colKey: 10 },
    { title: '创建时间', dataIndex: 'createTime', align: 'left', width: 150, key: 'create_time', sorter: true, defaultSortOrder: 'descend', colKey: 11 },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 160, colKey: 'actions' },
  ];
  return allCols.filter(col => getColumnVisibility(col.colKey));
});
// 列总宽超出容器时启用横向滚动，保证 fixed 列与内容完整展示
const tableScroll = computed(() => {
  const totalWidth = tableColumns.value.reduce(
    (sum, c) => sum + (typeof c.width === 'number' ? c.width : 0),
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
    taskType: "3",
    catCode: null,
    orderByColumn: "start_time",
  },
});

const { queryParams } = toRefs(data);

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

/** antd 范围日期选择 change 适配：dateStrings 为格式化后的字符串 */
function handleRangeChange(dates, dateStrings) {
  if (dateStrings && dateStrings.length === 2) {
    queryParams.value.startTime = dateStrings[0] + " 00:00:00";
    queryParams.value.endTime = dateStrings[1] + " 23:59:59";
  } else {
    handleTimeClear();
  }
}
function handleTimeClear() {
  queryParams.value.startTime = null;
  queryParams.value.endTime = null;
}
/** 查询数据集成任务-日志列表 */
function getList() {
  loading.value = true;
  queryParams.value.spaceCode = userStore.spaceCode;
  listDppEtlTaskInstance(queryParams.value).then((response) => {
    const pageData = normalizePageData(response);
    dppEtlTaskLogList.value = pageData.rows;
    total.value = pageData.total;
    loading.value = false;
  });
}

const logDialogRef = ref(null);
const logDetailCatList = (row) => {
  logDialogRef.value.open(row.id);
};

/** 导出按钮操作 */
async function handleExport(row) {
  proxy.download(
    "/col/etlTaskInstance/downloadLog",
    {
      taskInstanceId: row.id,
    },
    `${row.name}.log`
  );
}

let deptOptions = ref([]);
/** 下拉树结构 */
function getDeptTree() {
  listAttDataDevCat({
    spaceId: userStore.spaceId,
    spaceCode: userStore.spaceCode,
    validFlag: true,
  }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "数据开发目录",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
const DeptTreeRef = ref(null); /** 重置按钮操作 */
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
function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  queryParams.value.pageNum = 1;
  handleQuery();
}
// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 排序触发事件 */
function handleTableChange(pagination, filters, sorter) {
  const field = sorter.column?.key || sorter.field;
  const orderMap = { ascend: 'asc', descend: 'desc' };
  queryParams.value.orderByColumn = field;
  queryParams.value.isAsc = sorter.order ? orderMap[sorter.order] : null;
  getList();
}

// 监听spaceCode数据变化
watch(
  () => userStore.spaceCode,
  (spaceCode) => {
    if (spaceCode) {
      getList();
    }
  },
  { immediate: true } // 立即触发，防止数据已存在的情况
);
getDeptTree();
getList();
</script>
<style scoped lang="scss">
::v-deep {
  .selectlist .ant-tag.ant-tag {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.app-container {
  // margin: 13px 15px;
}

.pagecont-bottom {
  min-height: auto;
}

.ant-layout-content {
  padding: 2px 0px;
  // box-shadow: 1px 1px 3px rgba(0, 0, 0, .2);
}

.ellipsis-container {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>

