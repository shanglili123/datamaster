<template>
  <a-modal
    v-model:open="visibleDialog"
    class="medium-dialog"
    :title="form.taskParams.typeName"
    :closable="false"
    :destroy-on-close="true"
  >
    <a-spin :spinning="loading">
    <a-form
      ref="dpModelRefs"
      :model="form"
      :label-col="{ style: { width: '140px' } }"
      @submit.prevent
      :disabled="info"
    >
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item
            label="节点名称"
            name="name"
            :rules="[
              { required: true, message: '请输入节点名称', trigger: 'change' },
            ]"
          >
            <a-input
              v-if="!info"
              v-model:value="form.name"
              placeholder="请输入节点名称"
            />
            <div v-else class="form-readonly">{{ form.name }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="类型" name="taskParams.typeName">
            <template v-if="!info">
              <a-select
                v-model:value="form.taskParams.typeName"
                placeholder="请输入类型"
                show-search
                disabled
              >
                <a-select-option
                  v-for="dict in typeList"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </a-select>
            </template>
            <div v-else class="form-readonly">
              {{ form.taskParams.typeName }}
            </div>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider orientation="center">
        <span class="blue-text">常量字段</span>
      </a-divider>
      <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
          <a-col :span="1.5">
            <a-button type="primary" @click="handleAddField">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
          </a-col>
        </a-row>
      </div>
      <a-table
        height="500px"
        :data-source="tableFields"
        :loading="loadingList"
        :columns="tableColumns"
        :row-key="'columnName'"
      >
        <template #headerCell="{ column }">
          <template v-if="column.dataIndex === 'emptyString'">
            <div class="justify-center">
              <span>设为空串</span>
              <a-tooltip
                title="勾选后，即使“默认值”字段填写了内容，也会被覆盖为空字符串"
                placement="top"
              >
                <InfoCircleOutlined />
              </a-tooltip>
            </div>
          </template>
        </template>
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'index'">
            {{ index + 1 }}
          </template>
          <template v-else-if="column.dataIndex === 'columnName'">
            <a-input
              v-model:value="record.columnName"
              placeholder="请输入"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.dataIndex === 'type'">
            <a-select
              v-model:value="record.type"
              placeholder="请选择"
              style="width: 100%"
            >
              <a-select-option
                v-for="dict in columntype"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </a-select>
          </template>
          <template v-else-if="column.dataIndex === 'defaultValue'">
            <a-input
              v-model:value="record.defaultValue"
              placeholder="请输入"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.dataIndex === 'emptyString'">
            <a-select
              v-model:value="record.emptyString"
              placeholder="请选择"
              style="width: 100%"
            >
              <a-select-option label="是" :value="true" />
              <a-select-option label="否" :value="false" />
            </a-select>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" danger @click="handleDelete(record)">
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
          </template>
        </template>
      </a-table>
    </a-form>
    </a-spin>
    <template #footer>
      <div style="text-align: right">
        <a-button @click="closeDialog">关闭</a-button>
        <a-button type="primary" @click="saveData" v-if="!info"
          >保存</a-button
        >
      </div>
    </template>
  </a-modal>

  <FieldConflictDialog
    v-model="showConflictDialog"
    :existingFields="tableFields"
    :newFields="inputFields"
    @resolve="onResolveFields"
  />
  <CreateEditModal
    :visibleDialogs="opens"
    @update:visibleDialogs="opens = $event"
    @confirm="submitForm"
    :row="row"
    :tableFields="tableFields"
    :inputFields="inputFields"
  />
</template>

<script setup>
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, DeleteOutlined } from "@ant-design/icons-vue";
import CreateEditModal from "../fieldMergeModal.vue";

import FieldConflictDialog from "../fieldDetection.vue";

import {
  defineProps,
  defineEmits,
  ref,
  computed,
  watchEffect,
  getCurrentInstance,
} from "vue";


import { getLocalNodeUniqueKey as getNodeUniqueKey } from "@/api/col/task/index.js";

