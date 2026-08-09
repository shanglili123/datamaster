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
              {{ getFormatValue(form.name) }}
            </div>
            <div>
              <dict-tag
                :options="toValue(dicts.mc_task_status)"
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
                {{ form?.sourceSystemName || "--" }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建人</div>
              <div class="infotop-row-value">{{ form.createBy || "--" }}</div>
            </div>
          </a-col>
          <a-col :span="24" style="margin: 2px 0">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">描述</div>
              <div class="infotop-row-value">
                {{ form.description }}
              </div>
            </div>
          </a-col>

          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">最近执行时间</div>
              <div class="infotop-row-value">{{ form.lastExecuteTime }}</div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">下次执行时间</div>
              <div class="infotop-row-value">{{ form.createTime }}</div>
            </div>
          </a-col>
          <a-col :span="24" style="margin: 2px 0 0">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">备注</div>
              <div class="infotop-row-value">{{ form.remark }}</div>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>

    <div class="pagecont-bottom">
      <a-tabs v-model:activeKey="store.tab">
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
<script setup name="Detail">
import { computed, getCurrentInstance, reactive, toValue } from "vue";
import { useRouter, useRoute } from "vue-router";
import { getTask, sourceSystemTree } from "@/api/cat/task/task";
import { getParentLabelPath } from "@/utils/anivia.js";

const tabData = [
  {
    key: "BaseInfo",
    label: "基本信息",
  },
];
const tabComponent = {
  BaseInfo: defineAsyncComponent(() => import("./BaseInfo.vue")),
};

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "datasource_type",
  "mc_collect_scope",
  "mc_collect_mode",
  "mc_task_status"
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
  getTask(route.query.id).then((res) => {
    store.form = res.data;
    store.loading = false;
  });
}

// 获取来源系统路径
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

// 获取来源系统列表
function getSourceSystemTreeData() {
  sourceSystemTree().then((res) => {
    store.treeDomains = res.data;
  });
}

getSourceSystemTreeData();
getDetail();
</script>

