<template>
  <div v-if="tabs.length >= 2" class="submenu-tabs-wrapper">
    <a-menu
      mode="horizontal"
      class="submenu-tabs"
      :selected-keys="activeKeys"
      @click="handleTabClick"
    >
      <a-menu-item v-for="tab in tabs" :key="tab.routePath">
        {{ tab.meta && tab.meta.title }}
      </a-menu-item>
    </a-menu>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import usePermissionStore from "@/store/system/permission";
import { isHttp } from "@/utils/validate";
import { getNormalPath } from "@/utils/anivia";
import { parseRouteQuery } from "@/utils/routeQuery";

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();

// 给菜单树补充解析后的完整路径与可见子菜单（与 Sidebar 一致）
function decorateRoutes(routes, parentPath = "") {
  return (routes || [])
    .filter((r) => !r.hidden)
    .map((r) => {
      const routePath = resolveMenuPath(r.path, parentPath);
      const visibleChildren = decorateRoutes(r.children, routePath);
      return { ...r, routePath, visibleChildren };
    });
}

function resolveMenuPath(path, parentPath) {
  if (!path) return "";
  if (isHttp(path)) return path;
  if (path.startsWith("/")) return path;
  return getNormalPath(parentPath + "/" + path);
}

const decoratedRoutes = computed(() =>
  decorateRoutes(permissionStore.topbarRouters || [])
);

// 当前路径所属的二级菜单（其 children 即顶部页签）
const currentL2 = computed(() => {
  const path = route.path;
  if (!path || path === "/index" || path === "/") return null;
  for (const top of decoratedRoutes.value) {
    for (const child of top.visibleChildren || []) {
      if (path === child.routePath || path.startsWith(child.routePath + "/")) {
        return child;
      }
    }
  }
  return null;
});

const tabs = computed(() => {
  const l2 = currentL2.value;
  if (!l2) return [];
  return l2.visibleChildren || [];
});

const activeKeys = computed(() => {
  const path = route.path;
  const hit = tabs.value.find(
    (tab) => path === tab.routePath || path.startsWith(tab.routePath + "/")
  );
  return hit ? [hit.routePath] : [];
});

function handleTabClick({ key }) {
  if (isHttp(key)) {
    window.open(key, "_blank");
    return;
  }
  const menu = tabs.value.find((tab) => tab.routePath === key);
  if (menu && menu.query) {
    const query = parseRouteQuery(menu.query);
    router.push(query ? { path: key, query } : { path: key });
  } else {
    router.push({ path: key });
  }
}
</script>

<style lang="scss" scoped>
.submenu-tabs-wrapper {
  margin: 16px 16px 0 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8edf5;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
  overflow: hidden;
}

.submenu-tabs {
  border-inline-end: none !important;
  background: transparent;
  line-height: 44px;

  :deep(.ant-menu-item) {
    font-size: 14px;
  }

  :deep(.ant-menu-item-selected) {
    font-weight: 600;
  }
}
</style>
