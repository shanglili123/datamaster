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
              {{ getFormatValue(form.tableName) }}
            </div>
          </div>
          <div class="btn-style">
            <a-button
              type="primary"
              class="fh_btn"
              @click="goProbeHistory"
            >
              查看质量报告
            </a-button>
            <a-button
              type="primary"
              class="fh_btn"
              @mousedown="(e) => e.preventDefault()"
              @click="handleBack"
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
                {{ getFormatValue(form.dbName) }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">表注释</div>
              <div class="infotop-row-value">
                {{ getFormatValue(form.tableComment) }}
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
        type="tab"
        @update:detail="getDetail"
      />
    </div>
  </div>
  </a-spin>
</template>
<script setup name="DatabaseDetail">
import { computed, getCurrentInstance, reactive, toValue } from "vue";
import { useRouter, useRoute } from "vue-router";
import { message } from "ant-design-vue";
import { getTable } from "@/api/cat/unreleased/table";
import { listDomain } from "@/api/tax/domain/domain.js";
import { listProbeHistoryByTable } from "@/api/ast/quality/probeTaskInstance";
import { getParentLabelPath } from "@/utils/anivia.js";

const tabData = [
  {
    key: "BaseInfo",
    label: "基本信息",
  },
  {
    key: "ColumnList",
    label: "字段列表",
  },
  {
    key: "ProbeHistory",
    label: "任务历史",
  },
];
const tabComponent = {
  BaseInfo: defineAsyncComponent(() => import("./BaseInfo.vue")),
  ColumnList: defineAsyncComponent(() => import("./ColumnList.vue")),
  ProbeHistory: defineAsyncComponent(() => import("./ProbeHistory.vue")),
};

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "datasource_type",
  "mc_collect_scope",
  "mc_collect_mode"
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
function getDetail() {
  store.loading = true;
  getTable(route.query.id)
    .then((res) => {
      if (!res?.data) {
        store.form = {};
        return;
      }
      const datasource = res.data?.dbRespVO?.datasource;
      if (
        datasource?.datasourceConfig &&
        typeof datasource.datasourceConfig === "string"
      ) {
        try {
          datasource.datasourceConfig = JSON.parse(datasource.datasourceConfig);
        } catch (e) {
          // 配置串非法时保持原样，避免阻塞详情展示
        }
      }
      res.data.username = datasource?.datasourceConfig?.username;
      store.form = res.data;
    })
    .catch((err) => {
      console.error("表详情加载失败", err);
      store.form = {};
      message.error("表详情加载失败，请稍后重试");
    })
    .finally(() => {
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

// 直接打开详情页时没有可返回的路由记录，回退到元数据结果列表。
function handleBack() {
  router.push({ path: "/meta/probeResult" });
}

// 查看质量报告：直接跳转该表最近一次探查报告
function goProbeHistory() {
  const { datasourceId, tableName } = store.form || {};
  if (!datasourceId || !tableName) {
    message.warning("该表暂无质量报告，请先执行带质量规则的探查任务");
    return;
  }
  listProbeHistoryByTable({ datasourceId, tableName })
    .then((res) => {
      const records = res.data || [];
      if (!records.length) {
        message.warning("该表暂无质量报告，请先执行带质量规则的探查任务");
        return;
      }
      // 后端按开始时间倒序，第一条即最近一次探查结果
      const latest = records[0];
      router.push({
        path: "/ast/quality/probeTaskInstance/detail",
        query: { id: latest.id, score: latest.score },
      });
    })
    .catch(() => {
      message.error("质量报告加载失败，请稍后重试");
    });
}

getDetail();
</script>

<style lang="scss" scoped>
.app-container {
  height: 100%;
  min-height: 0 !important;
  margin: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  --el-text-color-regular: inherit;
}
.pagecont-top-wrap {
  flex-shrink: 0;
}
.pagecont-bottom {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
</style>
