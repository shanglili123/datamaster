<template>
    <div class="app-container" ref="app-container">

        <a-layout style="90%">
            <DeptTree :deptOptions="processedData" ref="DeptTreeRef" :leftWidth="leftWidth" :placeholder="'请输入稽查规则目录'"
                @node-click="handleNodeClick"
/>
        
            <a-layout-content>
                <div class="pagecont-top" v-show="showSearch">
                    <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline"
                        :label-col="{ style: { width: '75px' } }" v-show="showSearch" @submit.prevent
>
                        <a-form-item label="规则名称" name="name">
                            <a-input class="el-form-input-width" v-model:value="queryParams.name" placeholder="请输入规则名称"
                                allow-clear @pressEnter="handleQuery"
/>
                        </a-form-item>
                        <!-- <a-form-item label="编号" name="code">
                            <a-input class="el-form-input-width" v-model:value="queryParams.code" placeholder="请输入编号"
                                allow-clear @pressEnter="handleQuery" />
                        </a-form-item> -->
        
                        <a-form-item>
                            <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
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
                            <!--                            <a-button type="primary" @click="handleAdd"
                                v-hasPermi="['tax:rule:attcleanrule:add']" @mousedown="(e) => e.preventDefault()">
                                <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                            </a-button> -->
                        </div>
                        <div class="justify-end top-right-btn">
                            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"
                                :columns="columns"
></right-toolbar>
                        </div>
                    </div>
                    <a-table striped :loading="loading" :data-source="attCleanRuleList"
                        :columns="tableColumns" :pagination="false"
                        @change="handleTableChange"
                        :locale="{ emptyText: emptyContent }"
