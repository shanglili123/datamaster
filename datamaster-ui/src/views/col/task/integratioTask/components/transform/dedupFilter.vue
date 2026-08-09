<template>
  <a-modal
    v-model:open="visibleDialog"
    class="medium-dialog"
    :title="form.taskParams.typeName"
    :closable="false"
    :destroy-on-close="true"
  >
    <template #title>
      <div class="justify">
        <span class="ant-modal-title">{{ currentNode?.data?.name }}</span>
        <a-tooltip
          title="根据指定字段判断数据是否重复，并保留第一条出现的记录（即遇到重复时，保留数据集中第一次出现的那条），结合排序节点使用"
          placement="top"
        >
          <InfoCircleOutlined class="tip-icon" />
        </a-tooltip>
      </div>
    </template>
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
            <a-select
              v-if="!info"
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
        <span class="blue-text">字段</span>
      </a-divider>
      <div class="justify-between mb15" v-if="!info">
        <a-row :gutter="15" class="btn-style">
          <a-col :span="1.5">
            <a-button type="primary" @click="handleAddField">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
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
            <div
              class="allowDrag"
              style="
                cursor: move;
                display: flex;
                justify-content: center;
                align-items: center;
              "
            >
              <ControlOutlined />
              <span style="margin-left: 4px">{{ index + 1 }}</span>
            </div>
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
          <template v-else-if="column.dataIndex === 'ignoreCase'">
            <a-select v-model:value="record.ignoreCase" placeholder="请选择">
              <a-select-option label="是" :value="0" />
              <a-select-option label="否" :value="1" />
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
        <a-button type="warning" @click="handleFetchFields" v-if="!info"
          >获取字段</a-button
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
import { InfoCircleOutlined, ControlOutlined, DeleteOutlined } from "@ant-design/icons-vue";
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

import { createNodeSelect, getParentNode } from "@/views/col/utils/opBase.js";

import draggable from "vuedraggable";

import Sortable from "sortablejs";
const { proxy } = getCurrentInstance();
const userStore = useUserStore();

const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  currentNode: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
  graph: { type: Object, default: () => ({}) },
});

let dragTable = ref(null);
let sortableInstance = null;
const tableColumns = computed(() => {
  const cols = [
    { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
    { title: '字段名称', dataIndex: 'columnName', align: 'left' },
    { title: '忽略大小写', dataIndex: 'ignoreCase', align: 'left', ellipsis: true },
  ];
  if (!props.info) {
    cols.push({ title: '操作', key: 'actions', align: 'center', className: 'small-padding fixed-width', fixed: 'right', width: 150 });
  }
  return cols;
});
function setSort() {
  nextTick(() => {
    const tbody = dragTable.value?.$el.querySelector(
      ".ant-table-tbody"
    );
    if (!tbody) {
      console.warn("tbody 找不到，拖拽初始化失败");
      return;
    }

    if (sortableInstance) {
      sortableInstance.destroy();
    }

    sortableInstance = Sortable.create(tbody, {
      handle: ".allowDrag",
      animation: 150,
      onEnd: (evt) => {
        const movedItem = tableFields.value.splice(evt.oldIndex, 1)[0];
        tableFields.value.splice(evt.newIndex, 0, movedItem);
        console.log(
          "拖拽后顺序:",
          tableFields.value.map((f) => f.columnName)
        );
      },
    });
  });
}

function handleAddField() {
  if (!Array.isArray(inputFields.value) || inputFields.value.length === 0) {
    proxy.$message.warning("输入字段为空，无法添加字段");
    return;
  }
  // 已添加的字段名
  const usedNames = tableFields.value.map((item) => item.columnName);

  // 找到未使用的字段
  const nextField = inputFields.value.find(
    (item) => !usedNames.includes(item.columnName)
  );

  if (!nextField) {
    proxy.$message.warning("新增失败，已无可添加的字段");
    return;
  }

  tableFields.value.push({
    columnName: nextField.columnName,
    columnType: nextField.columnType,
    ignoreCase: 1,
    source: form.value.name,
  });
  setSort();
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
      const existingNames = tableFields.value.map((f) => f.columnName);
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
      // 这里先清空，再加全部字段，避免重复
      tableFields.value = deepCopy(inputFields.value);

      break;
    }

    case "clearAndAddAll": {
      console.log("父组件：清空并增加所有字段");

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

const isOptionDisabled = (optionValue, currentRow) => {
  return tableFields.value.some(
    (row) => row !== currentRow && row.columnName === optionValue
  );
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
let originalTableFieldsBackup = ref([]);
let inputFields = ref([]);
let loading = ref(false);
let loadingList = ref(false);
let opens = ref(false);
let row = ref();
let dpModelRefs = ref();
let form = ref({});
function canChangeIgnoreCase(row) {
  if (!row) return false;

  const normType = (row.columnType || "")
    .toLowerCase()
    .replace(/\(.+\)$/, "")
    .trim();
  const isString = ["varchar", "char", "text", "string"].some((t) =>
    normType.startsWith(t)
  );
  // 3. 非字符串列：禁用 + 强制 ignoreCase = 1
  if (!isString) {
    row.ignoreCase = 1;
    return false;
  }
  // 4. 字符串列：可编辑，若值为空给 0
  if (row.ignoreCase == null) row.ignoreCase = 0;
  return true;
}

function handleRule(data) {
  row.value = { ...data };
  opens.value = true;
}

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
  setSort();
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
      console.warn("无法解析 ruleConfig:", ruleItem.ruleConfig);
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
    const valid = await dpModelRefs.value.validate();
    if (!valid) return;
    // 判断表格是否为空
    if (!tableFields.value || tableFields.value.length === 0) {
      proxy.$message.warning("校验未通过，请至少添加一个字段");
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
    taskParams.tableFields = tableFields.value;
    taskParams.mainArgs = taskParams.mainArgs || {};

    // 构造 outputFields = inputFields + tableFields 的增强值
    taskParams.outputFields = inputFields.value.map((input) => {
      const matched = tableFields.value.find(
        (item) => item.columnName === input.columnName
      );
      return matched ? { ...input, ...matched } : { ...input };
    });

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
    props.currentNode?.data?.taskParams?.inputFields || []
  );
  inputFields.value = taskParams?.inputFields || [];
  tableFields.value = taskParams?.tableFields || [];
  setSort();
});
</script>

<style scoped lang="less">
.blue-text {
  color: #2666fb;
}
</style>

