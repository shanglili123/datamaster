<template>
    <!-- 属性字段  -->
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
    <a-table
      stripe
      :loading="loading"
      :data-source="dpModelColumnList"
      :columns="tableColumns"
      :pagination="false"
      :scroll="{ y: '38.5vh' }"
      row-key="id"
      :locale="{ emptyText: emptyContent }"
      @change="handleSortChange"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'index'">
          <span>{{
            (queryParams.pageNum - 1) * queryParams.pageSize + index + 1
          }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'dataElemName'">
          {{ record.dataElemName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'cnName'">
          {{ record.cnName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'engName'">
          {{ record.engName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'columnType'">
          {{ record.columnType || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'columnLength'">
          {{ record.columnLength || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'pkFlag'">
          <a-switch v-model:checked="record.pkFlag" checked-value="1" un-checked-value="0" disabled />
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

    <!-- 新增或修改逻辑模型属性信息对话框 -->
    <a-modal class="autoHeight" :title="title" v-model:open="open" width="800px" draggable>
        <a-form ref="dpModelColumnRef" :model="form" :rules="rules"
            :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="关联标准" name="dataElemId">
                        <a-select v-model:value="form.dataElemId" placeholder="请选择关联标准" @change="handleDatasourceChange"
                            show-search>
                            <a-select-option v-for="dict in DpData" :key="dict.id" :value="dict.id">{{ dict.name
                            }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="中文名称" name="cnName">
                        <a-input v-model:value="form.cnName" placeholder="请输入中文名称" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="英文名称" name="engName">
                        <a-input v-model:value="form.engName" placeholder="请输入英文名称"
                            @input="convertToUpperCase('engName', form.engName)" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="数据类型" name="columnType">
                        <a-select v-model:value="form.columnType" placeholder="请选择数据类型">
                            <a-select-option v-for="dict in column_type" :key="dict.value" :value="dict.value">{{
                                dict.label }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>

            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="属性长度" name="columnLength">
                        <a-input-number :step="1" v-model:value="form.columnLength" style="width: 100%"
                            :min="1" :max="9999999999" placeholder="请输入属性长度" />
                    </a-form-item>
                </a-col>

            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="描述" name="modelComment">
                        <a-textarea v-model:value="form.modelComment" placeholder="请输入描述" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <!-- decimal、NUMERIC、number -->
                    <a-form-item label="小数位数" name="columnScale">
                        <a-input-number :disabled="form.columnType !== 'DECIMAL' &&
                            form.columnType !== 'NUMBER' &&
                            form.columnType !== 'NUMERIC' &&
                            form.columnType !== 'FLOAT' &&
                            form.columnType !== 'DOUBLE'
                            " :step="1" v-model:value="form.columnScale" style="width: 100%"
                            :min="0" :max="9999999999" placeholder="请输入小数长度" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="默认值" name="defaultValue">
                        <a-input v-model:value="form.defaultValue" placeholder="请输入默认值" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="是否主键" name="pkFlag">
                        <a-radio-group v-model:value="form.pkFlag" @change="handlePkFlagChange">
                            <a-radio v-for="dict in dp_model_column_pk_flag" :key="dict.value" :value="dict.value">{{
                                dict.label }}</a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="是否必填" name="nullableFlag">
                        <a-radio-group v-model:value="form.nullableFlag" :disabled="form.pkFlag == 1">
                            <a-radio v-for="dict in dp_model_column_nullable_flag" :key="dict.value"
                                :value="dict.value">{{
                                    dict.label }}</a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>
            </a-row>

            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="备注">
                        <a-textarea placeholder="请输入备注" v-model:value="form.remark" :auto-size="{ minRows: 4 }" />
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

    <!-- 逻辑模型属性信息详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="800px" draggable>
        <a-form ref="dpModelColumnRef" :model="form" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="关联数据标准" name="dataElemId">
                        <div>
                            {{ form.dataElemId }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="英文名称" name="engName">
                        <div>
                            {{ form.engName }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="中文名称" name="cnName">
                        <div>
                            {{ form.cnName }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="数据类型" name="columnType">
                        <div>
                            {{ form.columnType }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="属性长度" name="columnLength">
                        <div>
                            {{ form.columnLength }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="小数长度" name="columnScale">
                        <div>
                            {{ form.columnScale }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="默认值" name="defaultValue">
                        <div>
                            {{ form.defaultValue }}
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
                    <a-form-item label="是否必填" name="nullableFlag">
                        <div>
                            {{ form.nullableFlag }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="排序" name="sortOrder">
                        <div>
                            {{ form.sortOrder }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="数据元id" name="dataElemId">
                        <div>
                            {{ form.dataElemId }}
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
</template>

<script setup name="ComponentOne">
import {
    listDpModelColumn,
    getDpModelColumn,
    delDpModelColumns,
    addDpModelColumn,
    updateDpModelColumns
} from '@/api/std/model/model';
import { getDpDataElemList } from '@/api/std/dataElem/dataElem';
import { deptTreeSelectNoPermi } from '@/api/system/system/user.js';
const { proxy } = getCurrentInstance();
const { column_type, dp_model_column_pk_flag, dp_model_column_nullable_flag } = proxy.useDict(
    'column_type',
    'dp_model_column_pk_flag',
    'dp_model_column_nullable_flag'
);
const dpModelColumnList = ref([]);
const tableColumns = [
    { title: '编号', key: 'index', align: 'left', width: 60 },
    { title: '关联标准', dataIndex: 'dataElemName', align: 'left' },
    { title: '中文名称', dataIndex: 'cnName', align: 'left', ellipsis: true },
    { title: '英文名称', dataIndex: 'engName', align: 'left', ellipsis: true },
    { title: '数据类型', dataIndex: 'columnType', align: 'left' },
    { title: '描述', dataIndex: 'description', align: 'left', width: 250, ellipsis: true },
    { title: '属性长度', dataIndex: 'columnLength', align: 'left' },
    { title: '是否主键', dataIndex: 'pkFlag', align: 'left' },
    { title: '创建人', dataIndex: 'createBy', align: 'left', ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', align: 'left', width: 150, ellipsis: true },
    { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 }
];
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
const deptList = ref([]);
const defaultSort = ref({ prop: 'createTime', order: 'desc' });
const DpData = ref([]);
const handlePkFlagChange = (value) => {
    if (value == 1) {
        form.value.nullableFlag = '1';
    }
}
const getDpDataElem = async () => {
    try {
        const response = await getDpDataElemList();
        DpData.value = response.data;
    } catch (error) {
        console.error('请求失败:', error);
    }
};
const handleDatasourceChange = (value) => {
    const selectedDatasource = DpData.value.find((item) => item.id === value);
    if (selectedDatasource) {
        form.value.dataElemName = selectedDatasource.name;
        form.value.cnName = selectedDatasource.name;
        form.value.engName = selectedDatasource.engName;
        form.value.columnType = selectedDatasource.columnType;
    }
};
const data = reactive({
    dpModelColumnDetail: {},
    form: {
        pkFlag: '0',
        nullableFlag: '0',
    },
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        modelId: null,
        engName: null,
        cnName: null,
        columnType: null,
        columnLength: null,
        columnScale: null,
        defaultValue: null,
        pkFlag: null,
        nullableFlag: null,
        sortOrder: null,
        authorityDept: null,
        dataElemId: null,
        createTime: null
    },
    rules: {
        // 示例规则（可以根据需求添加或修改）
        cnName: [{ required: true, message: '中文名称不能为空', trigger: 'blur' }],
        engName: [{ required: true, message: '英文名称不能为空', trigger: 'blur' }],
        columnType: [{ required: true, message: '数据类型不能为空', trigger: 'blur' }],
        columnLength: [{ required: true, message: '属性长度不能为空', trigger: 'blur' }],
        // pkFlag: [{ required: true, message: '是否主键不能为空', trigger: 'blur' }],
        // nullableFlag: [{ required: true, message: '是否必填不能为空', trigger: 'blur' }]
    }
});
const { queryParams, form, dpModelColumnDetail, rules } = toRefs(data);

const route = useRoute();
let modelId = route.query.id || 1;
// 监听 id 变化
watch(
    () => route.query.id,
    (newId) => {
        modelId = newId || 1; // 如果 id 为空，使用默认值 1
        getList();

        // getList();
    },
    { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);
// 转换输入值为大写
const convertToUpperCase = (key, value) => {
    const uppercasedValue = value.replace(/[a-z]/g, (char) => char.toUpperCase());

    form.value[key] = uppercasedValue;

    console.log('🚀 ~ convertToUpperCase ~ form.value[key]:', form.value[key]);
};
/** 查询逻辑模型属性信息列表 */
function getList() {
    loading.value = true;
    listDpModelColumn({ ...queryParams.value, modelId }).then((response) => {
        dpModelColumnList.value = response.data.rows;
        total.value = response.data.total;
        loading.value = false;
        // 部门
        deptTreeSelectNoPermi().then((response) => {
            deptList.value = response.data;
            dpModelColumnList.value.forEach((item) => {
                // 递归查找树形结构中匹配的节点
                const findLabel = (tree) => {
                    for (let node of tree) {
                        if (node.id == item.authorityDept) {
                            return node.label;
                        }
                        if (node.children) {
                            const found = findLabel(node.children);
                            if (found) return found;
                        }
                    }
                    return null;
                };
                item.deptLabel = findLabel(deptList.value) || '-';
            });
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
        modelId: null,
        engName: null,
        cnName: null,
        columnType: null,
        columnLength: null,
        columnScale: null,
        defaultValue: null,
        pkFlag: '0',
        nullableFlag: '0',
        sortOrder: null,
        authorityDept: null,
        dataElemId: null,
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
    proxy.resetForm('dpModelColumnRef');
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

/** 新增按钮操作 */
function handleAdd() {
    reset();
    open.value = true;
    title.value = '新增逻辑模型属性信息';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDpModelColumn(_id).then((response) => {
        form.value = response.data;
        open.value = true;
        title.value = '修改逻辑模型属性信息';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getDpModelColumn(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '逻辑模型属性信息详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['dpModelColumnRef']
        .validate()
        .then(() => {
            form.value.modelId = modelId;
            if (form.value.id != null) {
                updateDpModelColumns(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDpModelColumn(form.value)
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
        .confirm('是否确认删除逻辑模型属性信息编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDpModelColumns(_ids);
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
        'std/modelColumn/export',
        {
            ...queryParams.value
        },
        `dpModelColumn_${new Date().getTime()}.xlsx`
    );
}
getDpDataElem();
</script>

