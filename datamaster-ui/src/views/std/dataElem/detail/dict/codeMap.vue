<template>
    <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
            <a-col :span="1.5">
                <a-button type="primary" @click="handleAdd" @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                </a-button>
            </a-col>
        </a-row>
        <div class="justify-end top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </div>
    </div>
    <a-table stripe :loading="loading" :data-source="dpCodeMapList" :columns="tableColumns"
        :pagination="false" :scroll="{ y: 360 }"
        :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }" row-key="id"
        :locale="{ emptyText: emptyContent }" @change="handleSortChange">
        <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'originalValue'">
                {{ record.originalValue || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'codeName'">
                {{ record.codeName || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'codeValue'">
                {{ record.codeValue || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'createBy'">
                {{ record.createBy || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'remark'">
                {{ record.remark || '-' }}
            </template>
            <template v-else-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="handleUpdate(record)">修改</a-button>
                <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
            </template>
        </template>
    </a-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增或修改数据元代码映射对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" draggable>
        <a-form ref="dpCodeMapRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="原始值" name="originalValue">
                        <a-input v-model:value="form.originalValue" placeholder="请输入原始值" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="代码名" name="codeName">
                        <a-select v-model:value="form.codeName" placeholder="请选择代码名" @change="handleCodeNameChange">
                            <a-select-option v-for="item in dpDataElemCodeList" :key="item.id" :label="item.codeName"
                                :value="item.codeName" />
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="代码值" name="codeValue">
                        <a-input v-model:value="form.codeValue" placeholder="代码值" disabled />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="备注" name="remark">
                        <a-input v-model:value="form.remark" type="textarea" placeholder="请输入备注" />
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>
        <template #footer>
            <div class="dialog-footer">
                <a-button size="small" @click="cancel">取 消</a-button>
                <a-button type="primary" size="small" @click="submitForm">确 定</a-button>
            </div>
        </template>
    </a-modal>
</template>

<script setup name="ComponentOne">
import { h } from "vue";
import {
    listDpCodeMap,
    getDpCodeMap,
    delDpCodeMap,
    addDpCodeMap,
    updateDpCodeMap
} from '@/api/std/dataElem/dataElem';
import { listDpDataElemCode } from '@/api/std/dataElem/dataElem';
const route = useRoute();
const { proxy } = getCurrentInstance();

const dpCodeMapList = ref([]);
const dpDataElemCodeList = ref([]);

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
const id = ref(route.query.id || 1);

const tableColumns = [
    { title: "编号", dataIndex: "id", align: "left", width: 60, sorter: true },
    { title: "原始值", dataIndex: "originalValue", align: "left", width: 210, ellipsis: true },
    { title: "代码名", dataIndex: "codeName", align: "left", width: 220, ellipsis: true },
    { title: "代码值", dataIndex: "codeValue", align: "left", width: 180, ellipsis: true },
    { title: "创建人", dataIndex: "createBy", align: "left" },
    { title: "创建时间", dataIndex: "createTime", align: "left", width: 200, sorter: true },
    { title: "备注", dataIndex: "remark", align: "left", width: 320, ellipsis: true },
    { title: "操作", key: "actions", align: "center", fixed: "right", width: 200 },
];

const emptyContent = h("div", { class: "emptyBg" }, [
    h("img", { src: new URL("@/assets/system/images/no_data/noData.png", import.meta.url).href, alt: "" }),
    h("p", "暂无记录"),
]);

watch(
    () => route.query.id,
    (newId) => {
        if (newId !== id.value) {
            id.value = newId || -1;
            queryParams.value.dataElemId = id.value;
            getList();
            getDpDataElemCodeList();
        }
    },
    { immediate: true }
);

const data = reactive({
    dpCodeMapDetail: {},
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        dataElemId: null,
        originalValue: null,
        codeName: null,
        codeValue: null,
        createTime: null
    },
    rules: {
        originalValue: [{ required: true, message: '原始值不能为空', trigger: 'blur' }],
        codeName: [{ required: true, message: '代码名不能为空', trigger: 'change' }]
    }
});

const { queryParams, form, dpCodeMapDetail, rules } = toRefs(data);

/** 查询数据元代码映射列表 */
function getList() {
    if (id.value == -1) {
        return;
    }
    loading.value = true;
    queryParams.value.dataElemId = id.value;
    listDpCodeMap(queryParams.value).then((response) => {
        dpCodeMapList.value = response.data.rows;
        total.value = response.data.total;
        loading.value = false;
    });
}

function getDpDataElemCodeList() {
    if (id.value == -1) {
        return;
    }
    queryParams.value.dataElemId = id.value;
    listDpDataElemCode(queryParams.value).then((response) => {
        dpDataElemCodeList.value = response.data.rows;
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
        dataElemId: null,
        originalValue: null,
        codeName: null,
        codeValue: null,
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
    proxy.resetForm('dpCodeMapRef');
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
    title.value = '新增数据元代码映射';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDpCodeMap(_id).then((response) => {
        form.value = response.data;
        open.value = true;
        title.value = '修改数据元代码映射';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getDpCodeMap(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '数据元代码映射详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['dpCodeMapRef'].validate().then(() => {
        form.value.dataElemId = id.value;
        if (form.value.id != null) {
            updateDpCodeMap(form.value)
                .then((response) => {
                    proxy.$modal.msgSuccess('修改成功');
                    open.value = false;
                    getList();
                })
                .catch((error) => { });
        } else {
            addDpCodeMap(form.value)
                .then((response) => {
                    proxy.$modal.msgSuccess('新增成功');
                    open.value = false;
                    getList();
                })
                .catch((error) => { });
        }
    }).catch(() => { });
}

/** 删除按钮操作 */
function handleDelete(row) {
    const _ids = row.id || ids.value;
    proxy.$modal
        .confirm('是否确认删除数据元代码映射编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDpCodeMap(_ids);
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
        'std/codeMap/export',
        {
            ...queryParams.value
        },
        `dpCodeMap_${new Date().getTime()}.xlsx`
    );
}

// 新增代码名选择处理函数
function handleCodeNameChange(value) {
    const selectedCode = dpDataElemCodeList.value.find((item) => item.codeName === value);
    if (selectedCode) {
        form.value.codeValue = selectedCode.codeValue;
    }
}

getList();
getDpDataElemCodeList();

proxy.$bus.on('data_elem_code_change', (data) => {
    getDpDataElemCodeList();
});
</script>

