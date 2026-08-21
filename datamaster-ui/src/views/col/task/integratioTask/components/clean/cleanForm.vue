<template>
  <a-modal
    v-model:open="visibleDialog"
    :draggable="true"
    class="medium-dialog"
    :title="form.taskParams.typeName"
    :closable="false"
    :destroy-on-close="true"
    :mask-closable="false"
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
      <template v-if="!info">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item
              label="节点名称"
              name="name"
              :rules="[
                {
                  required: true,
                  message: '请输入节点名称',
                  trigger: 'change',
                },
              ]"
            >
              <a-input v-model:value="form.name" placeholder="请输入节点名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类型" name="typeName">
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
                >{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <a-textarea
                v-model:value="form.description"
                :maxlength="500"
                show-count
                placeholder="请输入描述"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="where条件" name="where">
              <a-textarea
                v-model:value="form.taskParams.where"
                :maxlength="500"
                show-count
                placeholder="请输入where条件"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </template>
      <template v-else>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="节点名称:" name="id">
              <div class="form-readonly">
                {{ form.name }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类型" name="typeName">
              <div class="form-readonly">
                {{ form.taskParams.typeName }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <div class="form-readonly textarea">
                {{ form.description ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="where条件" name="where">
              <div class="form-readonly textarea">
                {{ form.where ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </template>
      <div class="h2-title">规则设置</div>

      <div class="justify-between mb15" style="margin-top: 10px" v-if="!info">
        <a-row :gutter="15" class="btn-style">
          <a-col :span="1.5">
            <a-button
              type="primary"
              @click="openRuleSelector(undefined)"
              ><template #icon><PlusOutlined /></template>新增规则</a-button
            >
          </a-col>
        </a-row>
      </div>
      <a-table
        striped
        :pagination="false"
        :data-source="tableFields"
        :loading="loadingList"
        :scroll="{ y: 350 }"
        row-key="name"
        :columns="tableColumns"
        ref="dragTable"
      >
        <template #headerCell="{ column }">
          <template v-if="column.key === 'index'">
            <div class="justify-center">
              <span>序号</span>
              <a-tooltip
                title="清洗规则按照下面配置的列表顺序，依次执行"
                placement="top"
              >
                <InfoCircleOutlined class="tip-icon" />
              </a-tooltip>
            </div>
          </template>
        </template>
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
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
          <template v-else-if="column.key === 'name'">
            {{ record.name || "-" }}
          </template>
          <template v-else-if="column.key === 'columns'">
            {{
              record.columns && record.columns.length
                ? record.columns.join(", ")
                : "-"
            }}
          </template>
          <template v-else-if="column.key === 'ruleName'">
            {{ record.ruleName || "-" }}
          </template>
          <template v-else-if="column.key === 'ruleDescription'">
            {{ record.ruleDescription || "-" }}
          </template>
          <template v-else-if="column.key === 'parentName'">
            {{ record.parentName || "-" }}
          </template>
          <template v-else-if="column.key === 'status'">
            {{ record.status == "1" ? "上线" : "下线" }}
          </template>
          <template v-else-if="column.key === 'actions' && !info">
            <a-button
              type="link"
              size="small"
              @click="openRuleDialog(record, index + 1)"
              >修改</a-button
            >
            <a-button
              type="link"
              size="small"
              danger
              @click="handleRuleDelete(index + 1)"
              ><template #icon><DeleteOutlined /></template>删除</a-button
            >
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
        <a-tooltip
          title="会自动获取资产关联的数据元中的清洗规则"
          placement="top"
          v-if="!info"
        >
          <a-button type="warning" @click="renameRuleToRule">
            <template #icon><ReloadOutlined /></template>
            获取清洗规则
          </a-button>
        </a-tooltip>
      </div>
    </template>
  </a-modal>
  <RuleSelectorDialog
    ref="ruleSelectorDialog"
    @confirm="RuleSelectorconfirm"
    :inputFields="inputFields"
  />
</template>
<script setup>
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, ControlOutlined, DeleteOutlined, ReloadOutlined, PlusOutlined } from "@ant-design/icons-vue";
import { defineProps, defineEmits, ref, computed, watch } from "vue";


import { getLocalNodeUniqueKey as getNodeUniqueKey } from "@/api/col/task/index.js";
const { proxy } = getCurrentInstance();

import Sortable from "sortablejs";

import useUserStore from "@/store/system/user.js";

import {
  createNodeSelect,
  getParentNode,
  renameRuleToRuleConfig,
} from "@/views/col/utils/opBase.js";

import RuleSelectorDialog from "./rule/ruleBase.vue";
const userStore = useUserStore();
const {
  att_rule_clean_type,
  ast_discovery_task_status,
  col_etl_task_execution_type,
} = proxy.useDict(
  "att_rule_clean_type",
  "ast_discovery_task_status",
  "col_etl_task_execution_type"
);
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
        { title: '序号', key: 'index', width: 80, align: 'left' },
        { title: '清洗名称', key: 'name', align: 'left', ellipsis: true, width: 300 },
        { title: '清洗字段', key: 'columns', align: 'left', ellipsis: true, width: 300 },
        { title: '清洗规则', key: 'ruleName', align: 'left', ellipsis: true, width: 300 },
        { title: '规则描述', key: 'ruleDescription', align: 'left', ellipsis: true },
        { title: '维度', key: 'parentName', align: 'left', ellipsis: true, width: 150 },
        { title: '状态', key: 'status', align: 'left' },
    ];
    if (!props.info) {
        cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 180 });
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
          tableFields.value.map((f) => f.name)
        );
      },
    });
  });
}
let ruleSelectorDialog = ref();
const openRuleSelector = (row) => {
  ruleSelectorDialog.value.openDialog(row);
};
const openRuleDialog = (row, index, falg) => {
  ruleSelectorDialog.value.openDialog(row, index, falg);
};
const renameRuleToRule = () => {
  const result = renameRuleToRuleConfig(inputFields.value);
  let coverCount = 0;
  let addCount = 0;

  const norm = (v) =>
    String(v ?? "")
      .trim()
      .toUpperCase();
  const sameCols = (a, b) => {
    if (!Array.isArray(a) || !Array.isArray(b) || a.length !== b.length)
      return false;
    return (
      [...a].map(norm).sort().join("|") === [...b].map(norm).sort().join("|")
    );
  };

  result.forEach((newItem) => {
    // 找到是否存在相同 ruleName 且 columns 一样的旧数据
    const existingIndex = tableFields.value.findIndex(
      (oldItem) =>
        norm(oldItem.ruleName) === norm(newItem.ruleName) &&
        sameCols(oldItem.columns, newItem.columns)
    );

    if (existingIndex > -1) {
      // 覆盖
      tableFields.value[existingIndex] = newItem;
      coverCount++;
    } else {
      // 追加
      tableFields.value.push(newItem);
      addCount++;
    }
  });

  proxy.$message.success(`覆盖 ${coverCount} 条，追加 ${addCount} 条`);
};

