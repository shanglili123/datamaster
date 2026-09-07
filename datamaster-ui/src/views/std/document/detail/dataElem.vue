<template>
    <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
            <a-col :span="1.5">
                <a-button type="primary" @click="handleAdd" @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                </a-button>
            </a-col>
            <!-- <a-col :span="1.5">
                <a-button type="primary" :disabled="single" @click="handleUpdate"
                   @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-xiugai--copy mr5"></i>修改
                </a-button>
              </a-col>
              <a-col :span="1.5">
                <a-button type="primary" danger :disabled="multiple" @click="handleDelete"
                  @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-shanchu-huise mr5"></i>删除
                </a-button>
              </a-col> -->
            <!--          <a-col :span="1.5">-->
            <!--            <a-button @click="handleImport" v-hasPermi="['dp:dataElem:dataelem:export']"-->
            <!--                       @mousedown="(e) => e.preventDefault()">-->
            <!--              <i class="iconfont-mini icon-upload-cloud-line mr5"></i>导入-->
            <!--            </a-button>-->
            <!--          </a-col>-->
            <!--          <a-col :span="1.5">-->
            <!--            <a-button type="primary" @click="handleExport" v-hasPermi="['dp:dataElem:dataelem:export']"-->
            <!--                       @mousedown="(e) => e.preventDefault()">-->
            <!--              <i class="iconfont-mini icon-download-line mr5"></i>导出-->
            <!--            </a-button>-->
            <!--          </a-col>-->
        </a-row>
        <div class="justify-end top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </div>
    </div>

    <a-table
        striped
        :loading="loading"
        :data-source="dpDataElemList"
        :columns="tableColumns"
        :pagination="false"
        :scroll="{ y: 400 }"
        :locale="{ emptyText: '暂无记录' }"
    >
        <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'name'">
                {{ record.name || "-" }}
            </template>
            <template v-if="column.dataIndex === 'engName'">
                {{ record.engName || "-" }}
            </template>
            <template v-if="column.dataIndex === 'description'">
                {{ record.description || "-" }}
            </template>
            <template v-if="column.dataIndex === 'type'">
                <dict-tag :options="dp_data_elem_code_type" :value="record.type" />
            </template>
            <template v-if="column.dataIndex === 'catCode'">
                {{ record.catName || "-" }}
            </template>
            <template v-if="column.dataIndex === 'createBy'">
                {{ record.createBy || "-" }}
            </template>
            <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
            </template>
            <template v-if="column.dataIndex === 'status'">
                <a-switch v-model:checked="record.status" checked-value="1" un-checked-value="0"
                    @change="(e) => handleStatusChange(record.id, record, e)"
/>
            </template>
            <template v-if="column.dataIndex === 'remark'">
                {{ record.remark || "-" }}
            </template>
            <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="handleUpdate(record)"
                    v-hasPermi="['dp:dataElem:dataelem:edit']"
>修改</a-button>
                <a-button type="link" danger size="small" @click="handleDelete(record)"
                    v-hasPermi="['dp:dataElem:dataelem:remove']"
>删除</a-button>
                <a-button type="link" size="small" @click="handleDetail(record)"
                    v-hasPermi="['dp:dataElem:dataelem:edit']"
>详情</a-button>
            </template>
        </template>
    </a-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList"
/>

    <!-- 新增或修改数据元对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" draggable>
        <a-form ref="dpDataElemRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }"
            @submit.prevent
>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="中文名称" name="name">
                        <a-input v-model:value="form.name" placeholder="请输入中文名称" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="英文名称" name="engName">
                        <a-input v-model:value="form.engName" placeholder="请输入英文名称" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="数据元目录" name="catCode">
                        <a-tree-select show-search v-model:value="form.catCode" :tree-data="deptOptions"
                            :field-names="{ value: 'code', label: 'name', children: 'children' }"
                            placeholder="请选择数据元目录" tree-check-strictly
/>
                    </a-form-item>
                </a-col>

                <a-col :span="12">
                    <a-form-item label="字段类型" name="columnType">
                        <a-select v-model:value="form.columnType" placeholder="请选择字段类型">
                            <a-select-option v-for="dict in column_type" :key="dict.value" :label="dict.label"
                                :value="dict.value"
