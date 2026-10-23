<template>
    <a-modal wrap-class-name="integration-component-modal" v-model:open="visibleDialog" class="medium-dialog" :title="currentNode?.data?.name" :closable="false"
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
            <h3 class="etl-section-title">基本信息</h3>
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
                                :value="item.value" />
                        </a-select>
                    </a-form-item>
                </a-col>
                    <a-col :span="12" v-if="form.taskParams.fieldDerivationType">
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
<a-row :gutter="20">
            <a-col :span="12" >
            <a-form-item label="结果类型">
            <a-select v-model:value="form.taskParams.resultType" style="width: 100%">
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
<a-col :span="12" v-if="isDecimal(form.taskParams.resultType)"><a-form-item label="总位数 precision"><a-input-number v-model:value="form.taskParams.precision" :min="1" :max="38" :precision="0" /></a-form-item></a-col>
<a-col :span="12" v-if="isDecimal(form.taskParams.resultType)"><a-form-item label="小数位 scale"><a-input-number v-model:value="form.taskParams.scale" :min="0" :max="form.taskParams.precision || 38" :precision="0" /></a-form-item></a-col>
</a-row><a-row :gutter="20"><a-col :span="12" ><a-form-item label="结果可空"><a-switch v-model:checked="form.taskParams.nullable" /></a-form-item></a-col>
<a-col :span="12" v-if="form.taskParams.castFailure === 'DEFAULT' || form.taskParams.fieldDerivationType === 'FIELD_DERIVE_NULL'"><a-form-item label="默认值"><a-input v-model:value="form.taskParams.defaultValue" placeholder="允许空字符串" /></a-form-item></a-col>
<a-col :span="12" v-if="form.taskParams.fieldDerivationType === 'FIELD_DERIVE_CONCAT'">
            <a-form-item label="拼接空值处理">
            <a-select v-model:value="form.taskParams.nullMode" style="width: 100%">
            <a-select-option value="KEEP">保留 NULL（结果为 NULL）</a-select-option>
            <a-select-option value="SKIP">跳过空值</a-select-option>
            <a-select-option value="EMPTY">按空字符串</a-select-option>
            <a-select-option value="NULL_LITERAL">按文本 null（兼容旧配置）</a-select-option>
            </a-select>
            </a-form-item>
            </a-col>
<a-col :span="12" v-if="form.taskParams.fieldDerivationType === 'FIELD_DERIVE_NULL'">
            <a-form-item label="空值操作">
            <a-select v-model:value="form.taskParams.nullAction" style="width: 100%">
            <a-select-option value="COALESCE">首个非空值</a-select-option>
            <a-select-option value="DEFAULT">默认值替换</a-select-option>
            <a-select-option value="EMPTY_TO_NULL">空字符串转 NULL</a-select-option>
            </a-select>
            </a-form-item>
            </a-col>
<a-col :span="12" v-if="form.taskParams.fieldDerivationType === 'FIELD_DERIVE_NULL'"><a-form-item label="空串视为 NULL"><a-switch v-model:checked="form.taskParams.normalizeEmpty" /></a-form-item></a-col>
<a-col :span="12" v-if="form.taskParams.fieldDerivationType === 'FIELD_DERIVE_NULL'"><a-form-item label="去除首尾空白"><a-switch v-model:checked="form.taskParams.trimWhitespace" /></a-form-item></a-col>
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
            <template v-else-if="form.taskParams.fieldDerivationType == 'FIELD_DERIVE_REPLACE'">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="查找文本" name="taskParams.searchValue" :rules="[{ required: true, message: '请输入查找文本', trigger: 'change' }]">
                        <a-input v-model:value="form.taskParams.searchValue" placeholder="请输入要替换的文本" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="替换文本" name="taskParams.replaceValue" :rules="[{ required: true, message: '请输入替换文本', trigger: 'change' }]">
                        <a-input v-model:value="form.taskParams.replaceValue" placeholder="请输入替换后的文本" />
                    </a-form-item>
                </a-col>
            </a-row>
            </template>
            <template v-else-if="form.taskParams.fieldDerivationType == 'FIELD_DERIVE_EXPRESSION'">
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="表达式" name="taskParams.expression" :rules="[{ required: true, message: '请输入表达式', trigger: 'change' }]">
                        <a-textarea v-model:value="form.taskParams.expression" :rows="2" placeholder="支持 ${字段名} 占位符，例如 CONCAT(${first_name}, ' ', ${last_name})" />
                    </a-form-item>
                </a-col>
            </a-row>
            </template>
            <template v-else-if="form.taskParams.fieldDerivationType == 'FIELD_DERIVE_HASH'">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="哈希算法" name="taskParams.algorithm" :rules="[{ required: true, message: '请选择哈希算法', trigger: 'change' }]">
                        <a-select v-model:value="form.taskParams.algorithm" placeholder="请选择哈希算法">
                            <a-select-option value="MD5">MD5</a-select-option>
                            <a-select-option value="SHA-1">SHA-1</a-select-option>
                            <a-select-option value="SHA-256">SHA-256</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            </template>
            <template v-else-if="form.taskParams.fieldDerivationType == 'FIELD_DERIVE_REGEX'">
            <a-row :gutter="20">
                <a-col :span="16">
                    <a-form-item label="正则表达式" name="taskParams.regex" :rules="[{ required: true, message: '请输入正则表达式', trigger: 'change' }]">
                        <a-input v-model:value="form.taskParams.regex" placeholder="例如 ^(\\w+)-" />
                    </a-form-item>
                </a-col>
                <a-col :span="8">
                    <a-form-item label="捕获组" name="taskParams.group">
                        <a-input-number v-model:value="form.taskParams.group" :min="0" style="width: 100%" />
                    </a-form-item>
                </a-col>
            </a-row>
            </template>
            <template v-else-if="form.taskParams.fieldDerivationType == 'FIELD_DERIVE_CONSTANT'">
            <a-row :gutter="20">
                <a-col :span="16">
                    <a-form-item label="常量值" name="taskParams.constantValue" :rules="[{ required: true, message: '请输入常量值', trigger: 'change' }]">
                        <a-input v-model:value="form.taskParams.constantValue" placeholder="请输入要写入的新字段值" />
                    </a-form-item>
                </a-col>
                <a-col :span="8">
                    <a-form-item label="类型" name="taskParams.constantType">
                        <a-select v-model:value="form.taskParams.constantType" @change="form.taskParams.resultType = $event" placeholder="字符串">
                            <a-select-option value="string">字符串</a-select-option>
                            <a-select-option value="int">整数</a-select-option>
                            <a-select-option value="double">小数</a-select-option>
                            <a-select-option value="boolean">布尔</a-select-option>
                        </a-select>
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
            <h3 class="etl-section-title">字段值</h3>
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
import { isDecimal, validateDecimal, fieldSchema, withOutputField } from './transformFieldSchema';
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, ControlOutlined } from "@ant-design/icons-vue";
import CreateEditModal from "../fieldMergeModal.vue";