import useUserStore from "@/store/system/user.js";

import { createNodeSelect } from "@/views/col/utils/opBase.js";

import { hasDuplicateObjects } from "@/utils/index.js";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const columntype = [
  { value: "BigNumber", label: "BigNumber" },
  { value: "Binary", label: "Binary" },
  { value: "Boolean", label: "Boolean" },
  { value: "Date", label: "Date" },
  { value: "Integer", label: "Integer" },
  { value: "Internet Address", label: "Internet Address" },
  { value: "Number", label: "Number" },
  { value: "String", label: "String" },
  { value: "Timestamp", label: "Timestamp" },
];

const tableColumns = computed(() => {
  const cols = [
    { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
    { title: '字段名称', dataIndex: 'columnName', align: 'left' },
    { title: '字段类型', dataIndex: 'type', align: 'left', width: 150 },
    { title: '默认值', dataIndex: 'defaultValue', align: 'left' },
    { title: '设为空串', dataIndex: 'emptyString', align: 'left', width: 150 },
  ];
  if (!props.info) {
    cols.push({ title: '操作', key: 'actions', align: 'center', className: 'small-padding fixed-width', fixed: 'right', width: 120 });
  }
  return cols;
});
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  currentNode: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
  graph: { type: Object, default: () => ({}) },
});

function handleAddField() {
  // 如果有任意一个已有字段 columnName 为空，阻止新增
  const lastEmpty = tableFields.value.find((item) => !item.columnName);
  if (lastEmpty) {
    proxy.$message.warning("添加失败，请先填写当前字段名称");
    return;
  }
  // 最后一行名称
  let isRepeat = hasDuplicateObjects(tableFields.value, "columnName");
  if (isRepeat) {
    proxy.$message.warning("请不要填写重复的字段名称");
    return;
  }

  tableFields.value.push({
    columnName: null,
    type: "String",
    defaultValue: null,
    emptyString: false,
    source: form.value.name,
  });
}

const showConflictDialog = ref(false);

function onResolveFields(payload) {
  if (!payload) return;
  switch (payload.action) {
    case "addNewOnly":
      console.log("父组件：只增加新字段");
      break;
    case "addAll":
      console.log("父组件：增加所有字段");
      break;
    case "clearAndAddAll":
      tableFields.value = deepCopy(originalTableFieldsBackup.value);
      console.log(
        "🚀 ~ onResolveFields ~  tableFields.value:",
        tableFields.value
      );
      console.log("父组件：清空并增加所有字段");
      break;
    case "cancel":
      console.log("父组件：取消操作");
      break;
  }
}

const emit = defineEmits(["update", "confirm"]);

const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update", newValue);
  },
});

let tableFields = ref([]);
let originalTableFieldsBackup = ref([]);
let inputFields = ref([]);
let loading = ref(false);
let loadingList = ref(false);
let opens = ref(false);
let row = ref();
let dpModelRefs = ref();
let form = ref({});

function handleDelete(row) {
  // 从 tableFields 中删除对应字段
  const idxTable = tableFields.value.findIndex(
    (item) => item.columnName === row.columnName
  );
  if (idxTable !== -1) {
    tableFields.value.splice(idxTable, 1);
  } else {
    proxy.$message.warning("删除失败，字段未找到");
  }

  // 恢复 inputFields 中被删除字段的原始状态（如果有）
  const originalField = originalTableFieldsBackup.value.find(
    (item) => item.columnName === row.columnName
  );
  if (originalField) {
    const idxField = inputFields.value.findIndex(
      (item) => item.columnName === row.columnName
    );
    if (idxField !== -1) {
      inputFields.value[idxField] = deepCopy(originalField);
    } else {
      inputFields.value.push(deepCopy(originalField));
    }
  }
}

