<template>
    <!-- 矢量数据 -->
    <div v-if="visible">
        <a-form ref="dpModelRefs" :model="form" :label-col="{ style: { width: '110px' } }" @submit.prevent>
            <a-row :gutter="20" v-if="
                form.assetsAssetFiles.url != undefined &&
                form.assetsAssetFiles.url.indexOf('.xls') == -1 &&
                form.assetsAssetFiles.url.indexOf('.xlsx') == -1
            ">
                <a-col :span="12">
                    <a-form-item label="上传附件" name="assetsAssetFiles.url"
                        :rules="[{ required: true, message: '请上传附件', trigger: 'change' }]">
                        <FileUploadbtn :limit="1" v-model="form.assetsAssetFiles.url" :dragFlag="false" :file-type="[]"
                            :fileSize="50" @handleRemove="handleRemove" @customEvent="dataFileName" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-button v-if="form.assetsAssetFiles.url.indexOf('.csv') != -1" type="primary"
                        @click="parseExcel" style="margin-left: 60px" :disabled="isButtonDisabled">
                        解析CSV
                    </a-button>
                </a-col>
            </a-row>
            <a-row :gutter="20" v-if="
                form.assetsAssetFiles.url == undefined ||
                form.assetsAssetFiles.url.indexOf('.xls') != -1 ||
                form.assetsAssetFiles.url.indexOf('.xlsx') != -1
            ">
                <a-col :span="12">
                    <a-form-item label="上传附件" name="assetsAssetFiles.url"
                        :rules="[{ required: true, message: '请上传附件', trigger: 'change' }]">
                        <FileUploadbtn :limit="1" v-model="form.assetsAssetFiles.url" :dragFlag="false" :file-type="[]"
                            :fileSize="50" @handleRemove="handleRemove" @customEvent="dataFileName" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="起始行" name="assetsAssetFiles.startData"
                        :rules="[{ required: true, message: '请输入起始行', trigger: 'change' }]">
                        <a-input-number :step="1" :min="1" placeholder="请输入起始行"
                            v-model:value="form.assetsAssetFiles.startData" style="width: 100%" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20" v-if="
                form.assetsAssetFiles.url == undefined ||
                form.assetsAssetFiles.url.indexOf('.xls') != -1 ||
                form.assetsAssetFiles.url.indexOf('.xlsx') != -1
            ">
                <a-col :span="12">
                    <a-form-item label="起始列" name="assetsAssetFiles.startColumn"
                        :rules="[{ required: true, message: '请输入起始列', trigger: 'change' }]">
                        <a-input-number :step="1" :min="1" placeholder="请输入起始列"
                            v-model:value="form.assetsAssetFiles.startColumn" style="width: 100%" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-button type="primary" @click="parseExcel" style="margin-left: 60px"
                        :disabled="isButtonDisabled">
                        解析Excel
                    </a-button>
                </a-col>
            </a-row>
            <a-divider orientation="center" v-if="
                form.assetsAssetFiles.url &&
                (form.assetsAssetFiles.url.indexOf('.csv') != -1 ||
                    form.assetsAssetFiles.url.indexOf('.xls') != -1 ||
                    form.assetsAssetFiles.url.indexOf('.xlsx') != -1)
            ">
                <span class="blue-text">属性字段</span>
            </a-divider>
            <a-table height="310px" :loading="loadingList" :data-source="ColumnByAssettab" :columns="columns" v-if="
                form.assetsAssetFiles.url &&
                (form.assetsAssetFiles.url.indexOf('.csv') != -1 ||
                    form.assetsAssetFiles.url.indexOf('.xls') != -1 ||
                    form.assetsAssetFiles.url.indexOf('.xlsx') != -1)
            ">
                <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'action'">
                        <a-button type="link" :icon="h(EditOutlined)" @click="openDialog(record)">修改</a-button>
                    </template>
                </template>
            </a-table>
        </a-form>
    </div>
