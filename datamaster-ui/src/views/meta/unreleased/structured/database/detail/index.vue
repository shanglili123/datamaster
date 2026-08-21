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
              {{ getFormatValue(form.dbName) }}
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
              <div class="infotop-row-lable">业务域</div>
              <div class="infotop-row-value">
                {{ getDomainPath(form.domainId) }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">数据库类型</div>
              <div class="infotop-row-value">
                <dict-tag
                  :options="toValue(dicts.datasource_type)"
                  :value="form.dbType"
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
      />
    </div>
  </div>
  </a-spin>
</template>
<script setup name="DatabaseDetail">
import { computed, getCurrentInstance, reactive, toValue } from "vue";
import { useRouter, useRoute } from "vue-router";
import { message } from "ant-design-vue";
import { getDb } from "@/api/cat/unreleased/db.js";
import { listDomain } from "@/api/tax/domain/domain.js";
import { getParentLabelPath } from "@/utils/anivia.js";

const tabData = [
  {
    key: "BaseInfo",
    label: "基本信息",
  },
  {
    key: "TableList",
    label: "表列表",
  },
];
const tabComponent = {
  BaseInfo: defineAsyncComponent(() => import("./BaseInfo.vue")),
  TableList: defineAsyncComponent(() => import("./TableList.vue")),
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
});

const form = computed(() => store.form);

// 获取详情
function getDetail() {
  store.loading = true;
  getDb(route.query.id)
    .then((res) => {
      if (!res?.data) {
        store.form = {};
        return;
      }
      const datasource = res.data?.datasource;
      if (datasource?.datasourceConfig) {
        if (typeof datasource.datasourceConfig === "string") {
          try {
            datasource.datasourceConfig = JSON.parse(datasource.datasourceConfig);
          } catch (e) {
            // 配置串非法时保持原样，避免阻塞详情展示
          }
        }
        res.data.username = datasource.datasourceConfig?.username;
      }
      store.form = res.data;
    })
    .catch((err) => {
      console.error("库详情加载失败", err);
      store.form = {};
      message.error("库详情加载失败，请稍后重试");
    })
    .finally(() => {
      store.loading = false;
    });
}

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
  listDomain().then((res) => {
    store.domains = [...res.data];
    store.treeDomains.splice(0, store.treeDomains.length);
    const domains = {
      id: 0,
      code: 0,
      name: "顶级节点",
      children: proxy.handleTree(res.data, "id", "parentId"),
    };
    store.treeDomains.push(domains);
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

// getDomains();
getDetail();
</script>
