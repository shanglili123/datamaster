<template>
    <div class="app-container" ref="app-container">

        <div class="pagecont-top" v-show="showSearch">
            <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }"
                v-show="showSearch" @submit.prevent>
                <a-form-item label="名称" name="name">
                    <a-input style="width: 150px;" v-model:value="queryParams.name" placeholder="请输入规则名称"
                        allow-clear @pressEnter="handleQuery" />
                </a-form-item>
                <a-form-item label="场景" name="applicationScene">
                    <a-select style="width: 150px;" v-model:value="queryParams.applicationScene" placeholder="请选择应用场景"
                        allow-clear>
                        <a-select-option value="1">数据资产</a-select-option>
                        <a-select-option value="2">数据查询</a-select-option>
                        <a-select-option value="3">数据服务</a-select-option>
                    </a-select>
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
                <a-button type="primary" @click="handleAdd" v-hasPermi="['dg:desensitizerules:add']"
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
                :data-source="desensitizeRuleList"
                :columns="tableColumns"
                :pagination="false"
                :locale="{ emptyText: '暂无记录' }"
                @change="handleTableChange"
            >
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'name'">
                        {{ record.name || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'dataCategoryName'">
                        {{ record.dataCategoryName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'applicationScene'">
                        {{ applicationSceneMap[record.applicationScene] || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'maskType'">
                        {{ maskTypeMap[record.maskType] || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'replaceContent'">
                        {{ record.replaceContent || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'sortOrder'">
                        {{ record.sortOrder || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'createTime'">
                        <span>{{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
                    </template>
                    <template v-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="handleUpdate(record)"
                            v-hasPermi="['dg:desensitizerules:edit']">修改</a-button>
                        <a-button type="link" danger size="small" @click="handleDelete(record)"
                            v-hasPermi="['dg:desensitizerules:remove']">删除</a-button>
                    </template>
                </template>
            </a-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>

        <!-- 新增或修改脱敏规则对话框 -->
        <a-modal :title="title" v-model:open="open" width="800px" draggable @ok="submitForm" @cancel="cancel">
            <a-form ref="desensitizeRuleRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }"
                @submit.prevent>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="规则名称" name="name">
                            <a-input v-model:value="form.name" placeholder="请输入规则名称" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="数据分类" name="dataCategoryId">
                            <a-select v-model:value="form.dataCategoryId" placeholder="请选择数据分类"
                                :options="dataCategoryOptions" show-search :filter-option="filterOption" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="应用场景" name="applicationScene">
                            <a-select v-model:value="form.applicationScene" placeholder="请选择应用场景">
                                <a-select-option value="1">数据资产</a-select-option>
                                <a-select-option value="2">数据查询</a-select-option>
                                <a-select-option value="3">数据服务</a-select-option>
                            </a-select>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="脱敏类型" name="maskType">
                            <a-input value="展示脱敏" disabled placeholder="展示脱敏" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="替换内容" name="replaceContent">
                            <a-input v-model:value="form.replaceContent" placeholder="请输入替换内容" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="排序" name="sortOrder">
                            <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%;" placeholder="请输入排序" />
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

                <!-- 区间子表 -->
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="脱敏区间" :label-col="{ style: { width: '100px' } }">
                            <div style="width: 100%;">
                                <a-table
                                    :data-source="form.intervalList"
                                    :pagination="false"
                                    bordered
                                    size="small"
                                    :locale="{ emptyText: '暂无区间' }"
                                >
                                    <a-table-column title="区间序号" dataIndex="intervalNo" :width="120">
                                        <template #default="{ record }">
                                            <a-input-number v-model:value="record.intervalNo" :min="1" size="small" style="width: 100%;" />
                                        </template>
                                    </a-table-column>
                                    <a-table-column title="起始位置" dataIndex="startNum" :width="120">
                                        <template #default="{ record }">
                                            <a-input-number v-model:value="record.startNum" :min="1" size="small" style="width: 100%;" />
                                        </template>
                                    </a-table-column>
                                    <a-table-column title="结束位置" dataIndex="endNum" :width="120">
                                        <template #default="{ record }">
                                            <a-input-number v-model:value="record.endNum" :min="1" size="small" style="width: 100%;" />
                                        </template>
                                    </a-table-column>
                                    <a-table-column title="操作" key="action" :width="80" align="center">
                                        <template #default="{ index }">
                                            <a-button type="link" danger size="small" @click="removeInterval(index)">删除</a-button>
                                        </template>
                                    </a-table-column>
                                </a-table>
                                <a-button type="dashed" block style="margin-top: 8px;" @click="addInterval">
                                    <i class="iconfont-mini icon-xinzeng mr5"></i>添加区间
                                </a-button>
                            </div>
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

<script setup name="DesensitizeRule">
import {
    listDesensitizeRule,
    getDesensitizeRule,
    addDesensitizeRule,
    updateDesensitizeRule,
    delDesensitizeRule,
    listDataCategoryAll
} from '@/api/governance/desensitizeRule';
import { normalizePage, pageRows } from "@/utils/page.js";

const { proxy } = getCurrentInstance();
const desensitizeRuleList = ref([]);
const dataCategoryOptions = ref([]);

const applicationSceneMap = { '1': '数据资产', '2': '数据查询', '3': '数据服务' };
const maskTypeMap = { '1': '底层脱敏', '2': '展示脱敏' };

// 列显隐信息
const columns = ref([
    { key: 1, label: '编号', visible: true },
    { key: 2, label: '规则名称', visible: true },
    { key: 3, label: '数据分类', visible: true },
    { key: 4, label: '应用场景', visible: true },
    { key: 5, label: '脱敏类型', visible: true },
    { key: 6, label: '替换内容', visible: true },
    { key: 7, label: '排序', visible: true },
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
        { title: '规则名称', dataIndex: 'name', align: 'left', colKey: 2 },
        { title: '数据分类', dataIndex: 'dataCategoryName', align: 'center', colKey: 3 },
        { title: '应用场景', dataIndex: 'applicationScene', align: 'center', colKey: 4 },
        { title: '脱敏类型', dataIndex: 'maskType', align: 'center', colKey: 5 },
        { title: '替换内容', dataIndex: 'replaceContent', align: 'center', colKey: 6 },
        { title: '排序', dataIndex: 'sortOrder', align: 'center', width: 80, colKey: 7 },
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
        pageSize: 6,
        name: null,
        applicationScene: null
    },
    rules: {
        name: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
        dataCategoryId: [{ required: true, message: '数据分类不能为空', trigger: 'change' }],
        applicationScene: [{ required: true, message: '应用场景不能为空', trigger: 'change' }],
        maskType: [{ required: true, message: '脱敏类型不能为空', trigger: 'change' }],
        replaceContent: [{ required: true, message: '替换内容不能为空', trigger: 'blur' }]
    }
});

const { queryParams, form, rules } = toRefs(data);

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

/** 下拉搜索过滤 */
function filterOption(input, option) {
    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

/** 查询脱敏规则列表 */
function getList() {
    loading.value = true;
    listDesensitizeRule(queryParams.value).then((response) => {
        const page = normalizePage(response);
        total.value = page.total;
        desensitizeRuleList.value = pageRows(page.rows, page.total, queryParams.value);
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
        name: null,
        dataCategoryId: null,
        dataCategoryName: null,
        applicationScene: null,
        maskType: "2",
        replaceRule: null,
        replaceContent: null,
        intervalList: [],
        sortOrder: null,
        description: null,
        validFlag: null
    };
    proxy.resetForm('desensitizeRuleRef');
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
    title.value = '新增脱敏规则';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDesensitizeRule(_id).then((response) => {
        const data = response.data;
        if (!data.intervalList) {
            data.intervalList = [];
        }
        form.value = data;
        open.value = true;
        title.value = '修改脱敏规则';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['desensitizeRuleRef']
        .validate()
        .then(() => {
            if (form.value.id != null) {
                updateDesensitizeRule(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDesensitizeRule(form.value)
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
        .confirm('是否确认删除脱敏规则编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDesensitizeRule(_ids);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess('删除成功');
        })
        .catch(() => { });
}

/** 添加区间 */
function addInterval() {
    if (!form.value.intervalList) {
        form.value.intervalList = [];
    }
    form.value.intervalList.push({
        id: null,
        desensitizeRuleId: null,
        intervalNo: form.value.intervalList.length + 1,
        startNum: null,
        endNum: null
    });
}

/** 删除区间 */
function removeInterval(index) {
    form.value.intervalList.splice(index, 1);
}

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
