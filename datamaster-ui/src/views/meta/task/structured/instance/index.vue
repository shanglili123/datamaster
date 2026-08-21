<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '70px' } }"
        v-show="showSearch" @submit.prevent>
        <a-form-item label="任务名称" name="name">
          <a-input v-model:value="queryParams.name" placeholder="请输入任务名称" allow-clear
            @keyup.enter="handleQuery" style="width: 150px;" />
        </a-form-item>
        <a-form-item label="采集模式" name="collectionMode">
          <a-select v-model:value="queryParams.collectionMode" placeholder="请选择采集模式" allow-clear
            style="width: 150px;">
            <a-select-option v-for="dict in toValue(dicts.mc_collect_mode)" :key="dict.value" :value="dict.value">
              {{ dict.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear
            style="width: 150px;">
            <a-select-option value="1">运行中</a-select-option>
            <a-select-option value="2">失败</a-select-option>
            <a-select-option value="9">成功</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="执行时间" name="createTimeRange">
          <a-range-picker v-model:value="createTimeRange" :show-time="true" valueFormat="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss" :placeholder="['开始时间', '结束时间']" style="width: 340px;" />
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
      <div class="top-right-btn">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
      </div>
    </div>
    <div>
      <a-table striped :loading="loading" :data-source="TaskInstanceList" :pagination="false"
        :columns="tableColumns" :scroll="{ x: 2000 }" @change="handleTableChange"
        :locale="{ emptyText: '暂无记录' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            {{ record.name || '-' }}
          </template>
          <template v-if="column.dataIndex === 'sourceSystemName'">
            {{ record.sourceSystemName || '-' }}
          </template>
          <template v-if="column.dataIndex === 'datasourceName'">
            {{ record.datasourceName || '-' }}
          </template>
          <template v-if="column.dataIndex === 'collectionMode'">
            <dict-tag :options="toValue(dicts.mc_collect_mode)" :value="record.collectionMode" />
          </template>
          <template v-if="column.dataIndex === 'collectionScope'">
            <dict-tag :options="toValue(dicts.mc_collect_scope)" :value="record.collectionScope" />
          </template>
          <template v-if="column.key === 'tableCount'">
            <span>总 {{ record.totalCount ?? '-' }}</span>
            <span class="ml5 success-text">成 {{ record.successCount ?? '-' }}</span>
            <span class="ml5 fail-text">败 {{ record.failCount ?? '-' }}</span>
          </template>
          <template v-if="column.key === 'dataCount'">
            <span>增 {{ record.addCount ?? '-' }}</span>
            <span class="ml5">删 {{ record.delCount ?? '-' }}</span>
            <span class="ml5">变 {{ record.updateCount ?? '-' }}</span>
          </template>
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="tagColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-if="column.dataIndex === 'startTime'">
            <span>{{ parseTime(record.startTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
          <template v-if="column.dataIndex === 'endTime'">
            <span>{{ parseTime(record.endTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
          <template v-if="column.dataIndex === 'duration'">
            {{ record.duration != null ? record.duration + 's' : '-' }}
          </template>
          <template v-if="column.dataIndex === 'createBy'">
            {{ record.createBy || '-' }}
          </template>
          <template v-if="column.key === 'actions'">
            <a-button type="link" size="small" v-hasPermi="['cat:taskInstance:info']"
              @click="handleDetail(record)">详情</a-button>
            <a-button type="link" size="small" v-hasPermi="['cat:taskInstance:log']"
              @click="handleLog(record)">日志</a-button>
          </template>
        </template>
      </a-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>

    <!-- 详情弹窗 -->
    <a-modal title="任务实例详情" v-model:open="detailDialog.open" :width="720" destroy-on-close>
      <a-spin :spinning="detailDialog.loading">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="编号">{{ detailDialog.form.id }}</a-descriptions-item>
        <a-descriptions-item label="任务名称">{{ detailDialog.form.name || '-' }}</a-descriptions-item>
        <a-descriptions-item label="来源系统">{{ detailDialog.form.sourceSystemName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="数据源">{{ detailDialog.form.datasourceName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="采集模式">
          <dict-tag :options="toValue(dicts.mc_collect_mode)" :value="detailDialog.form.collectionMode" />
        </a-descriptions-item>
        <a-descriptions-item label="采集范围">
          <dict-tag :options="toValue(dicts.mc_collect_scope)" :value="detailDialog.form.collectionScope" />
        </a-descriptions-item>
        <a-descriptions-item label="表数量">
          总 {{ detailDialog.form.totalCount ?? '-' }} / 成 {{ detailDialog.form.successCount ?? '-' }} /
          败 {{ detailDialog.form.failCount ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="数据量">
          增 {{ detailDialog.form.addCount ?? '-' }} / 删 {{ detailDialog.form.delCount ?? '-' }} /
          变 {{ detailDialog.form.updateCount ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="tagColor(detailDialog.form.status)">{{ statusLabel(detailDialog.form.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="耗时">
          {{ detailDialog.form.duration != null ? detailDialog.form.duration + 's' : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="开始时间">
          {{ parseTime(detailDialog.form.startTime, '{y}-{m}-{d} {h}:{i}') }}
        </a-descriptions-item>
        <a-descriptions-item label="结束时间">
          {{ parseTime(detailDialog.form.endTime, '{y}-{m}-{d} {h}:{i}') }}
        </a-descriptions-item>
        <a-descriptions-item label="创建人">{{ detailDialog.form.createBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ parseTime(detailDialog.form.createTime, '{y}-{m}-{d} {h}:{i}') }}
        </a-descriptions-item>
        <a-descriptions-item label="失败原因" :span="2">
          {{ detailDialog.form.failCause || '-' }}
        </a-descriptions-item>
      </a-descriptions>
      </a-spin>
    </a-modal>

    <!-- 日志弹窗 -->
    <a-modal title="任务实例日志" v-model:open="logDialog.open" :width="900" destroy-on-close>
      <div class="log-meta" v-if="logDialog.form.taskName">
        <a-tag>任务名称：{{ logDialog.form.taskName }}</a-tag>
        <a-tag color="success">开始时间：{{ parseTime(logDialog.form.startTime, '{y}-{m}-{d} {h}:{i}') }}</a-tag>
        <a-tag :color="tagColor(logDialog.form.status)">状态：{{ statusLabel(logDialog.form.status) }}</a-tag>
      </div>
      <a-spin :spinning="logDialog.loading">
      <pre class="log-content">{{ logDialog.content || '暂无日志' }}</pre>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup name="TaskInstanceLog">
import { listTaskInstance, getTaskInstance, getTaskInstanceLog } from "@/api/cat/task/taskInstance";
const { proxy } = getCurrentInstance();
import { ref, computed, toValue } from "vue";
const defaultSort = ref({ prop: 'createTime', order: 'descending' });
const dicts = proxy.useDict(
  'mc_collect_mode',
  'mc_collect_scope'
);
const TaskInstanceList = ref([]);
// 列显隐信息
const columns = ref([
  { key: 0, label: "编号", visible: true },
  { key: 1, label: "任务名称", visible: true },
  { key: 2, label: "来源系统", visible: true },
  { key: 3, label: "数据源", visible: true },
  { key: 4, label: "采集模式", visible: true },
  { key: 5, label: "采集范围", visible: true },
  { key: 6, label: "表数量", visible: true },
  { key: 7, label: "数据量", visible: true },
  { key: 8, label: "状态", visible: true },
  { key: 9, label: "开始时间", visible: true },
  { key: 10, label: "结束时间", visible: true },
  { key: 11, label: "耗时", visible: true },
  { key: 12, label: "创建人", visible: true },
  { key: 13, label: "操作", visible: true },
]);
const getColumnVisibility = (key) => {
  const column = columns.value.find(col => col.key == key);
  if (!column) return true;
  return column.visible;
};
const tableColumns = computed(() => {
  const allCols = [
    { title: '编号', dataIndex: 'id', align: 'center', width: 180, ellipsis: true },
    { title: '任务名称', dataIndex: 'name', align: 'center', width: 180, ellipsis: true },
    { title: '来源系统', dataIndex: 'sourceSystemName', align: 'center', width: 120, ellipsis: true },
    { title: '数据源', dataIndex: 'datasourceName', align: 'center', width: 140, ellipsis: true },
    { title: '采集模式', dataIndex: 'collectionMode', align: 'center', width: 100 },
    { title: '采集范围', dataIndex: 'collectionScope', align: 'center', width: 110, ellipsis: true },
    { title: '表数量', key: 'tableCount', align: 'center', width: 170 },
    { title: '数据量', key: 'dataCount', align: 'center', width: 170 },
    { title: '状态', dataIndex: 'status', align: 'center', width: 90 },
    { title: '开始时间', dataIndex: 'startTime', key: 'start_time', align: 'center', width: 160, ellipsis: true, sorter: true },
    { title: '结束时间', dataIndex: 'endTime', key: 'end_time', align: 'center', width: 160, ellipsis: true, sorter: true },
    { title: '耗时', dataIndex: 'duration', key: 'duration', align: 'center', width: 100, sorter: true },
    { title: '创建人', dataIndex: 'createBy', align: 'center', width: 120, ellipsis: true },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 140 },
  ];
  return allCols.filter((_, i) => getColumnVisibility(i));
});
const loading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const createTimeRange = ref([]);
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    collectionMode: null,
    status: null,
    createTimeStart: null,
    createTimeEnd: null,
    orderByColumn: 'create_time',
    isAsc: 'descending',
  },
});
const { queryParams } = toRefs(data);
const detailDialog = reactive({
  open: false,
  loading: false,
  form: {},
});
const logDialog = reactive({
  open: false,
  loading: false,
  content: '',
  form: {},
});

/** 状态标签 */
function statusLabel(status) {
  const map = { '1': '运行中', '2': '失败', '9': '成功' };
  return map[status] || '未知';
}
function statusType(status) {
  const map = { '1': 'warning', '2': 'danger', '9': 'success' };
  return map[status] || 'info';
}

/** a-tag 颜色映射 */
function tagColor(status) {
  const t = statusType(status);
  return t === 'danger' ? 'error' : t === 'info' ? 'default' : t;
}
/** 表格变更（排序） */
function handleTableChange(pagination, filters, sorter) {
  const orderMap = { ascend: 'ascending', descend: 'descending' };
  if (sorter && sorter.order) {
    queryParams.value.orderByColumn = sorter.column?.key || sorter.field;
    queryParams.value.isAsc = orderMap[sorter.order];
  } else {
    queryParams.value.orderByColumn = 'create_time';
    queryParams.value.isAsc = 'descending';
  }
  queryParams.value.pageNum = 1;
  getList();
}

/** 查询采集任务实例列表 */
function getList() {
  loading.value = true;
  listTaskInstance(queryParams.value).then(response => {
    const page = response.data || {};
    TaskInstanceList.value = page.rows || [];
    total.value = Number(page.total || TaskInstanceList.value.length || 0);
    loading.value = false;
  });
}
/** 搜索按钮操作 */
function handleQuery() {
  if (createTimeRange.value && createTimeRange.value.length === 2) {
    queryParams.value.createTimeStart = createTimeRange.value[0];
    queryParams.value.createTimeEnd = createTimeRange.value[1];
  } else {
    queryParams.value.createTimeStart = null;
    queryParams.value.createTimeEnd = null;
  }
  queryParams.value.pageNum = 1;
  getList();
}
/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  createTimeRange.value = [];
  handleQuery();
}

/** 详情按钮操作 */
function handleDetail(row) {
  detailDialog.open = true;
  detailDialog.loading = true;
  detailDialog.form = {};
  getTaskInstance(row.id).then(response => {
    detailDialog.form = response.data || row;
    detailDialog.loading = false;
  });
}

/** 日志按钮操作 */
function handleLog(row) {
  logDialog.open = true;
  logDialog.loading = true;
  logDialog.content = '';
  logDialog.form = row;
  getTaskInstanceLog(row.id).then(response => {
    logDialog.content = response.data || '';
    logDialog.loading = false;
  });
}

getList();
</script>

<style scoped lang="scss">
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

  .top-right-btn {
    flex-shrink: 0;
  }
}

.success-text {
  color: #67c23a;
}

.fail-text {
  color: #f56c6c;
}

.log-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.log-content {
  margin: 0;
  padding: 12px;
  max-height: 60vh;
  overflow: auto;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.8;
}
</style>
