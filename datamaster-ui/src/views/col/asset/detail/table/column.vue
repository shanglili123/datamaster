<template>
    <!-- 资产字段 tab -->
    <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
            <!--      <a-col :span="1.5">-->
            <!--        <a-button type="primary" @click="handleAdd" v-hasPermi="['ast:assetColumn:assetcolumn:add']"-->
            <!--                   @mousedown="(e) => e.preventDefault()">-->
            <!--          <i class="iconfont-mini icon-xinzeng mr5"></i>新增-->
            <!--        </a-button>-->
            <!--      </a-col>-->
            <!--      <a-col :span="1.5">-->
            <!--        <a-button type="primary" @click="handleExport" v-hasPermi="['ast:assetColumn:assetcolumn:export']"-->
            <!--                   @mousedown="(e) => e.preventDefault()">-->
            <!--          <i class="iconfont-mini icon-download-line mr5"></i>导出-->
            <!--        </a-button>-->
            <!--      </a-col>-->
        </a-row>
        <div class="justify-end top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </div>
    </div>
    <a-table
        striped
        :loading="loading"
        :data-source="daAssetColumnList"
        :columns="tableColumns"
        :pagination="false"
        :scroll="{ x: 1975, y: 'calc(100vh - 500px)' }"
        :locale="{ emptyText: '暂无记录' }"
        @change="handleTableChange"
    >
        <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'columnComment'">
                {{ record.columnComment || '-' }}
            </template>
            <template v-if="column.dataIndex === 'columnName'">
                {{ record.columnName || '-' }}
            </template>
            <template v-if="column.dataIndex === 'columnType'">
                {{ record.columnType || '-' }}
            </template>
            <template v-if="column.dataIndex === 'columnLength'">
                {{ record.columnLength || '-' }}
            </template>
            <template v-if="column.dataIndex === 'columnScale'">
                {{ record.columnScale || '-' }}
            </template>
            <template v-if="column.dataIndex === 'nullableFlag'">
                <a-switch v-model:checked="record.nullableFlag" checked-value="1" un-checked-value="0" disabled />
            </template>
            <template v-if="column.dataIndex === 'pkFlag'">
                <a-switch v-model:checked="record.pkFlag" checked-value="1" un-checked-value="0" disabled />
            </template>
            <template v-if="column.dataIndex === 'description'">
                {{ record.description || '-' }}
            </template>
            <template v-if="column.dataIndex === 'sensitiveLevelName'">
                {{ record.sensitiveLevelName || '-' }}
            </template>
            <template v-if="column.dataIndex === 'dataElemCodeFlag'">
                <span v-show="record.dataElemCodeFlag">
                    {{ record.dataElemCodeName || '-' }}
                </span>
            </template>
            <template v-if="column.dataIndex === 'relDataElmeFlag'">
                <span v-show="record.dataElemCodeFlag">
                    {{ record.relDataElmeName || '-' }}
                </span>
            </template>
            <template v-if="column.dataIndex === 'createBy'">
                {{ record.createBy || '-' }}
            </template>
            <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
            </template>
            <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="handleUpdate(record)"
                    v-hasPermi="['ast:assetColumn:edit']"
>修改</a-button>
            </template>
        </template>
    </a-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList"
/>

    <!-- 新增或修改数据资产对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
        <template #header="{ close, titleId, titleClass }">
            <span role="heading" aria-level="2">
                {{ title }}
            </span>
        </template>
        <a-form ref="daAssetRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="中文名称" name="columnComment">
                        <a-input v-model:value="form.columnComment" placeholder="请输入中文名称" disabled />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="英文名称" name="columnName">
                        <a-input v-model:value="form.columnName" placeholder="请输入英文名称" disabled />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="字段类型" name="columnType">
                        <a-input v-model:value="form.columnType" placeholder="请输入字段类型" disabled />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="字段长度" name="columnLength">
                        <a-input v-model:value="form.columnLength" placeholder="请输入字段长度" disabled />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="小数位" name="columnScale">
                        <a-input-number :step="1" v-model:value="form.columnScale" style="width: 100%"
                            :min="0" :max="100" placeholder="请输入小数长度" disabled
/>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="是否可空" name="nullableFlag">
                        <a-radio-group v-model:value="form.nullableFlag">
                            <a-radio v-for="dict in dp_model_column_pk_flag" :key="dict.value" :value="dict.value"
                                disabled
