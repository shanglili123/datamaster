<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch" style="padding-bottom: 15px">
      <div class="infotop">
        <div class="infotop-title mb15">

          <div class="task-item">
            <!-- 正方形编号 -->
            <div class="task-id">
              {{ dpModelDetail.id || '-' }}
            </div>

            <!-- 名称 -->
            <div class="task-name">
              {{ dpModelDetail.modelComment || '' }}
            </div>
          </div>
        </div>
        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">英文名称</div>
              <div class="infotop-row-value">
                {{ dpModelDetail.modelName || "-" }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建方式</div>
              <div class="infotop-row-value">
                <dict-tag :options="dp_model_create_type" :value="dpModelDetail.createType" />
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">状态</div>
              <div class="infotop-row-value">
                <dict-tag :options="dp_model_status" :value="dpModelDetail.status" />
              </div>
            </div>
          </a-col>
          <a-col :span="24" style="margin: 2px 0;">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">描述</div>
              <div class="infotop-row-value">
                {{ dpModelDetail.description || "-" }}
              </div>
            </div>
          </a-col>
          <a-col :span="24">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">备注</div>
              <div class="infotop-row-value">
                {{ dpModelDetail.remark || "-" }}
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>

    <div class="pagecont-bottom">
      <a-tabs v-model:activeKey="activeName" class="demo-tabs" @change="handleClick">
        <a-tab-pane :tab="'属性字段'" key="1">
          <modelColumn />
        </a-tab-pane>
        <a-tab-pane :tab="'逻辑物化'" key="2">
          <modelMaterialized :modelId="route.query.id" :row="dpModelDetail"></modelMaterialized>
        </a-tab-pane>
        <a-tab-pane :tab="'详细信息'" key="3">
          <info :daDiscoveryTaskDetail="dpModelDetail"></info>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup name="StandardsModel">
import { getDpModel } from "@/api/std/model/model";
import { useRoute } from "vue-router";
import { deptUserTree } from "@/api/system/system/user.js";
import modelColumn from "@/views/std/model/detail/modelColumn.vue";
import modelMaterialized from "@/views/std/model/detail/materializationLog.vue";
import info from "@/views/std/model/detail/info.vue";
const { proxy } = getCurrentInstance();
const { dp_model_status, dp_model_create_type } = proxy.useDict(
  "dp_model_status",
  "dp_model_create_type"
);

const activeName = ref("1");
const getNickNameById = (userId) => {
  if (!userList.value || !Array.isArray(userList.value)) {
    return null;
  }

  if (!userId) return null;

  const user = userList.value.find((user) => user.userId == userId);
  return user ? user.nickName : null;
};

const handleClick = (tab, event) => {
  console.log(tab, event);
};

const showSearch = ref(true);
const route = useRoute();
let id = route.query.id || 1;
// 监听 id 变化
watch(
  () => route.query.id,
  (newId) => {
    id = newId || -1;
    activeName.value = "1";
    getDpModelDetailById();
  },
  { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);
const data = reactive({
  dpModelDetail: {},
  form: {},
});

const { dpModelDetail, rules } = toRefs(data);
const userList = ref();
/** 复杂详情页面上方表单查询 */
function getDpModelDetailById() {
  const _ID = id;
  if (_ID == -1) {
    return;
  }
  getDpModel(_ID).then((response) => {
    dpModelDetail.value = response.data;
    console.log("🚀 ~ getDpModel ~ response.data:", response.data);
  });
  deptUserTree().then((res) => {
    userList.value = res.data;
    console.log("userList", userList.value);
  });
}

getDpModelDetailById();
</script>

<style scoped lang="scss">
.app-container {
  margin: 15px 15px 0px 15px;

  .pagecont-bottom {
    min-height: calc(100vh - 345px) !important;
  }
}
</style>

