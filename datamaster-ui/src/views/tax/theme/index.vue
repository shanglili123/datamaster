<template>
    <div class="app-container" ref="app-container">

        <div class="pagecont-top" v-show="showSearch">
            <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline"
                :label-col="{ style: { width: '75px' } }" v-show="showSearch" @submit.prevent
>
                <a-form-item label="主题名称" name="name">
                    <a-input class="el-form-input-width" v-model:value="queryParams.name" placeholder="请输入主题名称"
                        allow-clear @pressEnter="handleQuery"
/>
                </a-form-item>
                <!-- <a-form-item label="描述" name="description">
                    <a-input class="el-form-input-width" v-model:value="queryParams.description" placeholder="请输入描述"
                        allow-clear @pressEnter="handleQuery" />
                </a-form-item> -->
                <a-form-item>
                    <a-button type="primary" v-hasPermi="['tax:theme:query']" @click="handleQuery"
                        @mousedown="(e) => e.preventDefault()"
>
                        <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                    </a-button>
                    <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                    </a-button>
                </a-form-item>
            </a-form>
        </div>
        <div class="pagecont-bottom">
            <div class="justify-between mb15">
                <div class="btn-style">
                    <a-button type="primary" @click="handleAdd" v-hasPermi="['tax:theme:add']"
                        @mousedown="(e) => e.preventDefault()"
>
                        <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                    </a-button>
                    <!-- <a-button type="primary" :disabled="single" @click="handleUpdate"
                        v-hasPermi="['tax:theme:theme:edit']" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-xiugai--copy mr5"></i>修改
                    </a-button>
                    <a-button type="primary" danger :disabled="multiple" @click="handleDelete"
                        v-hasPermi="['tax:theme:theme:remove']" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-shanchu-huise mr5"></i>删除
                    </a-button> -->
                </div>
                <div class="justify-end top-right-btn">
                    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"
                        :columns="columns"
></right-toolbar>
                </div>
            </div>
            <a-table striped :loading="loading" :data-source="attThemeList" :columns="tableColumns"
                :pagination="false" @change="handleTableChange"
                :locale="{ emptyText: emptyContent }"
>
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'name'">
                        {{ record.name || '-' }}
                    </template>
                    <template v-else-if="column.dataIndex === 'icon'">
                        <image-preview :src="record.icon || noDataImg" :width="50" :height="50" />
                    </template>
                    <template v-else-if="column.dataIndex === 'description'">
                        {{ record.description || '-' }}
                    </template>
                    <template v-else-if="column.dataIndex === 'sortOrder'">
                        {{ record.sortOrder || '-' }}
                    </template>
                    <template v-else-if="column.dataIndex === 'createBy'">
                        {{ record.createBy || "-" }}
                    </template>
                    <template v-else-if="column.dataIndex === 'createTime'">
                        <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'validFlag'">
                        <a-switch v-model:checked="record.validFlag" @change="() => handleStatusChange(record)" />
                    </template>
                    <template v-else-if="column.dataIndex === 'remark'">
                        {{ record.remark || '-' }}
                    </template>
                    <template v-else-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="handleUpdate(record)"
                            v-hasPermi="['tax:theme:edit']"
>修改</a-button>
                        <a-button type="link" danger size="small" @click="handleDelete(record)"
                            v-hasPermi="['tax:theme:remove']"
>删除</a-button>
                        <a-button type="link" size="small" v-hasPermi="['tax:theme:query']"
                            @click="handleDetail(record)"
>详情</a-button>
                    </template>
                </template>
            </a-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList"
/>
        </div>

        <!-- 新增或修改主题对话框 -->
        <a-modal :title="title" v-model:open="open" width="800px" draggable destroy-on-close>
            <a-form ref="attThemeRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }"
                @submit.prevent
>
                <a-row :gutter="20">
                    <a-col>
                        <a-form-item label="主题名称" name="name">
                            <a-input v-model:value="form.name" placeholder="请输入主题名称" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row>
                    <a-col :span="24">
                        <a-form-item label="描述" name="description">
                            <a-textarea v-model:value="form.description" placeholder="请输入描述"
                                :auto-size="{ minRows: 2, maxRows: 4 }"
