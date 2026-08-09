<template>
    <!-- 资产预览tab -->
    <div style="padding: 5px">
        <div class="justify-between mb15">
            <a-row :gutter="15" class="btn-style">
                <a-col :span="1.5" v-if="form1.type != '6'">
                    <a-button type="primary" @click="handleAdd" v-hasPermi="['ast:assetColumn:assetcolumn:add']"
                        :loading="loading" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                    </a-button>
                </a-col>
                <a-button style="margin-left: 7px" type="primary" :loading="loading" @click="handleQuery"
                    @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                </a-button>
                <a-button @click="handleReset" @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                </a-button>
            </a-row>
        </div>
        <a-row :gutter="24" v-if="!formVisible && form1.type != '6'">
            <a-col :span="1">
                <a-button style="" @click="toggleForm(true)" type="primary" size="small">+</a-button>
            </a-col>
            <a-col :span="7">
                <a-alert style="height: 24px" title="点击“+”以添加筛选准则" type="info" :closable="false" />
            </a-col>
        </a-row>
        <div class="custom-form">
            <a-form v-show="formVisible" :model="formData" ref="formRef" layout="inline">
                <div v-for="(item, index) in formData.rows" :key="index" class="form-row">
                    <a-form-item :name="'rows.' + index + '.checked'">
                        <a-checkbox v-model:checked="item.checked"> </a-checkbox>
                    </a-form-item>
                    <a-form-item :name="'rows.' + index + '.field'">
                        <div>
                            <a-select :disabled="!item.checked" :class="item.checked ? 'select' : ''"
                                style="margin: 0; width: 100px; border: none; color: red" v-model:value="item.field"
                                placeholder="选择字段">
                                <a-select-option v-for="field in tableColumns" :key="field.en" :label="field.en"
                                    :value="field.en">{{ field.en }}</a-select-option>
                            </a-select>
                        </div>
                    </a-form-item>
                    <a-form-item :name="'rows.' + index + '.operator'">
                        <a-select :disabled="!item.checked" style="margin: 0; width: 20px" v-model:value="item.operator">
                            <a-select-option style="text-align: center" v-for="operator in operators" :key="operator"
                                :label="operator" :value="operator">{{ operator }}</a-select-option>
                        </a-select>
                    </a-form-item>
                    <a-form-item :name="'rows.' + index + '.value'">
                        <div :class="item.checked ? 'inner' : ''">
                            <a-input v-if="item.operator === '='" :disabled="!item.checked" v-model:value="item.value"
                                placeholder="请输入值"></a-input>

                            <a-input v-else-if="item.operator === '>'" :disabled="!item.checked" v-model:value="item.value"
                                type="number" placeholder="请输入值"></a-input>
                        </div>
                    </a-form-item>
                    <a-form-item :name="'rows.' + index + '.logic'" style="display: block">
                        <a-select :disabled="!item.checked" v-if="index < formData.rows.length - 1"
                            style="margin: 0; width: 80px; display: block" v-model:value="item.logic" placeholder="选择逻辑">
                            <a-select-option value="AND" style="text-align: center">AND</a-select-option>
                        </a-select>
                        <div v-else style="width: 80px"></div>
                    </a-form-item>
                    <a-form-item>
                        <a-button danger size="small" @click="removeRow(index)"
                            style="margin-left: 10px">-</a-button>
                        <a-button v-if="index == formData.rows.length - 1" @click="addRow(index)" type="primary"
                            size="small">+</a-button>
                    </a-form-item>
                </div>
            </a-form>
        </div>
    </div>
    <a-table :data-source="tableData" ref="tableRef" striped :loading="loading" :scroll="{ y: '60vh' }"
        :columns="aTableColumns" :pagination="false" @change="handleTableChange"
        :locale="{ emptyText: '暂无记录' }">
        <template #headerCell="{ column }">
            <template v-if="column.en || column.cn">
                <div class="column-header">
                    <div class="column-item">
                        {{ column.en || '-' }}
                    </div>
                    <div class="column-item">
                        {{ column.cn || '-' }}
                    </div>
                </div>
            </template>
        </template>
        <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="handleUpdate(record)"
                    v-hasPermi="['ast:asset:edit']">修改</a-button>
                <a-button type="link" size="small" @click="openHistory(record)"
                    v-hasPermi="['ast:asset:edit']">修改记录</a-button>
            </template>
        </template>
    </a-table>
    <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize"
        @pagination="handleQuery" />
    <updateDataDialog ref="updateDialogRef" :columns="tableColumns" @ok="handleQuery" />
    <UpdateHistory ref="updateHistoryRef" :columns="tableColumns" @success="handleQuery" />
