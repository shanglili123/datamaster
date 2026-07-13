<template>
  <div class="ops-event">
    <el-table stripe height="500px" v-loading="loading" :data="list">
      <el-table-column width="180" label="事件类型" align="left" prop="eventType">
        <template #default="scope">
          <el-tag :type="scope.row.eventType === 'TASK_FAILED' ? 'danger' : 'warning'">
            {{ scope.row.eventType || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column width="110" label="执行类型" align="left" prop="taskType">
        <template #default="scope">
          {{ taskTypeLabel(scope.row.taskType) }}
        </template>
      </el-table-column>
      <el-table-column width="110" label="执行状态" align="left" prop="instanceStatus">
        <template #default="scope">
          <el-tag
            v-if="scope.row.instanceStatus"
            :type="taskInstanceStatusType(scope.row.instanceStatus)"
            size="small"
          >
            {{ taskInstanceStatusLabel(scope.row.instanceStatus) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column width="120" label="失败类型" align="left" prop="failureType">
        <template #default="scope">
          {{ scope.row.failureType || '-' }}
        </template>
      </el-table-column>
      <el-table-column width="90" label="风险等级" align="left" prop="riskLevel">
        <template #default="scope">
          <el-tag v-if="scope.row.riskLevel === 'HIGH'" type="danger" size="small">高</el-tag>
          <el-tag v-else-if="scope.row.riskLevel === 'MEDIUM'" type="warning" size="small">中</el-tag>
          <el-tag v-else-if="scope.row.riskLevel === 'LOW'" type="info" size="small">低</el-tag>
          <span v-else>{{ scope.row.riskLevel || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column width="110" label="执行动作" align="left" prop="action">
        <template #default="scope">
          {{ actionLabel(scope.row.action) }}
        </template>
      </el-table-column>
      <el-table-column width="90" label="动作状态" align="left" prop="actionStatus">
        <template #default="scope">
          <el-tag :type="scope.row.actionStatus === 'SUCCESS' ? 'success' : scope.row.actionStatus === 'FAILED' ? 'danger' : 'info'" size="small">
            {{ scope.row.actionStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="原因" align="left" prop="reason" min-width="200" show-overflow-tooltip />
      <el-table-column label="建议" align="left" prop="suggestion" min-width="200" show-overflow-tooltip />
      <el-table-column width="160" label="发生时间" align="left" prop="createTime">
        <template #default="scope">
          {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') || '-' }}
        </template>
      </el-table-column>
      <template #empty>
        <div class="emptyBg">
          <p>暂无运维事件</p>
        </div>
      </template>
    </el-table>
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

function taskInstanceStatusLabel(status) {
  return taskInstanceStatusMap[String(status)]?.label || status || "-";
}

function taskInstanceStatusType(status) {
  return taskInstanceStatusMap[String(status)]?.type || "info";
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
