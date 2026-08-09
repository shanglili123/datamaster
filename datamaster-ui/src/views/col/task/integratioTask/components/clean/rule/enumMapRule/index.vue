<template>
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-spin :spinning="loadingList">
      <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
          <template v-if="!falg">
            <a-col :span="1.5">
              <a-button
                type="primary"
                @click="opencodeDialog(undefined)"
                ><template #icon><PlusOutlined /></template>新增规则</a-button
              >
            </a-col>
            <a-col :span="1.5">
              <a-button
                type="primary"
                @click="showDialog(undefined)"
                ><template #icon><PlusOutlined /></template>导入规则</a-button
              >
            </a-col>
          </template>
        </a-row>
      </div>
      <a-table striped :loading="loading" :data-source="form.stringValue" :columns="tableColumns"
        :pagination="false">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'value'">
            <template v-if="!falg">
              <a-input
                v-model:value="record.value"
                style="width: 100%"
                placeholder="请输入原值"
              />
            </template>
            <div v-else class="form-readonly">{{ record.value || "-" }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <template v-if="!falg">
              <a-input
                v-model:value="record.name"
                style="width: 100%"
                placeholder="请输入标准值"
              />
            </template>
            <div v-else class="form-readonly">{{ record.name || "-" }}</div>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button
              type="link"
              danger
              size="small"
              @click="handleDelete(index + 1)"
              >删除</a-button
            >
          </template>
        </template>
      </a-table>
    </a-spin>
    <a-row> </a-row>
    <singleSelectTableDialog ref="dialogRef" @confirm="handleConfirm" />
  </a-form>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { reactive, ref, watch } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";

import {
  listDpDataElem,
  listDpDataElemCode,
} from "@/api/std/dataElem/dataElem.js";

import singleSelectTableDialog from "./dataElem.vue";
const props = defineProps({
  form: Object,
  inputFields: Array,
  falg: Boolean,
});
let loadingList = ref(false);
const emit = defineEmits(["update:form"]);
let loading = ref(false);
const formRef = ref(null);
const { proxy } = getCurrentInstance();
const form = reactive({ ...props.form });

const tableColumns = computed(() => {
  const cols = [
    { title: '原值', dataIndex: 'value', align: 'left' },
    { title: '标准值', dataIndex: 'name', align: 'left' },
  ];
  if (!props.falg) {
    cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 150 });
  }
  return cols;
});
let dpDataElemstringValue = ref([]);
let dpDataElemList = ref([]);

const dialogRef = ref();

function showDialog() {
  dialogRef.value.openDialog("选择数据元");
}

function handleConfirm(row, list) {
  console.log("选中行：", row);
  dpDataElemstringValue.value = [];

  loadCodeItemsByTableId(row.id);
}
function loadCodeItemsByTableId(id) {
  if (!id || id == -1) return;
  loading.value = true;
  listDpDataElemCode({
    pageNum: 1,
    pageSize: 999,
    dataElemId: id,
    ruleType: 2,
  }).then((res) => {
    // dpDataElemstringValue.value = res.data.rows;
    form.stringValue = (res?.data?.rows || []).map(
      ({ codeValue, codeName, ...rest }) => ({
        ...rest,
        value: codeValue ?? "",
        name: codeName ?? "",
      })
    );
    loading.value = false;
  });
}
function handleDelete(index) {
  form.stringValue.splice(Number(index) - 1, 1);
}
function opencodeDialog() {
  const hasIncomplete = (form.stringValue || []).some(
    (item) => !item.value || !item.name
  );

  if (hasIncomplete) {
    message.warning("请先填写完整所有项");
    return;
  }

  if (!Array.isArray(form.stringValue)) {
    form.stringValue = [];
  }

  // 新增一行空数据
  form.stringValue.push({
    value: "",
    name: "",
  });
}

function loadCodeTableList() {
  listDpDataElem({
    pageNum: 1,
    pageSize: 999,
    type: "2",
  })
    .then((res) => {
      dpDataElemList.value = res.data.rows;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}
function handleUseCodeTableChange(val) {
  if (val == "1") {
    loadCodeTableList();
  } else {
    form.codeTableId = "";
    form.stringValue = [];
    dpDataElemList.value = [];
  }
}

onMounted(() => {
  if (form.useCodeTable === "1" && form.codeTableId) {
    handleUseCodeTableChange("1", true);
  }
});
function checkValueAndName(list) {
  if (!list || list.length === 0) {
    return { formIsValid: false, message: "至少需要添加一条规则数据！" };
  }
  const values = [];
  const names = [];
  for (const item of list) {
    const v = item.value?.trim();
    const n = item.name?.trim();
    if (!v || !n) {
      return { formIsValid: false, message: "原值和标准值不能为空！" };
    }
    values.push(v);
    names.push(n);
  }

  const hasDuplicate = (arr) =>
    arr.some((val, idx) => arr.indexOf(val) !== idx);

  if (hasDuplicate(values)) {
    return { formIsValid: false, message: "原值不能重复！" };
  }
  if (hasDuplicate(names)) {
    return { formIsValid: false, message: "标准值不能重复！" };
  }

  return { formIsValid: true, message: "" };
}

function validate() {
  return new Promise((resolve) => {
    formRef.value.validate((valid) => {
      if (!valid) {
        resolve({ valid: false });
        return;
      }
      const { formIsValid, message } = checkValueAndName(form.stringValue);
      if (!formIsValid) {
        proxy.$message.warning(message);
        resolve({ valid: false });
        return;
      }
      const result = {
        stringValue: form.stringValue,
      };

      resolve({ valid: true, data: result });
    });
  });
}

defineExpose({ validate });
</script>
<style scoped>
.form-readonly {
  min-height: 32px;
  line-height: 32px;
}
</style>