</template>

<script setup>
import { message } from 'ant-design-vue'
import { ref, computed } from 'vue';

import { useRoute } from 'vue-router';

import { preview } from '@/api/ast/asset/assetColumn.js';

import useUserStore from '@/store/system/user';

import updateDataDialog from '../components/previewEdit.vue';

import UpdateHistory from '../components/previewEditLog.vue';
const props = defineProps({
    form1: {
        type: Object,
        default: {}
    }
});
let tableRef = ref(null);
const route = useRoute();
const userStore = useUserStore();
let assetId = route.query.id || 1;
const { proxy } = getCurrentInstance();
const tableColumns = ref([]);
const aTableColumns = computed(() => {
    const cols = tableColumns.value.map(col => {
        const order = sortField.value.get(col.field);
        return {
            title: col.cn || '-',
            dataIndex: col.field,
            en: col.en,
            cn: col.cn,
            width: 150,
            ellipsis: true,
            sorter: true,
            sortOrder: order === 'ascending' ? 'ascend' : order === 'descending' ? 'descend' : null,
        };
    });
    if (tableColumns.value.length > 0 && props.form1.type != '6') {
        cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 150 });
    }
    return cols;
});
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const query = ref();
const operators = ref(['=', '>']);
const formData = ref({
    rows: []
});
const sortField = ref(new Map()); // 存储多列排序状态
const orderBy = ref([]);
const updateDialogRef = ref(null); // 组件的 ref 引用
const formVisible = ref(false); // 表单默认隐藏
watch(
    () => route.query.id,
    (newId) => {
        // 解决数据发现详情和当前界面同时打开报错问题
        if (route.path == '/ast/asset/detail' || route.path == '/col/asset/detail') {
            assetId = newId || 1; // 如果 id 为空，使用默认值 1
            getListss();
        }
    },
    { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);
// 显示或隐藏表单
const toggleForm = (falg) => {
    formVisible.value = !formVisible.value;
    if (falg) {
        addRow(-1);
    }
};

// 生成SQL查询条件
const generateSqlQuery = () => {
    const selectedRows = formData.value.rows.filter((row) => row.checked); // 筛选出 checked 为 true 的行
    let query = '';

    const conditions = selectedRows.map((row) => {
        return `${row.field} ${row.operator} '${row.value}'`;
    });

    query += conditions.join(' AND '); // 拼接条件，默认用 AND 连接
    console.log('生成的 SQL 查询语句：', query);
    return query;
};

// 查询按钮点击事件
const handleQuery = () => {
    let falg = validateFields();
    if (!falg) return false;
    query.value = generateSqlQuery();
    getListss();
};
// 表单验证
const validateFields = () => {
    for (let row of formData.value.rows) {
        if (!row.field || !row.operator) {
            message.warning('校验未通过，查询条件请输入完整');
            return false;
        }
    }
    return true;
};
const removeRow = (index) => {
    if (formData.value.rows.length > 1) {
        formData.value.rows.splice(index, 1);
    } else {
        formData.value.rows = [];
        toggleForm();
    }
};
const addRow = (index) => {
    let flag = validateFields();
    if (!flag) return false;
    if (index !== undefined && formData.value.rows[index]) {
        formData.value.rows[index].logic = 'AND';
    }
    const newRow = {
        checked: true,
        field:
            tableColumns.value[index + 1]?.en ||
            tableColumns.value[tableColumns.value.length - 1]?.en,
        operator: '=',
        value: '',
        logic: ''
    };

    if (index !== undefined) {
        formData.value.rows.splice(index + 1, 0, newRow);
    } else {
        formData.value.rows.push(newRow);
    }
};

const tableData = ref();

function handleUpdate(row) {
    console.log('🚀 ~ handleUpdate ~ row:', row);
    // proxy.$message.error('功能开发中....');
    updateDialogRef.value?.addRow(row, props.form1);
}
function handleAdd() {
    // proxy.$message.error('功能开发中....');
    updateDialogRef.value?.addRow(undefined, props.form1);
}

const updateHistoryRef = ref(null);
function openHistory(row) {
    // 调用子组件的 show 方法，传入你想要的参数，触发弹窗显示
    if (updateHistoryRef.value) {
        updateHistoryRef.value.show(row, props.form1);
    }
}
function handleDelete() {
    proxy.$message.warning('功能开发中....');
}

function getListss() {
    if (route.query.id == null || route.query.id == undefined) {
        return;
    }
    loading.value = true;
    preview({
        id: assetId,
        filter: query.value,
        orderBy: orderBy.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        spaceId: userStore.spaceId,
        spaceCode: userStore.spaceCode
    })
        .then((response) => {
            tableColumns.value = response.data.columns;
            tableData.value = response.data.tableData;
            loading.value = false;
            total.value = response.data.total;
        })
        .catch(() => {
            loading.value = false;
        });
}
const updateOrderBy = () => {
    const list = [];
    for (const [key, value] of sortField.value) {
        if (value) {
            list.push({
                orderByColumn: key,
                isAsc: value === 'ascending' ? 'asc' : 'desc'
            });
        }
    }
    orderBy.value = list; // 独立维护
};
function handleTableChange(pagination, filters, sorter) {
    const field = sorter.column?.dataIndex || sorter.field;
    const orderMap = { ascend: 'ascending', descend: 'descending' };
    const order = sorter.order ? orderMap[sorter.order] : null;
    if (!order) {
        sortField.value.delete(field);
    } else {
        sortField.value.set(field, order);
    }
    updateOrderBy();
    getListss();
}
const handleReset = () => {
    // 清空多列排序状态
    sortField.value.clear();
    orderBy.value = [];
    updateOrderBy();

    // 清空表单
    formData.value.rows = [];
    formVisible.value = false;

    // 清空 SQL 查询条件
    query.value = '';

    // 刷新表格
    getListss();
};

</script>

<style scoped lang="scss">
.column-header {
    display: flex;
    flex-direction: column;
}

.column-item {
    white-space: nowrap;
}

.form-row {
    display: flex;
    align-items: center;
    margin-bottom: -17px;
}

.form-row .ant-form-item {
    margin-right: 10px;
}

.custom-form {
    margin-bottom: 10px;
    max-height: 100px;
    overflow: auto;

    ::v-deep .ant-input,
    ::v-deep .ant-select .ant-select-selector {
        border: none !important;
        box-shadow: none !important;
    }

    ::v-deep .ant-select-selector {
        box-shadow: none;
        padding: 0;
    }

    ::v-deep .ant-select-arrow {
        display: none;
    }

    .inner {
        ::v-deep .ant-input {
            color: #2666fb;
        }
    }

    .inner-text {
        ::v-deep .ant-input {
            color: #999093 !important;
        }
    }

    .select {
        ::v-deep .ant-select-selection-placeholder {
            color: #2666fb;
        }
    }

    :deep .ant-select-disabled .ant-select-selector {
        background-color: #fff;
    }

    .select-text {
        ::v-deep .ant-select-selection-placeholder {
            color: #999093 !important;
        }
    }

    :deep .ant-input-disabled {
        background-color: #fff;
    }
}
</style>

