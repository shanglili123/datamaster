<template>
  <a-modal v-model:open="visibleDialog" :draggable="true" class="medium-dialog" :title="currentNode?.data?.name"
    :closable="false" :destroy-on-close="true" :mask-closable="false">
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
          <a-form-item label="上传附件" name="taskParams.file" :rules="[
            { required: true, message: '请上传附件', trigger: 'change' },
          ]">
            <FileUploadbtn :limit="1" v-model="form.taskParams.file" :dragFlag="false" :file-type="['csv']"
              :fileSize="50" @handleRemove="handleRemove" :showDelete="!info" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-button type="primary" @click="parseExcel" style="margin-left: 60px" :disabled="isButtonDisabled">
            解析csv
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

import {
  getLocalNodeUniqueKey as getNodeUniqueKey,
  getExcelColumn,
  getCsvColumn,
} from "@/api/col/task/index.js";

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
const uploadFileUrl = ref(import.meta.env.VITE_APP_BASE_API + "/upload"); // 上传文件服务器地址
/*** 用户导入参数 */
const upload = reactive({
  // 是否禁用上传
  isUploading: false,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + getToken() },
});
// 修改
const open = ref(false);
let row = ref({});
const openDialog = (obj) => {
  row.value = obj;
  open.value = true;
};
const handletaskConfig = (form) => {
  // 找到对应的 id 并更新 ColumnByAssettab 中的相应项
  ColumnByAssettab.value = ColumnByAssettab.value.map((column) => {
    if (column.id == form.id) {
      // 更新匹配 id 的项
      return { ...column, ...form }; // 或者根据需要做其他的合并方式
    }
    return column; // 对于不匹配的项，保持不变
  });
};

let dpModelRefs = ref();
let form = ref({});
const tableFields = ref([]); // 来源表格
// 计算属性：判断按钮是否禁用
const isButtonDisabled = computed(() => {
  return !form.value.taskParams.file;
});
// 获取列数据
const parseExcel = async (id) => {
  if (!form.value.taskParams.file) {
    message.warning("解析失败，请添加附件");
    return;
  }

  loading.value = true; // Assuming 'loading' is a global loading state variable
  try {
    let res = await getCsvColumn({
      file: form.value.taskParams.file,
    });

    if (res?.data?.csvFile) {
      form.value.taskParams.csvFile = res.data.csvFile;
      ColumnByAssettab.value = res.data.columnList.map((item, index) => ({
        id: index,
        columnName: item,
        columnType: "string",
      }));
      message.success("CSV 解析成功，请确认属性字段类型！");
    } else {
      message.warning("CSV 解析失败，未获取到有效数据！");
    }
  } catch (error) {
    message.warning("解析文件时发生错误，请检查后重试");
    console.error(error);
  } finally {
    loading.value = false; // Ensure loading is turned off regardless of success or failure
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
    // 异步验证表单
    const valid = await dpModelRefs.value.validate();
    if (!valid) return;
    if (
      form.value?.taskParams.type == "1" &&
      (!ColumnByAssettab.value || ColumnByAssettab.value.length == 0)
    ) {
      return proxy.$message.warning("解析失败，请选择属性字段");
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

  } catch (error) {
    console.error("保存数据失败:", error);
    loading.value = false; // 确保错误发生时也结束加载状态
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
    form.value = deepCopy(props.currentNode.data);
    ColumnByAssettab.value = props.currentNode?.data.taskParams.tableFields;
  } else {
    off();
  }
});
// 上传前校验文件类型
function handleBeforeUpload(file) {
  // 校检文件类型
  let fileType = ["csv"];
  const fileName = file.name.split(".");
  const fileExt = fileName[fileName.length - 1];
  const isTypeOk = fileType.indexOf(fileExt) >= 0;
  if (!isTypeOk) {
    proxy.$modal.msgWarning(`文件格式不正确, 请上传csv格式文件!`);
    return false;
  }
  // 校验文件大小
  const maxSize = 50; // 最大文件大小，单位MB
  const fileSize = file.size / 1024 / 1024;
  if (fileSize > maxSize) {
    proxy.$modal.msgWarning(`大小超出限制，文件大小不能超过 ${maxSize}MB!`);
    return false;
  }
  return true;
}

// 文件删除
function handleRemove() {
  ColumnByAssettab.value = [];
  form.value.taskParams.file = undefined;
}
</script>
<style scoped lang="less">
.blue-text {
  color: #2666fb;
}

.upload-file {
  width: 100%;
}

.upload-file-uploader {
  margin-bottom: 5px;
}

.filelistcont {
  display: flex;
  align-items: center;

  .filelistcont-name {
    margin-right: 10px;
  }
}
</style>