</template>
<script setup>
import { h } from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined } from '@ant-design/icons-vue'
import { getToken } from '@/utils/auth.js';

import { typeList } from '@/utils/graph.js';

import { getNodeUniqueKey, getExcelColumn, getCsvColumn } from '@/api/col/task/index.js';
const { proxy } = getCurrentInstance();

import useUserStore from '@/store/system/user.js';
const userStore = useUserStore();
const props = defineProps({
    title: { type: String, default: '表单标题' },
    currentNode: { type: Object, default: () => ({}) },
    info: { type: Boolean, default: false },
    objData: { type: Object, default: () => ({}) }
});
const emit = defineEmits(['update', 'confirm']);
const visible = ref(false);
// 变量定义
let loading = ref(false);
let loadingList = ref(false);
let TablesByDataSource = ref([]);
let ColumnByAssettab = ref();
const columns = [
    { title: '序号', key: 'index', width: 80, align: 'left', customRender: ({ index }) => index + 1 },
    { title: '字段名称', dataIndex: 'columnName', key: 'columnName', align: 'left', ellipsis: true, customRender: ({ text }) => text || '-' },
    { title: '字段类型', dataIndex: 'columnType', key: 'columnType', align: 'left', customRender: ({ text }) => text || '-' },
    { title: '日期格式', dataIndex: 'format', key: 'format', align: 'left', customRender: ({ text }) => text || '-' },
    { title: '操作', key: 'action', align: 'center', fixed: 'right', width: 240 }
];
// 修改
const open = ref(false);
let row = ref({});
const openDialog = (obj) => {
    row.value = obj;
    open.value = true;
};
// 属性字段修改新增
const handletaskConfig = (form) => {
    ColumnByAssettab.value = ColumnByAssettab.value.map((column) => {
        if (column.id == form.id) {
            return { ...column, ...form };
        }
        return column;
    });
};

let dpModelRefs = ref();
let form = ref();
const tableFields = ref([]); // 来源表格
// 计算属性：判断按钮是否禁用
const isButtonDisabled = computed(() => {
    console.log(form.value.assetsAssetFiles.url);
    return (
        !form.value.assetsAssetFiles.startData ||
        !form.value.assetsAssetFiles.startColumn ||
        !form.value.assetsAssetFiles.url
    );
});
// 获取列数据
const parseExcel = async (id) => {
    if (form.value.assetsAssetFiles.url.indexOf('.csv') != -1) {
        if (!form.value.assetsAssetFiles.url) {
            message.warning('操作失败，请添加附件');
            return;
        }

        loading.value = true; // Assuming 'loading' is a global loading state variable
        try {
            let res = await getCsvColumn({
                file: form.value.assetsAssetFiles.url
            });

            if (res?.data?.csvFile) {
                form.value.assetsAssetFiles.csvFile = res.data.csvFile;
                ColumnByAssettab.value = res.data.columnList.map((item, index) => ({
                    id: index,
                    columnName: item,
                    columnType: 'string'
                }));
                message.success('CSV 解析成功，请确认属性字段类型！');
            } else {
                message.warning('CSV 解析失败，未获取到有效数据！');
            }
        } catch (error) {
            message.warning('解析文件时发生错误，请检查后重试');
            console.error(error);
        } finally {
            loading.value = false; // Ensure loading is turned off regardless of success or failure
        }
    } else {
        if (!form.value.assetsAssetFiles.startData) {
            message.warning('操作失败，请添加起始行');
            return;
        }
        if (!form.value.assetsAssetFiles.startColumn) {
            message.warning('操作失败，请添加起始列');
            return;
        }
        if (!form.value.assetsAssetFiles.url) {
            message.warning('操作失败，请添加附件');
            return;
        }
        loadingList.value = true;
        try {
            let res = await getExcelColumn({
                startData: form.value.assetsAssetFiles.startData,
                startColumn: form.value.assetsAssetFiles.startColumn,
                excelFile: form.value.assetsAssetFiles.url
            });

            if (res?.data?.csvFile) {
                form.value.assetsAssetFiles.csvFile = res.data.csvFile;
                ColumnByAssettab.value = res.data.columnList.map((item, index) => ({
                    id: index,
                    columnName: item,
                    columnType: 'string'
                }));

                message.success('Excel解析成功，请确认属性字段类型！');
            } else {
                message.warning('Excel解析失败，未获取到有效数据！');
            }
        } catch (error) {
            if (response.code == 200) message.warning('Excel解析失败，请检查文件格式或内容！');
        } finally {
            loadingList.value = false;
        }
    }
};

