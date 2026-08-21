<template>
  <a-modal
    v-model:open="visibleDialog"
    class="medium-dialog"
    :title="form.taskParams.typeName"
    :closable="false"
    :destroy-on-close="true"
    :width="1200"
  >
    <a-spin :spinning="loading">
    <a-form
      ref="dpModelRefs"
      :model="form"
      :label-col="{ style: { width: '110px' } }"
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
          <a-form-item label="类型" name="typeName">
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
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-input
              v-if="!info"
              v-model:value="form.description"
              type="textarea"
              :maxlength="500"
              show-count
              placeholder="请输入描述"
            />
            <div v-else class="form-readonly textarea">
              {{ form.description || "-" }}
            </div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-divider orientation="center">
        <span class="blue-text">需要选择与修改的字段</span>
      </a-divider>
      <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
          <a-col :span="1.5">
            <a-button type="primary" @click="handleAddField">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
          </a-col>
          <a-col :span="1.5">
            <a-button
              type="warning"
              @click="handleFetchFields"
              :disabled="info"
              v-if="!info"
              >获取字段</a-button
            >
          </a-col>
        </a-row>
      </div>
      <a-table
        height="310px"
        :data-source="tableFields"
        :loading="loadingList"
        :columns="tableColumns"
        :row-key="'columnName'"
        ref="dragTable"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'index'">
            <span>{{ index + 1 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'columnName'">
            <a-select
              v-model:value="record.columnName"
              placeholder="请选择字段"
              style="flex: 1"
            >
              <a-select-option
                v-for="item in inputFields"
                :key="item.value"
                :label="item.label"
                :value="item.columnName"
                :disabled="isOptionDisabled(item.columnName, record)"
              />
            </a-select>
          </template>
          <template v-else-if="column.dataIndex === 'outputField'">
            <a-input
              v-model:value="record.outputField"
              placeholder="请输入新的字段名称"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.dataIndex === 'type'">
            <a-select
              v-model:value="record.type"
              placeholder="请选择字段类型"
              style="width: 100%"
              allow-clear
            >
              <a-select-option
                v-for="dict in columntype"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </a-select>
          </template>
          <template v-else-if="column.dataIndex === 'length'">
            <a-input-number
              placeholder="请输入字段长度"
              v-model:value="record.length"
              :min="0"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.dataIndex === 'precision'">
            <a-input-number
              placeholder="请输入字段精度"
              v-model:value="record.precision"
              :min="0"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" danger @click="handleDelete(record)">
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
          </template>
        </template>
      </a-table>
      <a-divider orientation="center">
        <span class="blue-text">需要移除的字段</span>
      </a-divider>
      <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
          <a-col :span="1.5">
            <a-button type="primary" @click="handleAddField2">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
          </a-col>
        </a-row>
      </div>
      <a-table
        height="310px"
        :data-source="removeFields"
        :loading="loadingList"
        :columns="removeColumns"
        :row-key="'columnName'"
        ref="dragTable"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'index'">
            <span>{{ index + 1 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'columnName'">
            <a-select
              v-model:value="record.columnName"
              placeholder="请选择字段"
              style="flex: 1"
            >
              <a-select-option
                v-for="item in inputFields"
                :key="item.value"
                :label="item.label"
                :value="item.columnName"
                :disabled="isOptionDisabled(item.columnName, record)"
              />
            </a-select>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" danger @click="handleDelete2(record)">
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
import { DeleteOutlined } from "@ant-design/icons-vue";
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
const { proxy } = getCurrentInstance();
const userStore = useUserStore();

const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  currentNode: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
  graph: { type: Object, default: () => ({}) },
});
const columntype = [
  { value: "BigNumber", label: "BigNumber" },
  { value: "Binary", label: "Binary" },
  { value: "Boolean", label: "Boolean" },
  { value: "Date", label: "Date" },
  { value: "Integer", label: "Integer" },
  { value: "InternetAddress", label: "InternetAddress" },
  { value: "Number", label: "Number" },
  { value: "String", label: "String" },
  { value: "Timestamp", label: "Timestamp" },
];

const tableColumns = computed(() => {
  const cols = [
    { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
    { title: '字段名称', dataIndex: 'columnName', align: 'left' },
    { title: '字段别名', dataIndex: 'outputField', align: 'left', ellipsis: true },
    { title: '字段类型', dataIndex: 'type', align: 'left', width: 150 },
    { title: '字段长度', dataIndex: 'length', align: 'left', width: 150 },
    { title: '字段精度', dataIndex: 'precision', align: 'left', width: 150 },
  ];
  if (!props.info) {
    cols.push({ title: '操作', key: 'actions', align: 'center', className: 'small-padding fixed-width', fixed: 'right', width: 150 });
  }
  return cols;
});

const removeColumns = [
  { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
  { title: '字段名称', dataIndex: 'columnName', align: 'left' },
  { title: '操作', key: 'actions', align: 'center', className: 'small-padding fixed-width', fixed: 'right', width: 150 },
];

let dragTable = ref(null);

function hasDuplicateObjects(arr, key) {
  if (arr.length <= 1) return false;
  const seen = new Set(); // 记录已出现的键值
  for (const item of arr) {
    const value = item[key];
    // 若当前键值已存在于Set中，说明有重复
    if (seen.has(value)) {
      return true;
    }
    value !== "" && seen.add(value);
  }
  return false;
}
function handleAddField() {
  if (!Array.isArray(inputFields.value) || inputFields.value.length === 0) {
    proxy.$message.warning("输入字段为空，无法添加字段");
    return;
  }
  // 已添加的字段名
  const tableUsedNames = tableFields.value.map((item) => item.columnName);
  const removeUsedNames = removeFields.value.map((item) => item.columnName);
  const usedNames = [...tableUsedNames, ...removeUsedNames];
  // 找到未使用的字段
  const nextField = inputFields.value.find(
    (item) => !usedNames.includes(item.columnName)
  );

  if (!nextField) {
    proxy.$message.warning("新增失败，已无可添加的字段");
    return;
  }

  let isRepeat = hasDuplicateObjects(tableFields.value, "outputField");
  if (isRepeat) {
    proxy.$message.warning("请不要填写重复输出字段");
    return;
  }
  let names = inputFields.value.map((item) => item.columnName);
  let isOut = names.find((item) =>
    tableFields.value.some((row) => row.outputField === item)
  );
  if (isOut) {
    proxy.$message.warning("输出字段不能与已有字段名称重复");
    return;
  }

  tableFields.value.push({
    columnName: nextField.columnName,
    columnType: nextField.columnType,
    outputField: "",
    type: "",
    length: null,
    precision: null,
    ignoreCase: 1,
    source: form.value.name,
  });
}
function handleAddField2() {
  if (!Array.isArray(inputFields.value) || inputFields.value.length === 0) {
    proxy.$message.warning("输入字段为空，无法添加字段");
    return;
  }
  // 已添加的字段名
  const tableUsedNames = tableFields.value.map((item) => item.columnName);
  const removeUsedNames = removeFields.value.map((item) => item.columnName);
  const usedNames = [...tableUsedNames, ...removeUsedNames];
  // 找到未使用的字段
  const nextField = inputFields.value.find(
    (item) => !usedNames.includes(item.columnName)
  );

  if (!nextField) {
    proxy.$message.warning("已无可添加的字段");
    return;
  }

  removeFields.value.push({
    columnName: nextField.columnName,
    columnType: nextField.columnType,
    ignoreCase: 1,
    source: form.value.name,
  });
}
const showConflictDialog = ref(false);

const handleFetchFields = () => {
  showConflictDialog.value = true;
};
function onResolveFields(payload) {
  if (!payload || !payload.action) return;
  const tableNames = tableFields.value.map((f) => f.columnName).sort();
  const inputNames = inputFields.value.map((f) => f.columnName).sort();
  const isEqual =
    tableNames.length === inputNames.length &&
    tableNames.every((name, idx) => name === inputNames[idx]);
  switch (payload.action) {
    case "addNewOnly": {
      console.log("父组件：只增加新字段");

      // 计算已有字段名称
      const tableUsedNames = tableFields.value.map((item) => item.columnName);
      const removeUsedNames = removeFields.value.map((item) => item.columnName);
      const existingNames = [...tableUsedNames, ...removeUsedNames];
      // 找到新字段中不在已有字段中的字段
      const newUniqueFields = inputFields.value.filter(
        (f) => !existingNames.includes(f.columnName)
      );
      // 加入到 tableFields 中
      tableFields.value = tableFields.value.concat(deepCopy(newUniqueFields));
      break;
    }

    case "addAll": {
      console.log(
        "🚀 ~ onResolveFields ~  tableFields.value =:",
        tableFields.value
      );
      if (isEqual) {
        proxy.$message.warning("新增失败，当前已是最新字段");
      }
      console.log("父组件：增加所有字段");
      tableFields.value = [];
      removeFields.value = [];
      // 这里先清空，再加全部字段，避免重复
      tableFields.value = deepCopy(inputFields.value);

      break;
    }

    case "clearAndAddAll": {
      console.log("父组件：清空并增加所有字段");
      removeFields.value = [];
      // 恢复原始备份字段
      tableFields.value = deepCopy(inputFields.value);

      break;
    }

    case "cancel": {
      console.log("父组件：取消操作");
      break;
    }
  }
}

const isOptionDisabled = (optionValue) => {
  // 已添加的字段名
  const tableUsedNames = tableFields.value.map((item) => item.columnName);
  const removeUsedNames = removeFields.value.map((item) => item.columnName);
  const usedNames = [...tableUsedNames, ...removeUsedNames];
  return usedNames.some((row) => row === optionValue);
};

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
let removeFields = ref([]);
let originalTableFieldsBackup = ref([]);
let inputFields = ref([]);
let loading = ref(false);
let loadingList = ref(false);
let opens = ref(false);
let row = ref();
let dpModelRefs = ref();
let form = ref({});

function handleDelete(row) {
  // 1. 从 tableFields 中删除对应项
  const idxTable = tableFields.value.findIndex(
    (item) => item.columnName === row.columnName
  );
  if (idxTable !== -1) {
    tableFields.value.splice(idxTable, 1);
  }
  const originalField = originalTableFieldsBackup.value.find(
    (item) => item.columnName === row.columnName
  );
  if (originalField) {
    const idxField = inputFields.value.findIndex(
      (item) => item.columnName === row.columnName
    );
    if (idxField !== -1) {
      inputFields.value[idxField] = JSON.parse(JSON.stringify(originalField));
    } else {
      inputFields.value.push(JSON.parse(JSON.stringify(originalField)));
    }
  }
}
function handleDelete2(row) {
  // 1. 从 tableFields 中删除对应项
  const idxTable = removeFields.value.findIndex(
    (item) => item.columnName === row.columnName
  );
  if (idxTable !== -1) {
    removeFields.value.splice(idxTable, 1);
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
  inputFields.value = [];
  originalTableFieldsBackup.value = [];
};

const saveData = async () => {
  try {
    // 手动检查关键字段
    if (!form.value?.code) {
      // 代码生成逻辑
    }
    // 等待 DOM 更新
    await nextTick();
    // 直接检查关键字段（避免 a-form dot-notation path 校验失效）
    if (!form.value?.code) {
      return proxy.$message.warning('请配置代码');
    }
    // 判断表格是否为空
    if (!tableFields.value || tableFields.value.length === 0) {
      proxy.$message.warning("校验未通过，请至少添加一个字段");
      return;
    }
    let isRepeat = hasDuplicateObjects(tableFields.value, "outputField");
    if (isRepeat) {
      proxy.$message.warning("请不要填写重复输出字段");
      return;
    }

    let names = inputFields.value.map((item) => item.columnName);
    let isOut = names.find((item) =>
      tableFields.value.some((row) => row.outputField === item)
    );
    if (isOut) {
      proxy.$message.warning("输出字段不能与已有字段名称重复");
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
    const taskParams = form.value?.taskParams || {};
    taskParams.tableFields = tableFields.value.map((item) => ({
      ...item,
      inputField: item.columnName,
    }));
    taskParams.removeFields = removeFields.value;
    taskParams.mainArgs = taskParams.mainArgs || {};

    // 构造 outputFields = inputFields + tableFields 的增强值
    let outputFields = inputFields.value.map((input) => {
      const matched = tableFields.value.find(
        (item) => item.columnName === input.columnName
      );
      return matched
        ? {
            ...input,
            ...matched,
            columnName: matched.outputField || matched.columnName,
          }
        : { ...input };
    });
    // 过滤掉 removeFields 中的值
    taskParams.outputFields = outputFields.filter(
      (item) =>
        removeFields.value.findIndex(
          (f) => f.columnName === item.columnName
        ) === -1
    );

    console.log("保存数据 - outputFields:", taskParams.outputFields);
    emit("confirm", form.value);
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
    console.error("deepCopy error:", e);
    return {};
  }
}

let nodeOptions = ref([]);
watchEffect(() => {
  if (!props.visible) {
    off();
    return;
  }
  const copy = deepCopy(props.currentNode?.data || {});
  // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
  Object.keys(form.value).forEach(k => { delete form.value[k]; });
  Object.assign(form.value, copy);
  nodeOptions.value = createNodeSelect(props.graph, props.currentNode.id);
  let taskParams = deepCopy(props.currentNode?.data?.taskParams || {});
  originalTableFieldsBackup.value = deepCopy(
    props.currentNode?.data?.taskParams?.inputFields || []
  );
  inputFields.value = taskParams?.inputFields || [];
  tableFields.value = taskParams?.tableFields || [];
  removeFields.value = taskParams?.removeFields || [];
  // 等待 DOM 更新后清除旧的校验状态，避免残留红字
nextTick(() => {
    dpModelRefs.value?.clearValidate();
});
});
</script>

<style scoped lang="less">
.blue-text {
  color: #2666fb;
}
</style>

