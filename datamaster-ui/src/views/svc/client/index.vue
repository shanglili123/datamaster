<template>
  <div class="app-container" ref="app-container">

    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="'inline'"
        :label-col="{ style: { width: '75px' } }" v-show="showSearch" @submit.prevent
>
        <!-- <a-form-item label="编号" name="id">
          <a-input class="el-form-input-width" v-model:value="queryParams.id" placeholder="请输入编号" allow-clear
            @pressEnter="handleQuery" />
        </a-form-item> -->
        <a-form-item label="应用名称" name="name">
          <a-input class="el-form-input-width" v-model:value="queryParams.name" placeholder="请输入应用名称" allow-clear
            @pressEnter="handleQuery"
/>
        </a-form-item>
        <a-form-item label="应用类型" name="type">
          <a-select class="el-form-input-width" v-model:value="queryParams.type" placeholder="请选择应用类型" allow-clear>
            <a-select-option v-for="dict in auth_app_type" :key="dict.value" :value="dict.value">
              {{ dict.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="是否公开" name="publicFlag">
          <a-select class="el-form-input-width" v-model:value="queryParams.publicFlag" placeholder="请选择是否公开" allow-clear>
            <a-select-option v-for="dict in auth_public" :key="dict.value" :value="dict.value">
              {{ dict.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" v-hasPermi="['svc:client:query']" @click="handleQuery"
            @mousedown="(e) => e.preventDefault()"
>
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </a-button>
          <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item>
      </a-form>
      <div class="data-action-btns">
        <a-button type="primary" @click="handleAdd" v-hasPermi="['svc:client:add']"
          @mousedown="(e) => e.preventDefault()"
>
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </a-button>
      </div>
      <div class="top-right-btn">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
      </div>
    </div>

    <div class="pagecont-bottom">
      <a-table
        striped
        :loading="loading"
        :data-source="clientList"
        :pagination="false"
        :columns="tableColumns"
        :locale="{ emptyText: '暂无记录' }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            {{ record.name || "-" }}
          </template>
          <template v-if="column.dataIndex === 'type'">
            <dict-tag :options="auth_app_type" :value="record.type" />
          </template>
          <template v-if="column.dataIndex === 'description'">
            {{ record.description || "-" }}
          </template>
          <template v-if="column.dataIndex === 'logo'">
            <div class="clientInfo">
              <div>
                <image-preview :src="record.logo || noDataImg" :width="50" :height="50" />
              </div>
            </div>
          </template>
          <template v-if="column.dataIndex === 'publicFlag'">
            <dict-tag :options="auth_public" :value="record.publicFlag" />
          </template>
          <template v-if="column.dataIndex === 'createBy'">
            {{ record.createBy || "-" }}
          </template>
          <template v-if="column.dataIndex === 'createTime'">
            <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
          </template>
          <template v-if="column.dataIndex === 'remark'">
            {{ record.remark || '-' }}
          </template>
          <template v-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="handleUpdate(record)"
              v-hasPermi="['svc:client:edit']"
>修改</a-button>
            <a-button type="link" size="small" @click="handleDetail(record)"
              v-hasPermi="['svc:client:query']"
>详情</a-button>
            <a-popover placement="bottom" :overlay-inner-style="{ width: '150px' }" trigger="click">
              <template #content>
                <div style="width: 100px" class="butgdlist">
                  <a-button type="link" size="small" @click="handleReset(record)"
                    v-hasPermi="['svc:client:edit']"
>重置秘钥</a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record)"
                    v-hasPermi="['svc:client:remove']"
>删除</a-button>
                </div>
              </template>
              <a-button type="link" size="small">更多</a-button>
            </a-popover>
          </template>
        </template>
      </a-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList"
/>
    </div>

    <!-- 新增或修改应用对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px">
      <a-form ref="clientRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" @submit.prevent>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="应用名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入应用名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="应用类型" name="type">
              <a-select v-model:value="form.type" placeholder="请选择应用类型">
                <a-select-option v-for="dict in auth_app_type" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </a-select-option>
              </a-select>
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

        <!-- <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="主页地址" name="homepageUrl">
              <a-input v-model:value="form.homepageUrl" placeholder="请输入主页地址" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="同步地址" name="syncUrl">
              <a-input v-model:value="form.syncUrl" placeholder="请输入同步地址" />
            </a-form-item>
          </a-col>
        </a-row> -->
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="应用图标" name="logo">
              <image-upload v-model="form.logo" limit="1" :fileType="pdf" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="是否公开" name="publicFlag">
              <a-radio-group v-model:value="form.publicFlag">
                <a-radio v-for="dict in auth_public" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
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
    <!-- 应用详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="800px">
      <a-form ref="clientRef" :model="form" :label-col="{ style: { width: '100px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="编号" name="id">
              <div>{{ form.id || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="应用秘钥" name="secret">
              <div>{{ form.secret || "-" }}</div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="应用名称" name="name">
              <div>{{ form.name || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="应用图标" name="logo">
              <image-preview :src="form.logo || noDataImg" :width="50" :height="50" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="应用类型" name="type">
              <dict-tag :options="auth_app_type" :value="form.type" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否公开" name="publicFlag">
              <dict-tag :options="auth_public" :value="form.publicFlag" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="主页地址" name="homepageUrl">
              <div>{{ form.homepageUrl || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="同步地址" name="syncUrl">
              <div>{{ form.syncUrl || "-" }}</div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="授权路径" name="allowUrl">
              <div>{{ form.allowUrl || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <div>{{ form.description || "-" }}</div>
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
        @progress="handleFileUploadProgress" @success="handleFileSuccess"
>
        <CloudUploadOutlined style="font-size: 42px; color: #4096ff" />
        <div class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="ant-upload-tip text-center">
            <div class="ant-upload-tip">
              <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的应用数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <a-link type="primary" style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate"
>下载模板</a-link>
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

<script setup name="Client">
import {
  listClient,
  getClient,
  delClient,
  addClient,
  updateClient,
  resetSecret,
} from "@/api/svc/client/client";
import { getToken } from "@/utils/auth.js";
import { CloudUploadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { auth_public, auth_app_type } = proxy.useDict(
  "auth_public",
  "auth_app_type"
);
const noDataImg = new URL('../../../assets/system/images/D.png', import.meta.url).href
const clientList = ref([]);

// 列显隐信息
const columns = ref([
  { key: 0, label: "编号", visible: true },
  { key: 1, label: "应用名称", visible: true },
  { key: 3, label: "应用类型", visible: true },
  { key: 2, label: "描述", visible: true },
  { key: 16, label: "应用图标", visible: true },
  // { key: 5, label: "允许授权的url", visible: true },
  { key: 4, label: "是否公开", visible: true },
  { key: 12, label: "创建人", visible: true },
  { key: 14, label: "创建时间", visible: true },
  { key: 15, label: "备注", visible: true },
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  if (!column) return true;
  return column.visible;
};

const tableColumns = computed(() => {
  const allCols = [
    { title: '编号', dataIndex: 'id', align: 'center', width: 75, colKey: 0 },
    { title: '应用名称', dataIndex: 'name', align: 'left', width: 200, ellipsis: true, colKey: 1 },
    { title: '应用类型', dataIndex: 'type', align: 'center', width: 100, colKey: 3 },
    { title: '描述', dataIndex: 'description', align: 'left', width: 300, ellipsis: true, colKey: 2 },
    { title: '应用图标', dataIndex: 'logo', align: 'left', width: 80, ellipsis: true, colKey: 16 },
    { title: '是否公开', dataIndex: 'publicFlag', align: 'center', width: 100, colKey: 4 },
    { title: '创建人', dataIndex: 'createBy', align: 'center', colKey: 12 },
    { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 150, key: 'create_time', sorter: true, defaultSortOrder: 'descend', colKey: 14 },
    { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true, colKey: 15 },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 280 },
  ];
  return allCols.filter(col => col.colKey === undefined || getColumnVisibility(col.colKey));
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
  url: import.meta.env.VITE_APP_BASE_API + "/tax/client/importData",
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    id: null,
    name: null,
    type: null,
    secret: null,
    homepageUrl: null,
    allowUrl: null,
    syncUrl: null,
    logo: null,
    description: null,
    publicFlag: null,
    createTime: null,
  },
  rules: {
    name: [{ required: true, message: "应用名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "应用类型不能为空", trigger: "change" }],
  },
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

/** 查询应用列表 */
function getList() {
  loading.value = true;
  listClient(queryParams.value)
    .then((response) => {
      const pageData = normalizePageData(response);
      clientList.value = pageData.rows;
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
    id: null,
    name: null,
    type: null,
    secret: null,
    homepageUrl: null,
    allowUrl: null,
    syncUrl: null,
    logo: null,
    description: null,
    publicFlag: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  proxy.resetForm("clientRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
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
  title.value = "新增应用";

  data.form.publicFlag = "1";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getClient(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改应用";
  });
}

/** 重置秘钥按钮操作 */
function handleReset(row) {
  const _id = row.id || ids.value;

  proxy.$modal
    .confirm("是否确认重置秘钥，秘钥重置后请使用新秘钥访问。")
    .then(function () {
      resetSecret(_id).then((res) => {
        proxy.$modal.msgSuccess("新秘钥为：" + res.data);
        getList();
      });
    });
}

/** 详情按钮操作 */
function handleDetail(row) {
  // reset();
  // const _id = row.id || ids.value;
  // getClient(_id).then((response) => {
  //   form.value = response.data;
  //   openDetail.value = true;
  //   title.value = "应用详情";
  // });
  routeTo("/svc/client/clientDetail", row);
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["clientRef"].validate().then(() => {
    if (form.value.id != null) {
      updateClient(form.value)
        .then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        })
        .catch((error) => { });
    } else {
      addClient(form.value)
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
    .confirm('是否确认删除编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delClient(_ids);
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
    "tax/client/export",
    {
      ...queryParams.value,
    },
    `client_${new Date().getTime()}.xlsx`
  );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "应用导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `client_template_${new Date().getTime()}.xlsx`
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

getList();
</script>

<style scoped lang="scss">
.clientInfo {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

// :deep {
//   .el-popper.is-dark {
//     max-width: 900px !important;
//     max-height: 400px;
//     font-size: 14px;
//     text-align: start;
//   }
// }</style>

