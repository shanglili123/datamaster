<template>
  <a-breadcrumb class="app-breadcrumb">
    <a-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
      <span class="crumb-item" :class="{ 'is-last': index === levelList.length - 1 }">{{ item.meta.title }}</span>
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<script setup>
const route = useRoute();
const levelList = ref([]);

function isDashboard(route) {
  const name = route && route.name;
  if (!name) return false;
  return name.toString().trim().toLowerCase() === "Index".toLowerCase();
}

function getBreadcrumb() {
  let matched = route.matched.filter((item) => item.meta && item.meta.title);
  const first = matched[0];
  if (!isDashboard(first)) {
    matched = [{ path: "/index", meta: { title: "首页" } }].concat(matched);
  }
  levelList.value = matched.filter(
    (item) =>
      item.meta &&
      item.meta.title &&
      item.meta.breadcrumb !== false
  );
}

watchEffect(() => {
  if (route.path.startsWith("/redirect/")) {
    return;
  }
  getBreadcrumb();
});
getBreadcrumb();
</script>

<style lang="scss" scoped>
.app-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 60px;
  margin-left: 8px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  vertical-align: middle;

  :deep(.ant-breadcrumb-link) {
    display: inline-flex;
    align-items: center;
  }

  .crumb-item {
    color: #606266;
    cursor: text;

    &.is-last {
      color: #97a8be;
    }
  }
}
</style>
