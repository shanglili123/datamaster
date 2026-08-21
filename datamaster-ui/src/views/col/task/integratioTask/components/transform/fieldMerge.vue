<template>
    <a-modal v-model:open="visibleDialog" class="medium-dialog" :closable="false"
        :destroy-on-close="true" :confirm-loading="loading" ok-text="确定" cancel-text="取消"
        @ok="saveData" @cancel="closeDialog" :width="1200">
        <template #title>
            <div class="justify">
                <span class="ant-modal-title">字段合并</span>
                <a-tooltip title="用于将多个字段值按指定分隔符合并为一个新字段，支持空值处理和空格处理" placement="top">
                    <InfoCircleOutlined class="tip-icon" />
                </a-tooltip>
            </div>
        </template>
        <a-spin :spinning="loading">
        <a-form ref="formRef" :model="form" :label-col="{ style: { width: '140px' } }" @submit.prevent>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="节点名称" name="name"
                        :rules="[{ required: true, message: '请输入节点名称', trigger: 'change' }]">
                        <a-input v-model:value="form.name" placeholder="请输入节点名称" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="合并字段名" name="taskParams.mergeFieldName" :rules="[
                        { required: true, message: '请输入合并字段名', trigger: 'change' }]">
                        <template #label>
                            <div class="justify-center">
                                <span>合并字段名</span>
                                <a-tooltip title="合并结果将写入该新字段，作为新列追加到数据中" placement="top">
                                    <InfoCircleOutlined class="tip-icon" />
                                </a-tooltip>
                            </div>
                        </template>
                        <a-input v-model:value="form.taskParams.mergeFieldName" placeholder="请输入合并字段名" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="分隔符" name="taskParams.delimiter" :rules="[
                        { required: true, message: '请输入分隔符', trigger: 'change' }]">
                        <a-input v-model:value="form.taskParams.delimiter" placeholder="请输入分隔符" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="空值处理" name="taskParams.handleNull" :rules="[
                        { required: true, message: '请选择空值处理方式', trigger: 'change' }]">
                        <a-select v-model:value="form.taskParams.handleNull" placeholder="请选择空值处理方式">
                            <a-select-option value="1">忽略空值</a-select-option>
                            <a-select-option value="2">保留空值</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="空格处理" name="taskParams.trimSpace" :rules="[
                        { required: true, message: '请选择空格处理方式', trigger: 'change' }]">
                        <a-select v-model:value="form.taskParams.trimSpace" placeholder="请选择空格处理方式">
                            <a-select-option value="1">去除首尾空格</a-select-option>
                            <a-select-option value="2">不去除</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="描述" name="description">
                        <a-input v-model:value="form.description" type="textarea" placeholder="请输入描述" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-divider orientation="center">
                <span class="blue-text">合并字段（源字段）</span>
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
            <a-table :data-source="tableFields" :columns="tableColumns"
                :pagination="false" :scroll="{ y: 310 }" :row-key="'columnName'">
                <template #bodyCell="{ column, record, index }">
                    <template v-if="column.dataIndex === 'checked'">
                        <a-checkbox v-model:checked="record.checked" />
                    </template>
                    <template v-else-if="column.dataIndex === 'index'">
                        <span>{{ index + 1 }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'columnName'">
                        <a-select v-model:value="record.columnName" placeholder="请选择字段" style="flex: 1" show-search>
                            <a-select-option v-for="item in inputFields" :key="item.columnName" :label="item.columnName"
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
    </a-modal>
</template>

<script setup>
import { InfoCircleOutlined } from "@ant-design/icons-vue";
import { defineProps, defineEmits, ref, computed, watch, getCurrentInstance } from "vue";
import { getLocalNodeUniqueKey as getNodeUniqueKey } from "@/api/col/task/index.js";
import useUserStore from "@/store/system/user.js";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();

const props = defineProps({
    visible: { type: Boolean, default: true },
    currentNode: { type: Object, default: () => ({}) },
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

let form = ref({});
let formRef = ref();
let loading = ref(false);
let tableFields = ref([]);
let inputFields = ref([]);

const tableColumns = [
    { title: '选择', dataIndex: 'checked', width: 70, align: 'center' },
    { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
    { title: '字段名称', dataIndex: 'columnName', align: 'left' },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 150 },
];

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
        checked: true,
    });
}

function handleDelete(row) {
    const idxTable = tableFields.value.findIndex(
        (item) => item.columnName === row.columnName
    );
    if (idxTable !== -1) {
        tableFields.value.splice(idxTable, 1);
    }
}

const isOptionDisabled = (optionValue, currentRow) => {
    return tableFields.value.some(
        (row) => row !== currentRow && row.columnName === optionValue
    );
};

const off = () => {
    proxy.resetForm("formRef");
    tableFields.value = [];
    inputFields.value = [];
};

const saveData = async () => {
    try {
        await nextTick();
        // 手动校验合并字段名
        if (!form.value?.taskParams?.mergeFieldName) {
            proxy.$message.warning("请输入合并字段名");
            return;
        }
        // 参与合并的字段（已勾选且已选择字段名）
        const mergeFields = tableFields.value.filter(
            (field) => field.checked && field.columnName
        );
        if (mergeFields.length === 0) {
            proxy.$message.warning("校验未通过，请选择合并字段");
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
        taskParams.mergeFieldName = form.value.taskParams.mergeFieldName;
        taskParams.delimiter = form.value.taskParams.delimiter;
        taskParams.handleNull = form.value.taskParams.handleNull || '1';
        taskParams.trimSpace = form.value.taskParams.trimSpace || '1';
        taskParams.tableFields = mergeFields.map(({ columnName }) => ({ columnName }));
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

// 监听属性变化
watch(
    () => [props.visible, props.currentNode?.id],
    ([visible]) => {
        if (visible) {
            const nodeData = props.currentNode?.getProp?.("data") || props.currentNode?.data || {};
            const copy = deepCopy(nodeData);
            // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
            Object.keys(form.value).forEach(k => { delete form.value[k]; });
            Object.assign(form.value, copy);
            form.value.taskParams = form.value.taskParams || {};
            form.value.taskParams.mergeFieldName = form.value.taskParams.mergeFieldName || '';
            form.value.taskParams.delimiter = form.value.taskParams.delimiter || '';
            form.value.taskParams.handleNull = form.value.taskParams.handleNull || '1';
            form.value.taskParams.trimSpace = form.value.taskParams.trimSpace || '1';
            const savedFields = Array.isArray(form.value.taskParams.tableFields)
                ? form.value.taskParams.tableFields
                : [];
            inputFields.value = (Array.isArray(form.value.taskParams.inputFields) && form.value.taskParams.inputFields.length > 0)
                ? form.value.taskParams.inputFields
                : savedFields;
            tableFields.value = savedFields.map((field) => ({
                columnName: field.columnName,
                checked: true,
            }));
            // 等待 DOM 更新后清除旧的校验状态，避免残留红字
            nextTick(() => {
                formRef.value?.clearValidate();
                
            });
        } else {
            off();
        }
    },
    { immediate: true }
);
</script>

<style scoped lang="less">
.blue-text {
    color: #2666fb;
}
</style>
