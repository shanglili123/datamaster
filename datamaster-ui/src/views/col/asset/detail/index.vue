<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top back-host" v-show="showSearch" style="padding-bottom: 15px">
      <div class="infotop">
        <a-button
          type="primary"
          class="fh_btn detail-back-btn"
          @mousedown="(e) => e.preventDefault()"
          @click="handleBack"
        >
          <svg-icon iconClass="fhs" />返回
        </a-button>
        <div class="infotop-title mb15">{{ daAssetDetail?.name }}</div>
        <a-row :gutter="20">
          <a-col :span="desc.span || 8" v-for="desc in descList" :key="desc.label">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">{{ desc.label }}</div>
              <div class="infotop-row-value">
                <span v-if="desc.key == 'assetsAssetThemeRelList'">{{desc.value.length > 0 ? desc.value.map((ele) =>
                  ele.themeName).join(", ") : "-"}}</span>
                <span v-else-if="desc.key == 'status'"><dict-tag :options="da_assets_status"
                    :value="desc.value"
/></span>
                <span class="li-type" v-else-if="desc.key == 'type'"
                  :style="{ color: desc.value == 1 ? '#21a3dd' : desc.value == 7 ? '#edce2e' : '' }"
>
                  <img v-if="desc.value == 1" src="@/assets/da/asset/api (3).svg" alt="" />
                  <img v-if="desc.value == 7" src="@/assets/da/asset/api (5).svg" alt="" />
                  {{ desc.value == 1 ? "库表" : desc.value == 7 ? "文件" : "-" }}
                </span>
                <span v-else>{{ desc.value || "-" }}</span>
              </div>
            </div>
          </a-col>
        </a-row>
        <a-row :gutter="20" v-if="false">
          <a-col :span="8" v-if="daAssetDetail.type == 1">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">英文名称</div>
              <div class="infotop-row-value">{{ daAssetDetail.tableName }}</div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">主题名称</div>
              <div class="infotop-row-value">
                {{daAssetDetail.assetsAssetThemeRelList?.length ? daAssetDetail.assetsAssetThemeRelList.map((item) =>
                  item.themeName).join(", ") : "-"}}
              </div>
            </div>
          </a-col>

          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">资产目录</div>
              <div class="infotop-row-value">
                {{ daAssetDetail.catName || "-" }}
              </div>
            </div>
          </a-col>
          <template v-if="daAssetDetail.type == 1">
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">数据连接</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail.datasourceName || "-" }}
                </div>
              </div>
            </a-col>
            <!-- <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">表名称</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail.tableName || "-" }}
                </div>
              </div>
            </a-col> -->
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">表描述</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail.tableComment || "-" }}
                </div>
              </div>
            </a-col>
          </template>
          <template v-if="daAssetDetail.type == 4">
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">文件类型</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetGeo?.fileType || "-" }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">上传文件</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail.assetsAssetGeo?.fileUrl || "-" }}
                </div>
              </div>
            </a-col>
          </template>
          <template v-if="daAssetDetail.type == 5">
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">平台</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetVideo?.platform || "-" }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">平台ip</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetVideo?.ip || "-" }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">平台端口</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetVideo?.port || "-" }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">摄像头编码</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetVideo?.config?.cameraCode || "-" }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">摄像头名称</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetVideo?.config?.cameraName || "-" }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">公钥</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetVideo?.config?.appkey || "-" }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">私钥</div>
                <div class="infotop-row-value">
                  {{ daAssetDetail?.assetsAssetVideo?.config?.appSecret || "-" }}
                </div>
              </div>
            </a-col>
          </template>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">状态</div>
              <div class="infotop-row-value">
                <dict-tag :options="da_assets_status" :value="daAssetDetail.status" />
              </div>
            </div>
          </a-col>
          <!-- <a-col :span="8">
                        <div class="infotop-row border-top">
                            <div class="infotop-row-lable">描述</div>
                            <div class="infotop-row-value">
                                {{ daAssetDetail.description || '-' }}
                            </div>
                        </div>
                    </a-col> -->
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建人</div>
              <div class="infotop-row-value">
                {{ daAssetDetail.createBy || "-" }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建时间</div>
              <div class="infotop-row-value">
                {{ parseTime(daAssetDetail.createTime, "{y}-{m}-{d} {h}:{i}") }}
              </div>
            </div>
          </a-col>
          <a-col :span="24">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">备注</div>
              <div class="infotop-row-value">
                {{ daAssetDetail.remark || "-" }}
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>
    <div class="pagecont-bottom">
      <a-tabs v-model:activeKey="activeName" class="demo-tabs" @change="handleClick"
        v-if="!daAssetDetail.assetsAssetFiles || ['.xlsx', '.xls', '.csv'].includes(daAssetDetail.assetsAssetFiles.type)"
>
        <a-tab-pane v-for="pane in tabPanes" :key="pane.name" :tab="pane.label">
          <component v-if="activeName == pane.name" :is="pane.component" :form1="daAssetDetail" />
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>
<script setup name="AssetsAsset">
import { getDaAsset } from "@/api/ast/asset/asset";
import { useRoute, useRouter } from "vue-router";
import column from "@/views/col/asset/detail/table/column.vue";
import DataQualityControl from "@/views/col/asset/detail/table/quality.vue";
import lineage from "@/views/col/asset/detail/table/lineage.vue";
import preview from "@/views/col/asset/detail/table/preview.vue";
import info from "@/views/col/asset/detail/info.vue";

const { proxy } = getCurrentInstance();
const { da_assets_status } = proxy.useDict("da_assets_status");
const activeName = ref("0");

function handleClick(tab) {
  // 可根据需要自定义逻辑
}

const descList = ref([
  {
    key: "type",
    label: "类型",
    value: "",
  },
  {
    key: "tableName",
    label: "标识",
    value: "",
  },
  {
    key: "status",
    label: "状态",
    value: "",
  },
  {
    key: "assetsAssetThemeRelList",
    label: "所属主题",
    value: "",
    span: 24,
  },
]);

// 计算属性生成 tab pane 数组
const tabPanes = computed(() => {
  // 后端字段类型通常是字符串，但兼容数字类型，避免资产字段页因类型不一致不渲染。
  switch (String(daAssetDetail.value.type || "")) {
    case "1":
      return [
        { label: "资产字段", name: "0", component: column },
        { label: "资产预览", name: "2", component: preview },
        { label: '资产质量', name: '3', component: DataQualityControl },
        { label: '资产血缘', name: '4', component: lineage },
        { label: "资产概览", name: "5", component: info },

      ];
    case "4":
      return [
        { label: "资产概览", name: "0", component: info },
        { label: "资产预览", name: "1", component: preview },
      ];
    case "5":
      return [{ label: "资产概览", name: "0", component: info }];
    case "6":
      return [
        { label: "资产概览", name: "0", component: info },
        { label: "资产字段", name: "1", component: column },
        { label: "资产预览", name: "2", component: preview },
      ];
    case "7":
      return [{ label: "资产概览", name: "0", component: info }];
  }
});
const showSearch = ref(true);
const route = useRoute();
const router = useRouter();
const stationNavigation = inject("spaceWorkstationNavigation", null);
// 返回资产列表：优先历史回退保留列表状态；直接打开详情页时按入口回对应列表
// （列表为动态菜单路由：资产数据=/ast/asset，空间资产=/spaceBase/asset，meta.activeMenu 的 /col/asset 并不存在）
function handleBack() {
  if (stationNavigation?.back?.()) {
    return;
  }
  if (window.history.state && window.history.state.back) {
    router.back();
    return;
  }
  const fallbackMap = {
    "/ast/asset/detail": "/ast/asset",
    "/col/asset/detail": "/spaceBase/asset",
  };
  router.push({ path: fallbackMap[route.path] || "/ast/asset" });
}
let id = route.query.id || null;
// 监听 id 变化
watch(
  () => route.query.id,
  (newId) => {
    id = newId || null; // 如果 id 为空，使用默认值 1
    getDaAssetDetailById();
  },
  { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);
const data = reactive({
  daAssetDetail: {},
  form: {},
});

const { daAssetDetail } = toRefs(data);

/** 复杂详情页面上方表单查询 */
function getDaAssetDetailById() {
  if (!id) {
    return;
  }
  const _id = id;
  getDaAsset(_id).then((response) => {
    daAssetDetail.value = response.data;
    descList.value.forEach((item) => {
      item.value = response.data[item.key];
    });
    if (response.data.type == "5") {
      daAssetDetail.value.assetsAssetVideo.config = JSON.parse(response.data.assetsAssetVideo.config);
    }
  });
}

onActivated(() => {
  activeName.value = "0";
  getDaAssetDetailById();
  // listDaAssetColumn();
});
onBeforeUnmount(() => {
  // 清空参数或重置状态
  data.daAssetDetail = {};
  data.form = {};
  activeName.value = "0"; // 重置tab页
});
// listDaAssetColumn();
</script>
<style lang="scss" scoped>
.app-container {
  height: 100%;
  min-height: 0 !important;
  margin: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.pagecont-top {
  flex-shrink: 0;
}
.back-host {
  position: relative;
}
.detail-back-btn {
  position: absolute;
  top: 12px;
  right: 0;
  z-index: 5;
}
.pagecont-bottom {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.li-type {
  display: flex;
  align-items: center;

  img {
    width: 18px;
    margin: 0 5px;
  }
}
</style>

