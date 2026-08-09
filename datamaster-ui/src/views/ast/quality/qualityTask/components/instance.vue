<template>
  <!-- 列表的 执行记录 -->
  <a-modal v-model:open="visibleDialog" class="dialog" :title="title" style="width: 1200px" :destroy-on-close="true">
    <a-spin :spinning="loading">
      <a-table
        :data-source="jobLogList"
        :columns="tableColumns"
        :pagination="false"
        striped
        :scroll="{ y: 380 }"
        row-key="id"
        :locale="{ emptyText: emptyContent }"
        @change="handleSortChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            {{ record.name || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'score'">
            {{ record.score || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'successFlag'">
            <dict-tag :options="quality_log_success_flag" :value="record.successFlag" />
          </template>
          <template v-else-if="column.dataIndex === 'problemData'">
            {{ record.problemData || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'startTime'">
            <span>{{ parseTime(record.startTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'endTime'">
            <span>{{ parseTime(record.endTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="logDetailCatList(record)" v-hasPermi="['ast:qualityTask:query']">查看</a-button>
            <a-button type="link" size="small" @click="routeTo('/ast/quality/probeTaskInstance/detail', { ...record, info: true })">详情</a-button>
          </template>
          <template v-else>
            <span>{{ record[column.dataIndex] || '-' }}</span>
          </template>
        </template>
      </a-table>
    </a-spin>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList"
/>
    <!-- <template #footer>
            <div style="text-align: right">
        <a-button @click="closeDialog">关闭</a-button>
        <a-button type="primary" @click="saveData">保存</a-button>
        </div>
</template> -->
  </a-modal>
  <!-- 探查任务实例执行明细 -->
  <a-modal title="查看实例明细" v-model:open="open" width="800px" :destroy-on-close="true">
    <div v-html="formattedText"></div>
    <!-- <template #footer>
            <div class="dialog-footer">
                <a-button @click="open = false">关 闭</a-button>
            </div>
        </template> -->
  </a-modal>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { defineProps, defineEmits, ref, computed, watch } from 'vue';
import { h } from 'vue';

const tableColumns = [
  { title: '编号', dataIndex: 'id', align: 'center', width: 120 },
  { title: '任务名称', dataIndex: 'name', align: 'center' },
  { title: '质量评分', dataIndex: 'score', align: 'center', width: 80 },
  { title: '执行状态', dataIndex: 'successFlag', align: 'center' },
  { title: '问题数据', dataIndex: 'problemData', align: 'center' },
  { title: '开始时间', dataIndex: 'startTime', align: 'center', width: 160, sorter: true },
  { title: '结束时间', dataIndex: 'endTime', align: 'center', width: 160, sorter: true },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const { proxy } = getCurrentInstance();
const defaultSort = ref({ prop: 'startTime', order: 'descending' });

import { useRoute, useRouter } from "vue-router"
const { quality_log_success_flag } = proxy.useDict(
  'quality_log_success_flag'
);

import { listProbeTaskInstance } from "@/api/ast/quality/probeTaskInstance";

import {
  probeTaskInstanceLogDetail
} from "@/api/ast/quality/qualityTask";;
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: '表单标题' },
  data: { type: Object, default: () => ({}) }
});
const open = ref(false);
let form = ref();
let queryParams = ref({
  pageNum: 1,
  pageSize: 6,
  nodeId: undefined,
  taskId: undefined,
  orderByColumn: 'start_time',
  isAsc: 'descending'
});
const formattedText = computed(() => {
  return form.value.logContent.replace(/\n/g, '<br>');
});
const router = useRouter();

/** 排序触发事件 */
function handleSortChange(pag, filters, sorter) {
  const prop = sorter.field || sorter.column?.dataIndex;
  const order = sorter.order === 'ascend' ? 'ascending' : sorter.order === 'descend' ? 'descending' : null;
  queryParams.value.orderByColumn = prop === 'startTime' ? 'start_time' : prop === 'endTime' ? 'end_time' : prop;
  queryParams.value.isAsc = order;
  queryParams.value.pageNum = 1;
  getList();
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
          score: row.score
        },
      });
    }
  }
}

async function logDetailCatList(row) {
  try {
    if (!row.path) {
      proxy.$message.warning('操作失败，未查询到实例明细');
      return;
    }
    form.value = {};
    const response = await probeTaskInstanceLogDetail({ handleMsg: row.path });
    if (response && response.content) {
      form.value = response.content;
      open.value = true;
    }
  } catch (error) { }
}

const total = ref(0);
const dateRange = ref([]);
let jobLogList = ref([]);
let loading = ref(false);
/** 查询探查任务实例列表 */
function getList() {
  loading.value = true;
  queryParams.value.qualityId = props.data.id;
  listProbeTaskInstance({
    ...queryParams.value
  }).then((response) => {
    const page = response.data || {};
    jobLogList.value = page.rows || [];
    total.value = Number(page.total || jobLogList.value.length || 0);
    loading.value = false;
  });
}
const emit = defineEmits(['update:visible', 'confirm']);

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      getList();
    } else {
      jobLogList.value = [];
    }
  }
);

// 计算属性处理 v-model
const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit('update:visible', newValue);
  }
});

// 关闭对话框的方法
const closeDialog = () => {
  emit('update:visible', false);
};

// 保存数据的方法
const saveData = () => {
  emit('confirm', localNode.value); // 向父组件提交本地数据
  emit('update:visible', false);
};
</script>

