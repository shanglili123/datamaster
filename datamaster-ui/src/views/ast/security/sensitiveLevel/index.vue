<template>
    <div class="app-container" ref="app-container">

        <div class="pagecont-top" v-show="showSearch">
            <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }"
                v-show="showSearch" @submit.prevent>
                <a-form-item label="名称" name="sensitiveLevel">
                    <a-input style="width: 150px;" v-model:value="queryParams.sensitiveLevel" placeholder="请输入敏感级别名称"
                        allow-clear @pressEnter="handleQuery" />
                </a-form-item>
                <a-form-item label="规则" name="sensitiveRule">
                    <a-select style="width: 150px;" v-model:value="queryParams.sensitiveRule" placeholder="请选择替换规则"
                        allow-clear>
                        <a-select-option v-for="dict in da_sensitive_level_rule" :key="dict.value"
                            :value="dict.value">{{ dict.label }}</a-select-option>
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
                <a-button type="primary" @click="handleAdd" v-hasPermi="['ast:sensitiveLevel:add']"
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
                :data-source="daSensitiveLevelList"
                :columns="tableColumns"
                :pagination="false"
                :locale="{ emptyText: '暂无记录' }"
                @change="handleTableChange"
            >
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'id'">
                        {{ record.id || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'sensitiveLevel'">
                        {{ record.sensitiveLevel || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'description'">
                        {{ record.description || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'sensitiveRule'">
                        <dict-tag :options="da_sensitive_level_rule" :value="record.sensitiveRule" />
                    </template>
                    <template v-if="column.dataIndex === 'maskCharacter'">
                        {{ record.maskCharacter || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'createBy'">
                        {{ record.createBy || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'createTime'">
                        <span>{{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
                    </template>
                    <template v-if="column.dataIndex === 'onlineFlag'">
                        <a-switch v-model:checked="record.onlineFlag" checked-value="1" un-checked-value="0"
                            @change="handleStatusChange(record)" />
                    </template>
                    <template v-if="column.dataIndex === 'remark'">
                        {{ record.remark || '-' }}
                    </template>
                    <template v-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="handleUpdate(record)"
                            v-hasPermi="['ast:sensitiveLevel:edit']">修改</a-button>
                        <a-button type="link" danger size="small" @click="handleDelete(record)"
                            v-hasPermi="['ast:sensitiveLevel:remove']">删除</a-button>
                    </template>
                </template>
            </a-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>

        <!-- 新增或修改敏感等级对话框 -->
        <a-modal :title="title" v-model:open="open" width="800px" draggable @ok="submitForm" @cancel="cancel">
            <a-form ref="daSensitiveLevelRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }"
                @submit.prevent>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="级别名称" name="sensitiveLevel">
                            <a-input v-model:value="form.sensitiveLevel" placeholder="请输入敏感级别名称" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="替换规则" name="sensitiveRule">
                            <a-select v-model:value="form.sensitiveRule" placeholder="请选择替换规则">
                                <a-select-option v-for="dict in da_sensitive_level_rule" :key="dict.value"
                                    :value="dict.value">{{ dict.label }}</a-select-option>
                            </a-select>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="form.sensitiveRule != '1' && form.sensitiveRule != null">
                    <a-col :span="12">
                        <a-form-item label="起始字符位置" name="startCharLoc">
                            <a-input v-model:value="form.startCharLoc" placeholder="请输入起始字符位置" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="截止字符位置" name="endCharLoc">
                            <a-input v-model:value="form.endCharLoc" placeholder="请输入截止字符位置" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="替换内容" name="maskCharacter">
                            <a-input v-model:value="form.maskCharacter" placeholder="请输入替换内容" />
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
                        <a-form-item label="在线状态" name="onlineFlag">
                            <a-radio-group v-model:value="form.onlineFlag">
                                <a-radio v-for="dict in da_sensitive_status" :key="dict.value" :value="dict.value">{{
                                    dict.label }}</a-radio>
                            </a-radio-group>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="备注" name="remark">
                            <a-textarea v-model:value="form.remark" placeholder="请输入备注" />
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

        <!-- 敏感等级详情对话框 -->
        <a-modal :title="title" v-model:open="openDetail" width="800px" draggable @ok="cancel" @cancel="cancel">
            <template #title>
                {{ title }}
                <InfoFilled style="color: #909399; font-size: 16px" />
            </template>
            <a-form ref="daSensitiveLevelRef" :model="form" :label-col="{ style: { width: '80px' } }">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="敏感级别名称" name="sensitiveLevel">
                            <div>
                                {{ form.sensitiveLevel }}
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="替换规则" name="sensitiveRule">
                            <dict-tag :options="da_sensitive_level_rule" :value="form.sensitiveRule" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="起始字符位置" name="startCharLoc">
                            <div>
                                {{ form.startCharLoc }}
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="截止字符位置" name="endCharLoc">
                            <div>
                                {{ form.endCharLoc }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="替换内容" name="maskCharacter">
                            <div>
                                {{ form.maskCharacter }}
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="上下线标识" name="onlineFlag">
                            <div>
                                {{ form.onlineFlag }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="描述" name="description">
                            <div>
                                {{ form.description }}
                            </div>
                        </a-form-item>
                    </a-col>
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

        <!-- 用户导入对话框 -->
        <a-modal :title="upload.title" v-model:open="upload.open" width="800px" draggable destroy-on-close
            @ok="submitFileForm" @cancel="upload.open = false" :ok-button-props="{ disabled: upload.isUploading }">
            <a-upload ref="uploadRef" :max-count="1" accept=".xlsx, .xls" :headers="upload.headers"
                :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
                :before-upload="handleFileUploadProgress" :auto-upload="false" drag
                @change="handleUploadChange">
                <p class="ant-upload-drag-icon">
                    <UploadOutlined />
                </p>
                <p class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
                <template #tip>
                    <div class="ant-upload-tip text-center">
                        <div class="ant-upload-tip">
                            <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的敏感等级数据
                        </div>
                        <span>仅允许导入xls、xlsx格式文件。</span>
                        <a-typography-link type="primary" style="font-size: 12px; vertical-align: baseline"
                            @click="importTemplate">下载模板</a-typography-link>
                    </div>
                </template>
            </a-upload>
            <template #footer>
                <div class="dialog-footer">
                    <a-button @click="upload.open = false">取 消</a-button>
                    <a-button type="primary" @click="submitFileForm">确 定</a-button>
                </div>
            </template>
        </a-modal>
    </div>
</template>

<script setup name="SensitiveLevel">
import {
    listDaSensitiveLevel,
    getDaSensitiveLevel,
    delDaSensitiveLevel,
    addDaSensitiveLevel,
    updateDaSensitiveLevel,
    updateStatus
} from '@/api/ast/security/sensitiveLevel/sensitiveLevel';
import { getToken } from '@/utils/auth.js';
import { updateDaAsset } from '@/api/ast/asset/asset.js';
import { normalizePage, pageRows } from "@/utils/page.js";
import { InfoCircleFilled as InfoFilled, UploadOutlined } from '@ant-design/icons-vue';

const { proxy } = getCurrentInstance();
const { da_sensitive_level_rule, da_sensitive_status } = proxy.useDict(
    'da_sensitive_level_rule',
    'da_sensitive_status'
);
const daSensitiveLevelList = ref([]);

// 列显隐信息
const columns = ref([
    { key: 1, label: '编号', visible: true },
    { key: 2, label: '敏感级别名称', visible: true },
    { key: 3, label: '描述', visible: true },
    { key: 4, label: '替换规则', visible: true },
    { key: 5, label: '替换内容', visible: true },
    { key: 6, label: '创建人', visible: true },
    { key: 7, label: '创建时间', visible: true },
    { key: 8, label: '在线状态', visible: true },
    { key: 9, label: '备注', visible: true },
    { key: 10, label: '操作', visible: true }
]);

const getColumnVisibility = (key) => {
    const column = columns.value.find((col) => col.key === key);
    // 如果没有找到对应列配置，默认显示
    if (!column) return true;
    // 如果找到对应列配置，根据visible属性来控制显示
    return column.visible;
};

const tableColumns = computed(() => {
    const allCols = [
        { title: '编号', dataIndex: 'id', align: 'center', width: 80, colKey: 1 },
        { title: '敏感级别名称', dataIndex: 'sensitiveLevel', align: 'center', colKey: 2 },
        { title: '描述', dataIndex: 'description', align: 'left', width: 350, colKey: 3 },
        { title: '替换规则', dataIndex: 'sensitiveRule', align: 'center', colKey: 4 },
        { title: '替换内容', dataIndex: 'maskCharacter', align: 'center', colKey: 5 },
        { title: '创建人', dataIndex: 'createBy', align: 'center', width: 120, ellipsis: true, colKey: 6 },
        { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 160, key: 'create_time', sorter: true, defaultSortOrder: 'descend', colKey: 7 },
        { title: '在线状态', dataIndex: 'onlineFlag', align: 'center', width: 100, colKey: 8 },
        { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true, colKey: 9 },
        { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240, colKey: 10 },
    ];
    return allCols.filter(col => getColumnVisibility(col.colKey));
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
const defaultSort = ref({ columnKey: 'reate_time', order: 'desc' });
const router = useRouter();

/*** 用户导入参数 */
const upload = reactive({
    // 是否显示弹出层（用户导入）
    open: false,
    // 弹出层标题（用户导入）
    title: '',
    // 是否禁用上传
    isUploading: false,
    // 是否更新已经存在的用户数据
    updateSupport: 0,
    // 设置上传的请求头部
    headers: { Authorization: 'Bearer ' + getToken() },
    // 上传的地址
    url: import.meta.env.VITE_APP_BASE_API + '/ast/daSensitiveLevel/importData'
});

const data = reactive({
    form: {
        onlineFlag: 0
    },
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        sensitiveLevel: null,
        sensitiveRule: null,
        startCharLoc: null,
        endCharLoc: null,
        maskCharacter: null,
        onlineFlag: null,
        description: null,
        createTime: null
    },
    rules: {
        sensitiveLevel: [{ required: true, message: '敏感级别名称不能为空', trigger: 'blur' }],
        maskCharacter: [{ required: true, message: '替换内容不能为空', trigger: 'blur' }],
        sensitiveRule: [{ required: true, message: '替换规则不能为空', trigger: 'blur' }]
    }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询敏感等级列表 */
function getList() {
    loading.value = true;
    listDaSensitiveLevel(queryParams.value).then((response) => {
        const page = normalizePage(response);
        total.value = page.total;
        daSensitiveLevelList.value = pageRows(page.rows, page.total, queryParams.value);
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
        sensitiveLevel: null,
        sensitiveRule: null,
        startCharLoc: null,
        endCharLoc: null,
        maskCharacter: null,
        onlineFlag: '0',
        description: null,
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
    proxy.resetForm('daSensitiveLevelRef');
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
    title.value = '新增敏感等级';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDaSensitiveLevel(_id).then((response) => {
        form.value = response.data;
        open.value = true;
        title.value = '修改敏感等级';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getDaSensitiveLevel(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '敏感等级详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['daSensitiveLevelRef']
        .validate()
        .then(() => {
            if (form.value.id != null) {
                updateDaSensitiveLevel(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => { });
            } else {
                addDaSensitiveLevel(form.value)
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
        .confirm('是否确认删除敏感等级编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDaSensitiveLevel(_ids);
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
        'ast/sensitiveLevel/export',
        {
            ...queryParams.value
        },
        `daSensitiveLevel_${new Date().getTime()}.xlsx`
    );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
    upload.title = '敏感等级导入';
    upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
    proxy.download(
        'system/user/importTemplate',
        {},
        `daSensitiveLevel_template_${new Date().getTime()}.xlsx`
    );
}

/** 提交上传文件 */
function submitFileForm() {
    proxy.$refs['uploadRef'].submit();
}

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
    upload.isUploading = true;
};

/** antd a-upload @change 事件适配，复用原有上传回调 */
const handleUploadChange = (info) => {
    if (info.file.status === 'uploading') {
        handleFileUploadProgress(info.event, info.file, info.fileList);
    } else if (info.file.status === 'done') {
        handleFileSuccess(info.file.response, info.file, info.fileList);
    }
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
    upload.open = false;
    upload.isUploading = false;
    proxy.$refs['uploadRef'].handleRemove(file);
    proxy.$alert(
        "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
        response.msg +
        '</div>',
        '导入结果',
        { dangerouslyUseHTMLString: true }
    );
    getList();
};
/** ---------------------------------**/

function routeTo(link, row) {
    if (link !== '' && link.indexOf('http') !== -1) {
        window.location.href = link;
        return;
    }
    if (link !== '') {
        if (link === router.currentRoute.value.path) {
            window.location.reload();
        } else {
            router.push({
                path: link,
                query: {
                    id: row.id
                }
            });
        }
    }
}

/** 启用禁用开关 */
function handleStatusChange(row) {
    const text = row.onlineFlag === '1' ? '上线' : '下线';
    proxy.$modal
        .confirm('确认要' + text + '"' + row.sensitiveLevel + '"敏感等级吗？')
        .then(function () {
            updateStatus(row.id, row.onlineFlag)
                .then((response) => {
                    proxy.$modal.msgSuccess(text + '成功');
                    getList();
                })
                .catch((error) => {
                    row.onlineFlag = !row.onlineFlag;
                });
        })
        .catch(function () {
            row.onlineFlag = !row.onlineFlag;
        });
}

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
