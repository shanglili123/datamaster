<template>
  <div class="app-container" ref="app-container">

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
        <a-form-item label="标准名称" name="search">
          <a-input
            class="el-form-input-width"
            v-model:value="queryParams.search"
            placeholder="请输入标准名称"
            allow-clear
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            @click="handleQuery"
            @mousedown="(e) => e.preventDefault()"
          >
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </a-button>
          <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item>
      </a-form>
      <div class="list-title">
        共
        <span style="color: #ff9800"> {{ total }} </span>
        个标准，
        <span style="color: #0091ea">0</span>
        个逻辑模型，
        <span style="color: #0baa84">0</span>
        个数据元，
        <span style="color: #8876de">0</span>
        个代码表
      </div>
    </div>

    <div class="pagecont-bottom">
      <a-table
        striped
        :loading="loading"
        :data-source="searchList"
        :columns="tableColumns"
        :pagination="false"
        :scroll="{ x: 1100 }"
        :locale="{ emptyText: '暂无搜索内容～' }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <div class="doc-name-cell">
              <img :src="getFileIcon(record.fileUrl)" alt="" />
              <div class="doc-name">
                <span class="doc-name-title" :title="record.name">{{ record.name }}</span>
                <span class="doc-name-code ellipsis" :title="record.fileName">{{ record.fileName }}</span>
              </div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'type'">
            <div :class="['value-tag', 'type' + record.type]">{{ typeFormat1(record) }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <dict-tag :options="dp_document_status" :value="record.status" />
          </template>
          <template v-else-if="column.dataIndex === 'releaseDate'">
            {{ record.releaseDate || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'implementationDate'">
            {{ record.implementationDate || "-" }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="handleFilePreview(record.fileUrl)">查看</a-button>
            <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
          </template>
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
  </div>
</template>
<script setup name="Search">
import { dpDocumentList } from "@/api/std/document/search";
import handleFilePreview from "@/utils/filePreview.js";
// search
const { proxy } = getCurrentInstance();
const { dp_document_standard_type, dp_document_type, dp_document_status } =
  proxy.useDict(
    "dp_document_standard_type",
    "dp_document_type",
    "dp_document_status"
  );
const searchList = ref([]);
const loading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const router = useRouter();

const tableColumns = [
  { title: "标准名称", dataIndex: "name", key: "name", width: 300, ellipsis: true },
  { title: "标准分类", dataIndex: "type", key: "type", width: 120 },
  { title: "实施状态", dataIndex: "status", key: "status", width: 120 },
  { title: "发布日期", dataIndex: "releaseDate", key: "releaseDate", width: 150 },
  { title: "实施日期", dataIndex: "implementationDate", key: "implementationDate", width: 150 },
  { title: "操作", key: "actions", width: 130, fixed: "right" },
];

const column1 = ref([
  {
    label: "标准分类",
    prop: "type",
  },
  {
    label: "标准状态",
    prop: "status",
  },
  {
    label: "发布日期",
    prop: "releaseDate",
  },
  {
    label: "实施日期",
    prop: "implementationDate",
  },
  {
    label: "标准文件",
    prop: "fileName",
    width: "100%",
  },
]);
const column2 = ref([
  {
    label: "归属标准",
    prop: "name",
  },
  {
    label: "发布日期",
    prop: "releaseDate",
  },
  {
    label: "实施日期",
    prop: "implementationDate",
  },
  {
    label: "模型名称",
    prop: "createTime",
  },
  {
    label: "模型代号",
    prop: "createTime",
  },
]);
const column3 = ref([
  {
    label: "归属标准",
    prop: "createTime",
  },
  {
    label: "发布日期",
    prop: "releaseDate",
  },
  {
    label: "实施日期",
    prop: "implementationDate",
  },
]);
const column4 = ref([
  {
    label: "归属标准",
    prop: "createTime",
  },
  {
    label: "代码表名称",
    prop: "createTime",
  },
  {
    label: "代码表代号",
    prop: "createTime",
  },
  {
    label: "发布日期",
    prop: "releaseDate",
  },
  {
    label: "实施日期",
    prop: "implementationDate",
  },
]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    search: null,
  },
});

const { queryParams } = toRefs(data);
const typeFormat1 = (row) => {
  return proxy.selectDictLabel(dp_document_type.value, row.type);
};
const typeFormat2 = (row) => {
  return proxy.selectDictLabel(dp_document_status.value, row.status);
};
const getFileIcon = (fileUrl) => {
  let type = fileUrl.split(".")[fileUrl.split(".").length - 1];
  switch (type) {
    case "pdf":
      return new URL("@/assets/dp/standardSearch/file (2).svg", import.meta.url)
        .href;
    case "doc":
    case "docx":
      return new URL("@/assets/dp/standardSearch/file (1).svg", import.meta.url)
        .href;
    case "ppt":
      return new URL("@/assets/dp/standardSearch/file (3).svg", import.meta.url)
        .href;
    default:
      return new URL("@/assets/dp/standardSearch/file (4).svg", import.meta.url)
        .href;
  }
};
/** 查询应用API服务关联列表 */
function getList() {
  loading.value = true;
  dpDocumentList(queryParams.value)
    .then((response) => {
      searchList.value = response.data.rows;
      total.value = response.data.total;
    })
    .finally(() => {
      loading.value = false;
    });
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

function handleView(row) {
  routeTo("/mdl/document/search/detail", row);
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

getList();
</script>

<style lang="scss" scoped>
.pagecont-top {
  padding: 0 25px 0 15px;
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .el-form-item {
    margin-bottom: 0;
  }

  .list-title {
    font-size: 14px;
    font-family: PingFang SC;
    font-weight: 500;
    color: #262626;

    span {
      font-family: DINPro-Bold;
      font-size: 18px;
    }
  }
}

.pagecont-bottom {
  padding: 0;
  background-color: transparent;
  box-shadow: none;
}

.doc-name-cell {
  display: flex;
  align-items: center;

  img {
    width: 32px;
    height: 32px;
    margin-right: 10px;
    flex-shrink: 0;
  }

  .doc-name {
    min-width: 0;
    display: flex;
    flex-direction: column;

    .doc-name-title {
      display: block;
      font-family: PingFang SC;
      font-weight: 600;
      font-size: 14px;
      color: #3d446e;
      line-height: 22px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .doc-name-code {
      display: block;
      font-family: PingFang SC;
      font-size: 12px;
      color: rgba(88, 88, 88, 0.85);
      line-height: 18px;
      max-width: 220px;
    }
  }
}

.value-tag {
  width: 67px;
  height: 22px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: PingFang SC;
  font-weight: 400;
  font-size: 13px;
  color: #ffffff;
  border-radius: 10px 10px 10px 0;

  &.type1 {
    background: #e23d3d;
  }

  &.type2 {
    background: #ff9800;
  }

  &.type3 {
    background: #3062f2;
  }

  &.type4 {
    background: #05a5a0;
  }
}

.pagecont-bottom .ant-table-wrapper {
  background: #ffffff;
  border-radius: 2px;
  padding: 12px;
}

.pagination-container {
  height: 60px;
  background: #ffffff;
  border-radius: 2px;
  margin: 0px 0 0;
  padding: 14px 20px !important;

  :deep(.ant-pagination) {
    right: 20px;
  }
}
</style>


