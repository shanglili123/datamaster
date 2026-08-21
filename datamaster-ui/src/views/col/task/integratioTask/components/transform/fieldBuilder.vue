<template>
    <a-modal v-model:open="visibleDialog" class="medium-dialog" :title="currentNode?.data?.name" :closable="false"
        :destroy-on-close="true" :width="1200">
        <template #title>
            <div class="justify">
                <span class="ant-modal-title">{{ currentNode?.data?.name }}</span>
                <a-tooltip title="用于通过拼接多个字段值生成新字段，支持设置前缀、后缀和连接符，常用于构造唯一标识或业务编码" placement="top">
                    <InfoCircleOutlined class="tip-icon" />
                </a-tooltip>
            </div>
        </template>
        <a-spin :spinning="loading">
        <a-form ref="dpModelRefs" :model="form" :label-col="{ style: { width: '140px' } }" @submit.prevent
            :disabled="info">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="节点名称" name="name"
                        :rules="[{ required: true, message: '请输入节点名称', trigger: 'change' }]">
                        <a-input v-model:value="form.name" placeholder="请输入节点名称" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="类型" name="typeName">
                        <a-select v-model:value="form.taskParams.typeName" placeholder="请输入类型" show-search disabled>
                            <a-select-option v-for="dict in typeList" :key="dict.value" :label="dict.label"
                                :value="dict.value" />
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="操作类型" name="taskParams.fieldDerivationType" :rules="[
                        { required: true, message: '请输入操作类型', trigger: 'change' }
                    ]">
                        <a-select v-model:value="form.taskParams.fieldDerivationType" placeholder="请选择操作类型">
                            <a-select-option v-for="item in deriveFieldTypes" :key="item.value" :label="item.label"
                                :value="item.value" :disabled="!['FIELD_DERIVE_CONCAT', 'FIELD_DERIVE_SUBSTRING'].includes(item.value)" />
                        </a-select>
                    </a-form-item>
                </a-col>
                <a-col :span="12" v-if="['FIELD_DERIVE_CONCAT', 'FIELD_DERIVE_SUBSTRING'].includes(form.taskParams.fieldDerivationType)">
                    <a-form-item label="新增字段名称" name="taskParams.fieldDerivationName" :rules="[
                        { required: true, message: '请输入新增字段名称', trigger: 'change' }]">
                        <template #label>
                            <div class="justify-center">
                                <span>新增字段名称</span>
                                <a-tooltip title="生成结果将写入该字段，作为新列追加到数据中" placement="top">
                                    <InfoCircleOutlined class="tip-icon" />
                                </a-tooltip>
                            </div>
                        </template>
                        <a-input v-model:value="form.taskParams.fieldDerivationName" placeholder="请输入新增字段名称" />
                    </a-form-item>
                </a-col>
            </a-row>
            <template v-if="form.taskParams.fieldDerivationType == 'FIELD_DERIVE_CONCAT'">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="前缀" name="taskParams.fieldDerivationPrefix">
                        <a-input v-model:value="form.taskParams.fieldDerivationPrefix" placeholder="请输入后缀" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="后缀" name="taskParams.fieldDerivationSuffix">
                        <a-input v-model:value="form.taskParams.fieldDerivationSuffix" placeholder="请输入后缀" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="连接符" name="taskParams.delimiter">
                        <a-input v-model:value="form.taskParams.delimiter" placeholder="请输入连接符" />
                    </a-form-item>
                </a-col>
            </a-row>
            </template>
            <template v-else-if="form.taskParams.fieldDerivationType == 'FIELD_DERIVE_SUBSTRING'">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="截取方向" name="taskParams.direction">
                        <a-select v-model:value="form.taskParams.direction" placeholder="请选择截取方向">
                            <a-select-option value="1">从左截取</a-select-option>
                            <a-select-option value="2">从右截取</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="起始位置" name="taskParams.startIndex">
                        <a-input-number v-model:value="form.taskParams.startIndex" :min="0" style="width: 100%"
                            placeholder="0 表示从第 1 位开始"
                        />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="结束位置" name="taskParams.endIndex">
                        <a-input-number v-model:value="form.taskParams.endIndex" :min="0" style="width: 100%"
                            placeholder="留空表示截取到末尾"
                        />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="生成规则预览" name="preview">
                        <a-input :value="substringPreview" readonly />
                    </a-form-item>
                </a-col>
            </a-row>
            </template>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="描述" name="description">
                        <a-input v-model:value="form.description" type="textarea" placeholder="请输入描述" />
                    </a-form-item>
                </a-col>
            </a-row>
            <div class="mb10" v-if="tableFields.length > 0"
                style="display: flex; align-items: flex-start; margin-left: 38px;">
                <span style="font-weight: 500; white-space: nowrap; margin-right: 10px;">生成规则</span>
                <div v-html="expressionPreviewHtml" style="flex: 1; white-space: pre-wrap;"></div>
            </div>
            <a-divider orientation="center">
                <span class="blue-text">字段值</span>
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
            <a-table :loading="loadingList" :data-source="tableFields" :columns="tableColumns"
                :pagination="false" :scroll="{ y: 310 }" :row-key="'columnName'" ref="dragTable">
                <template #bodyCell="{ column, record, index }">
                    <template v-if="column.dataIndex === 'index'">
                        <div class="allowDrag"
                            style="cursor: move; display: flex; justify-content: center; align-items: center;">
                            <ControlOutlined />
                            <span style="margin-left: 4px;">{{ index + 1 }}</span>
                        </div>
                    </template>
                    <template v-else-if="column.dataIndex === 'columnName'">
                        <a-select v-model:value="record.columnName" placeholder="请选择字段" style="flex: 1">
                            <a-select-option v-for="item in inputFields" :key="item.value" :label="item.label"
                                :value="item.columnName" :disabled="isOptionDisabled(item.columnName, record)" />
                        </a-select>
                    </template>
                    <template v-else-if="column.key === 'actions'">
                        <a-button type="link" danger size="small" @click="handleDelete(record)">
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
                <a-button type="primary" @click="saveData" v-if="!info">保存</a-button>
                <!--  <a-button type="warning" @click="handleFetchFields"  v-if="!info">获取字段</a-button> -->
            </div>
        </template>
    </a-modal>

    <FieldConflictDialog v-model="showConflictDialog" :existingFields="tableFields" :newFields="inputFields"
        @resolve="onResolveFields" />
    <CreateEditModal :visibleDialogs="opens" @update:visibleDialogs="opens = $event" @confirm="submitForm" :row="row"
        :tableFields="tableFields" :inputFields="inputFields" />
