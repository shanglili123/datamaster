<template>
  <a-layout class="app-wrapper">
    <a-drawer
      v-if="device === 'mobile'"
      :open="sidebar.opened"
      placement="left"
      :width="248"
      :closable="false"
      @close="handleClickOutside"
    >
      <sidebar />
    </a-drawer>
    <a-layout-sider
      v-if="!sidebarHide && device !== 'mobile'"
      :width="248"
      :collapsed="!sidebar.opened"
      :collapsed-width="72"
      :trigger="null"
      collapsible
      class="sidebar-container"
      :style="{ background: sideTheme === 'theme-dark' ? '#001529' : '#fff' }"
    >
      <sidebar />
    </a-layout-sider>
    <a-layout>
      <div :class="{ 'fixed-header': fixedHeader }" class="layout-header-wrapper">
        <navbar @setLayout="setLayout" />
        <tags-view v-if="needTagsView" />
      </div>
      <a-layout-content class="main-container" :class="{ 'sidebarHide': sidebarHide }">
        <sub-menu-tabs v-if="!sidebarHide" />
        <app-main />
      </a-layout-content>
      <settings ref="settingRef" />
    </a-layout>
  </a-layout>
</template>

<script setup>
import { useRoute } from "vue-router";
import { useWindowSize } from "@vueuse/core";
import Sidebar from "./components/Sidebar/index.vue";
import SubMenuTabs from "./components/SubMenuTabs/index.vue";
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
    permissionStore.setSidebarRouters(sidebarRoutes);
    appStore.toggleSideBarHide(false);
    if (!appStore.sidebar.opened) {
      appStore.toggleSideBar(false);
    }
  },
  { immediate: true, deep: true }
);

const sidebarHide = computed(() => {
  const path = route.path;
  if (path === "/index") return true;
  const navbarLogoRoutes = defaultSettings.navbarLogoRoutes || [];
  if (navbarLogoRoutes.some((p) => path.startsWith(p))) return true;
  if (route.matched.length > 1) return false;
  return true;
});

const { width, height } = useWindowSize();
const WIDTH = 992;

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

  const currentL2 = findCurrentL2(topRoute, route.path);
  if (!currentL2 || !currentL2.children || currentL2.children.length === 0) return [];

  const l2Path = `${normalizePath(topRoute.path)}/${currentL2.path}`.replace(/\/+/g, "/");
  return currentL2.children.map((child) => {
    const routeItem = JSON.parse(JSON.stringify(child));
    routeItem.parentPath = l2Path;
    if (routeItem.path && !routeItem.path.startsWith("/") && !/^https?:/i.test(routeItem.path)) {
      routeItem.path = `${l2Path}/${routeItem.path}`.replace(/\/+/g, "/");
    }
    return routeItem;
  });
}

function findCurrentL2(topRoute, currentPath) {
  const topPath = normalizePath(topRoute.path);
  for (const child of topRoute.children || []) {
    if (child.hidden) continue;
    const childResolved = child.path
      ? (child.path.startsWith('/') ? child.path : `${topPath}/${child.path}`)
      : '';
    if (currentPath === childResolved || currentPath.startsWith(childResolved + '/')) {
      return child;
    }
  }
  return null;
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
  height: 100vh;
}

.sidebar-container {
  overflow: auto;
  border-right: 1px solid #f0f0f0;
}

.layout-header-wrapper {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
}

.main-container {
  background-color: var(--dm-bg-layout, #eef3f8);
  min-height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