/>
                        </a-form-item>
                    </a-col>
                </a-row>

                <a-row>
                    <a-col :span="24">
                        <a-form-item label="图标" name="icon">
                            <image-upload :limit="1" v-model="form.icon" :width="50" :height="50" />

                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="排序" name="sortOrder">
                            <a-input-number style="width: 100%" v-model:value="form.sortOrder" :min="0" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="状态" name="validFlag">
                            <a-radio-group v-model:value="form.validFlag">
                                <a-radio :value="true">启用</a-radio>
                                <a-radio :value="false">禁用</a-radio>
                            </a-radio-group>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row>
                    <a-col :span="24">
                        <a-form-item label="备注" name="remark">
                            <a-textarea v-model:value="form.remark" placeholder="请输入备注"
                                :auto-size="{ minRows: 2, maxRows: 4 }"
/>
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

        <!-- 主题详情对话框 -->
        <a-modal :title="title" v-model:open="openDetail" width="1000px" draggable destroy-on-close>
            <a-form ref="assetApplyRef" :model="form" :label-col="{ style: { width: '90px' } }">
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="编号:" name="id">
                            <div class="form-readonly">
                                {{ form.id }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="主题名称:" name="name">
                            <div class="form-readonly">
                                {{ form.name }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="图标:" name="icon">
                            <image-preview :src="form.icon || noDataImg" :width="50" :height="50" />

                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row>
                    <a-col :span="24">
                        <a-form-item label="描述" name="description">
                            <div class="form-readonly textarea">
                                {{ form.description ?? "-" }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="创建人:" name="createBy">
                            <div class="form-readonly">
                                {{ form.createBy }}
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="创建时间:" name="createTime">
                            <div class="form-readonly">
                                {{ parseTime(form.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}

                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="更新人:" name="updateBy">
                            <div class="form-readonly">
                                {{ form.updateBy }}
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="更新时间:" name="updateTime">
                            <div class="form-readonly">
                                {{ parseTime(form.updateTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="状态:" name="validFlag">
                            <div class="form-readonly">
                                {{ form.validFlag ? "启用" : "禁用" }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row>
                    <a-col :span="24">
                        <a-form-item label="备注" name="remark">
                            <div class="form-readonly textarea">
                                {{ form.remark ?? "-" }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
            </a-form>
            <template #footer>
                <div class="dialog-footer">
                    <a-button @click="openDetail = false">关闭</a-button>
                </div>
            </template>
        </a-modal>

        <!-- 用户导入对话框 -->
        <a-modal :title="upload.title" v-model:open="upload.open" width="800px" draggable destroy-on-close>
            <a-upload-dragger ref="uploadRef" :max-count="1" accept=".xlsx, .xls" :headers="upload.headers"
                :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
                @progress="handleFileUploadProgress" @success="handleFileSuccess"
>
                <CloudUploadOutlined class="ant-upload-drag-icon" />
                <p class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
            </a-upload-dragger>
            <div class="upload-hint text-center">
                <div class="upload-hint">
                    <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的主题数据
                </div>
                <span>仅允许导入xls、xlsx格式文件。</span>
                <a-typography-link type="primary" style="font-size: 12px; vertical-align: baseline"
                    @click="importTemplate"
>下载模板</a-typography-link>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <a-button @click="upload.open = false">取 消</a-button>
                    <a-button type="primary" @click="submitFileForm">确 定</a-button>
                </div>
            </template>
        </a-modal>
    </div>
</template>

<script setup name="Theme">
import {
    listAttTheme,
    getAttTheme,
    delAttTheme,
    addAttTheme,
    updateAttTheme
} from '@/api/tax/theme/theme.js';
import { getToken } from '@/utils/auth.js';
import { normalizePage, pageRows } from "@/utils/page.js";
import { CloudUploadOutlined } from '@ant-design/icons-vue';
const noDataImg = new URL('@/assets/system/images/D.png', import.meta.url).href
const { proxy } = getCurrentInstance();
const emptyContent = h('div', { class: 'emptyBg' }, [
    h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
    h('p', '暂无记录'),
]);

const tableColumns = computed(() => {
    const cols = [];
    if (getColumnVisibility(0)) cols.push({ title: '编号', dataIndex: 'id', align: 'center', width: 60 });
    if (getColumnVisibility(1)) cols.push({ title: '主题名称', dataIndex: 'name', align: 'left', width: 200 });
    if (getColumnVisibility(2)) cols.push({ title: '图标', dataIndex: 'icon', align: 'center', width: 100 });
    if (getColumnVisibility(3)) cols.push({ title: '描述', dataIndex: 'description', align: 'left', width: 300, ellipsis: true });
    if (getColumnVisibility(10)) cols.push({ title: '排序', dataIndex: 'sortOrder', align: 'left', width: 50 });
    if (getColumnVisibility(7)) cols.push({ title: '创建人', dataIndex: 'createBy', align: 'left', ellipsis: true });
    if (getColumnVisibility(6)) cols.push({ title: '创建时间', dataIndex: 'createTime', align: 'center', width: 150 });
    if (getColumnVisibility(4)) cols.push({ title: '状态', dataIndex: 'validFlag', align: 'center', width: 120 });
    if (getColumnVisibility(5)) cols.push({ title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true });
    cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 });
    return cols;
});

const attThemeList = ref([]);
// 列显隐信息
const columns = ref([
    { key: 0, label: '编号', visible: true },
    { key: 1, label: '主题名称', visible: true },
    { key: 2, label: '图标', visible: true },
    { key: 3, label: '描述', visible: true },
    { key: 10, label: '排序', visible: true },
    { key: 7, label: '创建人', visible: true },
    { key: 6, label: '创建时间', visible: true },
    { key: 4, label: '状态', visible: true },
    { key: 5, label: '备注', visible: true }
]);

const getColumnVisibility = (key) => {
    const column = columns.value.find((col) => col.key === key);
    // 如果没有找到对应列配置，默认显示
    if (!column) return true;
    // 如果找到对应列配置，根据visible属性来控制显示
    return column.visible;
};

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
    url: import.meta.env.VITE_APP_BASE_API + '/tax/theme/importData'
});

const data = reactive({
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: null,
        orderByColumn: 'sortOrder,createTime',
        description: null
    },
    rules: {
        name: [{ required: true, message: '主题名称不能为空', trigger: 'blur' }],
        // icon: [{ required: true, message: "图标url不能为空", trigger: "blur" }],
        // sortOrder: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
        // description: [{ required: true, message: '描述不能为空', trigger: 'blur' }],
        // validFlag: [{ required: true, message: '是否有效不能为空', trigger: 'blur' }]
    }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询主题列表 */
function getList() {
    loading.value = true;
    listAttTheme(queryParams.value).then((response) => {
        const page = normalizePage(response);
        total.value = page.total;
        attThemeList.value = pageRows(page.rows, page.total, queryParams.value);
        loading.value = false;
    });
}
/** 改变启用状态值 */
function handleStatusChange(row) {
    const text = row.validFlag === true ? '启用' : '禁用';
    proxy.$modal
        .confirm('确认要"' + text + '","' + row.name + '"主题吗？')
        .then(function () {
            updateAttTheme({ id: row.id, validFlag: row.validFlag }).then((response) => {
                proxy.$modal.msgSuccess(text + '成功');
                getList();
            });
        })
        .catch(function () {
            row.validFlag = !row.validFlag;
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
        icon: null,
        sortOrder: 0,
        description: null,
        validFlag: true,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updaterId: null,
        updateTime: null,
        remark: null
    };
    proxy.resetForm('attThemeRef');
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
function handleTableChange(pagination, filters, sorter) {
    if (sorter && sorter.field) {
        queryParams.value.orderByColumn = sorter.field;
        queryParams.value.isAsc = sorter.order === 'ascend' ? 'ascending' : 'descending';
        getList();
    }
}

/** 新增按钮操作 */
function handleAdd() {
    reset();
    open.value = true;
    title.value = '新增主题';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getAttTheme(_id).then((response) => {
        delete response.data.createTime;
        delete response.data.updateTime;
        form.value = response.data;
        open.value = true;
        title.value = '修改主题';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getAttTheme(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '主题详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['attThemeRef'].validate().then(() => {
        if (form.value.id != null) {
            updateAttTheme(form.value)
                .then((response) => {
                    proxy.$modal.msgSuccess('修改成功');
                    open.value = false;
                    getList();
                })
                .catch((error) => { });
        } else {
            addAttTheme(form.value)
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
        .confirm('是否确认删除主题编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delAttTheme(_ids);
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
        'tax/theme/export',
        {
            ...queryParams.value
        },
        `attTheme_${new Date().getTime()}.xlsx`
    );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
    upload.title = '主题导入';
    upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
    proxy.download(
        'system/user/importTemplate',
        {},
        `attTheme_template_${new Date().getTime()}.xlsx`
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

getList();
</script>

