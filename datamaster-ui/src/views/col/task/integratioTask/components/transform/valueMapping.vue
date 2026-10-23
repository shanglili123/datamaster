<template>
  <a-modal wrap-class-name="integration-component-modal"
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
      :label-col="{ style: { width: '180px' } }"
      :disabled="info"
      @submit.prevent
    >
      <h3 class="etl-section-title">基本信息</h3>
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
        <a-col :span="12">
          <a-form-item
            label="使用字段"
            name="taskParams.inputField"
            :rules="[
              { required: true, message: '请选择使用字段', trigger: 'blur' },
            ]"
          >
            <template v-if="!info">
              <a-select
                v-model:value="form.taskParams.inputField"
                placeholder="请选择字段名称"
                show-search
              >
                <a-select-option
                  v-for="dict in inputFields"
                  :key="dict.columnName"
                  :label="dict.columnName"
                  :value="dict.columnName"
                />
              </a-select>
            </template>
            <div v-else class="form-readonly">
              {{ form.taskParams.inputField }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="目标字段"
            name="taskParams.outputField"
            :rules="[
              { required: true, message: '请输入目标字段', trigger: 'change' },
            ]"
          >
            <a-input
              v-if="!info"
              v-model:value="form.taskParams.outputField"
              placeholder="请输入目标字段"
            />
            <div v-else class="form-readonly">
              {{ form.taskParams.outputField }}
            </div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item
            label="默认值"
            name="taskParams.defaultValue"
            :rules="[
              {
                required: false,
                message: '请输入不匹配时的默认值',
                trigger: 'change',
              },
            ]"
          >
            <template #label>
              <div class="justify-center">
                <span>不匹配时的默认值</span>
                <a-tooltip
                  title="按未匹配处理和转换失败策略使用此默认值，允许空字符串"
                  placement="top"
                >
                  <InfoCircleOutlined class="tip-icon" />
                </a-tooltip>
              </div>
            </template>
            <a-input
              v-if="!info"
              v-model:value="form.taskParams.defaultValue"
              placeholder="请选择不匹配时的默认值"
            />
            <div v-else class="form-readonly">
              {{ form.taskParams.defaultValue || "-" }}
            </div>
          </a-form-item>
        </a-col>
      </a-row>
<a-row :gutter="20">
            <a-col :span="12" >
            <a-form-item label="结果类型">
            <a-select v-model:value="form.taskParams.outputType" allow-clear placeholder="沿用原值类型（兼容旧配置）" style="width: 100%">
            <a-select-option value="string">string</a-select-option>
            <a-select-option value="int">int</a-select-option>
            <a-select-option value="bigint">bigint</a-select-option>
            <a-select-option value="double">double</a-select-option>
            <a-select-option value="decimal">decimal</a-select-option>
            <a-select-option value="boolean">boolean</a-select-option>
            <a-select-option value="date">date</a-select-option>
            <a-select-option value="timestamp">timestamp</a-select-option>
            </a-select>
            </a-form-item>
            </a-col>
<a-col :span="12" >
            <a-form-item label="转换失败策略">
            <a-select v-model:value="form.taskParams.castFailure" style="width: 100%">
            <a-select-option value="FAIL">原生 CAST（沿用引擎错误行为）</a-select-option>
            <a-select-option value="NULL">置为 NULL</a-select-option>
            <a-select-option value="DEFAULT">使用默认值</a-select-option>
            </a-select>
            </a-form-item>
            </a-col>
<a-col :span="12" v-if="isDecimal(form.taskParams.outputType)"><a-form-item label="总位数 precision"><a-input-number v-model:value="form.taskParams.precision" :min="1" :max="38" :precision="0" /></a-form-item></a-col>
<a-col :span="12" v-if="isDecimal(form.taskParams.outputType)"><a-form-item label="小数位 scale"><a-input-number v-model:value="form.taskParams.scale" :min="0" :max="form.taskParams.precision || 38" :precision="0" /></a-form-item></a-col>
</a-row>
            <a-row>
            <a-col :span="12" >
            <a-form-item label="未匹配处理">
            <a-select v-model:value="form.taskParams.unmatched" style="width: 100%">
            <a-select-option value="KEEP">保留原值</a-select-option>
            <a-select-option value="DEFAULT">使用默认值</a-select-option>
            <a-select-option value="NULL">置为 NULL</a-select-option>
            </a-select>
            </a-form-item>
            </a-col>