>{{ dict.label }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="描述" name="description">
                        <a-input v-model:value="form.description" type="textarea" placeholder="请输入描述" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <!-- <a-col :span="12">
                    <a-form-item label="类型" name="type">
                        <a-radio-group v-model:value="form.type" disabled>
                            <a-radio v-for="dict in dp_data_elem_code_type" :key="dict.value" :value="dict.value">{{
                                dict.label }}
                            </a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col> -->
                <a-col :span="12">
                    <a-form-item label="状态" name="status">
                        <a-radio-group v-model:value="form.status">
                            <a-radio v-for="dict in sys_disable" :key="dict.value" :value="dict.value">{{
                                dict.label }}
                            </a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>

            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="备注" name="remark">
                        <a-input v-model:value="form.remark" type="textarea" placeholder="请输入备注" />
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

    <!-- 用户导入对话框 -->
    <a-modal :title="upload.title" v-model:open="upload.open" width="800px" draggable destroy-on-close
        @ok="submitFileForm" @cancel="upload.open = false" :ok-button-props="{ disabled: upload.isUploading }"
>
        <a-upload ref="uploadRef" :max-count="1" accept=".xlsx, .xls" :headers="upload.headers"
            :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
            :before-upload="handleFileUploadProgress" :auto-upload="false" drag
            @change="handleUploadChange"
>
            <p class="ant-upload-drag-icon">
                <UploadOutlined />
            </p>
            <p class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
            <template #tip>
                <div class="ant-upload-tip text-center">
                    <div class="ant-upload-tip">
                        <a-checkbox v-model:checked="upload.updateSupport" />
                        是否更新已经存在的数据元数据
                    </div>
                    <span>仅允许导入xls、xlsx格式文件。</span>
                    <a-typography-link type="primary" style="font-size: 12px; vertical-align: baseline"
                        @click="importTemplate"
>下载模板
                    </a-typography-link>
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

    <!--        &lt;!&ndash;数据元字段详情&ndash;&gt;-->
    <!--        <data-elem-detail-dialog ref="detailDialog" />-->
    <!--        &lt;!&ndash;数据元代码详情&ndash;&gt;-->
    <!--        <data-elem-code-detail-dialog ref="detailCodeDialog" />-->
</template>

<script setup name="StandardsDataElem">
import DeptTree from "@/components/DeptTree";
import {
    listDpDataElem,
    getDpDataElem,
    delDpDataElem,
    addDpDataElem,
    updateDpDataElem,
    updateStatusDpDataElem,
} from "@/api/std/dataElem/dataElem";
import { listAttDataElemCat } from "@/api/tax/cat/dataElemCat/dataElemCat";
import { getToken } from "@/utils/auth.js";
import { UploadOutlined } from "@ant-design/icons-vue";
const { proxy } = getCurrentInstance();
import { useRoute } from 'vue-router';
const route = useRoute();
const props = defineProps({
    activeName: { type: Number, default: null },
});
const { column_type, sys_disable, dp_data_elem_code_type } = proxy.useDict(
    "column_type",
    "sys_disable",
    "dp_data_elem_code_type"
);
const deptOptions = ref(undefined);
const leftWidth = ref(240); // 初始左侧宽度
const isResizing = ref(false); // 判断是否正在拖拽
let startX = 0; // 鼠标按下时的初始位置// 初始左侧宽度
/** 类型字典翻译 */
// function typeFormat(row) {
//   return proxy.selectDictLabel(dp_data_elem_code_type.value, row.type);
// }

const dpDataElemList = ref([]);
const dpDataElemRuleRelList = ref([]);

// 列显隐信息
const columns = ref([
    { key: 0, label: "编号", visible: true },
    { key: 1, label: "中文名称", visible: true },
    { key: 2, label: "英文名称", visible: true },
    { key: 7, label: "描述", visible: true },
    { key: 3, label: "类型", visible: true },
    { key: 4, label: "数据元目录", visible: true },
    { key: 10, label: "创建人", visible: true },
    { key: 11, label: "创建时间", visible: true },
    { key: 5, label: "状态", visible: true },
    { key: 6, label: "描述", visible: true },
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
        { title: '编号', dataIndex: 'id', align: 'left', width: 50, colKey: 0 },
        { title: '中文名称', dataIndex: 'name', align: 'left', width: 200, ellipsis: true, colKey: 1 },
        { title: '英文名称', dataIndex: 'engName', align: 'left', width: 200, ellipsis: true, colKey: 2 },
        { title: '描述', dataIndex: 'description', align: 'left', width: 240, ellipsis: true, colKey: 7 },
        { title: '类型', dataIndex: 'type', align: 'left', width: 100, colKey: 3 },
        { title: '数据元目录', dataIndex: 'catCode', align: 'left', width: 120, ellipsis: true, colKey: 4 },
        { title: '创建人', dataIndex: 'createBy', align: 'left', ellipsis: true, colKey: 10 },
        { title: '创建时间', dataIndex: 'createTime', align: 'left', width: 150, colKey: 11 },
        { title: '状态', dataIndex: 'status', align: 'left', width: 80, colKey: 5 },
        { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true, colKey: 15 },
        { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200, colKey: 'actions' },
    ];
    return allCols.filter(col => col.colKey === 'actions' || getColumnVisibility(col.colKey));
});