function RuleSelectorconfirm(obj, mode) {
  console.log("🚀 ~ RuleSelectorconfirm ~ obj:", obj);
  const index = Number(mode) - 1;
  const list = tableFields.value;
  const isDuplicate = list.some((item, i) => {
    if (index >= 0) {
      return i !== index && item.name == obj.name;
    } else {
      return item.name === obj.name;
    }
  });

  if (isDuplicate) {
    proxy.$message.warning("清洗名称不能重复！");
    return;
  }

  if (!isNaN(index) && index >= 0 && index < list.length) {
    list.splice(index, 1, obj);
  } else {
    list.push(obj);
  }

  tableFields.value = list;
  ruleSelectorDialog.value.closeDialog();
  setSort();
}
function handleRuleDelete(index) {
  tableFields.value.splice(Number(index) - 1, 1);
  setSort();
}
// 输入字段
let inputFields = ref([]);
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
// 变量定义
let loading = ref(false);
let loadingList = ref(false);
let opens = ref(false);
let row = ref();
let TablesByDataSource = ref([]);
let ColumnByAssettab = ref([]);
let dpModelRefs = ref();
let form = ref({});

function handleRule(data) {
  row.value = {};
  row.value = data;
  opens.value = true;
}
const submitForm = (value) => {
  if (row.value?.index) {
    tableFields.value[row.value.index - 1] = {
      ...tableFields.value[row.value.index - 1],
      cleanRuleList: value,
      elementId: value.map((item) => item.ruleId),
    };

    opens.value = false;
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
    taskParams.tableFields = tableFields.value;
    taskParams.outputFields = inputFields.value;
    emit("confirm", form.value);
  } catch (error) {
    console.error("保存数据失败:", error);
    loading.value = false;
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
let nodeOptions = ref([]);

// 监听属性变化
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
  inputFields.value = props.currentNode?.data?.taskParams?.inputFields;
  tableFields.value = props.currentNode?.data?.taskParams?.tableFields;
  setSort();
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

