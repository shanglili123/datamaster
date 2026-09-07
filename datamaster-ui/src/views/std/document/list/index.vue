<template>
  <div class="app-container" ref="app-container">
    <a-layout style="90%">
      <DeptTree
        :deptOptions="deptOptions"
        :leftWidth="leftWidth"
        :placeholder="'请输入标准目录'"
        @node-click="handleNodeClick"
      />
      <a-layout-content>
        <div class="pagecont-top" v-show="showSearch">
          <a-form
            class="btn-style"
            :model="queryParams"
            ref="queryRef"
            layout="inline"
            :label-col="{ style: { width: '75px' } }"
            v-show="showSearch"
            @submit.prevent
          >
            <a-form-item label="标准号" name="code">
              <a-input
                class="el-form-input-width"
                v-model:value="queryParams.code"
                placeholder="请输入标准号"
                allow-clear
                @pressEnter="handleQuery"
              />
            </a-form-item>
            <a-form-item label="标准名称" name="name">
              <a-input
                class="el-form-input-width"
                v-model:value="queryParams.name"
                placeholder="请输入标准名称"
                allow-clear
                @pressEnter="handleQuery"
              />
            </a-form-item>
            <a-form-item label="标准级别" name="stdLevel">
              <a-select
                class="el-form-input-width"
                v-model:value="queryParams.stdLevel"
                placeholder="请选择标准级别"
                allow-clear
              >
                <a-select-option label="国家标准" value="国家标准" />
                <a-select-option label="行业标准" value="行业标准" />
                <a-select-option label="地方标准" value="地方标准" />
                <a-select-option label="团体标准" value="团体标准" />
              </a-select>
            </a-form-item>
            <a-form-item label="标准状态" name="status">
              <a-select
                class="el-form-input-width"
                v-model:value="queryParams.status"
                placeholder="请选择标准状态"
              >
                <a-select-option
                  v-for="dict in dp_document_status"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                >{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <a-button
                type="primary"
                @click="handleQuery"
                @mousedown="(e) => e.preventDefault()"
              >
                <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
              </a-button>
              <a-button
                @click="resetQuery"
                @mousedown="(e) => e.preventDefault()"
              >
                <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
              </a-button>
            </a-form-item>
          </a-form>
          <div class="data-action-btns">
            <a-button
              type="primary"
              @click="handleAdd"
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
        <div style="flex: 1; overflow-y: auto; min-height: 0;">
          <a-spin :spinning="loading">
            <a-table
              :data-source="dpDataElemList"
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
                <template v-if="column.dataIndex === 'code'">
                  {{ record.code || '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'name'">
                  {{ record.name || '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'stdLevel'">
                  <a-tag :type="getLevelTagType(record.stdLevel)" size="small">
                    {{ record.stdLevel || '-' }}
                  </a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'description'">
                  {{ record.description || '-' }}
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
                  <dict-tag
                    :options="dp_document_status"
                    :value="record.status"
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
                    >修改</a-button
                  >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleDetail(record)"
                    >详情</a-button
                  >
                  <a-popover trigger="click" placement="bottom">
                    <template #content>
                      <div style="width: 100px" class="butgdlist">
                        <a-button
                          type="link"
                          size="small"
                          @click="handleFilePreview(record.fileUrl)"
                          :disabled="!record.fileUrl"
                          >预览</a-button
                        >
                        <a-button
                          type="link"
                          size="small"
                          @click="handleDownload(record)"
                          :disabled="!record.fileUrl"
                          >下载</a-button
                        >
                        <a-button
                          type="link"
                          danger
                          size="small"
                          @click="handleDelete(record)"
                          >删除</a-button
                        >
                      </div>
                    </template>
                    <a-button type="link" size="small">更多</a-button>
                  </a-popover>
                </template>
                <template v-else>
                  <span>{{ record[column.dataIndex] || '-' }}</span>
                </template>
              </template>
            </a-table>
          </a-spin>
        </div>

          <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
      </a-layout-content>
    </a-layout>
    <!-- 标准弹窗 -->
    <StandardModal ref="standardModalRef" @update-success="handleQuery" />
  </div>
</template>

<script setup name="DocumentList">
import { h } from "vue";
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

import { getToken } from "@/utils/auth.js";
const { proxy } = getCurrentInstance();
const { column_type, sys_disable, dp_document_status } = proxy.useDict(
  "column_type",
  "sys_disable",
  "dp_document_status"
);
const deptOptions = ref(undefined);
const leftWidth = ref(240);

const dpDataElemList = ref([]);

const columns = ref([
  { key: 0, label: "编号", visible: true },
  { key: 1, label: "标准号", visible: true },
  { key: 2, label: "标准名称", visible: true },
  { key: 8, label: "标准级别", visible: true },
  { key: 7, label: "描述", visible: true },
  { key: 4, label: "标准目录", visible: true },
  { key: 10, label: "创建人", visible: true },
  { key: 11, label: "创建时间", visible: true },
  { key: 3, label: "标准状态", visible: true },
  { key: 15, label: "备注", visible: true },
]);

const tableColumns = [
  { title: "编号", dataIndex: "id", align: "left", width: 60, sorter: true },
  { title: "标准号", dataIndex: "code", align: "left", ellipsis: true },
  { title: "标准名称", dataIndex: "name", align: "left", ellipsis: true },
  { title: "标准级别", dataIndex: "stdLevel", align: "left", width: 100 },
  { title: "描述", dataIndex: "description", align: "left", width: 240, ellipsis: true },
  { title: "标准目录", dataIndex: "catName", align: "left", ellipsis: true },
  { title: "创建人", dataIndex: "createBy", align: "left", ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", align: "left", width: 150, sorter: true },
  { title: "标准状态", dataIndex: "status", align: "left" },
  { title: "备注", dataIndex: "remark", align: "left", ellipsis: true },
  { title: "操作", key: "actions", align: "center", fixed: "right", width: 200 },
];

const emptyContent = h("div", { class: "emptyBg" }, [
  h("img", { src: new URL("@/assets/system/images/no_data/noData.png", import.meta.url).href, alt: "" }),
  h("p", "暂无记录"),
]);

function getLevelTagType(level) {
  const map = {
    "国家标准": "error",
    "行业标准": "warning",
    "地方标准": "default",
    "团体标准": "default",
  };
  return map[level] || "default";
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

function getList() {
  loading.value = true;
  listDpDocument(queryParams.value).then((response) => {
    dpDataElemList.value = response.data.rows;
    total.value = Number(response.data.total);
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

function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRows.map((item) => item.id);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
}

function handleSortChange(pag, filters, sorter) {
  const prop = sorter.field || sorter.column?.dataIndex;
  const order =
    sorter.order === "ascend"
      ? "ascending"
      : sorter.order === "descend"
        ? "descending"
        : null;
  queryParams.value.orderByColumn =
    prop == "createTime" ? "create_time" : prop;
  queryParams.value.isAsc = order;
  getList();
}

function getDeptTree() {
  listAttDocumentCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "标准目录",
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

.ant-layout-content {
  padding: 2px 0px;
  overflow: hidden;
}

.pagecont-top {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 8px;

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

::v-deep {
  .el-upload-list__item {
    width: 100%;
    height: 25px;
  }
}
</style>
