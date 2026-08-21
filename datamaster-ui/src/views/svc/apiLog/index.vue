<template>
  <div class="app-container" ref="app-container">
    <a-layout style="90%">
      <DeptTree :deptOptions="deptOptions" :leftWidth="leftWidth" :placeholder="'请输入API服务目录'" ref="DeptTreeRef"
        @node-click="handleNodeClick" />

      <a-layout-content class="main-content">
        <div class="pagecont-top" v-show="showSearch">
          <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="'inline'"
            :label-col="{ style: { width: '95px' } }" v-show="showSearch" @submit.prevent>
            <a-form-item label="API服务名称" name="apiName">
              <a-input class="el-form-input-width" v-model:value="queryParams.apiName" placeholder="请输入API服务名称" allow-clear
                @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="状态" name="status">
              <a-select class="el-form-input-width" v-model:value="queryParams.status" placeholder="请选择状态" allow-clear>
                <a-select-option v-for="dict in ds_api_log_res_status" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="调用时间">
              <a-range-picker class="el-form-input-width" v-model:value="daterangeCreateTime" value-format="YYYY-MM-DD"
                :placeholder="['开始日期', '结束日期']" />
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

        <div class="pagecont-bottom">
          <a-table
            striped
            :loading="loading"
            :data-source="apiLogList"
            :pagination="false"
            :columns="tableColumns"
            :locale="{ emptyText: '暂无记录' }"
            @change="handleTableChange"
          >
            <template #headerCell="{ column }">
              <template v-if="column.dataIndex === 'status'">
                <div class="justify-center">
                  <span style="margin-right: 5px;">服务状态</span>
                  <a-tooltip title="当状态为开启则可以被调用，并且同步发布到资源门户中" placement="top">
                    <InfoCircleOutlined class="tip-icon" />
                  </a-tooltip>
                </div>
              </template>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'apiName'">
                {{ record.apiName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'catName'">
                {{ record.catName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'callerIp'">
                {{ record.callerIp || "-" }}
              </template>
              <template v-if="column.dataIndex === 'callerUrl'">
                {{ record.callerUrl || "-" }}
              </template>
              <template v-if="column.dataIndex === 'callerSize'">
                {{ record.callerSize || "-" }}
              </template>
              <template v-if="column.dataIndex === 'callerTime'">
                {{ record.callerTime / 1000 || "-" }}
              </template>
              <template v-if="column.dataIndex === 'status'">
                <dict-tag :options="ds_api_log_res_status" :value="record.status" />
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") }}</span>
              </template>
              <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="handleDetail(record)"
                  v-hasPermi="['svc:apiLog:query']">详情</a-button>
                <a-button type="link" danger size="small" @click="handleDelete(record)"
                  v-hasPermi="['svc:apiLog:remove']">删除</a-button>
              </template>
            </template>
          </a-table>

          <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 添加或修改API服务调用日志对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px">
      <a-form ref="apiLogRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" @submit.prevent>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="调用url" name="callerUrl">
              <a-input v-model:value="form.callerUrl" placeholder="请输入调用url" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="调用参数" name="callerParams">
              <a-textarea v-model:value="form.callerParams" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="调用开始时间" name="callerStartDate">
              <a-date-picker allow-clear style="width: 100%" v-model:value="form.callerStartDate" value-format="YYYY-MM-DD"
                placeholder="请选择调用开始时间" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调用结束时间" name="callerEndDate">
              <a-date-picker allow-clear style="width: 100%" v-model:value="form.callerEndDate" value-format="YYYY-MM-DD"
                placeholder="请选择调用结束时间" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="调用数据量" name="callerSize">
              <a-input v-model:value="form.callerSize" placeholder="请输入调用数据量" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调用耗时(毫秒)" name="callerTime">
              <a-input v-model:value="form.callerTime" placeholder="请输入调用耗时(毫秒)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="信息记录" name="MSG">
              <a-textarea v-model:value="form.MSG" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio v-for="dict in ds_api_log_res_status" :key="dict.value" :value="dict.value">{{ dict.label
                }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="备注" name="REMARK">
              <a-input v-model:value="form.REMARK" placeholder="请输入备注" />
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

    <!-- API服务调用日志详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="1000px">
      <a-form ref="apiLogRef" :model="form" :label-col="{ style: { width: '110px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="API服务名称">
              <div class="form-readonly">
                {{ form.apiName || "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调用者IP">
              <div class="form-readonly">
                {{ form.callerIp || "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="调用接口地址">
              <div class="form-readonly">
                {{ form.callerUrl || "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="调用参数">
              <div class="form-readonly textarea">
                {{ form.callerParams || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="调用时间">
              <div class="form-readonly">
                {{ parseTime(form.createTime, '{y}-{m}-{d} {h}:{i}') }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调用耗时(秒)">
              <div class="form-readonly">
                {{ form.callerTime / 1000 || '-' }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="请求方式">
              <div>
                <dict-tag :options="ds_api_bas_info_api_method_type" :value="form.reqMethod" />
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调用数据量">
              <div class="form-readonly">
                {{ form.callerSize || "-" }}
              </div>
            </a-form-item>
          </a-col>

        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="异常信息记录">
              <div class="form-readonly textarea">
                {{ form.MSG || "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <dict-tag :options="ds_api_log_res_status" :value="form.status" />
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
    <a-modal :title="upload.title" v-model:open="upload.open" width="800px">
      <a-upload-dragger ref="uploadRef" :max-count="1" accept=".xlsx, .xls" :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
        @progress="handleFileUploadProgress" @success="handleFileSuccess">
        <CloudUploadOutlined style="font-size: 42px; color: #4096ff" />
        <div class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="ant-upload-tip text-center">
            <div class="ant-upload-tip">
              <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的API服务调用日志数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <a-link type="primary" style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate">下载模板</a-link>
          </div>
        </template>
      </a-upload-dragger>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="upload.open = false">取 消</a-button>
          <a-button type="primary" @click="submitFileForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="ApiLog">
import {
  listApiLog,
  getApiLog,
  delApiLog,
  addApiLog,
  updateApiLog,
} from "@/api/svc/apiLog/apiLog";
import { getToken } from "@/utils/auth.js";
import { listAttApiCat } from "@/api/svc/apiCat/apiCat";
import DeptTree from "@/components/DeptTree";
import { CloudUploadOutlined, InfoCircleOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { ds_api_log_res_status, ds_api_bas_info_api_method_type } = proxy.useDict(
  'ds_api_log_res_status',
  'ds_api_bas_info_api_method_type'
);

const apiLogList = ref([]);

// 列显隐信息
const columns = ref([
  { key: 1, label: "编号", visible: true },
  { key: 2, label: "API服务名称", visible: true },
  { key: 3, label: "API服务目录", visible: true },
  { key: 4, label: "调用者IP", visible: true },
  { key: 5, label: "调用接口地址", visible: true },
  { key: 6, label: "调用数据量", visible: true },
  { key: 7, label: "调用耗时(秒)", visible: true },
  { key: 8, label: "状态", visible: true },
  { key: 9, label: "调用时间", visible: true },
  { key: 10, label: "操作", visible: true },
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  if (!column) return true;
  return column.visible;
};

const tableColumns = computed(() => {
  const allCols = [
    { title: '编号', dataIndex: 'id', align: 'center', width: 120, colKey: 1 },
    { title: 'API服务名称', dataIndex: 'apiName', align: 'left', width: 300, ellipsis: true, colKey: 2 },
    { title: 'API服务目录', dataIndex: 'catName', align: 'left', width: 160, ellipsis: true, colKey: 3 },
    { title: '调用者IP', dataIndex: 'callerIp', align: 'left', width: 130, ellipsis: true, colKey: 4 },
    { title: '调用接口地址', dataIndex: 'callerUrl', align: 'left', width: 250, ellipsis: true, colKey: 5 },
    { title: '调用数据量', dataIndex: 'callerSize', align: 'center', width: 120, key: 'caller_size', sorter: true, colKey: 6 },
    { title: '调用耗时(秒)', dataIndex: 'callerTime', align: 'center', width: 120, ellipsis: true, colKey: 7 },
    { title: '状态', dataIndex: 'status', align: 'center', width: 120, ellipsis: true, colKey: 8 },
    { title: '调用时间', dataIndex: 'createTime', align: 'center', width: 170, key: 'create_time', sorter: true, defaultSortOrder: 'descend', ellipsis: true, colKey: 9 },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200, colKey: 10 },
  ];
  return allCols.filter(col => getColumnVisibility(col.colKey));
});

const deptOptions = ref(undefined);
const leftWidth = ref(240); // 初始左侧宽度
const isResizing = ref(false); // 判断是否正在拖拽
let startX = 0; // 鼠标按下时的初始位置// 初始左侧宽度
const open = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const daterangeCreateTime = ref([]);
const defaultSort = ref({ columnKey: "create_time", order: "desc" });

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
  url: import.meta.env.VITE_APP_BASE_API + "/svc/apiLog/importData",
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    apiId: null,
    callerId: null,
    createTime: null,
  },
  rules: {},
});

const { queryParams, form, rules } = toRefs(data);


function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  handleQuery();
}

function getApiCatList() {
  listAttApiCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "API服务目录",
        value: "",
        children: deptOptions.value,
      },
    ];
  });
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

function normalizePageData(response) {
  const data = response?.data ?? response ?? {};
  if (Array.isArray(data)) {
    return { rows: data, total: data.length };
  }
  const rows = Array.isArray(data.rows)
    ? data.rows
    : Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(response?.rows)
          ? response.rows
          : [];
  const total = Number(data.total ?? data.totalCount ?? response?.total ?? rows.length);
  return { rows, total: Number.isNaN(total) ? rows.length : total };
}

/** 查询API服务调用日志列表 */
function getList() {
  loading.value = true;
  queryParams.value.params = {};
  if (null != daterangeCreateTime.value && "" != daterangeCreateTime.value) {
    queryParams.value.params["beginCreateTime"] =
      daterangeCreateTime.value[0] + " 00:00:00";
    queryParams.value.params["endCreateTime"] =
      daterangeCreateTime.value[1] + " 23:59:59";
  }
  listApiLog(queryParams.value)
    .then((response) => {
      const pageData = normalizePageData(response);
      apiLogList.value = pageData.rows;
      total.value = pageData.total;
    })
    .catch((error) => { })
    .finally(() => {
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
    ID: null,
    apiId: null,
    callerId: null,
    callerBy: null,
    callerIp: null,
    callerUrl: null,
    callerParams: null,
    callerStartDate: null,
    callerEndDate: null,
    callerSize: null,
    callerTime: null,
    MSG: null,
    STATUS: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    REMARK: null,
  };
  proxy.resetForm("apiLogRef");
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
  proxy.resetForm("queryRef");
  daterangeCreateTime.value = [];
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.ID);
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
  title.value = "新增API服务调用日志";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _ID = row.ID || ids.value;
  getApiLog(_ID).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改API服务调用日志";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _ID = row.id || ids.value;
  console.log("_ID::" + _ID);
  getApiLog(_ID).then((response) => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "API服务调用日志详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["apiLogRef"].validate().then(() => {
    if (form.value.ID != null) {
      updateApiLog(form.value)
        .then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        })
        .catch((error) => { });
    } else {
      addApiLog(form.value)
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
  const _IDs = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除API服务调用日志编号为"' + _IDs + '"的数据项？')
    .then(function () {
      return delApiLog(_IDs);
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
    "svc/apiLog/export",
    {
      ...queryParams.value,
    },
    `apiLog_${new Date().getTime()}.xlsx`
  );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "API服务调用日志导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `apiLog_template_${new Date().getTime()}.xlsx`
  );
}

/** 提交上传文件 */
function submitFileForm() {
  upload.open = false;
  upload.isUploading = false;
}

/**文件上传中处理 */
const handleFileUploadProgress = () => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
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
  //包含http直接跳转
  if (link !== "" && link.indexOf("http") !== -1) {
    window.location.href = link;
    return;
  }
  if (link !== "") {
    if (link === router.currentRoute.value.path) {
      //是当前页面直接刷新
      window.location.reload();
    } else {
      //跳转路由
      router.push({
        path: link,
        query: {
          id: row.id,
        },
      });
    }
  }
}
queryParams.value.orderByColumn = defaultSort.value.columnKey || defaultSort.value.prop;
queryParams.value.isAsc = defaultSort.value.order;
getList();
getApiCatList();
</script>
<style scoped lang="scss">
::v-deep {
  .selectlist .ant-tag.ant-tag--info {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.app-container {
  margin: 13px 15px;
}

.main-content {
  padding: 2px 0px;
  // box-shadow: 1px 1px 3px rgba(0, 0, 0, .2);
}

//上传附件样式调整
::v-deep {

  // .ant-upload-list{
  //    display: flex;
  // }
  .ant-upload-list-item {
    width: 100%;
    height: 25px;
  }
}
</style>

