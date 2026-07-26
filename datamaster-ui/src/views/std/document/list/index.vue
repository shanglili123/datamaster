<template>
  <div class="app-container" ref="app-container">
    <el-container style="90%">
      <DeptTree
        :deptOptions="deptOptions"
        :leftWidth="leftWidth"
        :placeholder="'请输入标准类目'"
        @node-click="handleNodeClick"
      />
      <el-main>
        <div class="pagecont-top" v-show="showSearch">
          <el-form
            class="btn-style"
            :model="queryParams"
            ref="queryRef"
            :inline="true"
            label-width="75px"
            v-show="showSearch"
            @submit.prevent
          >
            <el-form-item label="标准号" prop="code">
              <el-input
                class="el-form-input-width"
                v-model="queryParams.code"
                placeholder="请输入标准号"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="标准名称" prop="name">
              <el-input
                class="el-form-input-width"
                v-model="queryParams.name"
                placeholder="请输入标准名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="标准级别" prop="stdLevel">
              <el-select
                class="el-form-input-width"
                v-model="queryParams.stdLevel"
                placeholder="请选择标准级别"
                clearable
              >
                <el-option label="国家标准" value="国家标准" />
                <el-option label="行业标准" value="行业标准" />
                <el-option label="地方标准" value="地方标准" />
                <el-option label="团体标准" value="团体标准" />
              </el-select>
            </el-form-item>
            <el-form-item label="标准状态" prop="status">
              <el-select
                class="el-form-input-width"
                v-model="queryParams.status"
                placeholder="请选择标准状态"
              >
                <el-option
                  v-for="dict in dp_document_status"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                plain
                type="primary"
                @click="handleQuery"
                @mousedown="(e) => e.preventDefault()"
              >
                <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
              </el-button>
              <el-button
                @click="resetQuery"
                @mousedown="(e) => e.preventDefault()"
              >
                <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
              </el-button>
            </el-form-item>
          </el-form>
          <div class="data-action-btns">
            <el-button
              type="primary"
              plain
              @click="handleAdd"
              @mousedown="(e) => e.preventDefault()"
            >
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </el-button>
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
          <el-table
            stripe
            v-loading="loading"
            :data="dpDataElemList"
            @selection-change="handleSelectionChange"
            :default-sort="defaultSort"
            @sort-change="handleSortChange"
          >
            <el-table-column
              v-if="getColumnVisibility(0)"
              label="编号"
              align="left"
              prop="id"
              width="60"
              sortable
            />
            <el-table-column
              v-if="getColumnVisibility(1)"
              label="标准号"
              :show-overflow-tooltip="{ effect: 'light' }"
              align="left"
              prop="code"
            >
              <template #default="scope">
                {{ scope.row.code || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(2)"
              label="标准名称"
              :show-overflow-tooltip="{ effect: 'light' }"
              align="left"
              prop="name"
            >
              <template #default="scope">
                {{ scope.row.name || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(8)"
              label="标准级别"
              align="left"
              prop="stdLevel"
              width="100"
            >
              <template #default="scope">
                <el-tag
                  :type="getLevelTagType(scope.row.stdLevel)"
                  size="small"
                >
                  {{ scope.row.stdLevel || "-" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(7)"
              width="240"
              label="描述"
              align="left"
              prop="description"
              :show-overflow-tooltip="{ effect: 'light' }"
            >
              <template #default="scope">
                {{ scope.row.description || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(4)"
              label="标准类目"
              :show-overflow-tooltip="{ effect: 'light' }"
              align="left"
              prop="catCode"
            >
              <template #default="scope">
                {{ scope.row.catName || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(10)"
              label="创建人"
              :show-overflow-tooltip="{ effect: 'light' }"
              align="left"
              prop="createBy"
            >
              <template #default="scope">
                {{ scope.row.createBy || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(11)"
              label="创建时间"
              align="left"
              prop="createTime"
              width="150"
              sortable
            >
              <template #default="scope">
                <span>{{
                  parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}") || "-"
                }}</span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="getColumnVisibility(3)"
              label="标准状态"
              align="left"
              prop="status"
            >
              <template #default="scope">
                <dict-tag
                  :options="dp_document_status"
                  :value="scope.row.status"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="备注"
              align="left"
              prop="remark"
              :show-overflow-tooltip="{ effect: 'light' }"
              v-if="getColumnVisibility(15)"
            >
              <template #default="scope">
                {{ scope.row.remark || "-" }}
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              align="center"
              class-name="small-padding fixed-width"
              fixed="right"
              width="200"
            >
              <template #default="scope">
                <el-button
                  link
                  type="primary"
                  icon="Edit"
                  @click="handleUpdate(scope.row)"
                  >修改
                </el-button>
                <el-button
                  link
                  type="primary"
                  icon="view"
                  @click="handleDetail(scope.row)"
                  >详情
                </el-button>
                <el-popover placement="bottom" :width="150" trigger="click">
                  <template #reference>
                    <el-button link type="primary" icon="ArrowDown"
                      >更多</el-button
                    >
                  </template>
                  <div style="width: 100px" class="butgdlist">
                    <el-button
                      link
                      style="padding-left: 14px"
                      type="primary"
                      icon="View"
                      @click="handleFilePreview(scope.row.fileUrl)"
                      :disabled="!scope.row.fileUrl"
                      >预览</el-button
                    >
                    <el-button
                      link
                      type="primary"
                      icon="Download"
                      :disabled="!scope.row.fileUrl"
                      @click="handleDownload(scope.row)"
                      >下载</el-button
                    >
                    <el-button
                      link
                      type="danger"
                      icon="Delete"
                      @click="handleDelete(scope.row)"
                      >删除
                    </el-button>
                  </div>
                </el-popover>
              </template>
            </el-table-column>

            <template #empty>
              <div class="emptyBg">
                <img src="@/assets/system/images/no_data/noData.png" alt="" />
                <p>暂无记录</p>
              </div>
            </template>
          </el-table>

          <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </div>
      </el-main>
    </el-container>
    <!-- 标准弹窗 -->
    <StandardModal ref="standardModalRef" @update-success="handleQuery" />
  </div>
</template>

<script setup name="DocumentList">
import DeptTree from "@/components/DeptTree";
import {
  listDpDocument,
  getDpDocument,
  delDpDocument,
  addDpDocument,
  listAttDocumentCat,
} from "@/api/std/document/document";
import StandardModal from "../components/add";
import handleFilePreview from "@/utils/filePreview.js";
import { deptUserTree } from "@/api/system/system/user.js";

import { getToken } from "@/utils/auth.js";
const { proxy } = getCurrentInstance();
const { column_type, sys_disable, dp_document_status } = proxy.useDict(
  "column_type",
  "sys_disable",
  "dp_document_status"
);
const deptOptions = ref(undefined);
const leftWidth = ref(300);

const dpDataElemList = ref([]);

const columns = ref([
  { key: 0, label: "编号", visible: true },
  { key: 1, label: "标准号", visible: true },
  { key: 2, label: "标准名称", visible: true },
  { key: 8, label: "标准级别", visible: true },
  { key: 7, label: "描述", visible: true },
  { key: 4, label: "标准类目", visible: true },
  { key: 10, label: "创建人", visible: true },
  { key: 11, label: "创建时间", visible: true },
  { key: 3, label: "标准状态", visible: true },
  { key: 15, label: "备注", visible: true },
]);

function getLevelTagType(level) {
  const map = {
    "国家标准": "danger",
    "行业标准": "warning",
    "地方标准": "",
    "团体标准": "info",
  };
  return map[level] || "";
}

function handleDownload(row) {
  const baseUrl = import.meta.env.VITE_APP_BASE_API;
  const fullUrl = `${baseUrl}${row.fileUrl.trim()}`;
  const a = document.createElement("a");
  a.href = fullUrl;
  a.download = row.fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  if (!column) return true;
  return column.visible;
};

const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const defaultSort = ref({ prop: "create_time", order: "descending" });
const router = useRouter();

const data = reactive({
  form: { status: "0" },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    code: null,
    name: null,
    catCode: null,
    stdLevel: null,
  },
  rules: {},
});

const { queryParams, form, rules } = toRefs(data);
const managerOptions = ref([]);

function getList() {
  loading.value = true;
  listDpDocument(queryParams.value).then((response) => {
    dpDataElemList.value = response.data.rows;
    total.value = response.data.total;
    loading.value = false;
  });
}

function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  handleQuery();
}

function reset() {
  form.value = {
    ID: null,
    code: null,
    name: null,
    catCode: null,
    type: null,
    status: "0",
    issuingAgency: null,
    version: null,
    releaseDate: null,
    implementationDate: null,
    abolitionDate: null,
    standardUrl: null,
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
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.value.catCode = "";
  queryParams.value.stdLevel = null;
  queryParams.value.pageNum = 1;
  queryParams.value.orderByColumn = defaultSort.value.prop;
  queryParams.value.isAsc = defaultSort.value.order;
  reset();
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn =
    column.prop == "createTime" ? "create_time" : column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}

function getDeptTree() {
  listAttDocumentCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "标准类目",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
}

const standardModalRef = ref(null);

function handleAdd() {
  standardModalRef.value.openModal({}, deptOptions.value, null);
}

function handleUpdate(row) {
  standardModalRef.value.openModal(row, deptOptions.value, row.type);
}

function handleDetail(row) {
  router.push({
    path: "/mdl/document/detail",
    query: { id: row.id },
  });
}

function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除标准编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delDpDocument(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

queryParams.value.orderByColumn = defaultSort.value.prop;
queryParams.value.isAsc = defaultSort.value.order;
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

.el-container {
  height: auto !important;
  align-items: flex-start;
}

.el-main {
  padding: 2px 0px;
  overflow: visible !important;
}

.pagecont-top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;

  .data-action-btns {
    margin-left: 10px;
  }

  .top-right-btn {
    margin-left: auto;
  }
}

::v-deep {
  .el-upload-list__item {
    width: 100%;
    height: 25px;
  }
}
</style>
