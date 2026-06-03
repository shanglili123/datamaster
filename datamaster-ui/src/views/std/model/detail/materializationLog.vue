<template>
    <!-- 逻辑模型 物化 -->
    <div class="justify-between mb15">
        <el-row :gutter="15" class="btn-style">
            <el-col :span="1.5">
                <el-button type="primary" plain :disabled="row.status == 0" @click="handleMaterialization"
                    v-hasPermi="['dp:model:edit']" @mousedown="(e) => e.preventDefault()">
                    <svg-icon iconClass="wh" style="font-size: 14px; margin-right: 6px;" :class="{
                        'icon-disabled': single,
                        'icon-normal': !single
                    }" />物化
                </el-button>

            </el-col>
        </el-row>
        <div class="justify-end top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </div>
    </div>
    <el-table stripe height="38.5vh" v-loading="loading" :data="dpModelMaterializedList"
        @selection-change="handleSelectionChange" :default-sort="defaultSort" @sort-change="handleSortChange">
        <el-table-column label="编号" align="left" prop="id" width="50" />
        <el-table-column v-if="columns[1].visible" label="模型编码" align="left" prop="modelName" width="265">
            <template #default="scope">
                {{ scope.row.modelName || '-' }}
            </template>
        </el-table-column>
        <el-table-column v-if="columns[2].visible" :show-overflow-tooltip="{ effect: 'light' }" label="模型名称"
            align="left" prop="modelAlias" width="180">
            <template #default="scope">
                {{ scope.row.modelAlias || '-' }}
            </template>
        </el-table-column>
        <el-table-column label="描述" align="left" prop="description" :show-overflow-tooltip="{ effect: 'light' }"
            width="250">
            <template #default="scope">
                {{ scope.row.description || '-' }}
            </template>
        </el-table-column>
        <el-table-column v-if="columns[5].visible" :label="columns[5].label"
            :show-overflow-tooltip="{ effect: 'light' }" align="left" prop="message" width="220">
            <template #default="scope">
                {{ scope.row.message || '-' }}
            </template>
        </el-table-column>
        <el-table-column v-if="columns[8].visible" label="数据连接类型" align="left" prop="datasourceType" width="160">
            <template #default="scope">
                {{ scope.row.datasourceType || '-' }}
            </template>
        </el-table-column>
        <el-table-column v-if="columns[9].visible" label="数据连接名称" :show-overflow-tooltip="{ effect: 'light' }"
            align="left" prop="datasourceName" width="265">
            <template #default="scope">
                {{ scope.row.datasourceName || '-' }}
            </template>
        </el-table-column>

        <el-table-column v-if="columns[13].visible" label="创建人" align="left" prop="createBy" width="120">
            <template #default="scope">
                {{ scope.row.createBy || '-' }}
            </template>
        </el-table-column>
        <el-table-column v-if="columns[15].visible" label="创建时间" align="left" prop="createTime" width="265">
            <template #default="scope">
                <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
            </template>
        </el-table-column>
        <el-table-column v-if="columns[4].visible" label="状态" align="left" prop="status" width="80">
            <template #default="scope">
                <dict-tag :options="dp_template_build_log_build_status" :value="scope.row.status" />
            </template>
        </el-table-column>
        <el-table-column label="备注" align="left" prop="remark" :show-overflow-tooltip="{ effect: 'light' }">
            <template #default="scope">
                {{ scope.row.remark || '-' }}
            </template>
        </el-table-column>
        <template #empty>
            <div class="emptyBg">
                <img src="@/assets/system/images/no_data/noData.png" alt="" />
                <p>暂无记录</p>
            </div>
        </template>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增或修改物化模型记录对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" :append-to="$refs['app-container']" draggable>
        <el-form ref="dpModelMaterializedRef" :model="form" :rules="rules" label-width="80px">
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="模型编码" prop="modelName">
                        <el-input v-model="form.modelName" placeholder="请输入模型编码" />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="模型名称" prop="modelAlias">
                        <el-input v-model="form.modelAlias" placeholder="请输入模型名称" />
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="模型表id" prop="modelId">
                        <el-input v-model="form.modelId" placeholder="请输入模型表id" />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="状态" prop="status">
                        <el-radio-group v-model="form.status">
                            <el-radio v-for="dict in dp_template_build_log_build_status" :key="dict.value"
                                :label="dict.value">{{ dict.label }}</el-radio>
                        </el-radio-group>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="24">
                    <el-form-item label="执行日志信息" prop="message">
                        <el-input v-model="form.message" type="textarea" placeholder="请输入内容" />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="执行sql备份" prop="sqlCommand">
                        <el-input v-model="form.sqlCommand" placeholder="请输入执行sql备份" />
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="数据源id" prop="datasourceId">
                        <el-input v-model="form.datasourceId" placeholder="请输入数据源id" />
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="数据连接名称" prop="datasourceName">
                        <el-input v-model="form.datasourceName" placeholder="请输入数据连接名称" />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="资产表id" prop="assetId">
                        <el-input v-model="form.assetId" placeholder="请输入资产表id" />
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="备注" prop="remark">
                        <el-input v-model="form.remark" placeholder="请输入备注" />
                    </el-form-item>
                </el-col>
            </el-row>
        </el-form>
        <template #footer>
            <div class="dialog-footer">
                <el-button size="mini" @click="cancel">取 消</el-button>
                <el-button type="primary" size="mini" @click="submitForm">确 定</el-button>
            </div>
        </template>
    </el-dialog>

    <!-- 物化模型记录详情对话框 -->
    <el-dialog :title="title" v-model="openDetail" width="800px" :append-to="$refs['app-container']" draggable>
        <el-form ref="dpModelMaterializedRef" :model="form" label-width="80px">
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="模型编码" prop="modelName">
                        <div>
                            {{ form.modelName }}
                        </div>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="模型名称" prop="modelAlias">
                        <div>
                            {{ form.modelAlias }}
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="模型表id" prop="modelId">
                        <div>
                            {{ form.modelId }}
                        </div>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="状态" prop="status">
                        <dict-tag :options="dp_template_build_log_build_status" :value="form.status" />
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="24">
                    <el-form-item label="执行日志信息" prop="message">
                        <div>
                            {{ form.message }}
                        </div>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="执行sql备份" prop="sqlCommand">
                        <div>
                            {{ form.sqlCommand }}
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="数据源id" prop="datasourceId">
                        <div>
                            {{ form.datasourceId }}
                        </div>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="数据连接类型" prop="datasourceType">
                        <div>
                            {{ form.datasourceType }}
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="数据连接名称" prop="datasourceName">
                        <div>
                            {{ form.datasourceName }}
                        </div>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="资产表id" prop="assetId">
                        <div>
                            {{ form.assetId }}
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="备注" prop="remark">
                        <div>
                            {{ form.remark }}
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>
        </el-form>
        <template #footer>
            <div class="dialog-footer">
                <el-button size="mini" @click="cancel">关 闭</el-button>
            </div>
        </template>
    </el-dialog>

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
        pageSize: 10,
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
function handleSelectionChange(selection) {
    ids.value = selection.map((item) => item.id);
    single.value = selection.length != 1;
    multiple.value = !selection.length;
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
    queryParams.value.orderByColumn = column.prop;
    queryParams.value.isAsc = column.order;
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
    proxy.$refs['dpModelMaterializedRef'].validate((valid) => {
        if (valid) {
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
        }
    });
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
        'dp/model/export',
        {
            ...queryParams.value
        },
        `dpModelMaterialized_${new Date().getTime()}.xlsx`
    );
}

getList();
</script>

