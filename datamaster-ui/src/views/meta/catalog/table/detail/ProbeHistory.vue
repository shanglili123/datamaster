<template>
  <div class="probe-history">
    <div class="probe-history-tip" v-if="records.length">
      共 {{ records.length }} 次质量探查任务记录，点击「查看报告」可查看单次探查详情
    </div>
    <a-table
      :loading="loading"
      :data-source="records"
      :pagination="false"
      :columns="columns"
      :locale="{ emptyText: '暂无质量报告，请先执行带质量规则的探查任务' }"
      row-key="id"
      size="middle"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'name'">
          {{ record.name || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'score'">
          <a-tag :color="record.score == null ? 'default' : (record.score >= 90 ? 'green' : (record.score >= 60 ? 'orange' : 'red'))">
            {{ record.score == null ? "-" : record.score }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'problemData'">
          {{ record.problemData || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'successFlag'">
          <dict-tag :options="quality_log_success_flag" :value="record.successFlag" />
        </template>
        <template v-else-if="column.dataIndex === 'startTime'">
          {{ parseTime(record.startTime, "{y}-{m}-{d} {h}:{i}") }}
        </template>
        <template v-else-if="column.dataIndex === 'endTime'">
          {{ parseTime(record.endTime, "{y}-{m}-{d} {h}:{i}") }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button type="link" size="small" @click="handleDetail(record)">
            查看报告
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup name="ProbeHistory">
import { ref, computed, watch, getCurrentInstance } from "vue";
import { useRouter } from "vue-router";
import { listProbeHistoryByTable } from "@/api/ast/quality/probeTaskInstance";

const props = defineProps({
  detail: {
    type: Object,
    default: () => ({}),
  },
});

const { proxy } = getCurrentInstance();
const { quality_log_success_flag } = proxy.useDict("quality_log_success_flag");

const router = useRouter();
const loading = ref(false);
const records = ref([]);

const columns = computed(() => [
  { title: "任务名称", dataIndex: "name", align: "left", ellipsis: true, minWidth: 200 },
  { title: "质量评分", dataIndex: "score", align: "center", width: 120 },
  { title: "问题数据", dataIndex: "problemData", align: "center", width: 120 },
  { title: "执行状态", dataIndex: "successFlag", align: "center", width: 110 },
  { title: "开始时间", dataIndex: "startTime", align: "center", width: 170 },
  { title: "结束时间", dataIndex: "endTime", align: "center", width: 170 },
  { title: "操作", key: "actions", align: "center", fixed: "right", width: 120 },
]);

// 加载该表的全部探查历史
function loadHistory() {
  const { datasourceId, tableName } = props.detail || {};
  if (!datasourceId || !tableName) {
    records.value = [];
    return;
  }
  loading.value = true;
  listProbeHistoryByTable({ datasourceId, tableName })
    .then((res) => {
      records.value = res.data || [];
    })
    .catch((err) => {
      console.error("探查历史加载失败", err);
      records.value = [];
    })
    .finally(() => {
      loading.value = false;
    });
}

// 查看单次探查报告
function handleDetail(record) {
  if (!record?.id) return;
  router.push({
    path: "/ast/quality/probeTaskInstance/detail",
    query: {
      id: record.id,
      score: record.score,
    },
  });
}

watch(
  () => props.detail,
  () => loadHistory(),
  { immediate: true }
);
</script>

<style scoped lang="scss">
.probe-history {
  padding: 4px 0;

  .probe-history-tip {
    margin-bottom: 12px;
    color: #8a97a8;
    font-size: 13px;
  }
}
</style>
