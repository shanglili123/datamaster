
<template>
  <div :class="classObj" class="app-wrapper">
    <div
      v-if="device === 'mobile' && sidebar.opened"
      class="drawer-bg"
      @click="handleClickOutside"
    />
    <sidebar v-if="!sidebarHide" class="sidebar-container" />
    <div
      :class="{ hasTagsView: needTagsView, sidebarHide: sidebarHide }"
      class="main-container"
    >
      <div :class="{ 'fixed-header': fixedHeader }">
        <navbar @setLayout="setLayout" />
        <tags-view v-if="needTagsView" />
      </div>
      <app-main />
      <settings ref="settingRef" />
    </div>
  </div>
</template>

<script setup>
import { useRoute } from "vue-router";
import { useWindowSize } from "@vueuse/core";
import Sidebar from "./components/Sidebar/index.vue";
import { AppMain, Navbar, Settings, TagsView } from "./components";
import defaultSettings from "@/settings";

import useAppStore from "@/store/system/app";
import useSettingsStore from "@/store/system/settings";
import usePermissionStore from "@/store/system/permission";

const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const appStore = useAppStore();
const route = useRoute();
const theme = computed(() => settingsStore.theme);
const sideTheme = computed(() => settingsStore.sideTheme);
const sidebar = computed(() => appStore.sidebar);
const device = computed(() => useAppStore().device);
const needTagsView = computed(() => settingsStore.tagsView);
const fixedHeader = computed(() => settingsStore.fixedHeader);

watch(
  [() => route.path, () => permissionStore.topbarRouters],
  () => {
    const sidebarRoutes = getSidebarRoutesForCurrentTopMenu();
    if (sidebarRoutes.length > 0) {
      permissionStore.setSidebarRouters(sidebarRoutes);
      appStore.toggleSideBarHide(false);
      if (!appStore.sidebar.opened) {
        appStore.toggleSideBar(false);
      }
    }
  },
  { immediate: true, deep: true }
);

// 是否隐藏侧边栏：防止首次加载闪烁
const sidebarHide = computed(() => {
  const path = route.path;
  // 1. 如果是明确不需要侧边栏的页面（如配置中的 Logo 路由），直接隐藏
  const navbarLogoRoutes = defaultSettings.navbarLogoRoutes || [];
  if (navbarLogoRoutes.some((p) => path.startsWith(p))) return true;
  // 2. 如果已经有菜单数据了，按数据来
  if (permissionStore.sidebarRouters.length > 0) return false;
  // 3. 如果当前路由不是首页且有二级匹配，先假设有侧边栏，防止初始渲染时 v-if 销毁组件
  if (path !== "/index" && route.matched.length > 1) return false;

  return true;
});

const classObj = computed(() => ({
  hideSidebar: !sidebar.value.opened,
  openSidebar: sidebar.value.opened,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === "mobile",
}));

const { width, height } = useWindowSize();
const WIDTH = 992; // refer to Bootstrap's responsive design

watch(
  () => device.value,
  () => {
    if (device.value === "mobile" && sidebar.value.opened) {
      useAppStore().closeSideBar({ withoutAnimation: false });
    }
  }
);

watchEffect(() => {
  if (width.value - 1 < WIDTH) {
    useAppStore().toggleDevice("mobile");
    useAppStore().closeSideBar({ withoutAnimation: true });
  } else {
    useAppStore().toggleDevice("desktop");
  }
});

function handleClickOutside() {
  useAppStore().closeSideBar({ withoutAnimation: false });
}

const settingRef = ref(null);
function setLayout() {
  settingRef.value.openSetting();
}

function getSidebarRoutesForCurrentTopMenu() {
  const topPath = getTopPath(route.path);
  if (!topPath) return [];
  const topRoute = (permissionStore.topbarRouters || []).find((item) => normalizePath(item.path) === topPath);
  if (!topRoute || !topRoute.children || topRoute.children.length === 0) return [];
  return topRoute.children.map((child) => {
    const routeItem = JSON.parse(JSON.stringify(child));
    routeItem.parentPath = normalizePath(topRoute.path);
    if (routeItem.path && !routeItem.path.startsWith("/") && !/^https?:/i.test(routeItem.path)) {
      routeItem.path = `${normalizePath(topRoute.path)}/${routeItem.path}`.replace(/\/+/g, "/");
    }
    return routeItem;
  });
}

function getTopPath(path) {
  if (!path || path === "/" || path === "/index") return "";
  const segments = path.split("/").filter(Boolean);
  return segments.length > 0 ? `/${segments[0]}` : "";
}

function normalizePath(path) {
  if (!path) return "";
  return path.startsWith("/") ? path : `/${path}`;
}
</script>

<style lang="scss" scoped>
@import "@/assets/system/styles/mixin.scss";
@import "@/assets/system/styles/variables.module.scss";

.app-wrapper {
  @include clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - #{$base-sidebar-width});
  transition: width 0.28s;
}

.hideSidebar .fixed-header {
  width: calc(100% - 72px);
}

.sidebarHide .fixed-header {
  width: 100%;
}

.mobile .fixed-header {
  width: 100%;
}
</style>

