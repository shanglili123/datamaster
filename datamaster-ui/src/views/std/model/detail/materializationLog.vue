<template>
    <!-- 逻辑模型 物化 -->
    <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
            <a-col :span="1.5">
                <a-button type="primary" :disabled="row.status == 0" @click="handleMaterialization"
                    v-hasPermi="['dp:model:edit']" @mousedown="(e) => e.preventDefault()">
                    <svg-icon iconClass="wh" style="font-size: 14px; margin-right: 6px;" :class="{
                        'icon-disabled': single,
                        'icon-normal': !single
                    }" />物化
                </a-button>

            </a-col>
        </a-row>
        <div class="justify-end top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </div>
    </div>
    <a-table
      stripe
      :loading="loading"
      :data-source="dpModelMaterializedList"
      :columns="tableColumns"
      :pagination="false"
      :scroll="{ y: '38.5vh' }"
      row-key="id"
      :locale="{ emptyText: emptyContent }"
      @change="handleSortChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'id'">
          {{ record.id }}
        </template>
        <template v-else-if="column.dataIndex === 'modelName'">
          {{ record.modelName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'modelAlias'">
          {{ record.modelAlias || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'message'">
          {{ record.message || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'datasourceType'">
          {{ record.datasourceType || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'datasourceName'">
          {{ record.datasourceName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createBy'">
          {{ record.createBy || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <dict-tag :options="dp_template_build_log_build_status" :value="record.status" />
        </template>
        <template v-else-if="column.dataIndex === 'remark'">
          {{ record.remark || '-' }}
        </template>
      </template>
    </a-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增或修改物化模型记录对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" draggable>
        <a-form ref="dpModelMaterializedRef" :model="form" :rules="rules"
            :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="模型编码" name="modelName">
                        <a-input v-model:value="form.modelName" placeholder="请输入模型编码" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="模型名称" name="modelAlias">
                        <a-input v-model:value="form.modelAlias" placeholder="请输入模型名称" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="模型表id" name="modelId">
                        <a-input v-model:value="form.modelId" placeholder="请输入模型表id" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="状态" name="status">
                        <a-radio-group v-model:value="form.status">
                            <a-radio v-for="dict in dp_template_build_log_build_status" :key="dict.value"
                                :value="dict.value">{{ dict.label }}</a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="执行日志信息" name="message">
                        <a-textarea v-model:value="form.message" placeholder="请输入内容" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="执行sql备份" name="sqlCommand">
                        <a-input v-model:value="form.sqlCommand" placeholder="请输入执行sql备份" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="数据源id" name="datasourceId">
                        <a-input v-model:value="form.datasourceId" placeholder="请输入数据源id" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="数据连接名称" name="datasourceName">
                        <a-input v-model:value="form.datasourceName" placeholder="请输入数据连接名称" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="资产表id" name="assetId">
                        <a-input v-model:value="form.assetId" placeholder="请输入资产表id" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="备注" name="remark">
                        <a-input v-model:value="form.remark" placeholder="请输入备注" />
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

    <!-- 物化模型记录详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="800px" draggable>
        <a-form ref="dpModelMaterializedRef" :model="form" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="模型编码" name="modelName">
                        <div>
                            {{ form.modelName }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="模型名称" name="modelAlias">
                        <div>
                            {{ form.modelAlias }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="模型表id" name="modelId">
                        <div>
                            {{ form.modelId }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="状态" name="status">
                        <dict-tag :options="dp_template_build_log_build_status" :value="form.status" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="执行日志信息" name="message">
                        <div>
                            {{ form.message }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="执行sql备份" name="sqlCommand">
                        <div>
                            {{ form.sqlCommand }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="数据源id" name="datasourceId">
                        <div>
                            {{ form.datasourceId }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="数据连接类型" name="datasourceType">
                        <div>
                            {{ form.datasourceType }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="数据连接名称" name="datasourceName">
                        <div>
                            {{ form.datasourceName }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="资产表id" name="assetId">
                        <div>
                            {{ form.assetId }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="备注" name="remark">
                        <div>
                            {{ form.remark }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>
        <template #footer>
            <div class="dialog-footer">
                <a-button size="small" @click="cancel">关 闭</a-button>
            </div>
        </template>
    </a-modal>

    <MaterializationDialog :title="title" :visible="Materialization"
        @update:dialogFormVisible="Materialization = $event" :ids="modelIds" @confirm="getList" />
</template>

<script setup name="ComponentOne">
import {
    listDpModelMaterialized,
    getDpModelMaterialized,
    delDpModelMaterialized,
    addDpModelMaterialized,
    updateDpModelMaterialized
} from '@/api/std/model/model';
import MaterializationDialog from './materialization.vue';
const { proxy } = getCurrentInstance();
const { dp_template_build_log_build_status } = proxy.useDict(
    'dp_template_build_log_build_status'
);
const dpModelMaterializedList = ref([]);
const props = defineProps({
    row: { type: Array, default: () => [] }
});
// 列显隐信息
const columns = ref([
    { key: 0, label: 'ID', visible: true },
    { key: 1, label: '模型编码', visible: true },
    { key: 2, label: '模型名称', visible: true },
    { key: 3, label: '模型表id', visible: true },
    { key: 4, label: '状态', visible: true },
    { key: 5, label: '执行日志信息', visible: true },
    { key: 6, label: '执行sql备份', visible: true },
    { key: 7, label: '数据源id', visible: true },
    { key: 8, label: '数据连接类型', visible: true },
    { key: 9, label: '数据连接名称', visible: true },
    { key: 10, label: '资产表id', visible: true },
    { key: 11, label: '是否有效', visible: true },
    { key: 12, label: '删除标志', visible: true },
    { key: 13, label: '创建人', visible: true },
    { key: 14, label: '创建人id', visible: true },
    { key: 15, label: '创建时间', visible: true },
    { key: 16, label: '更新人', visible: true },
    { key: 17, label: '更新人id', visible: true },
    { key: 18, label: '更新时间', visible: true },
    { key: 19, label: '备注', visible: true }
]);

const tableColumns = computed(() =>
    [
        { title: '编号', dataIndex: 'id', align: 'left', width: 50 },
        { title: '模型编码', dataIndex: 'modelName', align: 'left', width: 265, colKey: 1 },
        { title: '模型名称', dataIndex: 'modelAlias', align: 'left', width: 180, ellipsis: true, colKey: 2 },
        { title: '描述', dataIndex: 'description', align: 'left', width: 250, ellipsis: true },
        { title: '执行日志信息', dataIndex: 'message', align: 'left', width: 220, ellipsis: true, colKey: 5 },
        { title: '数据连接类型', dataIndex: 'datasourceType', align: 'left', width: 160, colKey: 8 },
        { title: '数据连接名称', dataIndex: 'datasourceName', align: 'left', width: 265, ellipsis: true, colKey: 9 },
        { title: '创建人', dataIndex: 'createBy', align: 'left', width: 120, colKey: 13 },
        { title: '创建时间', dataIndex: 'createTime', align: 'left', width: 265, colKey: 15 },
        { title: '状态', dataIndex: 'status', align: 'left', width: 80, colKey: 4 },
        { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true }
    ].filter((col) =>
        col.colKey === undefined ? true : columns.value[col.colKey].visible
    )
);

const emptyContent = h('div', { class: 'emptyBg' }, [
    h('img', {
        src: new URL(
            '@/assets/system/images/no_data/noData.png',
            import.meta.url
        ).href,
        alt: ''
    }),
    h('p', '暂无记录')
]);

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

const data = reactive({
    dpModelMaterializedDetail: {},
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        modelName: null,
        modelAlias: null,
        modelId: null,
        status: null,
        message: null,
        sqlCommand: null,
        datasourceId: null,
        datasourceType: null,
        datasourceName: null,
        assetId: null,
        createTime: null
    },
    rules: {}
});

const { queryParams, form, dpModelMaterializedDetail, rules } = toRefs(data);
const route = useRoute();
let modelId = route.query.id || 1;
// 监听 id 变化
watch(
    () => route.query.id,
    (newId) => {
        modelId = newId || 1; // 如果 id 为空，使用默认值 1
        getList();
    },
    { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);
const Materialization = ref(false);

/** 查询物化模型记录列表 */
function getList() {
    loading.value = true;
    listDpModelMaterialized({
        ...queryParams.value,
        modelId
    }).then((response) => {
        dpModelMaterializedList.value = response.data.rows;
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
        modelName: null,
        modelAlias: null,
        modelId: null,
        status: null,
        message: null,
        sqlCommand: null,
        datasourceId: null,
        datasourceType: null,
        datasourceName: null,
        assetId: null,
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
    proxy.resetForm('dpModelMaterializedRef');
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
        sorter.order === 'ascend'
            ? 'ascending'
            : sorter.order === 'descend'
                ? 'descending'
                : null;
    queryParams.value.orderByColumn =
        prop == 'createTime' ? 'create_time' : prop;
    queryParams.value.isAsc = order;
    getList();
}
let modelIds = [];
/** 物化按钮操作 */
function handleMaterialization() {
    Materialization.value = true;
    title.value = '逻辑物化';
    console.log('🚀 ~ handleMaterialization ~ modelId:', modelId);

    modelIds = [modelId];
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDpModelMaterialized(_id).then((response) => {
        form.value = response.data;
        open.value = true;
        title.value = '修改物化模型记录';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getDpModelMaterialized(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '物化模型记录详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['dpModelMaterializedRef']
        .validate()
        .then(() => {
            if (form.value.id != null) {
                updateDpModelMaterialized(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDpModelMaterialized(form.value)
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
        .confirm('是否确认删除物化模型记录编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDpModelMaterialized(_ids);
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
        'std/model/export',
        {
            ...queryParams.value
        },
        `dpModelMaterialized_${new Date().getTime()}.xlsx`
    );
}

getList();
</script>

