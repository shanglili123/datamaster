<template>
  <a-menu
    v-if="sidebarRouters.length > 0"
    mode="horizontal"
    :selected-keys="[activeKey]"
    @click="handleSelect"
    class="second-level-menu"
    :style="{ borderBottom: 'none', flex: 1, minWidth: 0 }"
  >
    <a-menu-item v-for="item in visibleMenus" :key="item.path">
      <template #icon v-if="item.meta && item.meta.icon && item.meta.icon !== '#'">
        <svg-icon :icon-class="item.meta.icon" />
      </template>
      {{ item.meta && item.meta.title }}
    </a-menu-item>
    <a-sub-menu v-if="overflowMenus.length > 0" key="__more" title="更多">
      <a-menu-item v-for="item in overflowMenus" :key="item.path">
        <template #icon v-if="item.meta && item.meta.icon && item.meta.icon !== '#'">
          <svg-icon :icon-class="item.meta.icon" />
        </template>
        {{ item.meta && item.meta.title }}
      </a-menu-item>
    </a-sub-menu>
  </a-menu>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import usePermissionStore from '@/store/system/permission'
import useAppStore from '@/store/system/app'
import { isHttp } from '@/utils/validate'
import { getNormalPath } from '@/utils/anivia'
import { parseRouteQuery } from '@/utils/routeQuery'
import { ref, computed } from 'vue'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

const sidebarRouters = computed(() => permissionStore.sidebarRouters || [])

const activeKey = computed(() => {
  const path = route.path
  const meta = route.meta
  if (meta && meta.activeMenu) return meta.activeMenu
  return path
})

const visibleMenus = ref([])
const overflowMenus = ref([])

function calculateVisibleMenus() {
  const all = sidebarRouters.value.filter(item => !item.hidden)
  visibleMenus.value = all
  overflowMenus.value = []
}

// 空间切换/路由重建时 sidebarRouters 会被整体替换（长度可能不变），
// 必须监听内容而非 length，否则二级菜单不刷新
watch(() => sidebarRouters.value, () => {
  nextTick(() => calculateVisibleMenus())
}, { deep: true, immediate: true })

function handleSelect({ key }) {
  const route = sidebarRouters.value.find(item => item.path === key)
  if (!route) {
    router.push({ path: key })
    return
  }
  if (isHttp(key)) {
    window.open(key, '_blank')
    return
  }
  if (route.children && route.children.length > 0) {
    router.push({ path: key })
  } else if (route.query) {
    const query = parseRouteQuery(route.query)
    router.push(query ? { path: key, query } : { path: key })
  } else {
    router.push({ path: key })
  }
}
</script>

<style lang="scss" scoped>
.second-level-menu {
  flex: 1;
  min-width: 0;
  line-height: 60px;
  height: 60px;
}
</style>
