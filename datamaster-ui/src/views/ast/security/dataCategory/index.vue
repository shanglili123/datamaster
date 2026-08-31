<template>
    <div class="app-container" ref="app-container">

        <div class="pagecont-top" v-show="showSearch">
            <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '65px' } }"
                v-show="showSearch" @submit.prevent>
                <a-form-item label="分类名称" name="name">
                    <a-input style="width: 150px;" v-model:value="queryParams.name" placeholder="请输入分类名称"
                        allow-clear @pressEnter="handleQuery" />
                </a-form-item>
                <a-form-item label="类目编码" name="catCode">
                    <a-input style="width: 150px;" v-model:value="queryParams.catCode" placeholder="请输入类目编码"
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
                <a-button type="primary" @click="handleAdd" v-hasPermi="['dg:dataCategory:add']"
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
                :data-source="dataCategoryList"
                :columns="tableColumns"
                :pagination="false"
                :locale="{ emptyText: '暂无记录' }"
                @change="handleTableChange"
            >
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'catCode'">
                        {{ record.catCode || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'catName'">
                        {{ record.catName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'name'">
                        {{ record.name || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'shortName'">
                        {{ record.shortName || '-' }}
                    </template>

                    <template v-if="column.dataIndex === 'validFlag'">
                        <a-badge :status="record.validFlag ? 'success' : 'default'" :text="record.validFlag ? '有效' : '无效'" />
                    </template>
                    <template v-if="column.dataIndex === 'desensitizationRulesFlag'">
                        <a-badge :status="record.desensitizationRulesFlag === '1' ? 'success' : 'default'" :text="record.desensitizationRulesFlag === '1' ? '已配置' : '未配置'" />
                    </template>
                    <template v-if="column.dataIndex === 'createTime'">
                        <span>{{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
                    </template>
                    <template v-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="handleUpdate(record)"
                            v-hasPermi="['dg:dataCategory:edit']">修改</a-button>
                        <a-button type="link" danger size="small" @click="handleDelete(record)"
                            v-hasPermi="['dg:dataCategory:remove']">删除</a-button>
                    </template>
                </template>
            </a-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>

        <!-- 新增或修改数据分类对话框 -->
        <a-modal :title="title" v-model:open="open" width="700px" draggable @ok="submitForm" @cancel="cancel">
            <a-form ref="dataCategoryRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }"
                @submit.prevent>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="所属类目" name="catCode">
                            <a-select v-model:value="form.catCode" placeholder="请选择所属类目"
                                :options="catOptions" show-search :filter-option="filterOption" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="分类名称" name="name">
                            <a-input v-model:value="form.name" placeholder="请输入分类名称" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="缩写名" name="shortName">
                            <a-input v-model:value="form.shortName" placeholder="请输入缩写名" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="是否有效" name="validFlag">
                            <a-switch v-model:checked="form.validFlag" checked-children="有效" un-checked-children="无效" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="描述" name="description" :label-col="{ style: { width: '100px' } }">
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

<script setup name="DataCategory">
import {
    listDataCategory,
    getDataCategory,
    addDataCategory,
    updateDataCategory,
    delDataCategory,
    listDataCategoryCat
} from '@/api/governance/dataCategory';
import { normalizePage, pageRows } from "@/utils/page.js";

const { proxy } = getCurrentInstance();
const dataCategoryList = ref([]);
const catOptions = ref([]);

// 列显隐信息
const columns = ref([
    { key: 1, label: '编号', visible: true },
    { key: 2, label: '类目编码', visible: true },
    { key: 3, label: '所属类目', visible: true },
    { key: 4, label: '分类名称', visible: true },
    { key: 5, label: '缩写名', visible: true },
    { key: 6, label: '是否有效', visible: true },
    { key: 7, label: '脱敏配置', visible: true },
    { key: 8, label: '创建时间', visible: true },
    { key: 9, label: '操作', visible: true }
]);

const getColumnVisibility = (key) => {
    const column = columns.value.find((col) => col.key === key);
    if (!column) return true;
    return column.visible;
};

const tableColumns = computed(() => {
    const allCols = [
        { title: '编号', dataIndex: 'id', align: 'center', width: 80, colKey: 1 },
        { title: '类目编码', dataIndex: 'catCode', align: 'center', colKey: 2 },
        { title: '所属类目', dataIndex: 'catName', align: 'center', colKey: 3 },
        { title: '分类名称', dataIndex: 'name', align: 'left', colKey: 4 },
        { title: '缩写名', dataIndex: 'shortName', align: 'center', colKey: 5 },
        { title: '是否有效', dataIndex: 'validFlag', align: 'center', colKey: 6 },
        { title: '脱敏配置', dataIndex: 'desensitizationRulesFlag', align: 'center', colKey: 7 },
        { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 160, key: 'create_time', sorter: true, defaultSortOrder: 'descend', colKey: 8 },
        { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200, colKey: 9 },
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
        pageSize: 10,
        name: null,
        catCode: null
    },
    rules: {
        catCode: [{ required: true, message: '请选择所属类目', trigger: 'change' }],
        name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }]
    }
});

const { queryParams, form, rules } = toRefs(data);

/** 加载类目下拉列表 */
function loadCatOptions() {
    listDataCategoryCat({}).then((response) => {
        const list = response.data || [];
        catOptions.value = list.map(item => ({
            value: item.code,
            label: item.name
        }));
    });
}

/** 下拉搜索过滤 */
function filterOption(input, option) {
    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

/** 查询数据分类列表 */
function getList() {
    loading.value = true;
    listDataCategory(queryParams.value).then((response) => {
        const page = normalizePage(response);
        total.value = page.total;
        dataCategoryList.value = pageRows(page.rows, page.total, queryParams.value);
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
        catCode: null,
        name: null,
        shortName: null,
        description: null,
        validFlag: true
    };
    proxy.resetForm('dataCategoryRef');
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
    title.value = '新增数据分类';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDataCategory(_id).then((response) => {
        form.value = response.data;
        open.value = true;
        title.value = '修改数据分类';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['dataCategoryRef']
        .validate()
        .then(() => {
            if (form.value.id != null) {
                updateDataCategory(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDataCategory(form.value)
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
        .confirm('是否确认删除数据分类编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDataCategory(_ids);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess('删除成功');
        })
        .catch(() => { });
}

loadCatOptions();
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