</a-row>
      <h3 class="etl-section-title">字段值</h3>
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
              <SortDescendingOutlined />
              <span style="margin-left: 4px">{{ index + 1 }}</span>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'matchType'">
            <a-select v-model:value="record.matchType" style="width: 100%">
            <a-select-option value="VALUE">普通值</a-select-option>
            <a-select-option value="NULL">NULL</a-select-option>
            <a-select-option value="EMPTY">空字符串</a-select-option>
            </a-select></template>
          <template v-else-if="column.dataIndex === 'source'">
            <a-input v-model:value="record.source" :disabled="record.matchType !== 'VALUE'" placeholder="请输入原值" />
          </template>
          <template v-else-if="column.dataIndex === 'target'">
            <a-input v-model:value="record.target" placeholder="请输入目标值" />
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
import { isDecimal, validateDecimal, fieldSchema, withOutputField } from './transformFieldSchema';
import { typeList } from '@/utils/graph.js';
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, SortDescendingOutlined, DeleteOutlined } from "@ant-design/icons-vue";
import CreateEditModal from "../fieldMergeModal.vue";

import FieldConflictDialog from "../fieldDetection.vue";

import {
  defineProps,
  defineEmits,
  ref,
  computed,
  watch,
  getCurrentInstance,
  nextTick,
} from "vue";


import { getLocalNodeUniqueKey as getNodeUniqueKey } from "@/api/col/task/index.js";

import useUserStore from "@/store/system/user.js";

