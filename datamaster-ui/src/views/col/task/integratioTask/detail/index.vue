<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch" style="padding-bottom:15px">
      <div class="infotop">

        <div class="infotop-title mb15">
          <!-- <div class="h2-titles" style="font-weight: 600;">[&nbsp;{{ dpModelDetail.id || '-' }}&nbsp;]&nbsp;&nbsp;{{
            dpModelDetail.modelComment ||
            '' }}</div> -->
          <div class="task-item">
            <!-- 正方形编号 -->
            <div class="task-id">
              {{ dppEtlTaskDetail.id || '-' }}
            </div>

            <!-- 名称 -->
            <div class="task-name">
              {{ dppEtlTaskDetail.name || '' }}
            </div>
          </div>
        </div>
        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建人</div>
              <div class="infotop-row-value">
                {{ dppEtlTaskDetail?.createBy || '-' }}
              </div>
            </div>
          </a-col>

          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">任务状态</div>
              <div class="infotop-row-value">
                <a-tag :type="dppEtlTaskDetail.status == '1' ? 'success' : 'error'">
                  {{ dppEtlTaskDetail.status == '1' ? "开启" : "关闭" }}
                </a-tag>
              </div>
            </div>
          </a-col>

          <a-col :span="8" style="margin: 2px 0;">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">数据集成目录</div>
              <div class="infotop-row-value">
                {{ dppEtlTaskDetail.catName || '-' }}
              </div>
            </div>
          </a-col>

          <a-col :span="8" style="margin: 2px 0;">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">调度状态</div>
              <div class="infotop-row-value">
                <a-tag :type="dppEtlTaskDetail.schedulerState == '0' ? 'success' : 'error'">
                  {{ dppEtlTaskDetail.schedulerState == '0' ? "开启" : "关闭" }}
                </a-tag>
              </div>
            </div>
          </a-col>

          <a-col :span="8" style="margin: 2px 0;">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建时间</div>
              <div class="infotop-row-value">
                {{ parseTime(dppEtlTaskDetail.createTime, '{y}-{m}-{d} {h}:{i}') }}
              </div>
            </div>
          </a-col>

          <a-col :span="24" style="margin: 2px 0;">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">描述</div>
              <div class="infotop-row-value">
                {{ dppEtlTaskDetail.description || '-' }}
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>

    <div class="pagecont-bottom">
      <a-spin :spinning="loading">
      <a-tabs v-model:activeKey="activeName" class="demo-tabs" @change="handleClick">
        <a-tab-pane tab="任务流程" key="1">
          <process ref="compRef" :dppEtlTaskDetail="dppEtlTaskDetail" />
        </a-tab-pane>
        <a-tab-pane tab="详细信息" key="2">
          <info :dppEtlTaskDetail="dppEtlTaskDetail" />
        </a-tab-pane>
        <a-tab-pane tab="运维策略" key="3">
          <opsPolicy :taskId="dppEtlTaskDetail.id" />
        </a-tab-pane>
        <a-tab-pane tab="运维事件" key="4">
          <opsEvent :taskId="dppEtlTaskDetail.id" />
        </a-tab-pane>
      </a-tabs>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { etlTask } from "@/api/col/task/index.js";
import { useRoute } from "vue-router";
import process from "@/views/col/task/integratioTask/detail/process.vue";
import info from "@/views/col/task/integratioTask/detail/info.vue";
import opsPolicy from "@/views/col/task/integratioTask/detail/opsPolicy.vue";
import opsEvent from "@/views/col/task/integratioTask/detail/opsEvent.vue";
import { onActivated, reactive, ref, toRefs, watch, getCurrentInstance } from "vue";

const { proxy } = getCurrentInstance();
const activeName = ref("1");
const showSearch = ref(true);
const route = useRoute();
let loading = ref(false);
const data = reactive({
  dppEtlTaskDetail: {},
  form: {}
});
let compRef = ref(null);
const { dppEtlTaskDetail } = toRefs(data);
function getDppEtlTaskDetailById(id) {
  if (!id) return;
  loading.value = true;
  etlTask(id).then(response => {
    dppEtlTaskDetail.value = {
      ...response.data,
      ...JSON.parse(response.data.draftJson || "{}"),
      catName: response.data.catName
    };
    compRef.value?.updateFlow(dppEtlTaskDetail.value);
    loading.value = false;

  });
}
watch(
  () => route.query.id,
  (newId) => {
    getDppEtlTaskDetailById(newId);
  },
  { immediate: true }
);
onDeactivated(() => {
  activeName.value = '1'
  dppEtlTaskDetail.value = { taskConfig: {}, name: null };

});
const handleClick = (tab, event) => {
  console.log(tab, event);
};
</script>

<style lang="less" scoped>
.pagecont-bottom {
  min-height: calc(100vh - 340px) !important;
}
</style>