const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const checkedDpDataElemRuleRel = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const defaultSort = ref({ prop: "createTime", order: "desc" });
const router = useRouter();

/*** 用户导入参数 */
const upload = reactive({
    // 是否显示弹出层（用户导入）
    open: false,
    // 弹出层标题（用户导入）
    title: "",
    // 是否禁用上传
    isUploading: false,
    // 是否更新已经存在的用户数据
    updateSupport: 0,
    // 设置上传的请求头部
    headers: { Authorization: "Bearer " + getToken() },
    // 上传的地址
    url: import.meta.env.VITE_APP_BASE_API + "/std/dataElem/importData",
});

const data = reactive({
    form: { status: "0" },
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: null,
        engName: null,
        catCode: null,
        type: null,
        documentId: null,
    },
    rules: {
        name: [{ required: true, message: "中文名称不能为空", trigger: "blur" }],
        engName: [
            { required: true, message: "英文名称不能为空", trigger: "blur" },
            {
                pattern: /^[a-zA-Z_]+$/,
                message: "只能包含英文字母和下划线",
                trigger: "blur",
            },
        ],
        catCode: [{ required: true, message: "数据元目录不能为空", trigger: "blur" }],
        // status: [{ required: true, message: "状态不能为空", trigger: "change" }],
        // type: [{ required: true, message: "类型不能为空", trigger: "change" }],
        columnType: [
            { required: true, message: "字段类型不能为空", trigger: "change" },
        ],
    },
});

