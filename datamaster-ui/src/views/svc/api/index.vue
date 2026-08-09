<template>
  <div class="app-container" ref="app-container">

    <a-layout>
      <DeptTree :deptOptions="deptOptions" :leftWidth="leftWidth" :placeholder="'请输入API服务目录'" ref="DeptTreeRef"
        @node-click="handleNodeClick" />

      <a-layout-content class="main-content">
        <div class="pagecont-top" v-show="showSearch">
          <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="'inline'"
            :label-col="{ style: { width: '45px' } }" v-show="showSearch" @submit.prevent>
            <a-form-item label="名称" name="name">
              <a-input v-model:value="queryParams.name" placeholder="请输入API服务名称" allow-clear
                style="width: 180px;" @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="状态" name="status">
              <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear style="width: 160px;">
                <a-select-option v-for="dict in ds_api_log_status" :key="dict.value" :label="dict.label"
                  :value="dict.value" />
              </a-select>
            </a-form-item>
            <a-form-item label="时间">
              <a-range-picker v-model:value="daterangeCreateTime" valueFormat="YYYY-MM-DD"
                :placeholder="['开始', '结束']" style="width: 210px;"></a-range-picker>
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
            <a-button type="primary" @click="routeToAdd('/svc/api/add')" v-hasPermi="['svc:api:add']"
              @mousedown="(e) => e.preventDefault()">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
          </div>
          <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
          </div>
        </div>
        <div>
          <a-table
            striped
            :loading="loading"
            :data-source="dsApiList"
            :pagination="false"
            :columns="tableColumns"
            :locale="{ emptyText: '暂无记录' }"
            @change="handleTableChange"
          >
            <template #headerCell="{ column }">
              <template v-if="column.dataIndex === 'status'">
                <div class="justify-center">
                  <span style="margin-right: 5px;">状态</span>
                  <a-tooltip title="当状态为开启则可以被调用，并且同步发布到资源门户中" placement="top">
                    <InfoCircleOutlined class="tip-icon" />
                  </a-tooltip>
                </div>
              </template>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'name'">
                {{ record.name || "-" }}
              </template>
              <template v-if="column.dataIndex === 'catName'">
                {{ record.catName || "-" }}
              </template>
              <template v-if="column.dataIndex === 'description'">
                {{ record.description || '-' }}
              </template>
              <template v-if="column.dataIndex === 'apiVersion'">
                {{ record.apiVersion || "-" }}
              </template>
              <template v-if="column.dataIndex === 'apiUrl'">
                {{ record.apiUrl || "-" }}
              </template>
              <template v-if="column.dataIndex === 'reqMethod'">
                <dict-tag :options="ds_api_bas_info_api_method_type" :value="record.reqMethod" />
              </template>
              <template v-if="column.dataIndex === 'resDataType'">
                <dict-tag :options="ds_api_bas_info_res_data_type" :value="record.resDataType" />
              </template>
              <template v-if="column.dataIndex === 'createBy'">
                {{ record.createBy || '-' }}
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
              </template>
              <template v-if="column.dataIndex === 'status'">
                <a-switch v-model:checked="record.status" checked-value="1" un-checked-value="0"
                  @change="handleStatusChange(record)" />
              </template>
              <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="routeTo('/svc/api/edit', record)"
                  v-hasPermi="['svc:api:edit']">修改</a-button>
                <a-button type="link" size="small" @click="routeTo('/svc/api/detail', record)"
                  v-hasPermi="['svc:api:edit']">详情</a-button>
                <a-button type="link" danger size="small" @click="handleDelete(record)"
                  v-hasPermi="['svc:api:remove']">删除</a-button>
              </template>
            </template>
          </a-table>

          <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 添加或修改API服务对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px">
      <template #title>
        <span role="heading" aria-level="2">
          {{ title }}
        </span>
      </template>
      <a-form ref="dsApiRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" @submit.prevent>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="API服务名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入API服务名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="API版本" name="apiVersion">
              <a-input v-model:value="form.apiVersion" placeholder="请输入API版本" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="API路径" name="apiUrl">
              <a-input v-model:value="form.apiUrl" placeholder="请输入API路径" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="请求方式" name="reqMethod">
              <a-radio-group v-model:value="form.reqMethod">
                <a-radio v-for="dict in ds_api_bas_info_api_method_type" :key="dict.value" :value="dict.value">{{
                  dict.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="服务提供类型" name="apiServiceType">
              <a-radio-group v-model:value="form.apiServiceType">
                <a-radio v-for="dict in ds_api_bas_info_api_service_type" :key="dict.value" :value="dict.value">{{
                  dict.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="返回结果类型" name="resDataType">
              <a-select v-model:value="form.resDataType" placeholder="请选择返回结果类型">
                <a-select-option v-for="dict in ds_api_bas_info_res_data_type" :key="dict.value" :label="dict.label"
                  :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="IP黑名单多个，隔开" name="denyIp">
              <a-textarea v-model:value="form.denyIp" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="执行配置JSON" name="configJson">
              <a-textarea v-model:value="form.configJson" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="限流配置JSON" name="limitJson">
              <a-input v-model:value="form.limitJson" placeholder="请输入限流配置JSON" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="请求参数" name="reqParams">
              <a-textarea v-model:value="form.reqParams" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="返回参数" name="resParams">
              <a-textarea v-model:value="form.resParams" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="描述" name="description">
              <a-input v-model:value="form.description" placeholder="请输入描述" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio v-for="dict in ds_api_log_status" :key="dict.value" :value="dict.value">{{ dict.label
                }}</a-radio>
              </a-radio-group>
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

    <!-- API服务详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="800px">
      <template #title>
        <span role="heading" aria-level="2">
          {{ title }}
        </span>
      </template>
      <a-form ref="dsApiRef" :model="form" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="API服务名称" name="name">
              <div>
                {{ form.name }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="API版本" name="apiVersion">
              <div>
                {{ form.apiVersion }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="API路径" name="apiUrl">
              <div>
                {{ form.apiUrl }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="请求方式" name="reqMethod">
              <dict-tag :options="ds_api_bas_info_api_method_type" :value="form.reqMethod" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="服务提供类型" name="apiServiceType">
              <dict-tag :options="ds_api_bas_info_api_service_type" :value="form.apiServiceType" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="返回结果类型" name="resDataType">
              <dict-tag :options="ds_api_bas_info_res_data_type" :value="form.resDataType" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="IP黑名单多个，隔开" name="denyIp">
              <div>
                {{ form.denyIp }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="执行配置JSON" name="configJson">
              <div>
                {{ form.configJson }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="限流配置JSON" name="limitJson">
              <div>
                {{ form.limitJson }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="请求参数" name="reqParams">
              <div>
                {{ form.reqParams }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="返回参数" name="resParams">
              <div>
                {{ form.resParams }}
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
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <dict-tag :options="ds_api_log_status" :value="form.status" />
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
    <a-modal :title="upload.title" v-model:open="upload.open" width="800px" destroy-on-close>
      <a-upload-dragger ref="uploadRef" :max-count="1" accept=".xlsx, .xls" :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
        @progress="handleFileUploadProgress" @success="handleFileSuccess">
        <CloudUploadOutlined class="ant-upload-drag-icon" />
        <p class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
      </a-upload-dragger>
      <div class="upload-hint text-center">
        <div class="upload-hint">
          <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的API服务数据
        </div>
        <span>仅允许导入xls、xlsx格式文件。</span>
        <a-typography-link type="primary" style="font-size: 12px; vertical-align: baseline"
          @click="importTemplate">下载模板</a-typography-link>
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

<script setup name="DaApi">
import {
  listDsApi,
  getDsApi,
  delDsApi,
  addDsApi,
  updateDsApi,
  releaseDataApi,
  cancelDataApi,
} from "@/api/svc/api/api.js";
import { getToken } from "@/utils/auth.js";
import DeptTree from "@/components/DeptTree";
import { listAttApiCat } from "@/api/svc/apiCat/apiCat";
import useUserStore from "@/store/system/user";
import { CloudUploadOutlined, InfoCircleOutlined } from "@ant-design/icons-vue";
const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const {
  ds_api_log_status,
  ds_api_bas_info_api_service_type,
  ds_api_bas_info_api_method_type,
  ds_api_bas_info_res_data_type,
} = proxy.useDict(
  "ds_api_log_status",
  "ds_api_bas_info_api_service_type",
  "ds_api_bas_info_api_method_type",
  "ds_api_bas_info_res_data_type"
);

const dsApiList = ref([]);

// 列显隐信息
const columns = ref([
  { key: 0, label: "编号", visible: true },
  { key: 1, label: "API名称", visible: true },
  { key: 2, label: "API服务目录", visible: true },
  { key: 3, label: "描述", visible: true },
  { key: 4, label: "API版本", visible: true },
  { key: 5, label: "API路径", visible: true },
  { key: 6, label: "请求类型", visible: true },
  { key: 7, label: "返回格式", visible: true },
  { key: 8, label: "创建人", visible: true },
  { key: 9, label: "创建时间", visible: true },
  { key: 10, label: "状态", visible: true },
  { key: 11, label: "备注", visible: true },
  { key: 12, label: "操作", visible: true },
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  if (!column) return true;
  return column.visible;
};

const tableColumns = computed(() => {
  const allCols = [
    { title: '编号', dataIndex: 'id', align: 'center', width: 120, colKey: 0 },
    { title: 'API名称', dataIndex: 'name', align: 'left', width: 300, ellipsis: true, colKey: 1 },
    { title: 'API服务目录', dataIndex: 'catName', align: 'left', minWidth: 160, ellipsis: true, colKey: 2 },
    { title: '描述', dataIndex: 'description', align: 'left', width: 200, ellipsis: true, colKey: 3 },
    { title: 'API版本', dataIndex: 'apiVersion', align: 'center', width: 80, colKey: 4 },
    { title: 'API路径', dataIndex: 'apiUrl', align: 'left', minWidth: 200, ellipsis: true, colKey: 5 },
    { title: '请求类型', dataIndex: 'reqMethod', align: 'center', width: 80, colKey: 6 },
    { title: '返回格式', dataIndex: 'resDataType', align: 'center', width: 80, colKey: 7 },
    { title: '创建人', dataIndex: 'createBy', align: 'center', width: 120, ellipsis: true, colKey: 8 },
    { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 150, key: 'create_time', sorter: true, defaultSortOrder: 'descend', colKey: 9 },
    { title: '状态', dataIndex: 'status', align: 'center', width: 70, colKey: 10 },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 220, colKey: 12 },
  ];
  return allCols.filter(col => getColumnVisibility(col.colKey));
});

const deptOptions = ref(undefined);
const leftWidth = ref(300); // 初始左侧宽度
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
const defaultSort = ref({ prop: "create_time", order: "desc" });
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
  url: import.meta.env.VITE_APP_BASE_API + "/svc/dsApi/importData",
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    name: null,
    status: null,
    createTime: null,
  },
  rules: {},
});
const { queryParams, form, rules } = toRefs(data);

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

function handleNodeClick(data) {
  queryParams.value.catCode = data.code || data.value || "";
  handleQuery();
}

function getApiCatList() {
  listAttApiCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "API服务目录",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
}

/** 查询API服务列表 */
function getList() {
  loading.value = true;
  queryParams.value.params = {};
  queryParams.value.spaceId = userStore.spaceId || null;
  queryParams.value.spaceCode = userStore.spaceCode || "";
  if (null != daterangeCreateTime && "" != daterangeCreateTime) {
    queryParams.value.params["beginCreateTime"] = daterangeCreateTime.value[0];
    queryParams.value.params["endCreateTime"] = daterangeCreateTime.value[1];
  }
  console.log(queryParams.value);

  listDsApi(queryParams.value)
    .then((response) => {
      const pageData = normalizePageData(response);
      dsApiList.value = pageData.rows;
      total.value = pageData.total;
    })
    .catch((error) => { })
    .finally(() => {
      loading.value = false;
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

/** 启用禁用开关 */
function handleStatusChange(row) {
  const text = row.status === "1" ? "上线" : "下线";
  proxy.$modal
    .confirm("确认要" + text + '"' + row.name + '"服务吗？')
    .then(function () {
      loading.value = true;
      if (row.status === "1") {
        releaseDataApi(row.id).then((response) => {
          proxy.$modal.msgSuccess(text + "成功");
          getList();
        });
      } else {
        cancelDataApi(row.id).then((response) => {
          proxy.$modal.msgSuccess(text + "成功");
          getList();
        });
      }
    })
    .catch(function () {
      if (row.status === "1") {
        row.status = "0";
      } else {
        row.status = "1";
      }
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
    NAME: null,
    apiVersion: null,
    apiUrl: null,
    reqMethod: null,
    apiServiceType: null,
    resDataType: null,
    denyIp: null,
    configJson: null,
    limitJson: null,
    reqParams: null,
    resParams: null,
    description: null,
    status: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
  };
  proxy.resetForm("dsApiRef");
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
  daterangeCreateTime.value = [];
  queryParams.value.catCode = "";
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

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增API服务";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _ID = row.ID || ids.value;
  getDsApi(_ID).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改API服务";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _ID = row.id || ids.value;
  getDsApi(_ID).then((response) => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "API服务详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["dsApiRef"].validate().then(() => {
    if (form.value.ID != null) {
      updateDsApi(form.value)
        .then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        })
        .catch((error) => { });
    } else {
      addDsApi(form.value)
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
    .confirm('是否确认删除API服务编号为"' + _IDs + '"的数据项？')
    .then(function () {
      return delDsApi(_IDs);
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
    "svc/api/export",
    {
      ...queryParams.value,
    },
    `dsApi_${new Date().getTime()}.xlsx`
  );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "API服务导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `dsApi_template_${new Date().getTime()}.xlsx`
  );
}

/** 提交上传文件 */
function submitFileForm() {
  upload.open = false;
  getList();
}

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
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

function routeToAdd(link, row) {
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
      });
    }
  }
}
queryParams.value.orderByColumn = defaultSort.value.prop;
queryParams.value.isAsc = defaultSort.value.order;
onActivated(() => {
  getList();
});
getList();
getApiCatList();
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

.main-content {
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
  }
}
</style>


