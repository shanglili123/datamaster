<template>
    <a-modal v-model:open="visibleDialog" class="medium-dialog" :title="currentNode?.data?.name" :closable="false"
        :destroy-on-close="true" :confirm-loading="loading" :width="1200">
        <template #title>
            <div class="justify">
                <span class="ant-modal-title">{{ currentNode?.data?.name }}</span>
                <a-tooltip title="将一个字段按分隔符、正则表达式或固定位置拆分为多个输出字段" placement="top">
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
                        <a-form-item label="类型" name="taskParams.typeName">
                            <a-select v-model:value="form.taskParams.typeName" placeholder="请输入类型" show-search disabled>
                                <a-select-option v-for="dict in typeList" :key="dict.value" :label="dict.label"
                                    :value="dict.value" />
                            </a-select>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="拆分字段" name="taskParams.splitField" :rules="[
                            { required: true, message: '请选择拆分字段', trigger: 'change' }
                        ]">
                            <a-select v-if="splitFieldOptions.length > 0" v-model:value="form.taskParams.splitField"
                                placeholder="请选择拆分字段" show-search allow-clear>
                                <a-select-option v-for="item in splitFieldOptions" :key="item" :value="item">
                                    {{ item }}
                                </a-select-option>
                            </a-select>
                            <a-input v-else v-model:value="form.taskParams.splitField" placeholder="请输入拆分字段" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="拆分方式" name="taskParams.splitType" :rules="[
                            { required: true, message: '请选择拆分方式', trigger: 'change' }
                        ]">
                            <a-radio-group v-model:value="form.taskParams.splitType">
                                <a-radio value="delimiter">按分隔符</a-radio>
                                <a-radio value="regex">正则表达式</a-radio>
                                <a-radio value="position">按位置</a-radio>
                            </a-radio-group>
                        </a-form-item>
                    </a-col>
                </a-row>
                <template v-if="form.taskParams.splitType === 'delimiter'">
                    <a-row :gutter="20">
                        <a-col :span="12">
                            <a-form-item label="分隔符" name="taskParams.delimiter" :rules="[
                                { required: true, message: '请输入分隔符', trigger: 'change' }]">
                                <a-input v-model:value="form.taskParams.delimiter" placeholder="请输入分隔符" />
                            </a-form-item>
                        </a-col>
                        <a-col :span="12">
                            <a-form-item label="括符" name="taskParams.enclosure">
                                <a-input v-model:value="form.taskParams.enclosure" placeholder="请输入括符（可选）" />
                            </a-form-item>
                        </a-col>
                    </a-row>
                </template>
                <a-row :gutter="20" v-else-if="form.taskParams.splitType === 'regex'">
                    <a-col :span="12">
                        <a-form-item label="正则表达式" name="taskParams.regex" :rules="[
                            { required: true, message: '请输入正则表达式', trigger: 'change' }]">
                            <a-input v-model:value="form.taskParams.regex" placeholder="请输入正则表达式" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-divider orientation="center">
                    <span class="blue-text">输出字段</span>
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
                <a-table :data-source="tableFields" :columns="tableColumns" :pagination="false" :scroll="{ y: 310 }"
                    :row-key="rowKey">
                    <template #bodyCell="{ column, record }">
                        <template v-if="column.dataIndex === 'index'">
                            <a-input-number v-model:value="record.index" :min="1" :step="1" placeholder="序号"
                                style="width: 100%" />
                        </template>
                        <template v-else-if="column.dataIndex === 'columnName'">
                            <a-input v-model:value="record.columnName" placeholder="请输入输出字段名" />
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
            </div>
        </template>
    </a-modal>
</template>

<script setup>
import { InfoCircleOutlined } from "@ant-design/icons-vue";

import { defineProps, defineEmits, ref, computed, watch, getCurrentInstance } from "vue";

import { typeList } from "@/utils/graph.js";

import { getLocalNodeUniqueKey as getNodeUniqueKey } from "@/api/col/task/index.js";

import useUserStore from "@/store/system/user.js";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();

const props = defineProps({
    visible: { type: Boolean, default: true },
    title: { type: String, default: "表单标题" },
    currentNode: { type: Object, default: () => ({}) },
    info: { type: Boolean, default: false },
    graph: { type: Object, default: () => ({}) },
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

const tableColumns = computed(() => {
    const cols = [
        { title: '序号', dataIndex: 'index', width: 130, align: 'left' },
        { title: '输出字段名', dataIndex: 'columnName', align: 'left' },
    ];
    if (!props.info) {
        cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 100 });
    }
    return cols;
});

const splitFieldOptions = computed(() => {
    const taskParams = form.value?.taskParams || {};
    const sourceFields = [
        ...(taskParams.tableFields || []),
        ...(taskParams.inputFields || []),
    ];
    const names = [];
    sourceFields.forEach((item) => {
        if (item && item.columnName && !names.includes(item.columnName)) {
            names.push(item.columnName);
        }
    });
    return names;
});

let tableFields = ref([]);
let inputFields = ref([]);
let loading = ref(false);
let dpModelRefs = ref();
let form = ref({});

const rowKey = (record, index) => index;

function handleAddField() {
    const nextIndex = tableFields.value.reduce(
        (max, row) => Math.max(max, Number(row.index) || 0),
        0
    ) + 1;
    tableFields.value.push({
        columnName: '',
        index: nextIndex,
    });
}

function handleDelete(row) {
    const idx = tableFields.value.findIndex((item) => item === row);
    if (idx !== -1) {
        tableFields.value.splice(idx, 1);
    }
}

const off = () => {
    proxy.resetForm("dpModelRefs");
    tableFields.value = [];
    inputFields.value = [];
    form.value = {};
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
      if (!Array.isArray(tableFields.value) || tableFields.value.length === 0) {
            proxy.$message.warning("校验未通过，请添加输出字段");
            return;
        }
        const nameSet = new Set();
        for (const row of tableFields.value) {
            if (!row.columnName || !String(row.columnName).trim()) {
                proxy.$message.warning("请填写输出字段名");
                return;
            }
            if (!row.index || Number(row.index) < 1) {
                proxy.$message.warning("请填写序号");
                return;
            }
            if (nameSet.has(row.columnName)) {
                proxy.$message.warning("校验未通过，输出字段名不能重复");
                return;
            }
            nameSet.add(row.columnName);
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
        const splitField = form.value.taskParams.splitField;
        // 输出字段：保留除拆分字段外的输入字段，追加拆分后的输出字段
        taskParams.outputFields = [
            ...(inputFields.value || []).filter((item) => item.columnName !== splitField),
            ...tableFields.value.map((item) => ({
                ...item,
                columnType: 'String',
                source: form.value.name,
            })),
        ];
        taskParams.tableFields = tableFields.value;
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

watch(
    () => [props.visible, props.currentNode?.id],
    () => {
        if (!props.visible) {
            off();
            return;
        }
        const copy = deepCopy(props.currentNode?.data || {});
        // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
        Object.keys(form.value).forEach(k => { delete form.value[k]; });
        Object.assign(form.value, copy);
        if (!form.value.taskParams) {
            form.value.taskParams = {};
        }
        if (!form.value.taskParams.splitType) {
            form.value.taskParams.splitType = 'delimiter';
        }
        inputFields.value = form.value.taskParams.inputFields || [];
        tableFields.value = form.value.taskParams.tableFields || [];
        // 等待 DOM 更新后清除旧的校验状态，避免残留红字
nextTick(() => {
    dpModelRefs.value?.clearValidate();
});
    },
    { immediate: true }
);
</script>

<style scoped lang="less">
.blue-text {
    color: #2666fb;
}
</style>