const { queryParams, form, rules } = toRefs(data);
/** 查询数据元列表 */
function getList() {
    loading.value = true;
    if (!queryParams.value.documentId) {
        queryParams.value.documentId = route.query.id;
    }
    queryParams.value.type = Number(props.activeName) - 1;
    listDpDataElem(queryParams.value).then((response) => {
        dpDataElemList.value = response.data.rows;
        total.value = response.data.total;
        loading.value = false;
    });
}
// 取消按钮
function cancel() {
    open.value = false;
    reset();
}
function handleNodeClick(data) {
    queryParams.value.catCode = data.code;
    handleQuery();
}
const startResize = (event) => {
    isResizing.value = true;
    startX = event.clientX;
    document.addEventListener("mousemove", updateResize);
    document.addEventListener("mouseup", stopResize);
};
const stopResize = () => {
    isResizing.value = false;
    document.removeEventListener("mousemove", updateResize);
    document.removeEventListener("mouseup", stopResize);
};
const updateResize = (event) => {
    if (isResizing.value) {
        const delta = event.clientX - startX; // 计算鼠标移动距离
        leftWidth.value += delta; // 修改左侧宽度
        startX = event.clientX; // 更新起始位置
        // 使用 requestAnimationFrame 来减少页面重绘频率
        requestAnimationFrame(() => { });
    }
}; /** 查询部门下拉树结构 */
// 表单重置
function reset() {
    form.value = {
        id: null,
        code: null,
        name: null,
        engName: null,
        catCode: null,
        type: "1",
        columnType: null,
        status: "0",
        description: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updaterId: null,
        updateTime: null,
        remark: null,
        documentId: null,
    };
    dpDataElemRuleRelList.value = [];
    proxy.resetForm("dpDataElemRef");
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
    queryParams.value.catCode = "";
    queryParams.value.pageNum = 1;
    reset();
    proxy.resetForm("queryRef");
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
function getDeptTree() {
    listAttDataElemCat().then((response) => {
        deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
        deptOptions.value = [
            {
                name: "数据元目录",
                value: "",
                id: 0,
                children: deptOptions.value,
            },
        ];
    });
}
/** 新增按钮操作 */
function handleAdd() {
    reset();
    form.value.type = String(Number(props.activeName) - 1)
    console.log("🚀 ~ handleAdd ~ form.value.type:", form.value.type)
    if (queryParams.value.catCode) {
        form.value.catCode = queryParams.value.catCode;
    }
    open.value = true;
    title.value = "新增数据元";
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value;
    getDpDataElem(_id).then((response) => {
        form.value = response.data;
        dpDataElemRuleRelList.value = response.data.dpDataElemRuleRelList;
        open.value = true;
        title.value = "修改数据元";
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
    if (row.type == 1) {
        routeTo("/std/dataElem/column/detail", row);
    } else {
        routeTo("/std/dataElem/dict/detail", row);
    }
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs["dpDataElemRef"].validate().then(() => {
        form.value.dpDataElemRuleRelList = dpDataElemRuleRelList.value;
        if (form.value.id != null) {
            updateDpDataElem(form.value)
                .then((response) => {
                    proxy.$modal.msgSuccess("修改成功");
                    open.value = false;
                    getList();
                })
                .catch((error) => { });
        } else {
            form.value.documentId = queryParams.value.documentId
            addDpDataElem(form.value)
                .then((response) => {
                    proxy.$modal.msgSuccess("新增成功");
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
        .confirm('是否确认删除数据元编号为"' + _ids + '"的数据项？')
        .then(function () {
            return delDpDataElem(_ids);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess("删除成功");
        })
        .catch(() => { });
}

/** 数据元数据规则关联信息序号 */
function rowDpDataElemRuleRelIndex({ row, rowIndex }) {
    row.index = rowIndex + 1;
}

/** 数据元数据规则关联信息新增按钮操作 */
function handleAddDpDataElemRuleRel() {
    let obj = {};
    obj.ruleType = "";
    obj.ruleId = "";
    obj.ruleConfig = "";
    obj.remark = "";
    dpDataElemRuleRelList.value.push(obj);
}

/** 数据元数据规则关联信息删除按钮操作 */
function handleDeleteDpDataElemRuleRel() {
    if (checkedDpDataElemRuleRel.value.length == 0) {
        proxy.$modal.msgWarning("未选择要删除的数据元数据规则关联信息，请选择后重试");
    } else {
        const dpDataElemRuleRels = dpDataElemRuleRelList.value;
        const checkedDpDataElemRuleRels = checkedDpDataElemRuleRel.value;
        dpDataElemRuleRelList.value = dpDataElemRuleRels.filter(function (item) {
            return checkedDpDataElemRuleRels.indexOf(item.index) == -1;
        });
    }
}

/** 复选框选中数据 */
function handleDpDataElemRuleRelSelectionChange(selection) {
    checkedDpDataElemRuleRel.value = selection.map((item) => item.index);
}

/** 导出按钮操作 */
function handleExport() {
    proxy.download(
        "std/dataElem/export",
        {
            ...queryParams.value,
        },
        `dpDataElem_${new Date().getTime()}.xlsx`
    );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
    upload.title = "数据元导入";
    upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
    proxy.download(
        "system/user/importTemplate",
        {},
        `dpDataElem_template_${new Date().getTime()}.xlsx`
    );
}

/** 提交上传文件 */
function submitFileForm() {
    proxy.$refs["uploadRef"].submit();
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
    proxy.$refs["uploadRef"].handleRemove(file);
    proxy.$alert(
        "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
        response.msg +
        "</div>",
        "导入结果",
        { dangerouslyUseHTMLString: true }
    );
    getList();
};

/** 启用禁用开关 */
function handleStatusChange(id, row, e) {
    const text = e === "1" ? "启用" : "禁用";
    proxy.$modal
        .confirm('确认要"' + text + '","' + row.name + '"数据元吗？')
        .then(function () {
            updateStatusDpDataElem(id, row.status).then((response) => {
                proxy.$modal.msgSuccess("操作成功");
            });
        })
        .catch(function () {
            row.status = row.status === "1" ? "0" : "1";
        });
}
// function handleStatusChange(row) {
//   let text = row.status === "0" ? "启用" : "停用";
//   proxy.$modal
//     .confirm('确认要"' + text + '""' + row.roleName + '"角色吗?')
//     .then(function () {
//       return changeRoleStatus(row.roleId, row.status);
//     })
//     .then(() => {
//       proxy.$modal.msgSuccess(text + "成功");
//     })
//     .catch(function () {
//       row.status = row.status === "0" ? "1" : "0";
//     });
// }
/** ---------------------------------**/

function routeTo(link, row) {
    if (link !== "" && link.indexOf("http") !== -1) {
        window.location.href = link;
        return;
    }
    if (link !== "") {
        if (link === router.currentRoute.value.path) {
            window.location.reload();
        } else {
            router.push({
                path: link,
                query: {
                    id: row.id,
                },
            });
        }
    }
}
queryParams.value.documentId = route.query.id;
queryParams.value.type = Number(props?.activeName) - 1;

getDeptTree();
getList();
</script>
<style scoped lang="scss">
::v-deep {
    .selectlist .el-tag.el-tag--info {
        background: #f3f8ff !important;
        border: 0px solid #6ba7ff !important;
        color: #2666fb !important;
    }
}

.app-container {
    margin: 13px 15px;
}

.ant-layout-content {
    padding: 2px 0px;
    // box-shadow: 1px 1px 3px rgba(0, 0, 0, .2);
}

//上传附件样式调整
::v-deep {

    // .el-upload-list{
    //    display: flex;
    // }
    .el-upload-list__item {
        width: 100%;
        height: 25px;
    }
}
</style>

