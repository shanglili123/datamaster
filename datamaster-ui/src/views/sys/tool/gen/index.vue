<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '68px' } }">
        <a-form-item label="表名称" name="tableName">
          <a-input
            v-model:value="queryParams.tableName"
            placeholder="请输入表名称"
            allow-clear
            class="el-form-input-width"
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="表描述" name="tableComment">
          <a-input
            v-model:value="queryParams.tableComment"
            placeholder="请输入表描述"
            allow-clear
            class="el-form-input-width"
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="创建时间">
          <a-range-picker
            class="el-form-input-width"
            v-model:value="dateRange"
            valueFormat="YYYY-MM-DD"
            :placeholder="['开始日期', '结束日期']"
          />
        </a-form-item>
        <a-form-item>
              <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
              </a-button>
              <a-button @click="resetQuery" @mousedown="e => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
              </a-button>
        </a-form-item>
      </a-form>
    </div>
    <div  class="pagecont-bottom">

      <div class="justify-between mb15">
      <a-row :gutter="10" class="btn-style">
        <a-col :span="1.5">
          <a-button
            type="primary"
            :icon="h(DownloadOutlined)"
            :disabled="multiple"
            @click="handleGenTable"
            v-hasPermi="['tool:gen:code']"
          >生成</a-button>
        </a-col>
        <a-col :span="1.5">
          <a-button
            type="primary"
            :icon="h(PlusOutlined)"
            @click="openCreateTable"
            v-hasRole="['admin']"
          >创建</a-button>
        </a-col>
        <a-col :span="1.5">
          <a-button
            type="primary"
            :icon="h(UploadOutlined)"
            @click="openImportTable"
            v-hasPermi="['tool:gen:import']"
          >导入</a-button>
        </a-col>
        <a-col :span="1.5">
          <a-button
            type="primary"
            :icon="h(EditOutlined)"
            :disabled="single"
            @click="handleEditTable"
            v-hasPermi="['tool:gen:edit']"
          >修改</a-button>
        </a-col>
        <a-col :span="1.5">
          <a-button
            type="primary"
            danger
            :icon="h(DeleteOutlined)"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['tool:gen:remove']"
          >删除</a-button>
        </a-col>
      </a-row>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </div>

      <a-table height="60vh" :loading="loading" :data-source="tableList" row-key="tableId"
        :row-selection="{ selectedRowKeys: ids, onChange: handleSelectionChange }" :columns="columns"
>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-tooltip title="预览" placement="top">
            <a-button type="link" :icon="h(EyeOutlined)" @click="handlePreview(record)" v-hasPermi="['tool:gen:preview']"></a-button>
          </a-tooltip>
          <a-tooltip title="编辑" placement="top">
            <a-button type="link" :icon="h(EditOutlined)" @click="handleEditTable(record)" v-hasPermi="['tool:gen:edit']"></a-button>
          </a-tooltip>
          <a-tooltip title="删除" placement="top">
            <a-button type="link" danger :icon="h(DeleteOutlined)" @click="handleDelete(record)" v-hasPermi="['tool:gen:remove']"></a-button>
          </a-tooltip>
<!--          <a-tooltip title="同步" placement="top">-->
<!--            <a-button type="link" :icon="h(ReloadOutlined)" @click="handleSynchDb(record)" v-hasPermi="['tool:gen:edit']"></a-button>-->
<!--          </a-tooltip>-->
            <a-tooltip title="生成代码" placement="top">
              <a-button type="link" :icon="h(DownloadOutlined)" @click="handleGenTable(record)" v-hasPermi="['tool:gen:code']"></a-button>
            </a-tooltip>
          </template>
        </template>
      </a-table>
      <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
    <!-- 预览界面 -->
    <a-modal :title="preview.title" v-model:open="preview.open" width="80%" class="scrollbar" destroy-on-close>
      <a-tabs v-model:activeKey="preview.activeName">
        <a-tab-pane
          v-for="(value, key) in preview.data"
          :tab="key.substring(key.lastIndexOf('/')+1,key.indexOf('.vm'))"
          :key="key.substring(key.lastIndexOf('/')+1,key.indexOf('.vm'))"
        >
          <!-- <div class="justify-between mb15">
              <div class="justify-end top-right-btn">
                  <a-button type="link" v-copyText="value" v-copyText:callback="copyTextSuccess" style="float:right">&nbsp;复制</a-button>
              </div>
          </div> -->
          <div class="precont">
            <a-button type="link" :icon="h(CopyOutlined)" v-copyText="value" v-copyText:callback="copyTextSuccess" style="float:right">&nbsp;复制</a-button>
            <pre >{{ value }}</pre>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    <import-table ref="importRef" @ok="handleQuery" />
    <create-table ref="createRef" @ok="handleQuery" />
  </div>
