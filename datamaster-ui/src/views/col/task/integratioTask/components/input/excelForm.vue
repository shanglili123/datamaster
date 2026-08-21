<template>
  <a-modal v-model:open="visibleDialog" :draggable="true" class="medium-dialog" :title="currentNode?.data?.name"
    :closable="false" :destroy-on-close="true" :mask-closable="false" :width="1200">
    <a-spin :spinning="loading">
    <a-form ref="dpModelRefs" :model="form" :label-col="{ style: { width: '110px' } }" @submit.prevent
      :disabled="info">
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="节点名称" name="name" :rules="[
            { required: true, message: '请输入节点名称', trigger: 'change' },
          ]">
            <a-input v-model:value="form.name" placeholder="请输入节点名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="类型" name="typeName">
            <a-select v-model:value="form.taskParams.typeName" placeholder="请输入类型" show-search disabled>
              <a-select-option v-for="dict in typeList" :key="dict.value" :label="dict.label" :value="dict.value">{{ dict.label }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-textarea v-model:value="form.description" placeholder="请输入描述" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="上传附件" name="taskParams.excelFile" :rules="[
            { required: true, message: '请上传附件', trigger: 'change' },
          ]">
            <!-- <FileUploadbtn :limit="1" v-model="form.taskParams.excelFile" :dragFlag="false" :file-type="['xlsx', 'xls']"
              :fileSize="50" @handleRemove="handleRemove" /> -->
            <FileUploadbtn :limit="1" v-model="form.taskParams.excelFile" :dragFlag="false" :fileSize="50"
              @handleRemove="handleRemove" :file-type="['xlsx', 'xls']" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="起始行" name="taskParams.startData" :rules="[
            { required: true, message: '请输入起始行', trigger: 'change' },
          ]">
            <a-input-number :step="1" placeholder="请输入起始行" v-model:value="form.taskParams.startData"
              style="width: 100%" :min="1" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="起始列" name="taskParams.startColumn" :rules="[
            { required: true, message: '请输入起始列', trigger: 'change' },
          ]">
            <a-input-number :step="1" placeholder="请输入起始列" v-model:value="form.taskParams.startColumn"
              style="width: 100%" :min="1" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-button type="primary" @click="parseExcel" style="margin-left: 60px" :disabled="isButtonDisabled">
            解析Excel
          </a-button>
        </a-col>
      </a-row>
      <a-divider orientation="center">
        <span class="blue-text">属性字段</span>
      </a-divider>
      <a-table striped :loading="loadingList" :data-source="ColumnByAssettab" :columns="tableColumns"
        :pagination="false" :scroll="{ y: 310 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'index'">
            <span>{{ index + 1 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'columnName'">
            {{ record.columnName || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'columnType'">
            {{ record.columnType || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'format'">
            {{ record.format || "-" }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openDialog(record)">修改</a-button>
          </template>
        </template>
      </a-table>
    </a-form>
    </a-spin>
    <template #footer>
      <div style="text-align: right">
        <a-button @click="closeDialog">关闭</a-button>
        <a-button type="primary" @click="saveData" v-if="!info">保存</a-button>
      </div>
    </template>
  </a-modal>
  <excelUploadDialog :visible="open" title="属性字段编辑" @update:visible="open = $event" @confirm="handletaskConfig"
    :data="row" />
</template>
<script setup>
import { message } from 'ant-design-vue'
import { getToken } from "@/utils/auth.js";

import { typeList } from "@/utils/graph.js";

import { getLocalNodeUniqueKey as getNodeUniqueKey, getExcelColumn } from "@/api/col/task/index.js";

import excelUploadDialog from "../excelUpload.vue";

import FileUploadbtn from '@/components/FileUploadbtn/index1.vue'
const { proxy } = getCurrentInstance();

const tableColumns = [
    { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
    { title: '字段名称', dataIndex: 'columnName', align: 'left', ellipsis: true },
    { title: '字段类型', dataIndex: 'columnType', align: 'left' },
    { title: '日期格式', dataIndex: 'format', align: 'left' },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

import useUserStore from "@/store/system/user.js";
const userStore = useUserStore();
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  currentNode: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
  graph: {}
});
const emit = defineEmits(["update", "confirm"]);
const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update", newValue);
  },
});
// 变量定义
let loading = ref(false);
let loadingList = ref(false);
let TablesByDataSource = ref([]);
let ColumnByAssettab = ref();
// 修改
const open = ref(false);
let row = ref({});
const openDialog = (obj) => {
  row.value = obj;
  open.value = true;
};
// 属性字段修改新增
const handletaskConfig = (form) => {
  ColumnByAssettab.value = ColumnByAssettab.value.map((column) => {
    if (column.id == form.id) {
      return { ...column, ...form };
    }
    return column;
  });
};

let dpModelRefs = ref();
let form = ref({});
const tableFields = ref([]); // 来源表格
// 计算属性：判断按钮是否禁用
const isButtonDisabled = computed(() => {
  console.log(form.value.taskParams.excelFile);
  return (
    !form.value.taskParams.startData ||
    !form.value.taskParams.startColumn ||
    !form.value.taskParams.excelFile
  );
});
// 获取列数据
const parseExcel = async (id) => {
  if (!form.value.taskParams.startData) {
    message.warning("解析失败，请添加起始行");
    return;
  }
  if (!form.value.taskParams.startColumn) {
    message.warning("解析失败，请添加起始列");
    return;
  }
  if (!form.value.taskParams.excelFile) {
    message.warning("解析失败，请添加附件");
    return;
  }
  loadingList.value = true;
  try {
    let res = await getExcelColumn({
      startData: form.value.taskParams.startData,
      startColumn: form.value.taskParams.startColumn,
      excelFile: form.value.taskParams.excelFile,
    });

    if (res?.data?.csvFile) {
      form.value.taskParams.csvFile = res.data.csvFile;
      ColumnByAssettab.value = res.data.columnList.map((item, index) => ({
        id: index,
        columnName: item,
        columnType: "string",
      }));

      message.success("Excel解析成功，请确认属性字段类型！");
    } else {
      message.warning("Excel解析失败，未获取到有效数据！");
    }
  } catch (error) {
    if (response.code == 200)
      message.warning("Excel解析失败，请检查文件格式或内容！");
  } finally {
    loadingList.value = false;
  }
};

const off = () => {
  proxy.resetForm("dpModelRefs");
  // 清空表格字段数据
  ColumnByAssettab.value = [];
  TablesByDataSource.value = [];
  tableFields.value = [];
};
// 保存数据
const saveData = async () => {
  try {
    // 手动检查关键必填字段
    if (!form.value?.taskParams?.readerDatasource?.datasourceId) {
      return proxy.$message.warning('请选择源数据库连接');
    }
    if (!form.value?.taskParams?.asset_id) {
      return proxy.$message.warning('请选择表');
    }
    // 等待 DOM 更新
    await nextTick();
    // 保留原有的其它校验逻辑
    if (
      form.value?.taskParams.type == "1" &&
      (!ColumnByAssettab.value || ColumnByAssettab.value.length == 0)
    ) {
      return proxy.$message.warning("校验未通过，请选择属性字段");
    }
    // 如果没有 code，就调用接口获取唯一的 code
    if (!form.value.code) {
      loading.value = true;
      const response = await getNodeUniqueKey({
        spaceCode: userStore.spaceCode || "133545087166112",
        spaceId: userStore.spaceId,
      });
      loading.value = false; // 结束加载状态
      form.value.code = response.data; // 设置唯一的 code
    }
    const taskParams = form.value?.taskParams;
    taskParams.tableFields = ColumnByAssettab.value;
    taskParams.columnsList = ColumnByAssettab.value.map(({ columnName, columnType }) => ({
      colName: columnName,
      dataType: columnType,
    }));
    taskParams.columns = taskParams.tableFields.map((item) => {
      return {
        index: item.id,
        columnName: item.columnName,
        type: item.columnType,
        format: item.format
      };
    });
    emit("confirm", form.value);

  } finally {
    loadingList.value = false;
  }
};
const closeDialog = () => {
  off();
  // 关闭对话框
  emit("update", false);
};

// 监听属性变化
function deepCopy(data) {
  if (data === undefined || data === null) {
    return {}; // 或者返回一个默认值
  }
  try {
    return JSON.parse(JSON.stringify(data));
  } catch (e) {
    return {}; // 或者返回一个默认值
  }
}
// 监听属性变化
watchEffect(() => {
  if (props.visible) {
    // 数据源
    const copy = deepCopy(props.currentNode.data);
    // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
    Object.keys(form.value).forEach(k => { delete form.value[k]; });
    Object.assign(form.value, copy);
    ColumnByAssettab.value = props.currentNode?.data.taskParams.tableFields;
    // 等待 DOM 更新后清除旧的校验状态，避免残留红字
    nextTick(() => {
      dpModelRefs.value?.clearValidate();
      
    });
  } else {
    off();
  }
});
// 文件删除
function handleRemove() {
  ColumnByAssettab.value = [];
  form.value.taskParams.excelFile = undefined;
}
</script>
<style scoped lang="scss">
.blue-text {
  color: #2666fb;
}
</style>