>
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.dataIndex === 'code'">
                                {{ record.code }}
                            </template>
                            <template v-else-if="column.dataIndex === 'name'">
                                {{ record.name || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'catName'">
                                {{ record.catName || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'description'">
                                {{ record.description || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'useCase'">
                                {{ record.useCase || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'example'">
                                {{ record.example || '-' }}
                            </template>
                        </template>
                    </a-table>

                    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                        v-model:limit="queryParams.pageSize" @pagination="getList"
/>
                </div>
            </a-layout-content>
        </a-layout>

        <!-- 新增或修改清洗规则对话框 -->
        <a-modal :title="title" v-model:open="open" width="800px" draggable destroy-on-close>
            <a-form ref="attCleanRuleRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }"
                @submit.prevent
>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="规则名称" name="name">
                            <a-input v-model:value="form.name" placeholder="请输入规则名称" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="编号" name="code">
                            <a-input v-model:value="form.code" placeholder="请输入编号" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="规则类型" name="type">
                            <a-tree-select v-model:value="form.type" :tree-data="processedData"
                                :field-names="{ value: 'id', label: 'name', children: 'children' }"
                                placeholder="请选择规则类型" allow-clear
/>

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
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="场景" name="useCase">
                            <a-textarea v-model:value="form.useCase" placeholder="请输入场景"
                                :auto-size="{ minRows: 2, maxRows: 4 }"
/>
                        </a-form-item>
                    </a-col>
                    <!--                    <a-col :span="12">-->
                    <!--                        <a-form-item label="规则级别" name="level">-->
                    <!--                            <a-select v-model:value="form.level" placeholder="请选择规则级别">-->
                    <!--                                <a-select-option v-for="dict in att_rule_level" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>-->
                    <!--                            </a-select>-->
                    <!--                        </a-form-item>-->
                    <!--                    </a-col>-->
                    <a-col :span="24">
                        <a-form-item label="示例" name="example">
                            <a-textarea v-model:value="form.example" placeholder="请输入示例"
                                :auto-size="{ minRows: 2, maxRows: 4 }"
/>
                        </a-form-item>
                    </a-col>

                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="描述" name="description">
                            <a-textarea v-model:value="form.description" placeholder="请输入规则描述"
                                :auto-size="{ minRows: 2, maxRows: 4 }"
/>
                        </a-form-item>
                    </a-col>
                </a-row>
                <!--                <a-row :gutter="20">-->
                <!--                    <a-col :span="24">-->
                <!--                        <a-form-item label="备注" name="remark">-->
                <!--                            <a-textarea v-model:value="form.remark" placeholder="请输入备注" />-->
                <!--                        </a-form-item>-->
                <!--                    </a-col>-->
                <!--                </a-row>-->
            </a-form>
            <template #footer>
                <div class="dialog-footer">
                    <a-button @click="cancel">取 消</a-button>
                    <a-button type="primary" @click="submitForm">确 定</a-button>
                </div>
            </template>
        </a-modal>

        <!-- 清洗规则详情对话框 -->
        <a-modal :title="title" v-model:open="openDetail" width="800px" draggable destroy-on-close>
            <a-form ref="attCleanRuleRef" :model="form" :label-col="{ style: { width: '80px' } }">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="规则名称" name="name">
                            <div>
                                {{ form.name }}
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="规则类型" name="type">
                            <dict-tag :options="processedData" :value="form.type" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="规则级别" name="level">
                            <dict-tag :options="att_rule_level" :value="form.level" />
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
                    <a-button @click="cancel">关 闭</a-button>
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
                    <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的清洗规则数据
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

<script setup name="CleanRule">
import {
    listAttCleanRule,
    getAttCleanRule,
    delAttCleanRule,
    addAttCleanRule,
    updateAttCleanRule
} from '@/api/tax/rule/cleanRule';
import { getToken } from '@/utils/auth.js';
import DeptTree from '@/components/DeptTree';
import { computed, h } from 'vue';
import { listAttCleanCat } from "@/api/tax/cat/cleanCat/cleanCat.js";
import { normalizePage, pageRows } from "@/utils/page.js";
import { CloudUploadOutlined } from '@ant-design/icons-vue';
const { proxy } = getCurrentInstance();
const { att_rule_level, att_rule_clean_type } = proxy.useDict(
    'att_rule_level',
    'att_rule_clean_type'
);
const leftWidth = ref(240); // 初始左侧宽度
const isResizing = ref(false); // 判断是否正在拖拽
let startX = 0; // 鼠标按下时的初始位置// 初始左侧宽度
let Materialization = ref(false);
const startResize = (event) => {
    isResizing.value = true;
    startX = event.clientX;
    document.addEventListener('mousemove', updateResize);
    document.addEventListener('mouseup', stopResize);
};
const stopResize = () => {
    isResizing.value = false;
    document.removeEventListener('mousemove', updateResize);
    document.removeEventListener('mouseup', stopResize);
};
const updateResize = (event) => {
    if (isResizing.value) {
        const delta = event.clientX - startX; // 计算鼠标移动距离
        leftWidth.value += delta; // 修改左侧宽度
        startX = event.clientX; // 更新起始位置
        // 使用 requestAnimationFrame 来减少页面重绘频率
        requestAnimationFrame(() => { });
    }
};
const emptyContent = h('div', { class: 'emptyBg' }, [
    h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
    h('p', '暂无记录'),
]);

const tableColumns = computed(() => {
    const cols = [];
    if (getColumnVisibility(0)) cols.push({ title: '编号', dataIndex: 'code', align: 'left', width: 80 });
    if (getColumnVisibility(1)) cols.push({ title: '规则名称', dataIndex: 'name', align: 'left', width: 200, ellipsis: true });
    if (getColumnVisibility(2)) cols.push({ title: '规则类型', dataIndex: 'catName', align: 'left', width: 180 });
    if (getColumnVisibility(4)) cols.push({ title: '描述', dataIndex: 'description', align: 'left', width: 480 });
    if (getColumnVisibility(6)) cols.push({ title: '使用场景', dataIndex: 'useCase', align: 'left', width: 500 });
    if (getColumnVisibility(5)) cols.push({ title: '示例', dataIndex: 'example', align: 'left', width: 600 });
    return cols;
});

const attCleanRuleList = ref([]);
const processedData = ref([]);
const dataMapCat = new Map();

function handleNodeClick(data) {
    if (data.id == 0) {
        data.id = null;
    }
    queryParams.value.catCode = data.code;
    queryParams.value.pageNum = 1;
    handleQuery();
}
// 列显隐信息
const columns = ref([
    { key: 1, label: '规则名称', visible: true },
    { key: 2, label: '规则类型', visible: true },
    { key: 3, label: '规则级别', visible: true },
    { key: 4, label: '描述', visible: true },
    { key: 13, label: '备注', visible: true }
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
    url: import.meta.env.VITE_APP_BASE_API + '/tax/cleanRule/importData'
});

const data = reactive({
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: null,
        validFlag: true,
        code: null
    },
    rules: {
        name: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
        type: [{ required: true, message: '规则类型不能为空', trigger: 'change' }],
        level: [{ required: true, message: '规则级别不能为空', trigger: 'change' }],
        code: [{ required: true, message: '编号不能为空', trigger: 'change' }],
    }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询清洗规则列表 */
function getList() {
    loading.value = true;
    listAttCleanRule(queryParams.value).then((response) => {
        const page = normalizePage(response);
        page.rows.forEach(obj => {
            let name = dataMapCat.get(obj.type);
            obj.catName = name;
        });
        total.value = page.total;
        attCleanRuleList.value = pageRows(page.rows, page.total, queryParams.value);
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
        name: null,
        type: null,
        level: 1,
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
    proxy.resetForm('attCleanRuleRef');
}

/** 搜索按钮操作 */
function handleQuery() {
    queryParams.value.pageNum = 1;
    getList();
}

const DeptTreeRef = ref(null);
/** 重置按钮操作 */
function resetQuery() {
    if (DeptTreeRef.value?.resetTree) {
        DeptTreeRef.value.resetTree();
    }
    queryParams.value.catCode = '';
    queryParams.value.pageNum = 1;
    proxy.resetForm('queryRef');
    handleQuery();
}

/** 改变启用状态值 */
function handleStatusChange(row) {
    const text = row.validFlag === true ? '启用' : '禁用';
    proxy.$modal
        .confirm('确认要"' + text + '","' + row.name + '"数据文档吗？')
        .then(function () {
            updateAttCleanRule({ id: row.id, validFlag: row.validFlag }).then((response) => {
                proxy.$modal.msgSuccess(text + '成功');
                getList();
            });
        })
        .catch(function () {
            row.validFlag = !row.validFlag;
        });
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
    form.value.type = queryParams.value.type;
    open.value = true;
    title.value = '新增清洗规则';
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getAttCleanRule(_id).then((response) => {
        //把createTime过滤掉
        delete response.data.createTime;
        delete response.data.updateTime;
        form.value = response.data;
        open.value = true;
        title.value = '修改清洗规则';
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    reset();
    const _id = row.id || ids.value;
    getAttCleanRule(_id).then((response) => {
        form.value = response.data;
        openDetail.value = true;
        title.value = '清洗规则详情';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['attCleanRuleRef'].validate().then(() => {
        if (form.value.id != null) {
            updateAttCleanRule(form.value)
                .then((response) => {
                    proxy.$modal.msgSuccess('修改成功');
                    open.value = false;
                    getList();
                })
                .catch((error) => { });
        } else {
            addAttCleanRule(form.value)
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
    console.log(row, 'row');
    console.log(row.id, 'row');
    const _ids = row.id || ids.value;
    const _name = row.name;
    proxy.$modal
        .confirm('是否确认删除编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delAttCleanRule(_ids);
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
        'tax/cleanRule/export',
        {
            ...queryParams.value
        },
        `attCleanRule_${new Date().getTime()}.xlsx`
    );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
    upload.title = '清洗规则导入';
    upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
    proxy.download(
        'system/user/importTemplate',
        {},
        `attCleanRule_template_${new Date().getTime()}.xlsx`
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
function getDeptTree() {
    listAttCleanCat({ validFlag: true }).then((response) => {
        response.data.forEach(obj => {
            dataMapCat.set(obj.id + "", obj.name);
        });
        getList();
        processedData.value = proxy.handleTree(response.data, "id", "parentId");
        processedData.value = [
            {
                name: "清洗规则目录",
                value: "",
                id: 0,
                children: processedData.value,
            },
        ];
        console.log(processedData.value, "safsdfsd")
    });
};
getDeptTree();
</script>
<style scoped lang="scss">
.app-container {
    margin: 13px 15px;
}

.ant-layout-content {
    padding: 2px 0px;
    // box-shadow: 1px 1px 3px rgba(0, 0, 0, .2);
}
</style>
