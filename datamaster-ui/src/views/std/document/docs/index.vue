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

    <a-spin :spinning="loading">
    <div class="pagecont-bottom">
      <div class="page-list" v-if="total > 0">
        <a-row :gutter="15">
          <a-col :span="12" v-for="(item, index) in searchList" :key="index">
            <div class="page-item">
              <div class="item-title">
                <div class="item-title-left">
                  <img :src="getFileIcon(item.fileUrl)" alt="" />
                  <div class="item-name">
                    <span class="item-name-title" :title="item.name">{{
                      item.name
                    }}</span>
                    <span
                      class="item-name-code ellipsis"
                      :title="item.fileName"
                      >{{ item.fileName }}</span
                    >
                  </div>
                </div>
                <div class="item-title-right">
                  <div
                    class="form-btn"
                    @click="handleFilePreview(item.fileUrl)"
                  >
                    <span>查看</span>
                  </div>
                  <div class="form-btn" @click="handleView(item)">
                    <span>详情</span>
                  </div>
                </div>
              </div>
              <div class="item-con">
                <div class="item-form">
                  <div class="form-label">标准分类:</div>
                  <div class="form-value">
                    <div :class="['value-tag', 'type' + item.type]">
                      {{ typeFormat1(item) }}
                    </div>
                  </div>
                </div>
                <div class="item-form">
                  <div class="form-label">实施状态:</div>
                  <div class="form-value">
                    <dict-tag
                      :options="dp_document_status"
                      :value="item.status"
                    />
                  </div>
                </div>
                <div class="item-form">
                  <div class="form-label">发布日期:</div>
                  <div class="form-value">
                    <div class="ellipsis">{{ item.releaseDate || "-" }}</div>
                  </div>
                </div>
                <div class="item-form">
                  <div class="form-label">实施日期:</div>
                  <div class="form-value">
                    <div class="ellipsis">
                      {{ item.implementationDate || "-" }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
      <div class="empty" v-else>
        <img src="@/assets/da/asset/empty.png" alt="" />
        <span>暂无搜索内容～</span>
      </div>
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
    </a-spin>
  </div>
</template>
<script setup name="DocumentDocs">
import { dpDocumentList } from "@/api/std/document/search";
import handleFilePreview from "@/utils/filePreview.js";

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

const getFileIcon = (fileUrl) => {
  if (!fileUrl) return '';
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

function getList() {
  loading.value = true;
  dpDocumentList(queryParams.value)
    .then((response) => {
      searchList.value = response.data.rows;
      total.value = Number(response.data.total);
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleView(row) {
  routeTo("/mdl/document/detail", row);
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

.page-list {
  height: auto;
  overflow: visible;

  &::-webkit-scrollbar {
    width: 2px;
  }

  .page-item {
    padding: 18px 30px;
    background: #fff;
    margin-bottom: 15px;
    border-radius: 2px;

    .item-title {
      display: flex;
      padding-bottom: 15px;
      margin-bottom: 5px;
      border-bottom: 1px solid #eeeeee;

      .item-title-left {
        display: flex;
        align-items: center;
        width: 66%;

        img {
          width: 40px;
          height: 40px;
          margin-right: 20px;
        }

        .item-name {
          width: calc(100% - 72px);
          display: flex;
          flex-direction: column;

          .item-name-title {
            display: block;
            font-family: PingFang SC;
            font-weight: 600;
            font-size: 16px;
            color: #3d446e;
            line-height: 24px;
          }

          .item-name-code {
            display: block;
            font-family: PingFang SC;
            font-size: 14px;
            color: rgba(88, 88, 88, 0.85);
            line-height: 22px;
          }
        }
      }

      .item-title-right {
        margin: 5px 0 0 auto;
        display: flex;

        .form-btn {
          cursor: pointer;
          min-width: 50px;
          height: 24px;
          padding: 0 12px;
          border-radius: 2px;
          margin-left: 12px;
          display: flex;
          justify-content: center;
          align-items: center;
          background: #e6f4ff;

          span {
            font-family: PingFang SC;
            font-weight: 500;
            font-size: 12px;
            color: #2666fb;
          }
        }
      }
    }

    .item-con {
      display: flex;
      flex-wrap: wrap;

      .item-form {
        width: 50%;
        display: flex;
        align-items: center;
        margin-top: 10px;

        .form-label {
          width: 76px;
          font-family: PingFang SC;
          font-weight: 400;
          font-size: 14px;
          color: #717171;
        }

        .form-value {
          width: calc(100% - 76px);
          font-family: PingFang SC;
          font-weight: 400;
          font-size: 14px;
          color: #262626;

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
        }
      }
    }
  }
}

.pagination-container {
  height: 60px;
  background: #ffffff;
  border-radius: 2px;
  margin: 0px 0 0;
  padding: 14px 20px !important;

  :deep(.el-pagination) {
    right: 20px;
  }
}

.empty {
  min-height: calc(100vh - 250px);
  background: #ffffff;
  border-radius: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;

  img {
    width: 200px;
    height: 180px;
    margin: 240px 0 40px;
  }

  span {
    font-family: PingFang SC;
    font-weight: 400;
    font-size: 18px;
    color: rgba(0, 0, 0, 0.65);
  }
}
</style>
