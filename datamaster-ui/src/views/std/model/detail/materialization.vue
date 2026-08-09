<template>
   <!--  逻辑物化的弹窗  -->
  <a-modal v-model:open="localVisible" :title="title" draggable class="warn-dialog" :destroy-on-close="true">
    <!-- <div class="centered-text">
      您将对选择的{{
        ids?.length
      }}个逻辑模型进行逻辑物化，请选择数据资产的数据连接
    </div> -->
    <a-form ref="dpModelRefs" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }" @submit.prevent>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="数据库连接" name="datasourceId" :rules="[
            {
              required: true,
              message: '请选择数据库连接',
              trigger: 'change',
            },
          ]">
            <a-select v-model:value="form.datasourceId" placeholder="请选择数据连接" @change="handleDatasourceChange"
              show-search>
              <a-select-option v-for="dict in createTypeList" :key="dict.id" :value="dict.id">{{ dict.datasourceName
              }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="数据库类型" name="datasourceType">
            <a-input v-model:value="form.datasourceType" placeholder="请输入数据库类型" disabled />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="数据库地址" name="ip">
            <a-input v-model:value="form.ip" placeholder="请输入数据库类型" disabled />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述">
            <a-textarea placeholder="请输入描述" v-model:value="form.description" :auto-size="{ minRows: 4 }" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea placeholder="请输入备注" v-model:value="form.remark" :auto-size="{ minRows: 4 }" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <template #footer>
      <div class="dialog-footer">
        <a-button @click="closeDialog">取消</a-button>
        <a-button type="primary" @click="confirmDialog"> 确认 </a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { message } from 'ant-design-vue'
import {
  createMaterializedTable,
  getDaDatasourceList,
} from "@/api/std/model/model";

import { defineProps, defineEmits, ref, computed, watch } from "vue";
const { proxy } = getCurrentInstance();
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  ids: { type: Array, default: () => [] },
});
let createTypeList = ref();
// 监听 `visible` 的变化
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      getDaDatasourceListList();
    }
  }
);

const getDaDatasourceListList = async () => {
  try {
    const response = await getDaDatasourceList();
    createTypeList.value = response.data;
  } catch (error) {
    console.error("请求失败:", error);
  }
};

const emit = defineEmits(["update:dialogFormVisible", "confirm"]);

// 处理弹窗显示状态
const localVisible = computed({
  get() {
    return props.visible;
  },
  set(value) {
    emit("update:dialogFormVisible", value);
  },
});

const handleDatasourceChange = (value) => {
  const selectedDatasource = createTypeList.value.find(
    (item) => item.id === value
  );
  if (selectedDatasource) {
    form.value.ip = selectedDatasource.ip;
    form.value.datasourceConfig = selectedDatasource.datasourceConfig;
    form.value.datasourceType = selectedDatasource.datasourceType;
    form.value.datasourceName = selectedDatasource.datasourceName;
    form.value.port = selectedDatasource.port;
  }
};

const form = ref({
  datasourceId: "",
  datasourceType: "",
  ip: "",
  datasourceConfig: "",
  datasourceType: "",
  port: "",
  datasourceName: "",
});

const rules = ref({
  datasourceId: [
    { required: true, message: "请选择数据连接", trigger: "blur" },
  ],
});

const closeDialog = () => {
  form.value = {
    datasourceId: "",
    datasourceType: "",
    ip: "",
    datasourceConfig: "",
    datasourceType: "",
    port: "",
    datasourceName: "",
  };
  localVisible.value = false;
  proxy.resetForm("dpModelRefs");
};

const confirmDialog = async () => {
  try {
    // 使用 Promise 进行表单验证
    const isValid = await proxy.$refs["dpModelRefs"].validate();

    if (isValid) {
      // 创建物化表格
      const response = await createMaterializedTable({
        modelId: props.ids,
        ...form.value,
      });
      console.log(response);

      // 提交数据
      emit("confirm", form.value);

      // 关闭对话框
      closeDialog();
      // 提示成功
      proxy.$modal.msgSuccess(response.msg);
    }
  } catch (error) {
    // 捕获并提示错误信息
    proxy.$message.warning(response.msg);
    console.log(error);
  }
};
</script>

<style scoped lang="less">
.warn-dialog .ant-modal-body {
  max-height: 500px;
  overflow-y: auto;
}

.dialog-footer {
  text-align: right;
}

.dialog-footer .ant-btn {
  margin-left: 10px;
}

.centered-text {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 10%;
  text-align: center;
  font-size: 14px;
  color: #333;
}
</style>