</template>

<script setup>
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, ControlOutlined } from "@ant-design/icons-vue";
import CreateEditModal from "../fieldMergeModal.vue";

import FieldConflictDialog from "../fieldDetection.vue";

import { defineProps, defineEmits, ref, computed, watchEffect, getCurrentInstance } from "vue";

import { typeList } from "@/utils/graph.js";

import { getLocalNodeUniqueKey as getNodeUniqueKey } from "@/api/col/task/index.js";

import useUserStore from "@/store/system/user.js";

import { createNodeSelect, getParentNode } from "@/views/col/utils/opBase.js";

import draggable from "vuedraggable";

import Sortable from "sortablejs";
const { proxy } = getCurrentInstance();
const userStore = useUserStore();

const tableColumns = [
    { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
    { title: '字段名称', dataIndex: 'columnName', align: 'left' },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 150 },
];
const expressionPreviewHtml = computed(() => {
    if (form.value?.taskParams?.fieldDerivationType === 'FIELD_DERIVE_SUBSTRING') {
        const field = tableFields.value.map(f => f.columnName).filter(Boolean)[0];
        if (!field) return '';
        const direction = form.value?.taskParams?.direction || '1';
        const start = form.value?.taskParams?.startIndex ?? 0;
        const end = form.value?.taskParams?.endIndex;
        if (direction === '2') {
            return end != null
                ? `<span class="var">SUBSTRING(${field}, LENGTH(${field}) - ${start} + 1, ${Math.max(1, end - start)})</span>`
                : `<span class="var">SUBSTRING(${field}, LENGTH(${field}) - ${start} + 1)</span>`;
        }
        return end != null
            ? `<span class="var">SUBSTRING(${field}, ${start + 1}, ${Math.max(1, end - start)})</span>`
            : `<span class="var">SUBSTRING(${field}, ${start + 1})</span>`;
    }
    const prefix = form.value?.taskParams?.fieldDerivationPrefix || '';
    const suffix = form.value?.taskParams?.fieldDerivationSuffix || '';
    const delimiter = form.value?.taskParams?.delimiter || '';
    const fields = tableFields.value.map(f => f.columnName).filter(Boolean);

    if (fields.length === 0) return '';
    const parts = [];
    // 添加前缀（常量）
    if (prefix) {
        parts.push(`<span class="const">"${prefix}"</span>`);
        parts.push(`<span class="op"> + </span>`);
    }
    // 字段拼接
    fields.forEach((field, idx) => {
        if (idx > 0 && delimiter) {
            parts.push(`<span class="const">"${delimiter}"</span>`);
            parts.push(`<span class="op"> + </span>`);
        }
        parts.push(`<span class="var">\${${field}}</span>`);
        if (idx < fields.length - 1) {
            parts.push(`<span class="op"> + </span>`);
        }
    });
    // 添加后缀（常量）
    if (suffix) {
        parts.push(`<span class="op"> + </span>`);
        parts.push(`<span class="const">"${suffix}"</span>`);
    }

    return parts.join('');
});