// 提交弹窗规则数据
const submitForm = (value) => {
  if (!value || !Array.isArray(value)) return;

  value.forEach((ruleItem) => {
    if (!ruleItem?.ruleConfig) return;

    let parsedConfig;
    try {
      parsedConfig = JSON.parse(ruleItem.ruleConfig);
    } catch (e) {
      console.warn("无法解析 ruleConfig:", e, ruleItem.ruleConfig);
      return;
    }
    const sourceField = parsedConfig?.fieldMerge?.sourceField;
    if (!sourceField) return;

    const tableIndex = tableFields.value.findIndex(
      (item) => item.columnName == sourceField
    );
    if (tableIndex !== -1) {
      const updatedItem = {
        ...tableFields.value[tableIndex],
        cleanRuleList: [ruleItem],
        elementId: [ruleItem.ruleId],
      };
      tableFields.value[tableIndex] = updatedItem;

      const fieldIndex = inputFields.value.findIndex(
        (item) => item.columnName == sourceField
      );
      if (fieldIndex !== -1) {
        inputFields.value[fieldIndex] = updatedItem;
      } else {
        inputFields.value.push(updatedItem);
      }
    }
  });
  opens.value = false;
};

const off = () => {
  proxy.resetForm("dpModelRefs");
  tableFields.value = [];
  originalTableFieldsBackup.value = [];
  form.value = {};
  row.value = {};
};

const saveData = async () => {
  try {
    const valid = await dpModelRefs.value.validate();
    if (!valid) return;

    // 校验 tableFields 不为空
    if (!Array.isArray(tableFields.value) || tableFields.value.length === 0) {
      proxy.$message.warning("请至少一个字段值");
      return;
    }

    if (tableFields.value.length > 0) {
      const hasEmptyName = tableFields.value.some(
        (item) => !item.columnName?.trim()
      );
      if (hasEmptyName) {
        proxy.$message.warning("校验未通过，字段名称不能为空");
        return;
      }
    }

    // 最后一行名称
    let isRepeat = hasDuplicateObjects(tableFields.value, "columnName");
    if (isRepeat) {
      proxy.$message.warning("请不要填写重复的字段名称");
      return;
    }

    if (!form.value.code) {
      loading.value = true;
      const response = await getNodeUniqueKey({
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
      });
      loading.value = false;
      form.value.code = response.data;
    }

    const taskParams = form.value.taskParams || {};
    const splitField = form.value.taskParams.splitField;
    const inputWithoutSplit = inputFields.value.filter(
      (item) => item.columnName !== splitField
    );
    taskParams.outputFields = [
      ...inputWithoutSplit,
      ...tableFields.value.map((item) => ({ ...item, columnType: item.type })),
    ];
    taskParams.tableFields = tableFields.value.map((field) => ({
      ...field,
      name: field.columnName,
    }));
    taskParams.mainArgs = taskParams.mainArgs || { cleanRuleList: [] };
    form.value.taskParams = taskParams;
    console.log("保存数据 - outputFields:", taskParams.outputFields);
    emit("confirm", form.value);
    // closeDialog();
  } catch (error) {
    console.error("保存数据失败:", error);
    loading.value = false;
  }
};

const closeDialog = () => {
  off();
  emit("update", false);
};

function deepCopy(data) {
  if (data === undefined || data === null) {
    return {};
  }
  try {
    return JSON.parse(JSON.stringify(data));
  } catch (e) {
    console.log(e, "deepCopy error");
    return {};
  }
}

let nodeOptions = ref([]);
watchEffect(() => {
  if (!props.visible) {
    off();
    return;
  }
  form.value = deepCopy(props.currentNode?.data || {});
  nodeOptions.value = createNodeSelect(props.graph, props.currentNode.id);
  let taskParams = deepCopy(props.currentNode?.data?.taskParams || {});
  originalTableFieldsBackup.value = deepCopy(
    props.currentNode?.data?.taskParams?.tableFields || []
  );
  inputFields.value = taskParams?.inputFields || [];
  tableFields.value = taskParams?.tableFields || [];
});
</script>

<style scoped lang="less">
.blue-text {
  color: #2666fb;
}
</style>

