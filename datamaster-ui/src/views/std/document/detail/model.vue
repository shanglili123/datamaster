<template>
  <div class="justify-between mb15">
    <a-row :gutter="15" class="btn-style">
      <a-col :span="1.5">
        <a-button
          type="primary"
          @click="handleAdd"
          v-hasPermi="['dp:model:add']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </a-button>
      </a-col>
      <!-- <a-col :span="1.5">
                        <a-button type="primary" :disabled="single" @click="handleMaterialization"
                            v-hasPermi="['dp:model:edit']" @mousedown="(e) => e.preventDefault()">
                            <i class="iconfont-mini icon-xiugai--copy mr5"></i>物化
                        </a-button>
                    </a-col>
                    <a-col :span="1.5">
                        <a-button type="primary" danger :disabled="multiple" @click="handleDelete"
                            v-hasPermi="['dp:model:remove']" @mousedown="(e) => e.preventDefault()">
                            <i class="iconfont-mini icon-shanchu-huise mr5"></i>删除
                        </a-button>
                    </a-col> -->
      <!-- <a-col :span="1.5">
                <a-button @click="handleImport" v-hasPermi="['dp:model:export']"
                  @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-upload-cloud-line mr5"></i>导入
                </a-button>
              </a-col>
              <a-col :span="1.5">
                <a-button type="primary" @click="handleExport" v-hasPermi="['dp:model:export']"
                  @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-download-line mr5"></i>导出
                </a-button>
              </a-col> -->
    </a-row>
    <div class="justify-end top-right-btn">
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
        :columns="columns"
      ></right-toolbar>
    </div>
  </div>
  <a-spin :spinning="loading">
    <a-table
      :data-source="dpModelList"
      :columns="tableColumns"
      :pagination="false"
      striped
      :scroll="{ y: '60vh' }"
      :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
      row-key="id"
      :locale="{ emptyText: emptyContent }"
      @change="handleSortChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'modelName'">
          {{ record.modelName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'modelComment'">
          {{ record.modelComment || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'catName'">
          {{ record.catName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createBy'">
          {{ record.createBy || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{
            parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-"
          }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            v-model:checked="record.status"
            checked-value="1"
            un-checked-value="0"
            @change="(e) => handleStatusChange(record.id, record, e)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'remark'">
          {{ record.remark || '-' }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button
            type="link"
            size="small"
            @click="handleUpdate(record)"
            :disabled="record.status == 1"
            v-hasPermi="['dp:model:edit']"
            >修改</a-button>
          <a-button
            type="link"
            danger
            size="small"
            :disabled="record.status == 1"
            @click="handleDelete(record)"
            v-hasPermi="['dp:model:remove']"
            >删除</a-button>
          <a-button
            type="link"
            size="small"
            @click="handleDetail(record)"
            v-hasPermi="['dp:model:edit']"
            >详情</a-button>
        </template>
        <template v-else>
          <span>{{ record[column.dataIndex] || '-' }}</span>
        </template>
      </template>
    </a-table>
  </a-spin>

  <pagination
    v-show="total > 0"
    :total="total"
    v-model:page="queryParams.pageNum"
    v-model:limit="queryParams.pageSize"
    @pagination="getList"
  />

  <!-- 逻辑模型详情对话框 -->
  <a-modal
    :title="title"
    v-model:open="openDetail"
    width="800px"
    draggable
  >
    <a-form ref="dpModelRef" :model="form" :label-col="{ style: { width: '80px' } }">
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="英文名称" name="modelName">
            <div>
              {{ form.modelName }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="中文名称" name="modelComment">
            <div>
              {{ form.modelComment }}
            </div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="目录编码" name="catCode">
            <div>
              {{ form.catCode }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <dict-tag :options="dp_model_status" :value="form.status" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="创建方式" name="createType">
            <dict-tag
              :options="dp_model_create_type"
              :value="form.createType"
            />
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
    </a-form>
    <template #footer>
      <div class="dialog-footer">
        <a-button size="small" @click="cancel">关 闭</a-button>
      </div>
    </template>
  </a-modal>
  <my-form-dialog
    v-model:visible="open"
    :title="title"
    @submit="handleFormSubmit"
    :deptList="deptList"
    :column_type="column_type"
    :userList="userList"
    @confirm="submitForm"
    :dataList="dataList"
    :catCode="queryParams.catCode"
    :deptOptions="deptOptions"
    :documentId="form.documentId"
    ,
    type="3"
  />
  <MaterializationDialog
    :title="title"
    :visible="Materialization"
    @update:dialogFormVisible="Materialization = $event"
    :ids="ids"
    @confirm="getList"
    :documentId="form.documentId"
  />
</template>
<script setup name="StandardsModel">
import { deptUserTree } from "@/api/system/system/user.js";
import { deptTreeSelectNoPermi } from "@/api/system/system/user.js";
import DeptTree from "@/components/DeptTree";
import MyFormDialog from "@/views/std/model/components/add.vue";
import MaterializationDialog from "@/views/std/model/detail/materialization.vue";
import {
  listDpModel,
  getDpModel,
  delDpModel,
  delDpModelColumn,
  addDpModel,
  updateDpModelColumn,
  updateDpModel,
  listAttModelCat,
  dpModelColumn,
  updateStatusDpDataModel,
} from "@/api/std/model/model";
import { getToken } from "@/utils/auth.js";
import { ref, reactive, getCurrentInstance, h } from "vue";
import { useRoute } from "vue-router";
const route = useRoute();
const { proxy } = getCurrentInstance();
const { dp_model_status, dp_model_create_type } = proxy.useDict(
  "dp_model_status",
  "dp_model_create_type"
);
const dpModelList = ref([]);
const deptList = ref([]);
const userList = ref([]);
const deptOptions = ref(undefined);

const leftWidth = ref(240); // 初始左侧宽度
const isResizing = ref(false); // 判断是否正在拖拽
let startX = 0; // 鼠标按下时的初始位置// 初始左侧宽度
let Materialization = ref(false);
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
    requestAnimationFrame(() => {});
  }
};
const selectable = (row) => {
  return row.status != 0;
};

/** 查询部门下拉树结构 */
function getDeptTree() {
  listAttModelCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "逻辑模型目录",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
  // 部门
  deptTreeSelectNoPermi().then((response) => {
    deptList.value = response.data;
  });
  deptUserTree().then((res) => {
    userList.value = res.data;
    console.log("userList", userList.value);
  });
}
// 列显隐信息
const columns = ref([
  { key: 0, label: "编号", visible: true },
  { key: 1, label: "英文名称", visible: true },
  { key: 2, label: "中文名称", visible: true },
  { key: 3, label: "逻辑模型目录", visible: true },
  { key: 10, label: "创建人", visible: true },
  { key: 11, label: "创建时间", visible: true },
  { key: 4, label: "状态", visible: true },
  { key: 5, label: "备注", visible: true },
]);

const tableColumns = [
  { title: "编号", dataIndex: "id", align: "left", width: 50 },
  { title: "英文名称", dataIndex: "modelName", align: "left", width: 200, ellipsis: true },
  { title: "中文名称", dataIndex: "modelComment", align: "left", width: 180, ellipsis: true },
  { title: "逻辑模型目录", dataIndex: "catName", align: "left", width: 100, ellipsis: true },
  { title: "创建人", dataIndex: "createBy", align: "left", width: 120, ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", align: "left", width: 180 },
  { title: "状态", dataIndex: "status", align: "left", width: 120 },
  { title: "备注", dataIndex: "remark", align: "left", ellipsis: true },
  { title: "操作", key: "actions", align: "center", fixed: "right", width: 240 },
];

const emptyContent = h("div", { class: "emptyBg" }, [
  h("img", { src: new URL("@/assets/system/images/no_data/noData.png", import.meta.url).href, alt: "" }),
  h("p", "暂无记录"),
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
  url: import.meta.env.VITE_APP_BASE_API + "/std/model/importData",
});

/** 启用禁用开关 */
function handleStatusChange(id, row, e) {
  const text = e === "1" ? "启用" : "禁用";
  proxy.$modal
    .confirm('确认要"' + text + '","' + row.modelComment + '"逻辑模型吗？')
    .then(function () {
      updateStatusDpDataModel(id, row.status).then((response) => {
        proxy.$modal.msgSuccess("操作成功");
      });
    })
    .catch(function () {
      row.status = row.status === "1" ? "0" : "1";
    });
}

const data = reactive({
  form: { status: "1" },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    modelName: null,
    modelComment: null,
    catCode: null,
    documentId: null,
  },
  rules: {
    modelName: [
      { required: true, message: "模型编码不能为空", trigger: "blur" },
    ],
    modelComment: [
      { required: true, message: "模型名称不能为空", trigger: "blur" },
    ],
    catCode: [{ required: true, message: "目录编码不能为空", trigger: "blur" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    createType: [
      { required: true, message: "创建方式不能为空", trigger: "change" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);
function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  queryParams.value.pageNum = 1;
  handleQuery();
}
/** 查询逻辑模型列表 */
function getList() {
  if (!queryParams.value.documentId) {
    queryParams.value.documentId = route.query.id;
  }
  loading.value = true;
  listDpModel(queryParams.value).then((response) => {
    dpModelList.value = response.data.rows;
    total.value = response.data.total;
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
    modelName: null,
    modelComment: null,
    catCode: null,
    status: null,
    createType: null,
    datasourceId: null,
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
  proxy.resetForm("dpModelRef");
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
function handleSelectionChange(selectedRowKeys, selectedRows) {
  console.log("selection", selectedRows);
  ids.value = selectedRows.map((item) => item.id);
  console.log("selection.length ", selectedRows.length);
  single.value = selectedRows.length == 0 ? true : false;
  multiple.value = !selectedRows.length;
}

/** 排序触发事件 */
function handleSortChange(pag, filters, sorter) {
  const prop = sorter.field || sorter.column?.dataIndex;
  const order =
    sorter.order === "ascend"
      ? "ascending"
      : sorter.order === "descend"
        ? "descending"
        : null;
  queryParams.value.orderByColumn = prop;
  queryParams.value.isAsc = order;
  getList();
}

/** 新增按钮操作 */
function handleAdd() {
  dataList.value = {};
  reset();
  open.value = true;
  title.value = "新增逻辑模型";
}
let dataList = ref({});
/** 修改按钮操作 */
function handleUpdate(row) {
  console.log("row", row);
  reset();
  const _ID = row.id || ids.value;
  getDpModel(_ID).then((response) => {
    dataList.value = response.data;
    open.value = true;
    title.value = "修改逻辑模型";
  });
}

/** 物化按钮操作 */
function handleMaterialization() {
  const _ID = ids.value;
  // getDpModel(_ID).then(response => {
  //   form.value = response.data;

  // });
  Materialization.value = true;
  title.value = "逻辑物化";
}
/** 详情按钮操作 */
function handleDetail(row) {
  routeTo("/mdl/model/detail", row);
}

/** 提交按钮 */
function submitForm(obj) {
  console.log("obj", obj);
  if (obj.form.id != null) {
    updateDpModel({ ...obj.form, documentId: queryParams.value.documentId })
      .then((response) => {
        updateDpModelColumn(obj.tableData).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      })
      .catch((error) => {});
  } else {
    addDpModel({ ...obj.form, documentId: queryParams.value.documentId })
      .then((response) => {
        const id = response.data;
        const updatedTableData = obj.tableData.map((item) => ({
          ...item,
          modelId: id,
        }));
        dpModelColumn(updatedTableData)
          .then((dpModelColumnResponse) => {
            proxy.$modal.msgSuccess("新增成功");
            open.value = false;
            getList();
          })
          .catch((dpModelColumnError) => {});
      })
      .catch((error) => {
        console.error("新增失败:", error);
      });
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _IDs = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除逻辑模型编号为"' + _IDs + '"的数据项？')
    .then(function () {
      return delDpModelColumn(_IDs);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
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
queryParams.value.documentId = route.query.id;
getList();
getDeptTree();
</script>
<style scoped lang="scss">
::v-deep {
  .selectlist .el-tag.el-tag--info {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}
</style>