</template>

<script setup name="Gen">

import { h } from 'vue'
import { listTable, previewTable, delTable, genCode, synchDb } from "@/api/system/tool/gen.js";

import router from "@/router/index.js";

import importTable from "./importTable.vue";

import createTable from "./createTable.vue";

import { DownloadOutlined, PlusOutlined, UploadOutlined, EditOutlined, DeleteOutlined, EyeOutlined, CopyOutlined } from "@ant-design/icons-vue";

const route = useRoute();
const { proxy } = getCurrentInstance();

const tableList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const tableNames = ref([]);
const dateRange = ref([]);
const uniqueId = ref("");

const columns = [
  { title: "序号", key: "index", width: 80, align: "center", customRender: ({ index }) => (queryParams.value.pageNum - 1) * queryParams.value.pageSize + index + 1 },
  { title: "表名称", dataIndex: "tableName", align: "center", ellipsis: true },
  { title: "表描述", dataIndex: "tableComment", align: "center", ellipsis: true },
  { title: "实体", dataIndex: "className", align: "center", ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 160 },
  { title: "更新时间", dataIndex: "updateTime", align: "center", width: 160 },
  { title: "操作", key: "action", align: "center", fixed: "right", width: 240 }
];

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    tableName: undefined,
    tableComment: undefined
  },
  preview: {
    open: false,
    title: "代码预览",
    data: {},
    activeName: "do.java"
  }
});

const { queryParams, preview } = toRefs(data);

onActivated(() => {
  const time = route.query.t;
  if (time != null && time != uniqueId.value) {
    uniqueId.value = time;
    queryParams.value.pageNum = Number(route.query.pageNum);
    dateRange.value = [];
    proxy.resetForm("queryForm");
    getList();
  }
})

/** 查询表集合 */
function getList() {
  loading.value = true;
  listTable(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    tableList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 生成代码操作 */
function handleGenTable(row) {
  const tbNames = row.tableName || tableNames.value;
  if (tbNames == "") {
    proxy.$modal.msgError("请选择要生成的数据");
    return;
  }
  if (row.genType === "1") {
    genCode(row.tableName).then(response => {
      proxy.$modal.msgSuccess("成功生成到自定义路径：" + row.genPath);
    });
  } else {
    proxy.$download.zip("/tool/gen/batchGenCode?tables=" + tbNames, "anivia.zip");
  }
}

/** 同步数据库操作 */
function handleSynchDb(row) {
  const tableName = row.tableName;
  proxy.$modal.confirm('确认要强制同步"' + tableName + '"表结构吗？').then(function () {
    return synchDb(tableName);
  }).then(() => {
    proxy.$modal.msgSuccess("同步成功");
  }).catch(() => {});
}

/** 打开导入表弹窗 */
function openImportTable() {
  proxy.$refs["importRef"].show();
}

/** 打开创建表弹窗 */
function openCreateTable() {
  proxy.$refs["createRef"].show();
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 预览按钮 */
function handlePreview(row) {
  previewTable(row.tableId).then(response => {
    preview.value.data = response.data;
    preview.value.open = true;
    preview.value.activeName = "do.java";
  });
}

/** 复制代码成功 */
function copyTextSuccess() {
  proxy.$modal.msgSuccess("复制成功");
}

// 多选框选中数据
function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRowKeys;
  tableNames.value = selectedRows.map(item => item.tableName);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
}

/** 修改按钮操作 */
function handleEditTable(row) {
  const tableId = row.tableId || ids.value[0];
  router.push({ path: "/tool/gen-edit/index/" + tableId, query: { pageNum: queryParams.value.pageNum } });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const tableIds = row.tableId || ids.value;
  proxy.$modal.confirm('是否确认删除表编号为"' + tableIds + '"的数据项？').then(function () {
    return delTable(tableIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
<style scoped>
.precont{
  padding: 0px 15px;
  height: 444px;
  overflow-y: auto;
}
</style>
