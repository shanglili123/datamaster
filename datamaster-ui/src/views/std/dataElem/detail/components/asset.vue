<template>
    <div class="justify-between mb15">
        <div class="justify-end top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </div>
    </div>
    <a-table stripe :loading="loading" :data-source="dpDataElemAssetRelList" :columns="tableColumns"
        :pagination="false" :scroll="{ y: 360 }"
        :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }" row-key="id"
        :locale="{ emptyText: emptyContent }" @change="handleSortChange">
        <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'assetName'">
                {{ record.assetName || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'description'">
                {{ record.description || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'tableName'">
                {{ record.tableName || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'columnName'">
                {{ record.columnName || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'createBy'">
                {{ record.createBy || "-" }}
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-"
                }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'updateTime'">
                <span>{{ parseTime(record.updateTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</span>
            </template>
        </template>
    </a-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
</template>

<script setup name="ComponentOne">
import { h } from "vue";
import {
    listDpDataElemAssetRel,
    getDpDataElemAssetRel,
    delDpDataElemAssetRel,
    addDpDataElemAssetRel,
    updateDpDataElemAssetRel
} from '@/api/std/dataElem/dataElem';

const { proxy } = getCurrentInstance();
const route = useRoute();

const dpDataElemAssetRelList = ref([]);

const open = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');
const defaultSort = ref({ prop: 'createTime', order: 'desc' });

const tableColumns = [
    { title: "编号", dataIndex: "id", align: "left", width: 60, sorter: true },
    { title: "资产名称", dataIndex: "assetName", align: "left", width: 300, ellipsis: true },
    { title: "描述", dataIndex: "description", align: "left", width: 380, ellipsis: true },
    { title: "数据表", dataIndex: "tableName", align: "left", width: 290 },
    { title: "关联字段", dataIndex: "columnName", align: "left", width: 300 },
    { title: "创建人", dataIndex: "createBy", align: "left", width: 120, ellipsis: true },
    { title: "创建时间", dataIndex: "createTime", align: "left", width: 150, sorter: true },
    { title: "更新时间", dataIndex: "updateTime", align: "left", width: 300 },
];

const emptyContent = h("div", { class: "emptyBg" }, [
    h("img", { src: new URL("@/assets/system/images/no_data/noData.png", import.meta.url).href, alt: "" }),
    h("p", "暂无记录"),
]);

const data = reactive({
    dpDataElemAssetRelDetail: {},
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        dataElemType: null,
        dataElemId: null,
        assetId: null,
        tableName: null,
        columnId: null,
        columnName: null,
        createTime: null
    },
    rules: {}
});

const { queryParams, form, dpDataElemAssetRelDetail, rules } = toRefs(data);

queryParams.value.dataElemId = route.query.id;
// 监听 id 变化
watch(
    () => route.query.id,
    (newId) => {
        queryParams.value.dataElemId = newId;
        getList();
    },
    { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);
/** 查询数据元数据资产关联信息列表 */
function getList() {
    loading.value = true;
    listDpDataElemAssetRel(queryParams.value).then((response) => {
        dpDataElemAssetRelList.value = response.data.rows;
        total.value = response.data.total;
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
        id: null,
        dataElemType: null,
        dataElemId: null,
        assetId: null,
        tableName: null,
        columnId: null,
        columnName: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updaterId: null,
        updateTime: null,
        remark: null
    };
    proxy.resetForm('dpDataElemAssetRelRef');
}

/** 搜索按钮操作 */
function handleQuery() {
    queryParams.value.pageNum = 1;
    getList();
}

/** 重置按钮操作 */
function resetQuery() {
    proxy.resetForm('queryRef');
    handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selectedRowKeys, selectedRows) {
    ids.value = selectedRows.map((item) => item.id);
    single.value = selectedRows.length != 1;
    multiple.value = !selectedRows.length;
}

/** 排序触发事件 */
function handleSortChange(pag, filters, sorter) {
    const prop = sorter.field || sorter.column?.dataIndex;
    const order =
        sorter.order === "ascend"
            ? "ascending"
            : sorter.order === "descend"
                ? "descending"
                : null;
    queryParams.value.orderByColumn = prop;
    queryParams.value.isAsc = order;
    getList();
}

/** 新增按钮操作 */
function handleAdd() {
    reset();
    open.value = true;
    title.value = '新增数据元数据资产关联信息';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDpDataElemAssetRel(_id).then((response) => {
        form.value = response.data;
        open.value = true;
        title.value = '修改数据元数据资产关联信息';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getDpDataElemAssetRel(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '数据元数据资产关联信息详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['dpDataElemAssetRelRef'].validate((valid) => {
        if (valid) {
            if (form.value.id != null) {
                updateDpDataElemAssetRel(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDpDataElemAssetRel(form.value)
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
    const _ids = row.id || ids.value;
    proxy.$modal
        .confirm('是否确认删除数据元数据资产关联信息编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDpDataElemAssetRel(_ids);
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
        'std/dataElemAssetRel/export',
        {
            ...queryParams.value
        },
        `dpDataElemAssetRel_${new Date().getTime()}.xlsx`
    );
}

getList();
</script>