const substringPreview = computed(() => {
    if (form.value?.taskParams?.fieldDerivationType !== 'FIELD_DERIVE_SUBSTRING') return '';
    const field = tableFields.value.map(f => f.columnName).filter(Boolean)[0];
    if (!field) return '请先选择字段';
    const direction = form.value?.taskParams?.direction || '1';
    const start = form.value?.taskParams?.startIndex ?? 0;
    const end = form.value?.taskParams?.endIndex;
    if (direction === '2') {
        return end != null
            ? `SUBSTRING(${field}, LENGTH(${field}) - ${start} + 1, ${Math.max(1, end - start)})`
            : `SUBSTRING(${field}, LENGTH(${field}) - ${start} + 1)`;
    }
    return end != null
        ? `SUBSTRING(${field}, ${start + 1}, ${Math.max(1, end - start)})`
        : `SUBSTRING(${field}, ${start + 1})`;
});

const props = defineProps({
    visible: { type: Boolean, default: true },
    title: { type: String, default: "表单标题" },
    currentNode: { type: Object, default: () => ({}) },
    info: { type: Boolean, default: false },
    graph: { type: Object, default: () => ({}) },
});
const deriveFieldTypes = [
    { value: 'FIELD_DERIVE_CONCAT', label: '拼接' },
    { value: 'FIELD_DERIVE_SUBSTRING', label: '截取' },
    { value: 'FIELD_DERIVE_REPLACE', label: '替换' },
    { value: 'FIELD_DERIVE_EXPRESSION', label: '表达式' },
    { value: 'FIELD_DERIVE_HASH', label: '哈希' },
    { value: 'FIELD_DERIVE_REGEX', label: '正则提取' },
    { value: 'FIELD_DERIVE_CONSTANT', label: '常量赋值' }
]
let dragTable = ref(null);
let sortableInstance = null;
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
                console.log("拖拽后顺序:", tableFields.value.map((f) => f.columnName));
            },
        });
    });
}

function handleAddField() {
    if (!Array.isArray(inputFields.value) || inputFields.value.length === 0) {
        proxy.$message.warning("输入字段为空，无法添加字段");
        return;
    }
    // SUBSTRING 截取只支持单字段
    if (form.value?.taskParams?.fieldDerivationType === 'FIELD_DERIVE_SUBSTRING' && tableFields.value.length > 0) {
        proxy.$message.warning("截取操作仅支持选择一个字段");
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
        order: 'asc',
        caseSensitive: false,
        locale: true,
        collatorStrength: 0,
        presorted: false
    });
    setSort()

}
const showConflictDialog = ref(false);

const handleFetchFields = () => {
    const tableNames = tableFields.value.map(f => f.columnName).sort();
    const inputNames = inputFields.value.map(f => f.columnName).sort();

    if (
        tableNames.length === inputNames.length &&
        tableNames.every((name, idx) => name === inputNames[idx])
    ) {
        return proxy.$message.warning("新增失败，当前已是最新字段");
    }
    showConflictDialog.value = true;
};
function onResolveFields(payload) {
    if (!payload || !payload.action) return;

    switch (payload.action) {
        case "addNewOnly": {
            console.log("父组件：只增加新字段");

            // 计算已有字段名称
            const existingNames = tableFields.value.map(f => f.columnName);
            // 找到新字段中不在已有字段中的字段
            const newUniqueFields = inputFields.value.filter(
                f => !existingNames.includes(f.columnName)
            );
            // 加入到 tableFields 中
            tableFields.value = tableFields.value.concat(deepCopy(newUniqueFields));
            break;
        }

        case "addAll": {
            console.log("🚀 ~ onResolveFields ~  tableFields.value =:", tableFields.value)

            console.log("父组件：增加所有字段");
            tableFields.value = []
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
    setSort()
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
    if (!form.value.code) {
      loading.value = true;
      const response = await getNodeUniqueKey({
        spaceCode: userStore.spaceCode || "133545087166112",
        spaceId: userStore.spaceId,
      });
      loading.value = false;
      form.value.code = response.data;
    }
    const taskParams = form.value?.taskParams || {};
    taskParams.tableFields = tableFields.value;
    console.log("🚀 ~ saveData ~  form.value.taskParams.fieldDerivationName:", form.value.taskParams.fieldDerivationName)
    // 输出字段拼接目标字段
    taskParams.outputFields = [
      ...inputFields.value,
      {
        columnName: form.value.taskParams.fieldDerivationName,
        source: form.value.name
      }
    ];
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
    const copy = deepCopy(props.currentNode?.data || {});
    // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
    Object.keys(form.value).forEach(k => { delete form.value[k]; });
    Object.assign(form.value, copy);
    nodeOptions.value = createNodeSelect(props.graph, props.currentNode.id);
    // 备份初始表字段，避免被篡改
    originalTableFieldsBackup.value = deepCopy(
        props.currentNode?.data?.taskParams?.inputFields || []
    );
    inputFields.value = props.currentNode?.data?.taskParams?.inputFields;
    tableFields.value = props.currentNode?.data?.taskParams?.tableFields || [];
    setSort()
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

::v-deep(.var),
::v-deep(.const),
::v-deep(.op) {
    color: #000;
    font-weight: 700;
}
</style>

