<template>
  <a-modal v-model:open="visibleDialog" class="medium-dialog" :title="title" :destroy-on-close="true" @close="clearData">
    <div>
      <!-- 导出按钮 -->
      <a-button :disabled="!callData.dataTotal > 0" type="warning" ghost @click="downloadMethodNotification"
        :loading="downloadLoading">导出</a-button>

      <a-table :data-source="callData.dataList" striped bordered :loading="loading"
        :pagination="false" :scroll="{ y: 540 }" :columns="tableColumns"
        style="width: 100%; margin: 15px 0"
        :locale="{ emptyText: '暂无记录' }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            <span>{{
              (callData.pageNum - 1) * callData.pageSize + index + 1
            }}</span>
          </template>
          <template v-else>
            {{ formatCellValue(record[column.dataIndex]) }}
          </template>
        </template>
      </a-table>
      <pagination v-show="callData.dataTotal > 0" :total="callData.dataTotal" v-model:page="callData.pageNum"
        v-model:limit="callData.pageSize" @pagination="handleQuery" />
    </div>

    <template #footer>
      <div style="text-align: right">
        <a-button @click="closeDialog">关闭</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { ref, computed, watch, getCurrentInstance } from "vue";
import { encrypt } from "@/utils/aesEncrypt";
import { executeSqlQuery } from "@/api/ast/dataSource/dataSource";
import { Modal, notification } from "ant-design-vue";

const { proxy } = getCurrentInstance();
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  queryParams: { type: Object, default: () => ({}) },
  spl: { type: String, default: "" },
});

let loading = ref(false);
let downloadLoading = ref(false);
const callData = ref({
  dataList: [],
  columnList: [],
  pageNum: 1,
  pageSize: 6, // 查询每页默认6条
  dataTotal: 0,
});
const tableColumns = computed(() => {
  const cols = [];
  if (callData.value.dataTotal > 0) {
    cols.push({ title: '序号', key: 'index', width: 80, align: 'center' });
  }
  if (callData.value.columnList && callData.value.columnList.length > 0) {
    callData.value.columnList.forEach(col => {
      cols.push({ title: col, dataIndex: col, align: 'center', width: 180, ellipsis: true });
    });
  }
  return cols;
});

const emit = defineEmits(["update:visible", "confirm"]);

const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update:visible", newValue);
  },
});

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      const sqlText = encrypt(props.spl);
      callData.value = {
        ...props.queryParams,
        sqlText,
      };
      handleQuery();
    }
  }
);

async function handleQuery() {
  loading.value = true;
  try {
    const response = await executeSqlQuery(callData.value);
    const { data } = response;
    const dataList = Array.isArray(data.data) ? data.data.map(removeInternalColumns) : [];
    const columnList = dataList.length > 0 ? Object.keys(dataList[0]) : [];
    callData.value.dataList = dataList;
    callData.value.columnList = columnList;
    callData.value.dataTotal = data.total || 0;
  } catch (error) {
    throw error; // 👈 抛出错误给调用方处理
  } finally {
    loading.value = false;
  }
}

function removeInternalColumns(row) {
  if (!row || typeof row !== "object") {
    return row;
  }
  const result = { ...row };
  delete result.ROW_ID;
  delete result.__row_number__;
  return result;
}

function formatCellValue(value) {
  if (value === null || value === undefined) {
    return "";
  }
  if (typeof value === "object") {
    return JSON.stringify(value);
  }
  return value;
}

const closeDialog = () => {
  callData.value = {
    dataList: [],
    columnList: [],
    dataTotal: 0,
    pageNum: 1,
    pageSize: 6,
  };
  emit("update:visible", false);
};

const clearData = () => {
  callData.value.dataList = [];
  callData.value.columnList = [];
  callData.value.dataTotal = 0;
};

// 计算总文件数（导出用）
const totalFiles = computed(() => Math.ceil(callData.value.dataTotal / 5000));

// 导出逻辑（不影响当前分页）
const downloadMethod = () => {
  const pageSize = 5000;
  const total = callData.value.dataTotal;
  let pageNum = 1;

  downloadLoading.value = true;

  if (total === 0) {
    notification.info({
      message: "提示",
      description: "该表没有数据",
      duration: 2,
    });
    downloadLoading.value = false;
    return;
  }

  const exportParams = {
    ...callData.value,
    pageSize,
    exportType: 0,
  };

  while ((pageNum - 1) * pageSize < total) {
    exportParams.pageNum = pageNum;
    proxy.download2(
      "/ast/dataSource/exportSqlQueryResult/export",
      exportParams,
      `${new Date().getTime()}_${pageNum}.xlsx`
    );
    pageNum++;
  }

  downloadLoading.value = false;
};

// 导出确认提示
const downloadMethodNotification = () => {
  const totalFilesCount = totalFiles.value;

  Modal.confirm({
    title: "提示",
    content: `是否导出总数为：${callData.value.dataTotal}，以每5000数据一份文件进行导出，总共导出 ${totalFilesCount} 份？`,
    okText: "确定",
    cancelText: "取消",
    onOk: () => {
      downloadMethod();
    },
  });
};
</script>

<style lang="scss" scoped>
.column-header {
  display: flex;
  flex-direction: column;
}

.column-item {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>

