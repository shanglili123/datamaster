<template>
  <div class="app-container">
    <div class="pagecont-top" style="padding-bottom: 15px">
      <div class="infotop">
        <div class="infotop-title mb15">
          {{ form.name || "-" }}
        </div>
        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">编号</div>
              <div class="infotop-row-value">
                {{ form.id || "-" }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">英文名称</div>
              <div class="infotop-row-value">
                {{ form.engName || "-" }}
              </div>
            </div>
          </a-col>
          <!-- <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">中文名称</div>
              <div class="infotop-row-value">
                {{ form.name || "-" }}
              </div>
            </div>
          </a-col> -->
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">类型</div>
              <div class="infotop-row-value">
                <dict-tag :options="dp_data_elem_code_type" :value="form.type" />
              </div>
            </div>
          </a-col>
          <a-col :span="24" style="margin: 2px 0;">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">描述</div>
              <div class="infotop-row-value">
                {{ form.description || "-" }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">数据元目录</div>
              <div class="infotop-row-value">
                {{ form.catName || "-" }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">字段类型</div>
              <div class="infotop-row-value">
                <dict-tag :options="column_type" :value="form.columnType" />
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">状态</div>
              <div class="infotop-row-value">
                <dict-tag :options="sys_disable" :value="form.status" />
              </div>
            </div>
          </a-col>

        </a-row>
      </div>
    </div>
    <!-- 标签页部分 -->
    <div class="pagecont-bottom">
      <a-tabs v-model:activeKey="activeName" class="demo-tabs" @change="handleClick">
        <a-tab-pane tab="关联清洗规则" key="1" lazy>
          <cleanRule :dataElemId="dataElemId" dataType="2" />
        </a-tab-pane>
        <a-tab-pane tab="关联稽查规则" key="2" lazy>
          <auditRule :dataElemId="dataElemId" dataType="1" />
        </a-tab-pane>
        <a-tab-pane tab="关联信息" key="3" lazy>
          <asset />
        </a-tab-pane>
        <a-tab-pane tab="详细信息" key="5" lazy>
          <info :daDiscoveryTaskDetail="form" />

        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup name="dataElemDetailDialog">
import { onMounted } from "vue";

const { proxy } = getCurrentInstance();

import { getDpDataElem } from "@/api/std/dataElem/dataElem";

import cleanRule from "@/views/std/dataElem/detail/column/cleanRule";
import auditRule from "@/views/std/dataElem/detail/column/auditRule";
import asset from "@/views/std/dataElem/detail/components/asset.vue";
import info from "@/views/std/dataElem/detail/column/info.vue";
import { useRoute } from "vue-router";
const { column_type, sys_disable, dp_data_elem_code_type } = proxy.useDict(
  "column_type",
  "sys_disable",
  "dp_data_elem_code_type"
);

const dpDataElemRuleRelList = ref([]);

const data = reactive({
  form: {},
  activeName: "1",
});
const { form, activeName } = toRefs(data);
const dataElemId = ref("");
const route = useRoute();
dataElemId.value = route.query.id;

/** 详情按钮操作 */
function getDetail() {
  const id = dataElemId.value;
  if (!id) return;
  getDpDataElem(id).then((response) => {
    form.value = response.data;
  });
}

// 页面加载时获取数据
onMounted(() => {
  getDetail();
});

// 返回列表页
function goBack() {
  router.go(-1);
}
</script>

<style scoped lang="scss">
.app-container {
  margin: 15px 15px 0px 15px;

  .pagecont-bottom {
    min-height: calc(100vh - 345px) !important;
  }
}
</style>

