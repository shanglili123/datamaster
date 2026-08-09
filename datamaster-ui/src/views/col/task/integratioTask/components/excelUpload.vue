<template>
    <a-modal v-model:open="visibleDialog" :draggable="true" width="500px" class="excelUploadDialog-2025-03-28-17-05" :title="title"
        :destroy-on-close="true" :footer="null">
        <a-form ref="daDiscoveryTaskRef" :model="form" :label-col="{ style: { width: '90px' } }" @submit.prevent>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="字段名称" name="columnName"
                        :rules="[{ required: true, message: '请输入字段名称', trigger: 'blur' }]">
                        <a-input v-model:value="form.columnName" placeholder="请输入字段名称" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="字段类型" name="columnType"
                        :rules="[{ required: true, message: '请选择字段类型', trigger: 'change' }]">
                        <a-select v-model:value="form.columnType" placeholder="请选择字段类型">
                            <a-select-option v-for="dict in columntype" :key="dict.value" :label="dict.label"
                                :value="dict.value">{{ dict.label }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24" v-if="form.columnType == 'date'">
                    <a-form-item label="日期格式" name="format"
                        :rules="[{ required: true, message: '请输入日期格式', trigger: 'change' }]">
                        <a-input v-model:value="form.format" placeholder="日期格式如yyyy/MM/dd" />
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>

        <template #footer>
            <div style="text-align: right">
                <a-button @click="closeDialog">关闭</a-button>
                <a-button type="primary" @click="saveData">保存</a-button>
            </div>
        </template>
    </a-modal>
</template>

<script setup>
import { defineProps, defineEmits, ref, computed, watch } from 'vue';
const { proxy } = getCurrentInstance();
const { column_type } = proxy.useDict('column_type');

const props = defineProps({
    visible: { type: Boolean, default: true },
    title: { type: String, default: '' },
    data: { type: Object, default: () => ({}) }
});

const emit = defineEmits(['update:visible', 'confirm']);
// 定义字段类型数组
const columntype = [
    { value: 'long', label: 'long' },
    { value: 'boolean', label: 'boolean' },
    { value: 'string', label: 'string' },
    { value: 'date', label: 'date' },
    { value: 'double', label: 'double' }
];
const form = ref({
    name: '',
    catCode: '',
    executionType: 'PARALLEL',
    crontab: '',
    releaseState: 0,
    description: '',
    catCode: ''
});

watch(
    () => props.visible,
    (newVal) => {
        if (newVal) {
            form.value = JSON.parse(JSON.stringify(props.data || {}));
            console.log('🚀 ~ form.value:', props.data);
        } else {
            proxy.resetForm('daDiscoveryTaskRef');
        }
        console.log('🚀 ~ props.data:', props);
    }
);

// 计算属性处理 v-model
const visibleDialog = computed({
    get() {
        return props.visible;
    },
    set(newValue) {
        emit('update:visible', newValue);
    }
});

// 关闭对话框的方法
const closeDialog = () => {
    emit('update:visible', false);
};
let daDiscoveryTaskRef = ref();
// 保存数据的方法
const saveData = () => {
    daDiscoveryTaskRef.value.validate((valid) => {
        if (valid) {
            if (form.value.columnType !== 'date') {
                form.value.format = '';
            }
            emit('confirm', form.value);
            emit('update:visible', false);
        } else {

            console.log('表单校验未通过');
        }
    });
};
</script>
<style lang="scss">
.excelUploadDialog-2025-03-28-17-05 {
    .ant-modal-body {
        overflow: auto;
        height: 250px !important;
        padding: 20px 40px !important;
    }
}
</style>

