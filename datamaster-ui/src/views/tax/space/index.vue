<template>
  <div class="app-container" ref="app-container">

    <div class="pagecont-top" v-show="showSearch">
      <a-form
        class="btn-style"
        :model="queryParams"
        ref="queryRef"
        layout="inline"
        :label-col="{ style: { width: '45px' } }"
        v-show="showSearch"
        @submit.prevent
      >
        <a-form-item label="名称" name="name">
          <a-input
            style="width: 150px;"
            v-model:value="queryParams.name"
            placeholder="请输入空间名称"
            allow-clear
            @pressEnter="handleQuery"
          />
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            @click="handleQuery"
            @mousedown="(e) => e.preventDefault()"
            v-hasPermi="['tax:space:query']"
          >
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </a-button>
          <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item>
      </a-form>
      <div class="data-action-btns">
        <a-button
          type="primary"
          @click="handleAdd"
          v-hasPermi="['tax:space:add']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </a-button>
      </div>
      <div class="top-right-btn">
        <right-toolbar
          v-model:showSearch="showSearch"
          @queryTable="getList"
          :columns="columns"
        ></right-toolbar>
      </div>
    </div>

    <div>
      <a-table
        striped
        row-key="id"
        :loading="loading"
        :data-source="spaceList"
        :columns="tableColumns"
        :scroll="tableScroll"
        :pagination="false"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'id'">
            {{ record.id || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            {{ record.name || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'description'">
            {{ record.description || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'createBy'">
            {{ record.createBy || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <span>{{
              parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-"
            }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'validFlag'">
            <a-switch
              v-model:checked="record.validFlag"
              @change="() => handleStatusChange(record)"
            />
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            {{ record.remark || "-" }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button
              type="link"
              size="small"
              @click="handleUpdate(record)"
              v-hasPermi="['tax:space:edit']"
              >修改</a-button>
            <a-button
              type="link"
              danger
              size="small"
              @click="handleDelete(record)"
              v-hasPermi="['tax:space:remove']"
              >删除</a-button>
            <a-button
              type="link"
              size="small"
              v-hasPermi="['tax:space:query']"
              @click="handleDetail(record)"
              >详情</a-button>
          </template>
        </template>
        <template #empty>
          <div class="emptyBg">
            <img src="@/assets/system/images/no_data/noData.png" alt="" />
            <p>暂无记录</p>
          </div>
        </template>
      </a-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 新增或修改空间对话框 -->
    <a-modal
      :title="title"
      v-model:open="open"
      width="800px"
      draggable
      destroy-on-close
    >
      <a-form
        ref="spaceRef"
        :model="form"
        :rules="rules"
        :label-col="{ style: { width: '80px' } }"
        @submit.prevent
      >
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="空间名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入空间名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <a-textarea
                v-model:value="form.description"
                placeholder="请输入描述"
                :maxlength="500"
                show-count
                :auto-size="{ minRows: 6, maxRows: 10 }"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
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
              <a-textarea
                v-model:value="form.remark"
                placeholder="请输入备注"
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

    <!-- 空间详情对话框 -->
    <a-modal
      :title="title"
      v-model:open="openDetail"
      width="1000px"
      draggable
      destroy-on-close
    >
      <a-form
        ref="assetApplyRef"
        :model="form"
        :label-col="{ style: { width: '90px' } }"
      >
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
            <a-form-item label="空间名称:" name="name">
              <div class="form-readonly">
                {{ form.name }}
              </div>
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
  </div>
</template>

<script setup name="Space">
import {
  listSpace,
  getSpace,
  delSpace,
  addSpace,
  updateSpace,
  editSpaceStatus,
} from "@/api/tax/space/space.js";
import { getToken } from "@/utils/auth.js";
import { normalizePage, pageRows } from "@/utils/page.js";

const { proxy } = getCurrentInstance();
const { dp_model_status } = proxy.useDict("dp_model_status");

const spaceList = ref([]);

// 列显隐信息
const columns = ref([
  { key: 1, label: "编号", visible: true },
  { key: 2, label: "空间名称", visible: true },
  { key: 3, label: "空间描述", visible: true },
  { key: 6, label: "创建时间", visible: true },
  { key: 7, label: "创建人", visible: true },
  { key: 8, label: "状态", visible: true },
  { key: 9, label: "备注", visible: true },
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
    { title: "编号", dataIndex: "id", align: "center", width: 80, ellipsis: true, colKey: 1 },
    { title: "空间名称", dataIndex: "name", align: "left", width: 200, ellipsis: true, colKey: 2 },
    { title: "描述", dataIndex: "description", align: "left", width: 300, ellipsis: true, colKey: 3 },
    { title: "创建人", dataIndex: "createBy", align: "left", ellipsis: true, colKey: 7 },
    { title: "创建时间", dataIndex: "createTime", align: "center", width: 150, ellipsis: true, colKey: 6 },
    { title: "状态", dataIndex: "validFlag", align: "center", colKey: 8 },
    { title: "备注", dataIndex: "remark", align: "left", width: 200, ellipsis: true, colKey: 9 },
    { title: "操作", key: "actions", align: "center", fixed: "right", width: 240 },
  ];
  return allCols.filter((col) => !col.colKey || getColumnVisibility(col.colKey));
});
// 列总宽超出容器时启用横向滚动，保证 fixed 列与内容完整展示
const tableScroll = computed(() => {
  const totalWidth = tableColumns.value.reduce(
    (sum, c) => sum + (typeof c.width === 'number' ? c.width : 0),
    0
  );
  return totalWidth > 0 ? { x: totalWidth } : undefined;
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
  url: import.meta.env.VITE_APP_BASE_API + "/tax/space/importData",
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    name: null,
  },
  rules: {
    name: [{ required: true, message: "空间名称不能为空", trigger: "blur" }],
    // managerId: [{ required: true, message: "创建人不能为空", trigger: "blur" }],
    // validFlag: [{ required: true, message: '是否有效不能为空', trigger: 'change' }]
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询空间列表 */
function getList() {
  loading.value = true;
  listSpace(queryParams.value).then((response) => {
    const page = normalizePage(response);
    total.value = page.total;
    spaceList.value = pageRows(page.rows, page.total, queryParams.value);
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  openDetail.value = false;
  reset();
}
/** 改变启用状态值 */
function handleStatusChange(row) {
  const text = row.validFlag === true ? "启用" : "禁用";
  const status = row.validFlag === true ? 1 : 0;
  proxy.$modal
    .confirm('确认要"' + text + '","' + row.name + '"空间吗？')
    .then(function () {
      editSpaceStatus(row.id, status).then((response) => {
        proxy.$modal.msgSuccess(text + "成功");
        getList();
      });
    })
    .catch(function () {
      row.validFlag = !row.validFlag;
    });
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    description: null,
    validFlag: true,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  proxy.resetForm("spaceRef");
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
  const orderMap = { ascend: "asc", descend: "desc" };
  queryParams.value.orderByColumn = field;
  queryParams.value.isAsc = sorter.order ? orderMap[sorter.order] : null;
  getList();
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增空间";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  console.log(_id, "22");
  getSpace(_id).then((response) => {
    delete response.data.createTime;
    delete response.data.updateTime;
    form.value = response.data;
    open.value = true;
    title.value = "修改空间";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id || ids.value;
  getSpace(_id).then((response) => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "空间详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["spaceRef"]
    .validate()
    .then(() => {
      if (form.value.id != null) {
        updateSpace(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess("修改成功");

            open.value = false;
            getList();
          })
          .catch((error) => {});
      } else {
        addSpace(form.value)
          .then((response) => {
            if (response.code === 200) {
              proxy.$modal.msgSuccess("新增成功");
              open.value = false;
              getList();
            }
          })
          .catch((error) => {});
      }
    })
    .catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除空间编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delSpace(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "tax/space/export",
    {
      ...queryParams.value,
    },
    `space_${new Date().getTime()}.xlsx`
  );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "空间导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `space_template_${new Date().getTime()}.xlsx`
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

getList();
// getUserTree();
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