import FieldConflictDialog from "../fieldDetection.vue";

import { defineProps, defineEmits, ref, computed, watch, getCurrentInstance, nextTick } from "vue";

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
    { value: 'FIELD_DERIVE_CONSTANT', label: '常量赋值' },
    { value: 'FIELD_DERIVE_NULL', label: '空值处理' }
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
    // 单字段派生操作只允许选择一个输入字段。
    if (['FIELD_DERIVE_SUBSTRING', 'FIELD_DERIVE_REPLACE', 'FIELD_DERIVE_HASH', 'FIELD_DERIVE_REGEX'].includes(form.value?.taskParams?.fieldDerivationType)
        && tableFields.value.length > 0) {
        proxy.$message.warning("当前操作仅支持选择一个字段");
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
    const selectedType = form.value?.taskParams?.fieldDerivationType;
    // 常量赋值不依赖源字段，其它派生类型至少要选择一个输入字段。
    if (selectedType !== 'FIELD_DERIVE_CONSTANT' && (!tableFields.value || tableFields.value.length === 0)) {
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
    if (!form.value?.code) {
      return proxy.$message.warning('代码生成失败，请稍后重试');
    }
    const taskParams = form.value?.taskParams || {};
    const type = taskParams.fieldDerivationType;
    if (['FIELD_DERIVE_SUBSTRING', 'FIELD_DERIVE_REPLACE', 'FIELD_DERIVE_HASH', 'FIELD_DERIVE_REGEX'].includes(type)
        && tableFields.value.length > 1) {
      return proxy.$message.warning('当前操作只支持一个输入字段');
    }
    if (!taskParams.fieldDerivationName) {
      return proxy.$message.warning('请输入新增字段名称');
    }
    if (type === 'FIELD_DERIVE_REPLACE' && (taskParams.searchValue == null || taskParams.replaceValue == null)) {
      return proxy.$message.warning('请填写查找文本和替换文本');
    }
    if (type === 'FIELD_DERIVE_EXPRESSION' && !taskParams.expression) {
      return proxy.$message.warning('请输入表达式');
    }
    if (type === 'FIELD_DERIVE_REGEX' && !taskParams.regex) {
      return proxy.$message.warning('请输入正则表达式');
    }
    if (type === 'FIELD_DERIVE_CONSTANT' && taskParams.constantValue == null) {
      return proxy.$message.warning('请输入常量值');
    }
    const schemaError = validateDecimal(taskParams, taskParams.resultType);
    if (schemaError) return proxy.$message.warning(schemaError);
    if ((taskParams.castFailure === 'DEFAULT' || (type === 'FIELD_DERIVE_NULL' && taskParams.nullAction === 'DEFAULT')) && taskParams.defaultValue == null) return proxy.$message.warning('请填写默认值');
    taskParams.tableFields = tableFields.value;
    const output = fieldSchema({ columnName: taskParams.fieldDerivationName, source: form.value.name }, taskParams.resultType, taskParams);
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
    form.value.taskParams = form.value.taskParams || {};
    const params = form.value.taskParams;
    params.resultType = params.resultType || (params.fieldDerivationType === 'FIELD_DERIVE_CONSTANT' ? params.constantType || 'string' : 'string');
    params.nullable = params.nullable == null ? true : params.nullable;
    params.castFailure = params.castFailure || 'FAIL';
    params.nullMode = params.nullMode || 'NULL_LITERAL';
    params.nullAction = params.nullAction || 'COALESCE';
    nodeOptions.value = createNodeSelect(props.graph, props.currentNode.id);
    // 备份初始表字段，避免被篡改
    originalTableFieldsBackup.value = deepCopy(
        props.currentNode?.data?.taskParams?.inputFields || []
    );
    inputFields.value = deepCopy(params.inputFields || []);
    tableFields.value = deepCopy(params.tableFields || []);
    setSort()
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

::v-deep(.var),
::v-deep(.const),
::v-deep(.op) {
    color: #000;
    font-weight: 700;
}
</style>

