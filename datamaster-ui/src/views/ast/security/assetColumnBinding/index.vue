<template>
    <div class="app-container" ref="app-container">

        <div class="pagecont-top" v-show="showSearch">
            <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }"
                v-show="showSearch" @submit.prevent>
                <a-form-item label="资产" name="assetName">
                    <a-input style="width: 150px;" v-model:value="queryParams.assetName" placeholder="请输入资产名称"
                        allow-clear @pressEnter="handleQuery" />
                </a-form-item>
                <a-form-item label="分类" name="dataCategoryName">
                    <a-input style="width: 150px;" v-model:value="queryParams.dataCategoryName" placeholder="请输入数据分类"
                        allow-clear @pressEnter="handleQuery" />
                </a-form-item>

                <a-form-item>
                    <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                    </a-button>
                    <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                    </a-button>
                </a-form-item>
            </a-form>
            <div class="data-action-btns">
                <a-button type="primary" @click="handleAdd" v-hasPermi="['dg:Standardsdesensitizelist:add']"
                    @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                </a-button>
            </div>
            <div class="top-right-btn">
                <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"
                    :columns="columns"></right-toolbar>
            </div>
        </div>

        <div>
            <a-table
                striped
                :loading="loading"
                :data-source="bindingList"
                :columns="tableColumns"
                :pagination="false"
                :locale="{ emptyText: '暂无记录' }"
                @change="handleTableChange"
            >
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'assetName'">
                        {{ record.assetName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'assetTableName'">
                        {{ record.assetTableName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'assetcolumnName'">
                        {{ record.assetcolumnName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'assetcolumnComment'">
                        <span :title="record.assetcolumnComment">{{ record.assetcolumnComment || '-' }}</span>
                    </template>
                    <template v-if="column.dataIndex === 'dataCategoryName'">
                        {{ record.dataCategoryName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'dataLevelName'">
                        {{ record.dataLevelName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'desensitizeRuleName'">
                        {{ record.desensitizeRuleName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'sortOrder'">
                        {{ record.sortOrder || '-' }}
                    </template>
                    <template v-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="handleUpdate(record)"
                            v-hasPermi="['dg:Standardsdesensitizelist:edit']">修改</a-button>
                        <a-button type="link" danger size="small" @click="handleDelete(record)"
                            v-hasPermi="['dg:Standardsdesensitizelist:remove']">删除</a-button>
                    </template>
                </template>
            </a-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>

        <!-- 新增或修改字段脱敏绑定对话框 -->
        <a-modal :title="title" v-model:open="open" width="800px" draggable @ok="submitForm" @cancel="cancel">
            <a-form ref="assetcolumnBindingRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }"
                @submit.prevent>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="数据资产" name="assetId">
                            <a-select v-model:value="form.assetId" placeholder="请选择数据资产"
                                :options="assetOptions" show-search :filter-option="filterOption"
                                @change="onAssetChange" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="资产字段" name="assetcolumnId">
                            <a-select v-model:value="form.assetcolumnId" placeholder="请先选择数据资产"
                                :options="columnOptions" show-search :filter-option="filterOption"
                                :disabled="!form.assetId" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="数据分类" name="dataCategoryId">
                            <a-select v-model:value="form.dataCategoryId" placeholder="请选择数据分类"
                                :options="dataCategoryOptions" show-search :filter-option="filterOption" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="排序" name="sortOrder">
                            <a-input-number v-model:value="form.sortOrder" placeholder="请输入排序"
                                style="width: 100%;" :min="0" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="描述" name="description">
                            <a-textarea v-model:value="form.description" placeholder="请输入描述" />
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

    </div>
</template>

<script setup name="AssetColumnBinding">
import {
    listAssetcolumnBinding,
    getAssetcolumnBinding,
    addAssetcolumnBinding,
    updateAssetcolumnBinding,
    delAssetcolumnBinding,
    listDataCategoryAll,
    listAsset,
    listAssetColumn
} from '@/api/governance/assetcolumn';
import { normalizePage, pageRows } from "@/utils/page.js";

const { proxy } = getCurrentInstance();
const bindingList = ref([]);
const assetOptions = ref([]);
const columnOptions = ref([]);
const dataCategoryOptions = ref([]);

// 列显隐信息
const columns = ref([
    { key: 1, label: '编号', visible: true },
    { key: 2, label: '资产名称', visible: true },
    { key: 3, label: '表名', visible: true },
    { key: 4, label: '字段名称', visible: true },
    { key: 5, label: '字段注释', visible: true },
    { key: 6, label: '数据分类', visible: true },
    { key: 7, label: '敏感等级', visible: true },
    { key: 8, label: '脱敏规则', visible: true },
    { key: 9, label: '排序', visible: true },
    { key: 10, label: '操作', visible: true }
]);

const getColumnVisibility = (key) => {
    const column = columns.value.find((col) => col.key === key);
    if (!column) return true;
    return column.visible;
};

const tableColumns = computed(() => {
    const allCols = [
        { title: '编号', dataIndex: 'id', align: 'center', width: 80, colKey: 1 },
        { title: '资产名称', dataIndex: 'assetName', align: 'left', colKey: 2 },
        { title: '表名', dataIndex: 'assetTableName', align: 'center', colKey: 3 },
        { title: '字段名称', dataIndex: 'assetcolumnName', align: 'center', colKey: 4 },
        { title: '字段注释', dataIndex: 'assetcolumnComment', align: 'left', ellipsis: true, colKey: 5 },
        { title: '数据分类', dataIndex: 'dataCategoryName', align: 'center', colKey: 6 },
        { title: '敏感等级', dataIndex: 'dataLevelName', align: 'center', colKey: 7 },
        { title: '脱敏规则', dataIndex: 'desensitizeRuleName', align: 'center', colKey: 8 },
        { title: '排序', dataIndex: 'sortOrder', align: 'center', width: 80, colKey: 9 },
        { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200, colKey: 10 },
    ];
    return allCols.filter(col => getColumnVisibility(col.colKey));
});

const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');

const data = reactive({
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        assetName: null,
        dataCategoryName: null
    },
    rules: {
        assetId: [{ required: true, message: '请选择数据资产', trigger: 'change' }],
        assetcolumnId: [{ required: true, message: '请选择资产字段', trigger: 'change' }],
        dataCategoryId: [{ required: true, message: '请选择数据分类', trigger: 'change' }]
    }
});

const { queryParams, form, rules } = toRefs(data);

/** 加载资产下拉列表 */
function loadAssetOptions() {
    listAsset({ pageSize: 1000 }).then((response) => {
        const list = response.data?.rows || response.data || [];
        assetOptions.value = list.map(item => ({
            value: item.id,
            label: item.assetName || item.tableName || ('资产-' + item.id)
        }));
    });
}

/** 加载数据分类下拉列表 */
function loadDataCategoryOptions() {
    listDataCategoryAll({}).then((response) => {
        const list = response.data || [];
        dataCategoryOptions.value = list.map(item => ({
            value: item.id,
            label: item.name
        }));
    });
}

/** 资产切换，加载字段列表 */
function onAssetChange(assetId) {
    form.value.assetcolumnId = null;
    columnOptions.value = [];
    if (!assetId) return;
    listAssetColumn({ assetId: assetId, pageSize: 1000 }).then((response) => {
        const list = response.data?.rows || response.data || [];
        columnOptions.value = list.map(item => ({
            value: item.id,
            label: item.columnName + (item.columnComment ? ' (' + item.columnComment + ')' : '')
        }));
    });
}

/** 下拉搜索过滤 */
function filterOption(input, option) {
    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

/** 查询字段脱敏绑定列表 */
function getList() {
    loading.value = true;
    listAssetcolumnBinding(queryParams.value).then((response) => {
        const page = normalizePage(response);
        total.value = page.total;
        bindingList.value = pageRows(page.rows, page.total, queryParams.value);
        loading.value = false;
    });
}

// 取消按钮
function cancel() {
    open.value = false;
    reset();
}

// 表单重置
function reset() {
    form.value = {
        id: null,
        assetId: null,
        assetcolumnId: null,
        dataCategoryId: null,
        sortOrder: null,
        description: null
    };
    columnOptions.value = [];
    proxy.resetForm('assetcolumnBindingRef');
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

/** 排序触发事件 */
function handleTableChange(pagination, filters, sorter) {
    const field = sorter.column?.key || sorter.field;
    const orderMap = { ascend: 'asc', descend: 'desc' };
    queryParams.value.orderByColumn = field;
    queryParams.value.isAsc = sorter.order ? orderMap[sorter.order] : null;
    getList();
}

/** 新增按钮操作 */
function handleAdd() {
    reset();
    open.value = true;
    title.value = '新增字段脱敏绑定';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getAssetcolumnBinding(_id).then((response) => {
        form.value = response.data;
        // 加载对应资产的字段列表
        if (form.value.assetId) {
            onAssetChange(form.value.assetId);
        }
        open.value = true;
        title.value = '修改字段脱敏绑定';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['assetcolumnBindingRef']
        .validate()
        .then(() => {
            if (form.value.id != null) {
                updateAssetcolumnBinding(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addAssetcolumnBinding(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('新增成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            }
        })
        .catch(() => { });
}

/** 删除按钮操作 */
function handleDelete(row) {
    const _ids = row.id || ids.value;
    proxy.$modal
        .confirm('是否确认删除字段脱敏绑定编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delAssetcolumnBinding(_ids);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess('删除成功');
        })
        .catch(() => { });
}

loadAssetOptions();
loadDataCategoryOptions();
getList();
</script>

<style scoped lang="scss">
.pagecont-top {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 8px;

  .ant-form {
    display: flex !important;
    flex-wrap: nowrap !important;
    flex: 0 1 auto !important;

    .ant-form-item {
      display: inline-flex !important;
      flex-shrink: 0 !important;
      margin-bottom: 0 !important;
    }
  }

  .data-action-btns {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .top-right-btn {
    flex-shrink: 0;
  }
}
</style>
