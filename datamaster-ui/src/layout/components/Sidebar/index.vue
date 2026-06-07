<template>
  <div :class="{ 'has-logo': showLogo }" :style="{ backgroundColor: sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar :class="sideTheme" wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
        :text-color="sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
        :unique-opened="true"
        :active-text-color="/*系统配置 theme*/ '#fff'"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          :style="{ '--bgColor': theme}"
          v-for="(route, index) in sidebarRouters"
          :key="route.path + index"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import Logo from './Logo'
import SidebarItem from './SidebarItem'
import variables from '@/assets/system/styles/variables.module.scss'
import useAppStore from '@/store/system/app'
import useSettingsStore from '@/store/system/settings'
import usePermissionStore from '@/store/system/permission'
import { getNormalPath } from '@/utils/anivia'

const route = useRoute();
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()

const sidebarRouters =  computed(() => permissionStore.sidebarRouters);
const showLogo = computed(() => settingsStore.sidebarLogo);
const sideTheme = computed(() => settingsStore.sideTheme);
const theme = computed(() => settingsStore.theme);
const isCollapse = computed(() => !appStore.sidebar.opened);

const activeMenu = computed(() => {
  const { meta, path } = route;
  if (meta.activeMenu) {
    const sidebarPath = findSidebarActivePath(sidebarRouters.value, meta.activeMenu);
    if (sidebarPath) return sidebarPath;
    return meta.activeMenu;
  }
  return path;
})

function findSidebarActivePath(routes, activeMenuPath) {
  const parts = activeMenuPath.split('/').filter(Boolean);
  if (parts.length < 2) return null;
  const trailingSegments = parts.slice(1);
  for (const route of routes) {
    const result = traverseSidebarRoute(route, '', trailingSegments);
    if (result) return result;
  }
  return null;
}

function traverseSidebarRoute(route, parentPath, targetSegments) {
  const routePath = route.path || '';
  const resolved = routePath.startsWith('/')
    ? routePath
    : parentPath ? getNormalPath(parentPath + '/' + routePath) : routePath;
  const resolvedParts = resolved.split('/').filter(Boolean);
  if (resolvedParts.length >= targetSegments.length) {
    const trailing = resolvedParts.slice(-targetSegments.length);
    if (trailing.every((p, i) => p === targetSegments[i])) {
      return resolved;
    }
  }
  if (route.children) {
    for (const child of route.children) {
      const result = traverseSidebarRoute(child, resolved, targetSegments);
      if (result) return result;
    }
  }
  return null;
}

</script>

<style lang="scss" scoped>
/* 子菜单颜色 */
.theme-dark {
  ::v-deep .nest-menu li {
    background-color: #0C2135 !important;
  }
}

/* 选中子菜单颜色 */
.theme-dark {
  ::v-deep div .nest-menu li.is-active {
    background-color: var(--bgColor) !important;
  }
}
</style>

