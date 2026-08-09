<template>
  <a-breadcrumb class="app-breadcrumb">
    <a-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
      <span v-if="item.redirect === 'noRedirect' || index == levelList.length - 1" class="no-redirect">{{ item.meta.title }}</span>
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<script setup>
const route = useRoute();
const router = useRouter();
const levelList = ref([])

function getBreadcrumb() {
  let matched = route.matched.filter(item => item.meta && item.meta.title);
  levelList.value = matched.filter(item => item.meta && item.meta.title && item.meta.breadcrumb !== false)
}

watchEffect(() => {
  if (route.path.startsWith('/redirect/')) {
    return
  }
  getBreadcrumb()
})
getBreadcrumb();
</script>

<style lang='scss' scoped>
.app-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 60px;
  margin-left: 8px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }
}
</style>
