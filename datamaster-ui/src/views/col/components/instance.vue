<template>
  <a-modal v-model:open="visibleDialog" class="medium-dialog" :title="title" destroyOnClose>
    <a-table
      :loading="loading"
      :data-source="jobLogList"
      :columns="columns"
      :scroll="{ y: 580 }"
      :pagination="false"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'taskInstanceName'">
          {{ record.name || "-" }}
        </template>
        <template v-else-if="column.key === 'commandType'">
          <dict-tag
            v-if="record.commandType !== null && record.commandType !== undefined && record.commandType !== ''"
            :options="dpp_etl_task_instance_command_type"
            :value="String(record.commandType).trim()"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <dict-tag
            v-if="record.status !== null && record.status !== undefined && record.status !== ''"
            :options="dpp_etl_node_instance"
            :value="String(record.status).trim()"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'start_time'">
          <span>{{ parseTime(record.startTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
        </template>
        <template v-else-if="column.key === 'end_time'">
          <span>{{ parseTime(record.endTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
        </template>
        <template v-else-if="column.key === 'extractionCount'">
          {{ "-" }}
        </template>
        <template v-else-if="column.key === 'writeCount'">
          {{ "-" }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-button type="link" :icon="h(EyeOutlined)" @click="logDetailCatList(record)">查看日志</a-button>
          <a-button
            type="link"
            style="color: #faad14"
            :icon="h(DownloadOutlined)"
            @click="handleExport(record)"
            @mousedown="(e) => e.preventDefault()"
          >
            下载日志
          </a-button>
        </template>
      </template>
      <template #emptyText>
        <div class="emptyBg">
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
    <template #footer>
      <div style="text-align: right">
        <a-button @click="visibleDialog = false">关闭</a-button>
      </div>
    </template>
  </a-modal>
  <TaskLogDialog ref="logDialogRef" />
</template>

<script setup>
import { computed, getCurrentInstance, h, ref, watch } from "vue";
import { listDppEtlTaskInstance } from "@/api/col/instance/job";
import TaskLogDialog from "@/views/col/components/taskLog.vue";
import { EyeOutlined, DownloadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { dpp_etl_node_instance } = proxy.useDict("dpp_etl_node_instance");
const { dpp_etl_task_instance_command_type } = proxy.useDict("dpp_etl_task_instance_command_type");

const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  taskType: { type: Number, default: 1 },
  data: { type: Object, default: () => ({}) },
});

const emit = defineEmits(["update:visible", "confirm"]);
const logDialogRef = ref(null);
const total = ref(0);
const jobLogList = ref([]);
const loading = ref(false);
const queryParams = ref({
  pageNum: 1,
  pageSize: 6,
  nodeId: undefined,
  taskId: undefined,
  orderByColumn: "start_time",
  isAsc: "descending",
});

const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update:visible", newValue);
  },
});

const columns = [
  { title: "编号", dataIndex: "id", key: "id", width: 225, align: "left" },
  {
    title: "任务名称",
    dataIndex: "taskInstanceName",
    key: "taskInstanceName",
    width: 300,
    align: "left",
    ellipsis: true,
  },
  { title: "执行类型", dataIndex: "commandType", key: "commandType", width: 100, align: "left" },
  { title: "执行状态", dataIndex: "status", key: "status", width: 100, align: "left" },
  {
    title: "开始时间",
    dataIndex: "startTime",
    key: "start_time",
    width: 160,
    align: "left",
    ellipsis: true,
    sorter: true,
    sorterKey: "start_time",
    defaultSortOrder: "descend",
  },
  {
    title: "结束时间",
    dataIndex: "endTime",
    key: "end_time",
    width: 160,
    align: "left",
    ellipsis: true,
    sorter: true,
    sorterKey: "end_time",
  },
  { title: "抽取量", dataIndex: "extractionCount", key: "extractionCount", width: 80, align: "left" },
  { title: "写入量", dataIndex: "writeCount", key: "writeCount", width: 80, align: "left" },
  { title: "创建人", dataIndex: "createBy", key: "createBy", width: 80, align: "left" },
  {
    title: "操作",
    key: "operation",
    width: 200,
    align: "center",
    className: "small-padding fixed-width",
    fixed: "right",
  },
];

function handleTableChange(pagination, filters, sorter) {
  if (!sorter || Array.isArray(sorter)) {
    return;
  }
  handleSortChange({
    column: { columnKey: sorter.key },
    prop: sorter.field,
    order: sorter.order === "ascend" ? "ascending" : sorter.order === "descend" ? "descending" : sorter.order,
  });
}

function handleSortChange({ column, prop, order }) {
  queryParams.value.orderByColumn = column?.columnKey || prop;
  queryParams.value.isAsc = order;
  queryParams.value.pageNum = 1;
  getList();
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

function logDetailCatList(row) {
  logDialogRef.value?.open(row.id);
}

function getList() {
  loading.value = true;
  queryParams.value.taskId = props.data.id;
  queryParams.value.taskType = props.taskType;
  listDppEtlTaskInstance({ ...queryParams.value })
    .then((response) => {
      const page = response.data || {};
      jobLogList.value = page.rows || [];
      total.value = Number(page.total || jobLogList.value.length || 0);
    })
    .finally(() => {
      loading.value = false;
    });
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      queryParams.value.pageNum = 1;
      getList();
    } else {
      jobLogList.value = [];
    }
  }
);
</script>
