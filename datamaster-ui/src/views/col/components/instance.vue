<template>
  <el-dialog v-model="visibleDialog" draggable class="medium-dialog" :title="title" destroy-on-close>
    <el-table
      stripe
      height="580px"
      v-loading="loading"
      :data="jobLogList"
      :default-sort="defaultSort"
      @sort-change="handleSortChange"
    >
      <el-table-column width="225" label="编号" align="left" prop="id" />
      <el-table-column
        :show-overflow-tooltip="{ effect: 'light' }"
        label="任务名称"
        align="left"
        width="300"
        prop="taskInstanceName"
      >
        <template #default="scope">
          {{ scope.row.name || "-" }}
        </template>
      </el-table-column>
      <el-table-column width="100" label="执行类型" align="left" prop="commandType">
        <template #default="scope">
          <dict-tag
            v-if="scope.row.commandType !== null && scope.row.commandType !== undefined && scope.row.commandType !== ''"
            :options="dpp_etl_task_instance_command_type"
            :value="String(scope.row.commandType).trim()"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column width="100" label="执行状态" align="left" prop="status">
        <template #default="scope">
          <dict-tag
            v-if="scope.row.status !== null && scope.row.status !== undefined && scope.row.status !== ''"
            :options="dpp_etl_node_instance"
            :value="String(scope.row.status).trim()"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        width="160"
        label="开始时间"
        align="left"
        prop="startTime"
        sortable="custom"
        column-key="start_time"
        :sort-orders="['descending', 'ascending']"
        :show-overflow-tooltip="{ effect: 'light' }"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column
        width="160"
        label="结束时间"
        align="left"
        prop="endTime"
        sortable="custom"
        column-key="end_time"
        :sort-orders="['descending', 'ascending']"
        :show-overflow-tooltip="{ effect: 'light' }"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="抽取量" align="left" prop="extractionCount" width="80">
        <template #default>
          {{ "-" }}
        </template>
      </el-table-column>
      <el-table-column label="写入量" align="left" prop="writeCount" width="80">
        <template #default>
          {{ "-" }}
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="left" prop="personChargeName" width="80">
        <template #default="scope">
          {{ scope.row.personChargeName || "-" }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="200">
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
    <template #footer>
      <div style="text-align: right">
        <el-button @click="visibleDialog = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
  <TaskLogDialog ref="logDialogRef" />
</template>

<script setup>
import { computed, getCurrentInstance, ref, watch } from "vue";
import { listDppEtlTaskInstance } from "@/api/col/instance/job";
import TaskLogDialog from "@/views/col/components/taskLog.vue";

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
const defaultSort = ref({ prop: "startTime", order: "descending" });
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