const off = () => {
    proxy.resetForm('dpModelRefs');
    // 清空表格字段数据
    ColumnByAssettab.value = [];
    TablesByDataSource.value = [];
    tableFields.value = [];
};
// 保存数据
const saveData = async () => {
    try {
        // 异步验证表单
        const valid = await dpModelRefs.value.validate();
        if (!valid) return;
        if (
            form.value?.assetsAssetFiles.type == '1' &&
            (!ColumnByAssettab.value || ColumnByAssettab.value.length == 0)
        ) {
            return proxy.$message.warning('校验未通过，请选择属性字段');
        }
        // 如果没有 code，就调用接口获取唯一的 code
        if (!form.value.code) {
            loading.value = true;
            const response = await getNodeUniqueKey({
                spaceCode: userStore.spaceCode || '133545087166112',
                spaceId: userStore.spaceId
            });
            loading.value = false; // 结束加载状态
            form.value.code = response.data; // 设置唯一的 code
        }
        const assetsAssetFiles = form.value?.assetsAssetFiles;
        assetsAssetFiles.tableFields = ColumnByAssettab.value;
        assetsAssetFiles.columnsList = ColumnByAssettab.value.map(({ columnName, columnType }) => ({
            colName: columnName,
            dataType: columnType
        }));
        assetsAssetFiles.columns = assetsAssetFiles.tableFields.map((item) => {
            return {
                index: item.id,
                columnName: item.columnName,
                type: item.columnType,
                format: item.format
            };
        });
        emit('confirm', form.value);
        emit('update', false);
    } finally {
        loadingList.value = false;
    }
};

function dataFileName(file) {
    form.value.assetsAssetFiles.name = file.originalFilename;
    form.value.assetsAssetFiles.type = file.ext;
    console.log(form.value, '451561649861');
}
const closeDialog = () => {
    off();
    // 关闭对话框
    emit('update', false);
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
const show = (data) => {
    visible.value = false;
    console.log(data, '文件类型。。。。。。。。。');
    if (data.createType == '2' && data.type == '6') {
        visible.value = true;
        form.value = deepCopy(data);
        ColumnByAssettab.value = data.assetsAssetFiles.tableFields;
    }
    if (data.id != undefined && data.type == '6') {
        visible.value = true;
        if (data.assetsAssetFiles == undefined) {
            data.assetsAssetFiles = {
                url: null,
                startData: '',
                tableFields: [],
                startColumn: ''
            };
        }
        form.value = deepCopy(data);
        ColumnByAssettab.value = data.assetsAssetFiles.tableFields;
    }
};
// 监听属性变化
// watchEffect(() => {
//   console.log(userStore)
//   if (props.visible) {
//     // 数据源
//     console.log(props.objData,'==========')
//     form.value = deepCopy(props.data);
//     ColumnByAssettab.value = props.data.assetsAssetFiles.tableFields;
//   } else {
//     off();
//   }
// });
// 文件删除
function handleRemove() {
    ColumnByAssettab.value = [];
    form.value.assetsAssetFiles.url = undefined;
}
defineExpose({ show, form });
</script>
<style scoped lang="scss">
.blue-text {
    color: #2666fb;
}
</style>