>{{ dict.label }}</a-radio>
                        </a-radio-group>
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
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="是否主键" name="pkFlag">
                        <a-radio-group v-model:value="form.pkFlag">
                            <a-radio v-for="dict in dp_model_column_pk_flag" :key="dict.value" :value="dict.value"
                                disabled
>
                                {{ dict.label }}
                            </a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="敏感等级" name="sensitiveLevelId">
                        <a-select v-model:value="form.sensitiveLevelId" show-search allow-clear placeholder="请选择敏感等级"
                            @change="val => form.sensitiveLevelId = val ?? null"
>
                            <a-select-option v-for="item in daSensitiveLevelList" :key="item.id" :label="item.sensitiveLevel"
                                :value="item.id"
>{{ item.sensitiveLevel }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="数据分类" name="dataCategoryId" :label-col="{ style: { width: '100px' } }">
                        <a-select v-model:value="form.dataCategoryId" show-search allow-clear placeholder="请选择数据分类（绑定后用于脱敏规则匹配）"
                            @change="val => form.dataCategoryId = val ?? null"
>
                            <a-select-option v-for="item in dataCategoryList" :key="item.id" :label="item.name"
                                :value="item.id"
>{{ item.name }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="关联代码表" name="dataElemCodeFlag">
                        <a-radio-group v-model:value="form.dataElemCodeFlag">
                            <a-radio v-for="dict in dp_model_column_pk_flag" :key="dict.value" :value="dict.value">
                                {{ dict.label }}
                            </a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="选择代码表" name="dataElemCodeId">
                        <a-select v-model:value="form.dataElemCodeId" :disabled="form.dataElemCodeFlag == '0'" allow-clear
                            show-search placeholder="选择代码表"
>
                            <a-select-option v-for="item in codeTableList" :key="item.id" :label="item.name" :value="item.id">{{ item.name }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>

            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="关联数据元" name="relDataElmeFlag">
                        <a-radio-group v-model:value="form.relDataElmeFlag">
                            <a-radio v-for="dict in dp_model_column_pk_flag" :key="dict.value" :value="dict.value">
                                {{ dict.label }}
                            </a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="选择数据元" name="elementId">
                        <a-select v-model:value="form.elementId" mode="multiple" :disabled="form.relDataElmeFlag == '0'" allow-clear
                            show-search placeholder="请选择数据元"
>
                            <a-select-option v-for="item in elementList" :key="item.id" :label="item.name" :value="item.id">{{ item.name }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>
        <template #footer>
            <div class="dialog-footer">
                <a-button @click="cancel">取 消</a-button>
                <a-button type="primary" @click="submitForm">确 定</a-button>
            </div>
        </template>
    </a-modal>

    <!-- 数据资产详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="800px" destroy-on-close>
        <template #header="{ close, titleId, titleClass }">
            <span role="heading" aria-level="2">
                {{ title }}
            </span>
        </template>
        <a-form ref="daAssetRef" :model="form" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="中文名称" name="columnComment">
                        <div>
                            {{ form.columnComment }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="英文名称" name="columnName">
                        <div>
                            {{ form.columnName }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="字段类型" name="columnType">
                        <div>
                            {{ form.columnType }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="小数位" name="columnScale">
                        <div>
                            {{ form.columnScale }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="是否可空" name="nullableFlag">
                        <div>
                            {{ form.nullableFlag }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="是否主键" name="pkFlag">
                        <div>
                            {{ form.pkFlag }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="敏感等级" name="sensitiveLevelName">
                        <div>
                            {{ form.sensitiveLevelName }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="关联代码表" name="dataElemCodeFlag">
                        <div>
                            {{ form.dataElemCodeFlag }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="关联数据元" name="relDataElmeFlag">
                        <div>
                            {{ form.relDataElmeFlag }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="描述" name="description">
                        <div>
                            {{ form.description }}
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
</template>

<script setup name="ComponentOne">
import {
    listDaAsset,
    getDaAsset,
    delDaAsset,
    addDaAsset,
    updateDaAsset
} from '@/api/ast/asset/asset';
import {
    listDaAssetColumn,
    getDaAssetColumn,
    updateDaAssetColumn
} from '@/api/ast/asset/assetColumn.js';
import { listDaSensitiveLevel } from '@/api/ast/security/sensitiveLevel/sensitiveLevel';
import { listDpDataElem } from '@/api/std/dataElem/dataElem';
import { listDataCategoryAll, listAssetcolumnBinding, addAssetcolumnBinding, updateAssetcolumnBinding } from '@/api/governance/assetcolumn.js';
import { useRoute } from 'vue-router';
import { ref, computed } from "vue";

const { proxy } = getCurrentInstance();
const { column_type, dp_model_column_pk_flag, dp_model_column_nullable_flag } = proxy.useDict(
    'column_type',
    'dp_model_column_pk_flag',
    'dp_model_column_nullable_flag'
);

const daAssetColumnList = ref([]);
const daSensitiveLevelList = ref([]);
const dataCategoryList = ref([]); //数据分类
const bindingId = ref(null); //当前绑定记录ID
const codeTableList = ref([]); //代码表
const elementList = ref([]); //数据元
const defaultSort = ref({ columnKey: 'create_time', order: 'desc' });
// 列显隐信息
const columns = ref([
    { key: 0, label: '中文名称', visible: true },
    { key: 1, label: '英文名称', visible: true },
    { key: 2, label: '字段类型', visible: true },
    { key: 3, label: '字段长度', visible: true },
    { key: 4, label: '小数位', visible: true },
    { key: 5, label: '是否可空', visible: true },
    { key: 6, label: '是否主键', visible: true },
    { key: 7, label: '描述', visible: true },
    { key: 8, label: '敏感等级', visible: true },
    { key: 9, label: '关联代码表', visible: true },
    { key: 10, label: '关联数据元', visible: true },
    { key: 11, label: '创建人', visible: true },
    { key: 12, label: '创建时间', visible: true },
    { key: 13, label: '备注', visible: true },
    { key: 14, label: '操作', visible: true },
]);

const tableColumns = computed(() => {
    const allCols = [
        { title: '中文名称', dataIndex: 'columnComment', align: 'left', width: 185, ellipsis: true, colKey: 0 },
        { title: '英文名称', dataIndex: 'columnName', align: 'left', width: 180, ellipsis: true, colKey: 1 },
        { title: '字段类型', dataIndex: 'columnType', align: 'left', width: 140, ellipsis: true, colKey: 2 },
        { title: '字段长度', dataIndex: 'columnLength', align: 'left', width: 80, ellipsis: true, colKey: 3 },
        { title: '小数位', dataIndex: 'columnScale', align: 'left', width: 80, ellipsis: true, colKey: 4 },
        { title: '是否可空', dataIndex: 'nullableFlag', align: 'left', width: 80, colKey: 5 },
        { title: '是否主键', dataIndex: 'pkFlag', align: 'left', width: 100, colKey: 6 },
        { title: '描述', dataIndex: 'description', align: 'left', width: 260, ellipsis: true, colKey: 7 },
        { title: '敏感等级', dataIndex: 'sensitiveLevelName', align: 'left', width: 100, colKey: 8 },
        { title: '关联代码表', dataIndex: 'dataElemCodeFlag', align: 'left', width: 200, ellipsis: true, colKey: 9 },
        { title: '关联数据元', dataIndex: 'relDataElmeFlag', align: 'left', width: 200, ellipsis: true, colKey: 10 },
        { title: '创建人', dataIndex: 'createBy', align: 'left', width: 120, ellipsis: true, colKey: 11 },
        { title: '创建时间', dataIndex: 'createTime', align: 'left', width: 150, key: 'create_time', sorter: true, defaultSortOrder: 'descend', colKey: 12 },
        { title: '操作', key: 'actions', align: 'left', fixed: 'right', width: 100, colKey: 14 },
    ];
    return allCols.filter(col => {
        const colConfig = columns.value[col.colKey];
        return colConfig ? colConfig.visible : true;
    });
});

const open = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');
const data = reactive({
    daAssetDetail: {},
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 20,
        name: null,
        catCode: null,
        themeId: null,
        datasourceId: null,
        tableName: null,
        tableComment: null,
        dataCount: null,
        fieldCount: null,
        status: null,
        description: null,
        createTime: null
    },
    rules: {
        // columnComment: [{ required: true, message: '请输入中文名称', trigger: 'blur' }],
        // columnName: [{ required: true, message: '请输入英文名称', trigger: 'blur' }],
        dataElemCodeId: [{ required: false, message: '请选择代码表', trigger: 'blur' }],
        elementId: [{ required: false, message: '请选择数据元', trigger: 'blur' }],
        // pkFlag: [{ required: true, message: '请选择是否主键', trigger: 'blur' }],
        // nullableFlag: [{ required: true, message: '请选择是是否必填', trigger: 'blur' }],
        // dataElemCodeFlag: [{ required: true, message: '请选择是是否必填', trigger: 'blur' }],
        // relDataElmeFlag: [
        //     { required: true, message: '请选择是是否关联数据元', trigger: 'blur' }
        // ],
        // relCleanFlag: [
        //     { required: true, message: '请选择是是否关联清洗规则', trigger: 'blur' }
        // ],
        relAuditFlag: [{ required: true, message: '请选择是是否关联稽查规则', trigger: 'blur' }]
    }
});

const { queryParams, form, daAssetDetail, rules } = toRefs(data);
const route = useRoute();
let assetId = route.query.id || 1;
watch(
    () => route.query.id,
    (newId) => {
        assetId = newId || 1;
        getList();
    },
    { immediate: true }
);

/** 查询数据资产列表 */
function getList() {
    loading.value = true;
    queryParams.value.assetId = assetId;
    listDaAssetColumn(queryParams.value).then((response) => {
        const page = response.data || {};
        const rows = Array.isArray(page) ? page : (page.rows || []);
        daAssetColumnList.value = rows;
        total.value = Number(page.total) || rows.length;
        loading.value = false;
    });
}

/** 获取敏感等级列表 */
function getDaSensitiveLevelList() {
    listDaSensitiveLevel().then((response) => {
        daSensitiveLevelList.value = response.data.rows;
        //将id转换为String类型
        daSensitiveLevelList.value.forEach((item) => {
            item.id = item.id.toString();
        });
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
        name: null,
        catCode: null,
        themeId: null,
        datasourceId: null,
        tableName: null,
        tableComment: null,
        dataCount: null,
        fieldCount: null,
        status: null,
        description: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updaterId: null,
        updateTime: null,
        dataCategoryId: null
    };
    bindingId.value = null;
    proxy.resetForm('daAssetRef');
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
    title.value = '新增数据资产';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDaAssetColumn(_id).then((response) => {
        form.value = response.data;
        // 加载该字段的脱敏绑定
        loadBinding(_id, response.data.assetId);
        open.value = true;
        title.value = '修改数据资产';
    });
}

/** 加载字段脱敏绑定 */
function loadBinding(columnId, assetIdVal) {
    listAssetcolumnBinding({ assetcolumnId: columnId, pageNum: 1, pageSize: 10 }).then((res) => {
        const rows = res.data?.rows || [];
        if (rows.length > 0) {
            bindingId.value = rows[0].id;
            form.value.dataCategoryId = rows[0].dataCategoryId;
        } else {
            bindingId.value = null;
        }
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getDaAsset(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '数据资产详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['daAssetRef'].validate().then(() => {
        const columnId = form.value.id;
        const dataCategoryId = form.value.dataCategoryId;
        // 1. 先保存/更新绑定关系
        const saveBinding = () => {
            if (!columnId || !dataCategoryId) return Promise.resolve();
            const bindingData = {
                assetId: assetId,
                assetcolumnId: columnId,
                dataCategoryId: dataCategoryId
            };
            if (bindingId.value) {
                bindingData.id = bindingId.value;
                return updateAssetcolumnBinding(bindingData);
            } else {
                return addAssetcolumnBinding(bindingData);
            }
        };
        // 2. 保存字段本身
        const saveColumn = () => {
            if (columnId != null) {
                const colData = { ...form.value };
                delete colData.updateTime;
                delete colData.dataCategoryId; // 字段表无此列
                return updateDaAssetColumn(colData);
            } else {
                return addDaAsset(form.value);
            }
        };
        saveColumn().then(() => {
            return saveBinding();
        }).then(() => {
            proxy.$modal.msgSuccess(columnId != null ? '修改成功' : '新增成功');
            open.value = false;
            getList();
        }).catch(() => {});
    }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
    const _ids = row.id || ids.value;
    proxy.$modal
        .confirm('是否确认删除数据资产编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDaAsset(_ids);
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
        'ast/asset/export',
        {
            ...queryParams.value
        },
        `daAsset_${new Date().getTime()}.xlsx`
    );
}

/** 获取数据元列表 */
function getElementList() {
    listDpDataElem({ type: 1 }).then((response) => {
        elementList.value = response.data.rows;
    });
}
/** 获取代码表列表 */
function getCodeTableList() {
    listDpDataElem({ type: 2 }).then((response) => {
        codeTableList.value = response.data.rows;
        codeTableList.value.forEach((item) => {
            item.id = item.id.toString();
        });
    });
}

/** 获取数据分类列表 */
function getDataCategoryList() {
    listDataCategoryAll({}).then((response) => {
        dataCategoryList.value = response.data?.rows || response.data || [];
    });
}

getElementList();
getCodeTableList();
getDaSensitiveLevelList();
getDataCategoryList();
</script>

