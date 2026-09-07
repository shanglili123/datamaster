<template>
  <div class="app-container" ref="app-container">

    <a-layout style="90%">
      <DeptTree :deptOptions="deptOptions" :leftWidth="leftWidth" :placeholder="'请输入资产目录名称'" ref="DeptTreeRef"
        @node-click="handleNodeClick" />

      <a-layout-content>
        <div class="pagecont-top" v-show="showSearch">
          <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }"
            v-show="showSearch" @submit.prevent>
            <a-form-item label="名称" name="assetName">
              <a-input style="width: 140px" v-model:value="queryParams.assetName" placeholder="请输入资产名称" allow-clear
                @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="主题" name="themeName">
              <a-input style="width: 140px" v-model:value="queryParams.themeName" placeholder="请输入主题名称" allow-clear
                @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="申请人" name="createBy">
              <a-input style="width: 140px" v-model:value="queryParams.createBy" placeholder="请输入申请人" allow-clear
                @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="状态" name="status">
              <a-select style="width: 140px" allow-clear v-model:value="queryParams.status" placeholder="请选择审核状态">
                <a-select-option v-for="dict in da_asset_apply_status" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>
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
          <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
          </div>
        </div>
        <div>
          <a-table
            striped
            :loading="loading"
            :data-source="assetApplyList"
            :columns="tableColumns"
            :pagination="false"
            :locale="{ emptyText: '暂无记录' }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'assetName'">
                {{ record.assetName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'assetTableName'">
                {{ record.assetTableName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'catAssetName'">
                {{ record.catAssetName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'themeName'">
                {{ record.themeName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'spaceName'">
                {{ record.spaceName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") }}</span>
              </template>
              <template v-if="column.dataIndex === 'createBy'">
                {{ record.createBy || "-" }}
              </template>
              <template v-if="column.dataIndex === 'status'">
                <dict-tag :options="da_asset_apply_status" :value="record.status" />
              </template>
              <template v-if="column.key === 'actions'">
                <a-button v-if="record.status == 1" type="link" size="small"
                  @click="handleUpdate(record)" v-hasPermi="['ast:assetApply:edit']">审核</a-button>
                <a-button type="link" size="small" @click="handleDetail(record)"
                  v-hasPermi="['ast:assetApply:edit']">详情</a-button>
              </template>
            </template>
          </a-table>

          <pagination :total="total || 0" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 添加或修改数据资产申请对话框 -->
    <a-modal :title="title" v-model:open="open" width="1000px" draggable @ok="submitForm" @cancel="cancel">
      <a-form ref="assetApplyRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }" @submit.prevent>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="资产名称">
              <div class="form-readonly">
                {{ form.assetName }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="英文名称">
              <div class="form-readonly">
                {{ form.assetTableName }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据连接">
              <div class="form-readonly">
                {{ form.datasourceName ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据库地址">
              <div class="form-readonly">
                {{ form.datasourceIp ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">

          <a-col :span="12">
            <a-form-item label="数据库类型:" name="datasourceType">
              <dict-tag :options="datasource_type" :value="form.datasourceType" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="资产描述">
              <div class="form-readonly textarea">
                {{ form.description ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="申请空间" name="spaceCode">
              <div class="form-readonly">
                {{ form.spaceName }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phonenumber">
              <div class="form-readonly">
                {{ form.phonenumber }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="申请理由:" name="applyReason">
              <div class="form-readonly textarea">
                {{ form.applyReason ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="审核结果" name="status">
              <a-radio-group v-model:value="form.status" @change="handleStatusChange">
                <a-radio :value="2">驳回</a-radio>
                <a-radio :value="3">通过</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20" v-if="form.status == 2">
          <a-col :span="24">
            <a-form-item label="驳回原因" name="approvalReason">
              <a-textarea :rows="12" v-model:value="form.approvalReason" placeholder="请输入驳回原因" />
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

    <!-- 数据资产申请详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="1000px" draggable @ok="cancel" @cancel="cancel">
      <a-form :model="form" :label-col="{ style: { width: '90px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="资产名称:" name="assetName">
              <div class="form-readonly">
                {{ form.assetName }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="英文名称:" name="assetTableName">
              <div class="form-readonly">
                {{ form.assetTableName }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">

          <a-col :span="12">
            <a-form-item label="数据连接:" name="datasourceName">
              <div class="form-readonly">
                {{ form.datasourceName }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据库地址:" name="datasourceIp">
              <div class="form-readonly">
                {{ form.datasourceIp }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">

          <a-col :span="12">
            <a-form-item label="数据库类型:" name="datasourceType">
              <dict-tag :options="datasource_type" :value="form.datasourceType" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="资产描述:" name="description">
              <div class="form-readonly textarea">
                {{ form.description ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="申请空间:" name="spaceName">
              <div class="form-readonly">
                {{ form.spaceName }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="申请状态:" name="status">
              <dict-tag :options="da_asset_apply_status" :value="form.status" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="申请人:" name="createBy">
              <div class="form-readonly">
                {{ form.createBy }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话:" name="phonenumber">
              <div class="form-readonly">
                {{ form.phonenumber }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="申请时间:" name="createTime">
              <div class="form-readonly">
                {{ form.createTime }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="申请理由:" name="applyReason">
              <div class="form-readonly textarea">
                {{ form.applyReason ?? "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="审批理由:" name="approvalReason">
              <div class="form-readonly textarea">
                {{ form.approvalReason ?? "-" }}
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
              <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的数据资产申请数据
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

<script setup name="AssetApply">
import {
  listDaAssetApply,
  getDaAssetApply,
  delDaAssetApply,
  updateDaAssetApply,
} from "@/api/ast/assetApply/assetApply";
import { listSpace } from "@/api/tax/space/space.js";
import { getToken } from "@/utils/auth.js";
import { listAttAssetCat } from "@/api/tax/cat/assetCat/assetCat.js";
import DeptTree from "@/components/DeptTree";
import { normalizePage, pageRows } from "@/utils/page.js";
import { UploadOutlined } from "@ant-design/icons-vue";
const { proxy } = getCurrentInstance();
const { da_asset_apply_status, datasource_type } = proxy.useDict(
  "da_asset_apply_status",
  "datasource_type"
);
const assetApplyList = ref([]);

// 列显隐信息
const columns = ref([
  { key: 1, label: "资产名称", visible: true },
  { key: 2, label: "英文名称", visible: true },
  { key: 3, label: "资产目录", visible: true },
  { key: 4, label: "主题名称", visible: true },
  { key: 5, label: "申请空间", visible: true },
  { key: 6, label: "申请时间", visible: true },
  { key: 7, label: "申请人", visible: true },
  { key: 8, label: "审核状态", visible: true },
  { key: 9, label: "操作", visible: true },
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
    { title: '资产名称', dataIndex: 'assetName', align: 'left', width: 200, ellipsis: true, colKey: 1 },
    { title: '英文名称', dataIndex: 'assetTableName', align: 'left', width: 280, ellipsis: true, colKey: 2 },
    { title: '资产目录', dataIndex: 'catAssetName', align: 'left', ellipsis: true, colKey: 3 },
    { title: '主题名称', dataIndex: 'themeName', align: 'left', width: 150, ellipsis: true, colKey: 4 },
    { title: '申请空间', dataIndex: 'spaceName', align: 'left', width: 150, ellipsis: true, colKey: 5 },
    { title: '申请时间', dataIndex: 'createTime', align: 'center', width: 160, ellipsis: true, key: 'create_time', sorter: true, defaultSortOrder: 'descend', colKey: 8 },
    { title: '申请人', dataIndex: 'createBy', align: 'center', width: 100, ellipsis: true, colKey: 6 },
    { title: '审核状态', dataIndex: 'status', align: 'center', width: 80, colKey: 7 },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 140, colKey: 9 },
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
const title = ref("");
const defaultSort = ref({ columnKey: "create_time", order: "desc" });
const router = useRouter();
const deptOptions = ref(undefined);
const leftWidth = ref(240); // 初始左侧宽度
const isResizing = ref(false); // 判断是否正在拖拽
const spaceOptions = ref([]);
let startX = 0; // 鼠标按下时的初始位置
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
  url: import.meta.env.VITE_APP_BASE_API + "/ast/assetApply/importData",
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    assetId: null,
    assetName: null,
    spaceId: null,
    spaceCode: null,
    applyReason: null,
    approvalReason: null,
    status: null,
    sourceType: 0,
    createBy: null,
    themeName: null,
    createTime: null,
  },
  rules: {
    status: [{ required: true, message: "请选择审核结果", trigger: "change" }],
    approvalReason: [
      { required: true, message: "请输入驳回原因", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

function handleNodeClick(data) {
  queryParams.value.catAssetCode = data.code || data.value || "";
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
};

function handleStatusChange(value) {
  form.value.status = value;
}

function getAssetCat() {
  listAttAssetCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "资产目录",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
}

/** 查询数据资产申请列表 */
function getList() {
  loading.value = true;
  listDaAssetApply(queryParams.value).then((response) => {
    const page = normalizePage(response);
    total.value = page.total;
    assetApplyList.value = pageRows(page.rows, page.total, queryParams.value);
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
    assetId: null,
    spaceId: null,
    spaceCode: null,
    applyReason: null,
    approvalReason: null,
    status: null,
    validFlag: null,
    delFlag: null,
    sourceType: 0,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  proxy.resetForm("assetApplyRef");
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
  queryParams.value.catAssetCode = "";
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
function handleTableChange(pagination, filters, sorter) {
  const field = sorter.column?.key || sorter.field;
  const orderMap = { ascend: 'asc', descend: 'desc' };
  queryParams.value.orderByColumn = field;
  queryParams.value.isAsc = sorter.order ? orderMap[sorter.order] : null;
  getList();
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getDaAssetApply(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "数据资产申请审核";
    form.value.status = null;
  });
  listSpace().then((response) => {
    spaceOptions.value = response.data.rows;
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id || ids.value;
  getDaAssetApply(_id).then((response) => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "数据资产申请详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["assetApplyRef"]
    .validate()
    .then(() => {
      if (form.value.id != null) {
        updateDaAssetApply(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess("修改成功");
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
    .confirm('是否确认删除数据资产申请编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delDaAssetApply(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => { });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "ast/assetApply/export",
    {
      ...queryParams.value,
    },
    `assetApply_${new Date().getTime()}.xlsx`
  );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "数据资产申请导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `assetApply_template_${new Date().getTime()}.xlsx`
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
  if (info.file.status === "uploading") {
    handleFileUploadProgress(info.event, info.file, info.fileList);
  } else if (info.file.status === "done") {
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
getAssetCat();
getList();
</script>
<style scoped lang="scss">
::v-deep {
  .selectlist .ant-tag {
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
  .ant-upload-list-item {
    width: 100%;
    height: 25px;
  }
}

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
    margin-left: auto;
  }
}
</style>


