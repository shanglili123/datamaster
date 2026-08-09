<template>
    <a-form ref="form2" :model="form2" :label-col="{ style: { width: '100px' } }" label="字段列表：">
        <template>
            <div class="clearfix header-text">
                <div class="header-left">
                    <div class="blue-bar"></div>
                    请求参数
                </div>
            </div>

            <a-table :data-source="form2.reqParams" :scroll="{ y: 250 }" striped :pagination="false" :columns="reqParamColumns">
                <template #bodyCell="{ column, record, index }">
                    <template v-if="column.key === 'index'">
                        <span>{{ index + 1 }}</span>
                    </template>
                    <template v-if="column.dataIndex === 'nullable'">
                        <a-checkbox disabled :checked="record.nullable === '1'" />
                    </template>
                    <template v-if="column.dataIndex === 'paramComment'">
                        {{ record.paramComment || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'paramType'">
                        <dict-tag :options="ds_api_param_type" :value="record.paramType" />
                    </template>
                    <template v-if="column.dataIndex === 'whereType'">
                        <dict-tag :options="da_api_param_operator" :value="record.whereType" />
                    </template>
                    <template v-if="column.dataIndex === 'exampleValue'">
                        {{ record.exampleValue || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'defaultValue'">
                        {{ record.defaultValue || '-' }}
                    </template>
                </template>
            </a-table>
            <div class="clearfix header-text">
                <div class="header-left">
                    <div class="blue-bar"></div>
                    返回字段
                </div>
            </div>
            <a-table :data-source="form2.resParams" striped :pagination="false" :columns="[
                { title: '序号', key: 'index', align: 'center', width: 80 },
                { title: '中文名称', dataIndex: 'fieldName', align: 'center', ellipsis: true },
                { title: '描述', dataIndex: 'fieldComment', align: 'center', ellipsis: true },
                { title: '数据类型', dataIndex: 'dataType', align: 'center', ellipsis: true },
                { title: '示例值', dataIndex: 'exampleValue', align: 'center', ellipsis: true },
            ]">
                <template #bodyCell="{ column, record, index }">
                    <template v-if="column.key === 'index'">
                        <span>{{ index + 1 }}</span>
                    </template>
                    <template v-if="column.dataIndex === 'fieldComment'">
                        {{ record.fieldComment || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'dataType'">
                        {{ record.dataType || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'exampleValue'">
                        {{ record.exampleValue || '-' }}
                    </template>
                </template>
            </a-table>
        </template>
    </a-form>
</template>

<script setup name="ComponentOne">
import { listDsApi, getDsApi, delDsApi, addDsApi, updateDsApi } from '@/api/svc/api/api.js';

const { proxy } = getCurrentInstance();
const {
    ds_api_log_status,
    ds_api_bas_info_api_service_type,
    ds_api_bas_info_api_method_type,
    ds_api_bas_info_res_data_type,
    da_api_param_operator,
    ds_api_param_type,
} = proxy.useDict(
    'ds_api_log_status',
    'ds_api_bas_info_api_service_type',
    'ds_api_bas_info_api_method_type',
    'ds_api_bas_info_res_data_type',
    "da_api_param_operator",
    "ds_api_param_type",
);

const dsApiList = ref([]);
const props = defineProps({
    form2: {
        type: Object,
        default: {}
    }
});

const reqParamColumns = [
    { title: '序号', key: 'index', align: 'center', width: 80 },
    { title: '参数名称', dataIndex: 'paramName', align: 'center', ellipsis: true },
    { title: '是否允许为空', dataIndex: 'nullable', align: 'center', ellipsis: true },
    { title: '描述', dataIndex: 'paramComment', align: 'center' },
    { title: '参数类型', dataIndex: 'paramType', align: 'center' },
    { title: '操作符', dataIndex: 'whereType', align: 'center' },
    { title: '示例值', dataIndex: 'exampleValue', align: 'center', ellipsis: true },
    { title: '默认值', dataIndex: 'defaultValue', align: 'center', ellipsis: true },
];

const open = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');
const daterangeCreateTime = ref([]);
const defaultSort = ref({ prop: 'createTime', order: 'desc' });

const data = reactive({
    dsApiDetail: {},
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        NAME: null,
        status: null,
        createTime: null
    },
    rules: {}
});

const { queryParams, form, dsApiDetail, rules } = toRefs(data);

function normalizePageData(response) {
    const data = response?.data ?? response ?? {};
    if (Array.isArray(data)) {
        return { rows: data, total: data.length };
    }
    const rows = Array.isArray(data.rows)
        ? data.rows
        : Array.isArray(data.list)
            ? data.list
            : Array.isArray(data.records)
                ? data.records
                : Array.isArray(response?.rows)
                    ? response.rows
                    : [];
    const total = Number(data.total ?? data.totalCount ?? response?.total ?? rows.length);
    return { rows, total: Number.isNaN(total) ? rows.length : total };
}

/** 查询API服务列表 */
function getList() {
    loading.value = true;
    queryParams.value.params = {};
    if (null != daterangeCreateTime && '' != daterangeCreateTime) {
        queryParams.value.params['beginCreateTime'] = daterangeCreateTime.value[0];
        queryParams.value.params['endCreateTime'] = daterangeCreateTime.value[1];
    }
    listDsApi(queryParams.value)
        .then((response) => {
            const pageData = normalizePageData(response);
            dsApiList.value = pageData.rows;
            total.value = pageData.total;
        })
        .catch((error) => { })
        .finally(() => {
            loading.value = false;
        });
}

// 取消按钮
function cancel() {
    open.value = false;
    openDetail.value = false;
    reset();
}

// 表单重置
function reset() {
    form.value = {
        ID: null,
        NAME: null,
        apiVersion: null,
        apiUrl: null,
        reqMethod: null,
        apiServiceType: null,
        resDataType: null,
        denyIp: null,
        configJson: null,
        limitJson: null,
        reqParams: null,
        resParams: null,
        description: null,
        status: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updaterId: null,
        updateTime: null
    };
    proxy.resetForm('dsApiRef');
}

/** 搜索按钮操作 */
function handleQuery() {
    queryParams.value.pageNum = 1;
    getList();
}

/** 重置按钮操作 */
function resetQuery() {
    daterangeCreateTime.value = [];
    proxy.resetForm('queryRef');
    handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
    ids.value = selection.map((item) => item.ID);
    single.value = selection.length != 1;
    multiple.value = !selection.length;
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
    queryParams.value.orderByColumn = column.prop;
    queryParams.value.isAsc = column.order;
    getList();
}

/** 新增按钮操作 */
function handleAdd() {
    reset();
    open.value = true;
    title.value = '新增API服务';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _ID = row.ID || ids.value;
    getDsApi(_ID).then((response) => {
        form.value = response.data;
        open.value = true;
        title.value = '修改API服务';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _ID = row.ID || ids.value;
    getDsApi(_ID).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = 'API服务详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['dsApiRef'].validate((valid) => {
        if (valid) {
            if (form.value.ID != null) {
                updateDsApi(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDsApi(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('新增成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            }
        }
    });
}

/** 删除按钮操作 */
function handleDelete(row) {
    const _IDs = row.ID || ids.value;
    proxy.$modal
        .confirm('是否确认删除API服务编号为"' + _IDs + '"的数据项？')
        .then(function () {
            return delDsApi(_IDs);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess('删除成功');
        })
        .catch(() => { });
}

/** 导出按钮操作 */
function handleExport() {
    proxy.download(
        'svc/api/export',
        {
            ...queryParams.value
        },
        `dsApi_${new Date().getTime()}.xlsx`
    );
}

getList();
</script>

<style scoped lang="scss">
.blue-bar {
    background-color: #2666fb;
    width: 5px;
    height: 20px;
    margin-right: 10px;
    border-radius: 2px;
}

.header-text {
    margin: 12px 0
}

.header-left {
    display: flex;
    align-items: center;
    font-size: 16px;
    line-height: 24px;
    font-style: normal;
}
</style>

