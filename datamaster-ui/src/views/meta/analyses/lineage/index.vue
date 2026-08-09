<template>
  <div class="app-container">

    <a-layout style="flex-direction: row">
      <DeptTree
        ref="DeptTreeRef"
        v-bind="store.dept"
        placeholder="请输入业务域名称"
      />
      <a-layout-content class="pagecont-bottom">
        <a-spin :spinning="!store.shape.loaded">
          <div class="shape-content">
            <LineageShape
              :origins="store.shape.origins"
              :target="store.shape.target"
              :type="store.shape.type"
              v-if="store.shape.loaded"
            />
          </div>
        </a-spin>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script setup name="MetaAnalysesLineage">
import DeptTree from "@/components/DeptTree/index.vue";
import LineageShape from "@/views/meta/components/LineageShape.vue";
import { getCurrentInstance, reactive } from "vue";
import { listDomain } from "@/api/tax/domain/domain.js";
import { tableDataRes } from "@/views/meta/components/data";

const { proxy } = getCurrentInstance();

const store = reactive({
  dept: {
    leftWidth: 300,
    deptOptions: [],
    defaultExpand: true,
  },
  shape: {
    loaded: false,
    origins: [],
    target: {},
    type: "Table",
  },
});

function getList() {
  store.shape.loaded = false;
  setTimeout(() => {
    store.shape.target = tableDataRes.origin;
    store.shape.origins = tableDataRes.targets;
    store.shape.loaded = true;
    store.shape.type = "Table";
  }, 1000);
}

// 获取业务域列表
function getDomains() {
  listDomain().then((res) => {
    store.dept.deptOptions.splice(0, store.dept.deptOptions.length);
    const domains = {
      id: 0,
      code: 0,
      name: "顶级节点",
      children: proxy.handleTree(res.data, "id", "parentId"),
    };
    store.dept.deptOptions.push(domains);
  });
}

getList();
// getDomains();
</script>

<style lang="scss" scoped>
.pagecont-bottom {
  overflow: hidden;
  position: relative;
  .demo-actions {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 9999;
  }
  .shape-content {
    width: 100%;
    height: 100%;
  }
  :deep(.ant-spin-nested-loading),
  :deep(.ant-spin-container) {
    width: 100%;
    height: 100%;
  }
}
</style>

