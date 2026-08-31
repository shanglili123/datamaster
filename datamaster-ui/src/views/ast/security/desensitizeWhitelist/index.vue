<template>
    <div class="app-container" ref="app-container">

        <div class="pagecont-top" v-show="showSearch">
            <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '65px' } }"
                v-show="showSearch" @submit.prevent>
                <a-form-item label="白名单名称" name="name">
                    <a-input style="width: 150px;" v-model:value="queryParams.name" placeholder="请输入白名单名称"
                        allow-clear @pressEnter="handleQuery" />
                </a-form-item>
                <a-form-item label="生效范围" name="effectiveCategory">
                    <a-select style="width: 150px;" v-model:value="queryParams.effectiveCategory" placeholder="请选择生效范围"
                        allow-clear>
                        <a-select-option value="1">用户</a-select-option>
                        <a-select-option value="2">角色</a-select-option>
                        <a-select-option value="3">部门</a-select-option>
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
                <a-button type="primary" @click="handleAdd" v-hasPermi="['dg:desensitizewhitelist:add']"
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
                :data-source="whitelistList"
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
                    <template v-if="column.dataIndex === 'effectiveCategory'">
                        <span v-if="record.effectiveCategory === '1'">用户</span>
                        <span v-else-if="record.effectiveCategory === '2'">角色</span>
                        <span v-else-if="record.effectiveCategory === '3'">部门</span>
                        <span v-else>-</span>
                    </template>
                    <template v-if="column.dataIndex === 'startTime'">
                        <span>{{ parseTime(record.startTime, '{y}-{m}-{d}') }}</span>
                    </template>
                    <template v-if="column.dataIndex === 'endTime'">
                        <span>{{ parseTime(record.endTime, '{y}-{m}-{d}') }}</span>
                    </template>
                    <template v-if="column.dataIndex === 'sortOrder'">
                        {{ record.sortOrder || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'description'">
                        {{ record.description || '-' }}
                    </template>
                    <template v-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="handleUpdate(record)"
                            v-hasPermi="['dg:desensitizewhitelist:edit']">修改</a-button>
                        <a-button type="link" danger size="small" @click="handleDelete(record)"
                            v-hasPermi="['dg:desensitizewhitelist:remove']">删除</a-button>
                    </template>
                </template>
            </a-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>

        <!-- 新增或修改脱敏白名单对话框 -->
        <a-modal :title="title" v-model:open="open" width="900px" draggable @ok="submitForm" @cancel="cancel">
            <a-form ref="desensitizeWhitelistRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }"
                @submit.prevent>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="白名单名称" name="name">
                            <a-input v-model:value="form.name" placeholder="请输入白名单名称" />
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
                        <a-form-item label="生效范围" name="effectiveCategory">
                            <a-select v-model:value="form.effectiveCategory" placeholder="请选择生效范围">
                                <a-select-option value="1">用户</a-select-option>
                                <a-select-option value="2">角色</a-select-option>
                                <a-select-option value="3">部门</a-select-option>
                            </a-select>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="排序" name="sortOrder">
                            <a-input-number v-model:value="form.sortOrder" style="width: 100%;" placeholder="请输入排序" :min="0" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="生效开始时间" name="startTime">
                            <a-date-picker v-model:value="form.startTime" value-format="YYYY-MM-DD"
                                style="width: 100%;" placeholder="请选择开始时间" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="生效结束时间" name="endTime">
                            <a-date-picker v-model:value="form.endTime" value-format="YYYY-MM-DD"
                                style="width: 100%;" placeholder="请选择结束时间" />
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

                <!-- 用户子表格 -->
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="豁免用户">
                            <a-table
                                bordered
                                size="small"
                                :data-source="form.userList"
                                :pagination="false"
                                :locale="{ emptyText: '暂无用户' }"
                                :columns="userTableColumns"
                            >
                                <template #bodyCell="{ column, record: userRecord, index }">
                                    <template v-if="column.dataIndex === 'userId'">
                                        <a-select
                                            v-model:value="userRecord.userId"
                                            placeholder="请选择用户"
                                            show-search
                                            :filter-option="filterUserOption"
                                            style="width: 100%;"
                                            @change="(val) => onUserSelect(val, userRecord)"
                                        >
                                            <a-select-option v-for="u in userOptions" :key="u.value" :value="u.value">
                                                {{ u.label }}
                                            </a-select-option>
                                        </a-select>
                                    </template>
                                    <template v-if="column.dataIndex === 'userName'">
                                        {{ userRecord.userName || '-' }}
                                    </template>
                                    <template v-if="column.key === 'action'">
                                        <a-button type="link" danger size="small" @click="removeUser(index)">删除</a-button>
                                    </template>
                                </template>
                            </a-table>
                            <a-button type="dashed" style="width: 100%; margin-top: 8px;" @click="addUser">
                                <i class="iconfont-mini icon-xinzeng mr5"></i>添加用户
                            </a-button>
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

<script setup name="DesensitizeWhitelist">
import {
    listDesensitizeWhitelist,
    getDesensitizeWhitelist,
    addDesensitizeWhitelist,
    updateDesensitizeWhitelist,
    delDesensitizeWhitelist,
    listDataCategoryAll,
    listUser
} from '@/api/governance/desensitizeWhitelist';
import { normalizePage, pageRows } from "@/utils/page.js";

const { proxy } = getCurrentInstance();
const whitelistList = ref([]);
const dataCategoryOptions = ref([]);
const userOptions = ref([]);

// 列显隐信息
const columns = ref([
    { key: 1, label: '编号', visible: true },
    { key: 2, label: '白名单名称', visible: true },
    { key: 3, label: '数据分类', visible: true },
    { key: 4, label: '生效范围', visible: true },
    { key: 5, label: '生效开始时间', visible: true },
    { key: 6, label: '生效结束时间', visible: true },
    { key: 7, label: '排序', visible: true },
    { key: 8, label: '描述', visible: true },
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
        { title: '白名单名称', dataIndex: 'name', align: 'left', colKey: 2 },
        { title: '数据分类', dataIndex: 'dataCategoryName', align: 'center', colKey: 3 },
        { title: '生效范围', dataIndex: 'effectiveCategory', align: 'center', colKey: 4 },
        { title: '生效开始时间', dataIndex: 'startTime', align: 'center', width: 120, colKey: 5 },
        { title: '生效结束时间', dataIndex: 'endTime', align: 'center', width: 120, colKey: 6 },
        { title: '排序', dataIndex: 'sortOrder', align: 'center', width: 80, colKey: 7 },
        { title: '描述', dataIndex: 'description', align: 'left', ellipsis: true, colKey: 8 },
        { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200, colKey: 9 },
    ];
    return allCols.filter(col => getColumnVisibility(col.colKey));
});

// 用户子表格列配置
const userTableColumns = [
    { title: '选择用户', dataIndex: 'userId', align: 'center', width: 250 },
    { title: '用户名称', dataIndex: 'userName', align: 'center' },
    { title: '操作', key: 'action', align: 'center', width: 80 }
];

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
        effectiveCategory: null
    },
    rules: {
        name: [{ required: true, message: '白名单名称不能为空', trigger: 'blur' }],
        dataCategoryId: [{ required: true, message: '数据分类不能为空', trigger: 'change' }],
        effectiveCategory: [{ required: true, message: '生效范围不能为空', trigger: 'change' }]
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

/** 加载用户下拉列表 */
function loadUserOptions() {
    listUser({ pageSize: 1000 }).then((response) => {
        const list = response.data?.rows || response.data || [];
        userOptions.value = list.map(item => ({
            value: item.userId,
            label: item.nickName || item.userName
        }));
    });
}

/** 下拉搜索过滤 */
function filterOption(input, option) {
    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

/** 用户下拉搜索过滤 */
function filterUserOption(input, option) {
    return option.children?.toLowerCase().indexOf(input.toLowerCase()) >= 0
        || option.label?.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

/** 用户选择回调 */
function onUserSelect(userId, record) {
    const user = userOptions.value.find(u => u.value === userId);
    if (user) {
        record.userName = user.label;
    }
}

/** 查询脱敏白名单列表 */
function getList() {
    loading.value = true;
    listDesensitizeWhitelist(queryParams.value).then((response) => {
        const page = normalizePage(response);
        total.value = page.total;
        whitelistList.value = pageRows(page.rows, page.total, queryParams.value);
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
        effectiveCategory: null,
        startTime: null,
        endTime: null,
        sortOrder: null,
        description: null,
        validFlag: null,
        userList: []
    };
    proxy.resetForm('desensitizeWhitelistRef');
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
    title.value = '新增脱敏白名单';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDesensitizeWhitelist(_id).then((response) => {
        form.value = response.data;
        if (!form.value.userList) {
            form.value.userList = [];
        }
        open.value = true;
        title.value = '修改脱敏白名单';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['desensitizeWhitelistRef']
        .validate()
        .then(() => {
            if (form.value.id != null) {
                updateDesensitizeWhitelist(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDesensitizeWhitelist(form.value)
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
        .confirm('是否确认删除脱敏白名单编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDesensitizeWhitelist(_ids);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess('删除成功');
        })
        .catch(() => { });
}

/** 添加用户行 */
function addUser() {
    if (!form.value.userList) {
        form.value.userList = [];
    }
    form.value.userList.push({
        id: null,
        desensitizeId: null,
        userId: null,
        userName: null,
        effectiveCategory: form.value.effectiveCategory
    });
}

/** 删除用户行 */
function removeUser(index) {
    form.value.userList.splice(index, 1);
}

loadDataCategoryOptions();
loadUserOptions();
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
