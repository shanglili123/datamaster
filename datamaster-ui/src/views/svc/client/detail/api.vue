<template>
  <div ref="app-container">
    <div class="justify-between mb15">
      <a-row :gutter="15" class="btn-style">
        <a-col :span="1.5">
          <a-button type="primary" @click="handleAdd" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-xinzeng mr5"></i>新增
          </a-button>
        </a-col>
      </a-row>
    </div>
    <a-table stripe :loading="loading" :data-source="clientApiRelList" :columns="tableColumns"
      :default-sort="defaultSort" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'apiName'">
          {{ record.apiName || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'apiUrl'">
          {{ record.apiUrl || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'reqMethod'">
          <dict-tag :options="ds_api_bas_info_api_method_type" :value="record.reqMethod" />
        </template>
        <template v-else-if="column.dataIndex === 'startTime'">
          <span v-if="record.pvFlag == 1">永久</span>
          <div v-else>
            <span>{{ parseTime(record.startTime, "{y}-{m}-{d} ") }}</span>
            <span>- </span>
            <span>{{ parseTime(record.endTime, "{y}-{m}-{d} ") }}</span>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createBy'">
          {{ record.createBy || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{
            parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}")
          }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch v-model:checked="record.status" checked-value="1" un-checked-value="0"
            @change="(e) => handleStatusChange(record.id, record, e)" />
        </template>
        <template v-else-if="column.dataIndex === 'remark'">
          {{ record.remark || '-' }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button type="link" @click="handleUpdate(record)">
            <template #icon><EditOutlined /></template>修改
          </a-button>
          <a-button type="link" danger @click="handleDelete(record)">
            <template #icon><DeleteOutlined /></template>删除
          </a-button>
        </template>
      </template>

      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p>暂无记录</p>
        </div>
      </template>
    </a-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>

  <!-- 添加或修改应用API服务关联对话框 -->
  <a-modal :title="title" v-model:open="open" class="dialog">
    <a-form ref="clientApiRelRef" :model="form" :rules="rules" :label-col="{ style: { width: '110px' } }" @submit.prevent>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="API服务" name="apiName">
            <a-auto-complete :disabled="form.id" v-model:value="form.apiName" :options="apiOptions"
              placeholder="请输入API服务名称" :filter-option="false" @search="remoteMethod"
              @select="(value, option) => handleApiIdSelect(option)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="是否永久有效" name="pvFlag">
            <a-radio-group v-model:value="form.pvFlag" @change="handlePvFlagChange">
              <a-radio v-for="dict in sys_is_or_not" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="12" v-if="form.pvFlag == 0">
          <a-form-item label="有效期" name="dateRange">
            <a-range-picker class="el-form-input-width" v-model:value="form.dateRange" value-format="YYYY-MM-DD"
              :placeholder="['开始日期', '结束日期']" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述">
            <a-textarea placeholder="请输入描述" v-model:value="form.description" :auto-size="{ minRows: 7 }" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea placeholder="请输入备注" v-model:value="form.remark" :auto-size="{ minRows: 7 }" />
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

  <!-- 应用API服务关联详情对话框 -->
  <a-modal :title="title" v-model:open="openDetail" width="800px">
    <a-form ref="clientApiRelRef" :model="form" :label-col="{ style: { width: '80px' } }">
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="应用ID" name="clientId">
            <div>
              {{ form.clientId }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="API服务ID" name="apiId">
            <div>
              {{ form.apiId }}
            </div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="是否永久有效" name="pvFlag">
            <dict-tag :options="sys_is_or_not" :value="form.pvFlag" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="开始时间" name="startTime">
            <a-date-picker allow-clear style="width: 100%" v-model:value="form.startTime" value-format="YYYY-MM-DD"
              placeholder="请选择开始时间" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="结束时间" name="endTime">
            <a-date-picker allow-clear style="width: 100%" v-model:value="form.endTime" value-format="YYYY-MM-DD"
              placeholder="请选择结束时间" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="授权状态" name="status">
            <div>
              {{ form.status }}
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

<script setup name="ClientApiRel">
import { listClientApiRel, getClientApiRel, delClientApiRel, addClientApiRel, updateClientApiRel } from "@/api/svc/client/clientApiRel";
import { selectByName } from "@/api/svc/api/api.js";
import { EditOutlined, DeleteOutlined } from "@ant-design/icons-vue";
import { status } from "nprogress";

const { proxy } = getCurrentInstance();
const { sys_is_or_not, ds_api_bas_info_api_method_type } = proxy.useDict("sys_is_or_not", "ds_api_bas_info_api_method_type");

const tableColumns = [
  { title: '编号', key: 'index', align: 'center', width: 75 },
  { title: 'API编码', dataIndex: 'apiId', align: 'center', ellipsis: true },
  { title: 'API名称', dataIndex: 'apiName', align: 'center', width: 150, ellipsis: true },
  { title: '路径', dataIndex: 'apiUrl', align: 'center', width: 150, ellipsis: true },
  { title: '请求方式', dataIndex: 'reqMethod', align: 'center', ellipsis: true },
  { title: '有效期', dataIndex: 'startTime', align: 'center', width: 260, ellipsis: true },
  { title: '描述', dataIndex: 'description', align: 'left', width: 250, ellipsis: true },
  { title: '创建人', dataIndex: 'createBy', align: 'center', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180, key: 'createTime', sorter: true, ellipsis: true },
  { title: '授权状态', dataIndex: 'status', align: 'center', ellipsis: true },
  { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 140 },
];

const props = defineProps({
  clientDetail: {
    type: Object,
    default: () => { },
  },
});
const clientId = ref(null);
watch(
  () => props.clientDetail,
  (newValue) => {
    clientId.value = newValue.id;
    getList();
  }
);
const clientApiRelList = ref([]);

const open = ref(false);
const openDetail = ref(false);
const loading = ref(false);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const defaultSort = ref({ columnKey: "createTime", order: "descend" });
const router = useRouter();

const data = reactive({
  form: {
    pvFlag: "0",
    dateRange: [],
  },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    clientId: null,
    apiId: null,
    pvFlag: null,
    startTime: null,
    endTime: null,
    status: null,
  },
  rules: {
    apiName: [{ required: true, message: "API服务不能为空", trigger: "change" }],
    pvFlag: [{ required: true, message: "是否永久有效不能为空", trigger: "blur" }],
    dateRange: [{ required: true, message: "有效期不能为空", trigger: "blur" }],
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

/** 查询应用API服务关联列表 */
function getList() {
  queryParams.value.clientId = clientId.value;
  loading.value = true;
  listClientApiRel(queryParams.value)
    .then((response) => {
      const pageData = normalizePageData(response);
      clientApiRelList.value = pageData.rows;
      total.value = pageData.total;
    })
    .finally(() => {
      loading.value = false;
    });
}

/** 改变启用状态值 */
function handleStatusChange(id, row, e) {
  console.log(e);
  const text = row.status == "1" ? "授权" : "取消授权";
  // 弹出确认框
  proxy.$modal
    .confirm('确认要"' + text + '","' + row.apiName + '"吗？')
    .then(function () {
      loading.value = true; // 开始加载
      // 调用后台接口更新发布状态
      updateClientApiRel({ ...row })
        .then((res) => {
          if (res.code == 200) {
            proxy.$modal.msgSuccess("操作成功");
          }
        })
        .catch((error) => {
          console.log(error);
          // 失败时恢复状态
          row.status = row.status === "1" ? "0" : "1";
        })
        .finally(() => {
          loading.value = false; // 无论成功失败都停止加载
        });
    })
    .catch((error) => {
      // 失败时恢复状态
      row.status = row.status === "1" ? "0" : "1";
    });
}

const handlePvFlagChange = (e) => {
  if (e == "1") {
    form.value.dateRange = [];
  }
};
const apiIdloading = ref(false);
const apiOptions = ref([]);
const handleApiIdSelect = (option) => {
  if (!option) return;
  form.value.apiId = option.id;
  form.value.reqMethod = option.reqMethod;
  form.value.apiUrl = option.apiUrl;
};
const remoteMethod = (queryString) => {
  apiIdloading.value = true;
  selectByName(queryString || "")
    .then((res) => {
      if (res.code === 200 && Array.isArray(res.data)) {
        apiOptions.value = res.data.map((item) => ({
          ...item,
          value: item.name,
        }));
      } else {
        apiOptions.value = [];
      }
    })
    .catch(() => {
      apiOptions.value = [];
    })
    .finally(() => {
      apiIdloading.value = false;
    });
};

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
    clientId: null,
    apiId: null,
    pvFlag: "0",
    dateRange: [],
    startTime: null,
    endTime: null,
    status: null,
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
  proxy.resetForm("clientApiRelRef");
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
  title.value = "添加API服务授权";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  form.value = JSON.parse(JSON.stringify(row));
  form.value.dateRange = [form.value.startTime, form.value.endTime];
  open.value = true;
  title.value = "修改API服务授权";
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id || ids.value;
  getClientApiRel(_id).then((response) => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "API服务授权详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["clientApiRelRef"].validate().then(() => {
    form.value.clientId = clientId.value;
    form.value.startTime = form.value.dateRange[0];
    form.value.endTime = form.value.dateRange[1];
    if (form.value.id != null) {
      updateClientApiRel(form.value)
        .then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        })
        .catch((error) => { });
    } else {
      addClientApiRel(form.value)
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
    .confirm('是否确认删除API服务关联编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delClientApiRel(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => { });
}

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
</script>
<style lang="scss" scoped>
/* 操作按钮统一靠右，与其他列表页保持一致 */
.justify-between {
  justify-content: flex-end;
}
</style>

