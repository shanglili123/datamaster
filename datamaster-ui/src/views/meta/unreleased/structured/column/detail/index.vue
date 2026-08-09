<template>
  <a-spin :spinning="store.loading">
  <div class="app-container">
    <div class="pagecont-top-wrap">
      <div class="infotop">
        <div class="infotop-title mb15 clearfixs">
          <div class="task-item">
            <div class="task-id">
              {{ getFormatValue(form.id) }}
            </div>
            <div class="task-name">
              {{ getFormatValue(form.columnName) }}
            </div>
            <div>
              <dict-tag
                :options="toValue(dicts.meta_task_status)"
                :value="form.status"
              />
            </div>
          </div>
          <div class="btn-style">
            <a-button
              type="primary"
              class="fh_btn"
              @mousedown="(e) => e.preventDefault()"
              @click="router.back"
            >
              <svg-icon iconClass="fhs" />返回
            </a-button>
          </div>
        </div>

        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">来源系统</div>
              <div class="infotop-row-value">
                {{ form.sourceSystemName || "-" }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">数据库类型</div>
              <div class="infotop-row-value">
                <dict-tag
                  :options="toValue(dicts.datasource_type)"
                  :value="form.dbRespVO?.dbType"
                />
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">版本号</div>
              <div class="infotop-row-value">
                {{ formatVersion(form.version) }}
              </div>
            </div>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">所属库名</div>
              <div class="infotop-row-value">
                {{ getFormatValue(form.dbRespVO?.dbName) }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">所属表名</div>
              <div class="infotop-row-value">
                {{ getFormatValue(form.tableRespVO?.tableName) }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">字段注释</div>
              <div class="infotop-row-value">
                {{ getFormatValue(form.columnComment) }}
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>

    <div class="pagecont-bottom">
      <a-tabs v-model:activeKey="store.tab" @change="handleTabChange">
        <a-tab-pane
          v-for="tab in tabData"
          :tab="tab.label"
          :key="tab.key"
        />
      </a-tabs>

      <component
        v-if="!store.loading"
        :is="tabComponent[store.tab]"
        :detail="store.form"
        @update:detail="getDetail"
      />
    </div>
  </div>
  </a-spin>
</template>
<script setup name="DatabaseDetail">
import { computed, getCurrentInstance, reactive, toValue } from "vue";
import { useRouter, useRoute } from "vue-router";
import { getColumn } from "@/api/cat/unreleased/column";
import { listDomain } from "@/api/tax/domain/domain.js";
import { getParentLabelPath } from "@/utils/anivia.js";

const tabData = [
  {
    key: "BaseInfo",
    label: "基本信息",
  },
  // {
  //   key: "LineageAnalysis",
  //   label: "血缘分析",
  // },
  // {
  //   key: "ImpactAnalysis",
  //   label: "影响分析",
  // },
  {
    key: "VersionManagement",
    label: "版本与变更",
  },
];
const tabComponent = {
  BaseInfo: defineAsyncComponent(() => import("./BaseInfo.vue")),
  LineageAnalysis: defineAsyncComponent(() => import("./LineageAnalysis.vue")),
  ImpactAnalysis: defineAsyncComponent(() => import("./ImpactAnalysis.vue")),
  VersionManagement: defineAsyncComponent(() =>
    import("./VersionManagement.vue")
  ),
};

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "datasource_type",
  "mc_collect_scope",
  "mc_collect_mode",
  "meta_task_status"
);

const router = useRouter();
const route = useRoute();
if (!route.query.id) router.go(-1);

const store = reactive({
  loading: false,
  form: {},
  treeDomains: [],
  domains: [],
  tab: route.query.tab || "BaseInfo",
  datasources: [],
});

const form = computed(() => store.form);

// 获取业务域路径
const getDomainPath = computed(() => {
  return function (id) {
    let domainName = getParentLabelPath(store.treeDomains, id, {
      idKey: "id",
      labelKey: "name",
      childrenKey: "children",
    });
    const idx = domainName.indexOf("/");
    return idx == -1 ? domainName : domainName.slice(idx + 1);
  };
});

// 获取业务域列表
function getDomains() {
  return listDomain().then((res) => {
    store.domains = [...res.data];
    store.treeDomains.splice(0, store.treeDomains.length);
    const domains = {
      id: 0,
      code: 0,
      name: "顶级节点",
      children: proxy.handleTree(res.data, "id", "parentId"),
    };
    store.treeDomains.push(domains);
    return res;
  });
}

// 获取详情
async function getDetail() {
  store.loading = true;
  // await getDomains();
  getColumn(route.query.id).then((res) => {
    const dbRespVO = res.data?.tableRespVO?.dbRespVO;
    res.data.dbRespVO = dbRespVO;
    const datasource = dbRespVO?.datasource;
    if (datasource?.datasourceConfig) {
      datasource.datasourceConfig = JSON.parse(datasource.datasourceConfig);
    }
    res.data.username = datasource?.datasourceConfig?.username;
    store.form = res.data;
    store.loading = false;
  });
}

// 切换tab
function handleTabChange(tab) {
  router.push({
    query: {
      ...route.query,
      tab,
    },
  });
}

getDetail();
</script>
