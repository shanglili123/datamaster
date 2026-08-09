<template>
  <div class="ops-event">
    <a-table height="500px" :loading="loading" :data-source="list" :columns="columns">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'eventType'">
          <a-tag :type="record.eventType === 'TASK_FAILED' ? 'error' : 'warning'">
            {{ record.eventType || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'taskType'">
          {{ taskTypeLabel(record.taskType) }}
        </template>
        <template v-else-if="column.key === 'instanceStatus'">
          <a-tag
            v-if="record.instanceStatus"
            :type="taskInstanceStatusType(record.instanceStatus)"
          >
            {{ taskInstanceStatusLabel(record.instanceStatus) }}
          </a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'failureType'">
          {{ record.failureType || '-' }}
        </template>
        <template v-else-if="column.key === 'riskLevel'">
          <a-tag v-if="record.riskLevel === 'HIGH'" type="error">高</a-tag>
          <a-tag v-else-if="record.riskLevel === 'MEDIUM'" type="warning">中</a-tag>
          <a-tag v-else-if="record.riskLevel === 'LOW'">低</a-tag>
          <span v-else>{{ record.riskLevel || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          {{ actionLabel(record.action) }}
        </template>
        <template v-else-if="column.key === 'actionStatus'">
          <a-tag :type="record.actionStatus === 'SUCCESS' ? 'success' : record.actionStatus === 'FAILED' ? 'error' : 'default'">
            {{ record.actionStatus || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') || '-' }}
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <p>暂无运维事件</p>
        </div>
      </template>
    </a-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="OpsEvent">
import { ref, reactive, watch } from "vue";
import { listTaskOpsEvents } from "@/api/col/taskOps.js";

const props = defineProps({
  taskId: { type: [Number, String], default: null },
});

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const queryParams = reactive({
  pageNum: 1,
  pageSize: 6,
});

const columns = [
  { title: '事件类型', dataIndex: 'eventType', key: 'eventType', width: 180, align: 'left' },
  { title: '执行类型', dataIndex: 'taskType', key: 'taskType', width: 110, align: 'left' },
  { title: '执行状态', dataIndex: 'instanceStatus', key: 'instanceStatus', width: 110, align: 'left' },
  { title: '失败类型', dataIndex: 'failureType', key: 'failureType', width: 120, align: 'left' },
  { title: '风险等级', dataIndex: 'riskLevel', key: 'riskLevel', width: 90, align: 'left' },
  { title: '执行动作', dataIndex: 'action', key: 'action', width: 110, align: 'left' },
  { title: '动作状态', dataIndex: 'actionStatus', key: 'actionStatus', width: 90, align: 'left' },
  { title: '原因', dataIndex: 'reason', key: 'reason', minWidth: 200, align: 'left', ellipsis: true },
  { title: '建议', dataIndex: 'suggestion', key: 'suggestion', minWidth: 200, align: 'left', ellipsis: true },
  { title: '发生时间', dataIndex: 'createTime', key: 'createTime', width: 160, align: 'left' },
];

function actionLabel(action) {
  const map = {
    UNLOAD_TASK: "下线任务",
    RECOVER_FROM_FAILURE: "从失败恢复",
    SUGGEST_RECOVER: "建议恢复",
    WAIT_MANUAL_RECOVER: "等待人工恢复",
  };
  return map[action] || action || "-";
}

function taskTypeLabel(taskType) {
  const map = {
    1: "数据集成",
    3: "数据开发",
    4: "作业任务",
  };
  return map[String(taskType)] || taskType || "-";
}

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

function taskInstanceStatusLabel(status) {
  return taskInstanceStatusMap[String(status)]?.label || status || "-";
}

function taskInstanceStatusType(status) {
  return taskInstanceStatusMap[String(status)]?.type || "default";
}

async function getList() {
  if (!props.taskId) return;
  loading.value = true;
  try {
    const res = await listTaskOpsEvents({
      taskId: props.taskId,
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
    });
    if (res?.data) {
      list.value = res.data.rows || res.data.list || res.data.records || [];
      total.value = res.data.total || 0;
    } else {
      list.value = [];
      total.value = 0;
    }
  } catch (e) {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

watch(() => props.taskId, (val) => {
  if (val) {
    queryParams.pageNum = 1;
    getList();
  }
}, { immediate: true });
</script>

<style lang="less" scoped>
.ops-event {
  padding: 16px;
}
</style>