import { createNodeSelect } from "@/views/col/utils/opBase.js";



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
    { title: '匹配类型', dataIndex: 'matchType', width: 160 },
    { title: '原值', dataIndex: 'source', align: 'left' },
    { title: '目标值', dataIndex: 'target', align: 'left' },
  ];
  if (!props.info) {
    cols.push({ title: '操作', key: 'actions', align: 'center', className: 'small-padding fixed-width', fixed: 'right', width: 250 });
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
  // 1. 校验已有行的目标值是否都有填写
  const incompleteRow = tableFields.value.find(
    (row) => (row.matchType === 'VALUE' && (row.source == null || row.source === '')) || row.target == null
  );
  if (incompleteRow) {
    proxy.$message.warning("新增失败，请先填写字段值");
    return;
  }

  // 最后一行名称
  let isRepeat = hasDuplicateMappings(tableFields.value);
  if (isRepeat) {
    proxy.$message.warning("新增失败，请不要填写重复的原值");
    return;
  }

  // 4. 新增字段对象（可以根据需要扩展属性）
  tableFields.value.push({
    columnName: `mapping-${Date.now()}-${tableFields.value.length}`,
    matchType: "VALUE",
    source: "", // 也可以初始化为 nextField.columnName 或其他默认值
    target: "", // 目标值默认空，需用户填写
    order: "asc",
    caseSensitive: false,
    locale: true,
    collatorStrength: 0,
    presorted: false,
  });

  // 5. 重新初始化拖拽排序
  setSort();
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
      setSort();
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

function hasDuplicateMappings(rows) {
  const keys = rows.map(row => `${row.matchType || 'VALUE'}:${row.matchType === 'VALUE' ? row.source : ''}`);
  return new Set(keys).size !== keys.length;
}
function handleDelete(row) {
  const index = tableFields.value.indexOf(row);
  if (index !== -1) tableFields.value.splice(index, 1);
}

const off = () => {
  proxy.resetForm("dpModelRefs");
  dpModelRefs.value?.clearValidate?.();
  tableFields.value = [];
  inputFields.value = [];
  originalTableFieldsBackup.value = [];
};

const saveData = async () => {
  try {
    // 手动检查关键字段
    // 等待 DOM 更新，确保 a-form 内部 state 与 model 同步
    await nextTick();
    // 直接检查关键字段（避免 a-form dot-notation path 校验失效）

    // 校验 tableFields 不为空
    if (!Array.isArray(tableFields.value) || tableFields.value.length === 0) {
      proxy.$message.warning("校验未通过，请至少一个字段值");
      return;
    }

    // 1. 校验已有行的目标值是否都有填写
    const incompleteRow = tableFields.value.find(
      (row) => (row.matchType === 'VALUE' && (row.source == null || row.source === '')) || row.target == null
    );
    if (incompleteRow) {
      proxy.$message.warning("校验未通过，请先填写字段值");
      return;
    }

    // 最后一行名称
    let isRepeat = hasDuplicateMappings(tableFields.value);
    if (isRepeat) {
      proxy.$message.warning("校验未通过，请不要填写重复的原值");
      return;
    }

    // 没有 code 时生成唯一 code
    if (!form.value.code) {
      loading.value = true;
      const response = await getNodeUniqueKey({
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
      });
      loading.value = false;
      form.value.code = response.data;
    }

    if (!form.value.code) return proxy.$message.warning('代码生成失败，请稍后重试');
    const taskParams = form.value?.taskParams || {};
    taskParams.tableFields = tableFields.value;
    taskParams.mainArgs = taskParams.mainArgs || {};

    if (!taskParams.inputField || !taskParams.outputField) return proxy.$message.warning('请填写使用字段和目标字段');
    const schemaError = validateDecimal(taskParams, taskParams.outputType);
    if (schemaError) return proxy.$message.warning(schemaError);
    if ((taskParams.unmatched === 'DEFAULT' || taskParams.castFailure === 'DEFAULT') && taskParams.defaultValue == null) return proxy.$message.warning('请填写默认值');
    const source = inputFields.value.find(field => field.columnName === taskParams.inputField) || {};
    const output = fieldSchema({ ...source, columnName: taskParams.outputField, source: form.value.name }, taskParams.outputType, taskParams);
    taskParams.outputFields = withOutputField(inputFields.value, output);

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
    console.log(e, "deepCopy error");
    return {};
  }
}

let nodeOptions = ref([]);
watch(() => [props.visible, props.currentNode?.id, props.currentNode?.data], () => {
  if (!props.visible) {
    off();
    return;
  }
  const copy = deepCopy(props.currentNode?.data || {});
  // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
  Object.keys(form.value).forEach(k => { delete form.value[k]; });
  Object.assign(form.value, copy);
  nodeOptions.value = createNodeSelect(props.graph, props.currentNode.id);
  // 备份初始表字段，避免被篡改
  originalTableFieldsBackup.value = deepCopy(
    props.currentNode?.data?.taskParams?.inputFields || []
  );
  let taskParams = deepCopy(props.currentNode?.data?.taskParams || {});
  inputFields.value = taskParams?.inputFields || [];
  tableFields.value = (taskParams?.tableFields || []).map((row, index) => ({ ...row, columnName: row.columnName || `mapping-${index}`, matchType: row.matchType || 'VALUE' }));
  const params = form.value.taskParams = form.value.taskParams || {};
  params.inputField = params.inputField || params.selectedSourceField || '';
  params.outputField = params.outputField || params.targetFieldName || '';
  if (params.defaultValue == null && params.defaultValueWhenUnmatched != null) params.defaultValue = params.defaultValueWhenUnmatched;
  params.castFailure = params.castFailure || 'FAIL';
  params.unmatched = params.unmatched || (params.defaultValue != null && params.defaultValue !== '' ? 'DEFAULT' : 'KEEP');
  // 等待 DOM 更新后清除旧的校验状态，避免残留红字
nextTick(() => {
    dpModelRefs.value?.clearValidate();
});
}, { immediate: true });
</script>

<style scoped lang="less">
.blue-text {
  color: #2666fb;
}
</style>

