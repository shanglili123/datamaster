<template>
  <div class="space-station">
    <header class="space-station__header">
      <button type="button" class="space-station__brand" @click="goHome">
        <span class="space-station__brand-mark"><svg-icon icon-class="dashboard-3-line" /></span>
        <span>
          <strong>DataMaster Exploration</strong>
          <small>空间数据工作站</small>
        </span>
      </button>

      <div class="space-station__header-actions">
        <span>当前空间</span>
        <strong>{{ spaceName || "未命名空间" }}</strong>
      </div>
    </header>

    <main class="space-station__body">
      <aside class="space-station__sidebar">
        <div class="space-station__sidebar-head">
          <span>当前空间</span>
          <strong>{{ spaceName || "未命名空间" }}</strong>
        </div>

        <div v-if="menuGroups.length" class="space-station__menu-groups">
          <section v-for="group in menuGroups" :key="group.id" class="space-station__menu-group">
            <button
              type="button"
              class="space-station__menu-title"
              :class="{ 'is-collapsed': isGroupCollapsed(group.id) }"
              @click="toggleGroup(group.id)"
            >
              <svg-icon :icon-class="group.iconName" />
              <span>{{ group.title }}</span>
              <DownOutlined class="space-station__group-arrow" />
            </button>
            <template v-if="!isGroupCollapsed(group.id)">
              <button
                v-for="item in group.items"
                :key="item.id"
                type="button"
                :class="{ 'is-active': selectedItem?.id === item.id }"
                @click="selectItem(item)"
              >
                <svg-icon v-if="item.iconName" :icon-class="item.iconName" />
                <span>{{ item.title }}</span>
                <RightOutlined class="space-station__menu-arrow" />
              </button>
            </template>
          </section>
        </div>
        <a-empty v-else description="当前空间暂无可用菜单" :image-style="{ height: '72px' }" />
      </aside>

      <section class="space-station__content">
        <div class="space-station__content-head">
          <div>
            <span>{{ isOntologyShell ? "ONTOLOGY WORKSPACE" : (selectedGroup?.title || "SPACE WORKSPACE") }}</span>
            <h1>{{ isOntologyWorkspace ? "本体工作台" : (isOntologyList ? "本体管理" : (selectedItem?.title || "空间工作台")) }}</h1>
            <p>{{ currentPageDescription }}</p>
          </div>
          <div class="space-station__signal">
            <i></i>
            LIVE WORKSPACE
          </div>
        </div>

        <nav v-if="!isOntologyShell && selectedItem?.tabs?.length" class="space-station__page-tabs" aria-label="三级目录">
          <button
            v-for="tab in selectedItem.tabs"
            :key="tab.id"
            type="button"
            :class="{ 'is-active': selectedTabId === tab.id }"
            @click="selectTab(tab)"
          >
            {{ tab.title }}
          </button>
        </nav>

        <section class="space-station__module">
          <OntologyWorkspaceView v-if="isOntologyWorkspace" />
          <OntologyListView v-else-if="isOntologyList" />
          <component
            v-else-if="selectedTab?.component"
            :is="selectedTab.component"
            :key="selectedTab.id"
          />
          <a-empty v-else description="请选择左侧菜单进入模块" />
        </section>
      </section>
    </main>
  </div>
</template>

<script setup name="SpaceWorkstation">
import { computed, defineAsyncComponent, provide, reactive, ref, watch } from "vue";
import { routeLocationKey, useRoute, useRouter } from "vue-router";
import {
  DownOutlined,
  RightOutlined,
} from "@ant-design/icons-vue";
import useUserStore from "@/store/system/user";
import usePermissionStore from "@/store/system/permission";
import { parseRouteQuery } from "@/utils/routeQuery";

const OntologyWorkspaceView = defineAsyncComponent(() => import("@/views/ont/workspace/index.vue"));
const OntologyListView = defineAsyncComponent(() => import("@/views/ont/ontology/index.vue"));

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const permissionStore = usePermissionStore();

// 原页面仍然按真实菜单路由运行；这里只把选中菜单的路由上下文提供给直接挂载的页面。
const stationRoute = reactive({
  fullPath: route.fullPath || "/space",
  path: route.path || "/space",
  query: route.query || {},
  hash: "",
  name: route.name || "SpaceWorkstation",
  params: route.params || {},
  matched: [],
  meta: { fullScreen: true },
  redirectedFrom: undefined,
});
provide(routeLocationKey, stationRoute);

const isOntologyWorkspace = computed(() =>
  route.name === "OntWorkspace" || route.path.startsWith("/ont/workspace/")
);
const isOntologyList = computed(() => route.name === "OntologyList" || route.path === "/ont/ontology");
const isOntologyShell = computed(() => isOntologyWorkspace.value || isOntologyList.value);

const iconMap = {
  data: "da-database",
  database: "da-database",
  table: "table",
  message: "message",
  "mind-map": "mind-map",
  "node-tree": "node-tree",
  folder: "file-list-2-line",
  scan: "search",
  setting: "settings-line",
  tool: "tool",
  service: "server",
  skill: "skill",
  asset: "money-dollar-box-line",
  catalog: "tree-table",
};

const titleIconMap = {
  "探查任务": "list-view",
  "采集任务": "list-view",
  "探查任务日志": "log",
  "探查记录": "log",
  "探查元数据": "search",
  "元数据结果": "search",
  "数据统计": "chart",
};

const componentCache = new WeakMap();

function resolveComponent(component) {
  if (!component) return null;
  if (typeof component !== "function") return component;
  if (!componentCache.has(component)) {
    componentCache.set(component, defineAsyncComponent(component));
  }
  return componentCache.get(component);
}

const metadataFallbackComponent = resolveComponent(() => import("@/views/meta/catalog/table/index.vue"));

function normalizePath(parentPath, path) {
  if (!path) return parentPath || "/";
  if (path.startsWith("/")) return path;
  return `${parentPath || ""}/${path}`.replace(/\/+/g, "/");
}

function isHidden(route) {
  return !route || route.hidden || route.meta?.title === "首页" || route.path === "/index" || route.path === "index";
}

function collectPages(route, parentPath = "") {
  if (isHidden(route)) return [];
  const currentPath = normalizePath(parentPath, route.path);
  const children = (route.children || []).filter((child) => !isHidden(child));
  if (children.length) {
    return children.flatMap((child) => collectPages(child, currentPath) || []);
  }
  if (route.component && currentPath !== "/" && currentPath !== "/index") {
    return [{
      id: `${route.name || "page"}:${currentPath}`,
      title: route.meta?.title || route.name || currentPath,
      component: route.component,
      path: currentPath,
      query: route.query,
      name: route.name,
      meta: route.meta || {},
    }];
  }
  return [];
}

function makeItem(route, parentPath) {
  const path = normalizePath(parentPath, route.path);
  const hasChildRoutes = (route.children || []).some((child) => !isHidden(child));
  const pages = collectPages(route, parentPath).map((page) => ({
    ...page,
    component: resolveComponent(page.component),
  }));
  const page = pages[0];
  const iconName = getIconName(route, "file-list-2-line");
  return {
    id: `${route.name || "route"}:${path}`,
    title: route.meta?.title || route.name || path,
    iconName,
    component: page?.component || null,
    pagePath: page?.path || path,
    query: page?.query ?? route.query,
    routeName: page?.name || route.name,
    meta: page?.meta || route.meta || {},
    // 二级目录下的真实叶子页面作为三级 Tab，保留原菜单层级但不再挤进左侧菜单。
    tabs: hasChildRoutes && pages.length ? pages : [],
  };
}

function getIconName(route, fallback) {
  const titleIcon = titleIconMap[route?.meta?.title];
  if (titleIcon) return titleIcon;
  const rawIcon = route?.meta?.icon;
  if (rawIcon && rawIcon !== "#") {
    return iconMap[rawIcon] || rawIcon;
  }
  return iconMap[fallback] || fallback;
}

function createGroups(routes, parentPath = "") {
  const groups = [];
  (routes || []).forEach((route, index) => {
    if (isHidden(route)) return;
    const routePath = normalizePath(parentPath, route.path);
    const children = (route.children || []).filter((child) => !isHidden(child));

    if (routePath === "/" && !route.meta?.title) {
      groups.push(...createGroups(children, routePath));
      return;
    }

    const items = children.length
      ? children.map((child) => makeItem(child, routePath)).filter((item) => item.component)
      : [makeItem(route, parentPath)].filter((item) => item.component);

    if (items.length) {
      groups.push({
        id: `${route.name || "route"}:${routePath}:${index}`,
        title: route.meta?.title || route.name || routePath,
        iconName: getIconName(route, "file-list-2-line"),
        items,
      });
    }
  });
  return ensureProbeMetadataItem(groups);
}

function ensureProbeMetadataItem(groups) {
  const taskItem = (item) => {
    const title = String(item.title || "");
    const path = String(item.pagePath || "").toLowerCase();
    return title.includes("探查任务") || title.includes("采集任务") || path.includes("/task");
  };
  const metadataItem = (item) => {
    const title = String(item.title || "");
    const path = String(item.pagePath || "").toLowerCase();
    return title === "探查元数据" || title === "元数据结果" || path.includes("meta/catalog/table");
  };

  const group = groups.find((candidate) =>
    candidate.items.some(taskItem) || /数据探查|元数据管理/.test(candidate.title)
  );
  if (!group || group.items.some(metadataItem)) return groups;

  const fallbackItem = {
    id: "ProbeResult:/meta/catalog/table/index",
    title: "探查元数据",
    iconName: "search",
    component: metadataFallbackComponent,
    pagePath: "/meta/catalog/table/index",
    query: undefined,
    routeName: "ProbeResult",
    meta: { title: "探查元数据", icon: "eye-line" },
    tabs: [],
  };
  const taskIndex = group.items.findIndex(taskItem);
  group.items.splice(taskIndex >= 0 ? taskIndex + 1 : group.items.length, 0, fallbackItem);
  return groups;
}

const menuGroups = computed(() => createGroups(permissionStore.topbarRouters || []));
const selectedItem = ref(null);
const selectedTabId = ref("");
const collapsedGroups = ref(new Set());
const knownGroupIds = ref(new Set());
const selectedGroup = computed(() => menuGroups.value.find((group) => group.items.some((item) => item.id === selectedItem.value?.id)));
const selectedTab = computed(() => {
  const item = selectedItem.value;
  if (!item) return null;
  return item.tabs?.find((tab) => tab.id === selectedTabId.value) || {
    id: item.id,
    title: item.title,
    component: item.component,
    pagePath: item.pagePath,
    path: item.pagePath,
    query: item.query,
    routeName: item.routeName,
    meta: item.meta,
  };
});
const spaceName = computed(() => userStore.spaceName || "");
const currentPageDescription = computed(() => {
  if (isOntologyWorkspace.value) return "管理本体概念、属性、关系、动作编排和对象实例，构建可执行的业务语义网络。";
  if (isOntologyList.value) return "创建和维护本体模型，并进入可视化工作台完成业务语义建模。";
  return getPageDescription(selectedTab.value?.title || selectedItem.value?.title, selectedTab.value?.path || selectedItem.value?.pagePath);
});

function getPageDescription(title, path) {
  const pageTitle = String(title || "");
  const pagePath = String(path || "").toLowerCase();
  const descriptions = {
    "探查任务": "配置并运行元数据与质量探查任务，持续发现数据结构和质量变化。",
    "采集任务": "配置数据源采集范围和执行计划，将库、表、字段结构同步到元数据目录。",
    "探查任务日志": "查看探查任务的执行状态、运行日志、异常原因和历史结果。",
    "探查记录": "查看历次数据探查记录、质量结果和问题数据。",
    "探查元数据": "浏览探查得到的数据库、数据表和字段元数据，并查看结构与质量信息。",
    "元数据结果": "浏览探查得到的数据库、数据表和字段元数据，并查看结构与质量信息。",
    "数据统计": "汇总当前空间的数据库、数据表和数据行数，快速掌握数据规模。",
    "数据源管理": "维护数据源连接和认证配置，为采集、查询与服务提供数据连接。",
    "资产目录": "浏览和管理已登记的数据资产，查看资产结构、状态与治理信息。",
    "资产申请": "提交和处理数据资产使用申请，管理授权审批过程。",
    "数据查询": "基于已授权的数据资产执行安全的数据检索与结果查看。",
    "函数管理": "维护本体动作和计算逻辑可复用的函数能力。",
    "智能问数": "使用自然语言查询数据资产与本体语义，生成分析结果和业务洞察。",
    "问数": "使用自然语言查询数据资产与本体语义，生成分析结果和业务洞察。",
    "Skill管理": "维护问数知识、字段语义和分析规则，并同步给智能问数使用。",
    "Skill 管理": "维护问数知识、字段语义和分析规则，并同步给智能问数使用。",
  };
  if (descriptions[pageTitle]) return descriptions[pageTitle];
  if (pagePath.includes("statistics")) return descriptions["数据统计"];
  if (pagePath.includes("probe") && pagePath.includes("log")) return descriptions["探查任务日志"];
  if (pagePath.includes("catalog/table") || pagePath.includes("management")) return descriptions["探查元数据"];
  return `管理${pageTitle || "当前模块"}相关数据、配置和业务流程。`;
}

watch(menuGroups, (groups) => {
  // 新进入空间时一级目录默认全部收起；后续菜单刷新只为新增目录补上收起状态，保留用户手动展开的状态。
  const validGroupIds = new Set(groups.map((group) => group.id));
  const nextCollapsedGroups = new Set(
    [...collapsedGroups.value].filter((groupId) => validGroupIds.has(groupId))
  );
  groups.forEach((group) => {
    if (!knownGroupIds.value.has(group.id)) nextCollapsedGroups.add(group.id);
  });
  collapsedGroups.value = nextCollapsedGroups;
  knownGroupIds.value = validGroupIds;

  const allItems = groups.flatMap((group) => group.items);
  if (!selectedItem.value || !allItems.some((item) => item.id === selectedItem.value.id)) {
    const defaultItem = allItems.find((item) => {
      const title = String(item.title || "");
      const path = String(item.pagePath || item.path || "").toLowerCase();
      return title.includes("数据统计") || path.includes("statistics");
    }) || allItems[0] || null;
    selectItem(defaultItem, { expandGroup: false, preserveOntology: true });
  }
}, { immediate: true });

function isGroupCollapsed(groupId) {
  return collapsedGroups.value.has(groupId);
}

function toggleGroup(groupId) {
  const next = new Set(collapsedGroups.value);
  if (next.has(groupId)) next.delete(groupId);
  else next.add(groupId);
  collapsedGroups.value = next;
}

function selectItem(item, options = {}) {
  selectedItem.value = item;
  if (!item) return;
  if (isOntologyShell.value) {
    if (options.preserveOntology !== true) router.push("/space");
    return;
  }
  const group = menuGroups.value.find((candidate) => candidate.items.some((entry) => entry.id === item.id));
  if (options.expandGroup !== false && group && collapsedGroups.value.has(group.id)) {
    const next = new Set(collapsedGroups.value);
    next.delete(group.id);
    collapsedGroups.value = next;
  }
  selectedTabId.value = item.tabs?.[0]?.id || "";
  updateStationRoute(item.tabs?.[0] || item);
}

function selectTab(tab) {
  if (!selectedItem.value || !tab) return;
  selectedTabId.value = tab.id;
  updateStationRoute(tab);
}

function updateStationRoute(page) {
  const pagePath = page.pagePath || page.path;
  const query = parseRouteQuery(page.query) || {};
  stationRoute.path = pagePath;
  const queryString = Object.entries(query)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value == null ? "" : value)}`)
    .join("&");
  stationRoute.fullPath = queryString ? `${pagePath}?${queryString}` : pagePath;
  stationRoute.query = query;
  stationRoute.name = page.routeName || page.name;
  stationRoute.meta = page.meta || {};
}

function goHome() {
  router.push("/index");
}
</script>

<style lang="scss" scoped>
.space-station {
  min-height: 100%;
  margin: -16px;
  color: #15213b;
  background: #f4f7fb;
}

.space-station__header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(240px, 1fr) minmax(240px, 1fr);
  align-items: center;
  min-height: 76px;
  padding: 0 28px;
  color: #15213b;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e1e7f0;
  box-shadow: 0 8px 26px rgba(34, 54, 91, 0.08);
  backdrop-filter: blur(14px);
}

.space-station__brand { display: inline-flex; align-items: center; gap: 10px; padding: 0; color: inherit; text-align: left; background: none; border: 0; cursor: pointer; }
.space-station__brand strong, .space-station__brand small { display: block; }
.space-station__brand strong { color: #1b2944; font-size: 14px; letter-spacing: 0.02em; }
.space-station__brand small { margin-top: 2px; color: #8290aa; font-size: 11px; }
.space-station__brand-mark { display: grid; width: 38px; height: 38px; place-items: center; color: #fff; font-size: 19px; background: linear-gradient(135deg, #5b6df6, #8d5ce5); border-radius: 11px; box-shadow: 0 8px 22px rgba(91, 109, 246, 0.35); }
.space-station__header-actions { display: flex; align-items: baseline; justify-content: flex-end; gap: 9px; color: #8a96aa; font-size: 11px; }
.space-station__header-actions strong { color: #273651; font-size: 15px; }

.space-station__body { display: grid; grid-template-columns: 252px minmax(0, 1fr); min-height: calc(100vh - 76px); }
.space-station__sidebar { position: sticky; top: 76px; display: flex; flex-direction: column; height: calc(100vh - 76px); padding: 22px 16px; overflow-y: auto; background: #fff; border-right: 1px solid #e1e7f0; }
.space-station__sidebar-head { padding: 0 8px 18px; border-bottom: 1px solid #edf0f5; }
.space-station__sidebar-head span, .space-station__sidebar-head strong, .space-station__sidebar-head small { display: block; }
.space-station__sidebar-head span { color: #8995a9; font-size: 10px; letter-spacing: .1em; }
.space-station__sidebar-head strong { margin-top: 6px; color: #202d47; font-size: 18px; }
.space-station__sidebar-head small { margin-top: 4px; color: #9aa5b7; font-size: 11px; }
.space-station__menu-groups { margin-top: 16px; }
.space-station__menu-group + .space-station__menu-group { margin-top: 18px; }
.space-station__menu-title { display: flex; align-items: center; gap: 9px; width: 100%; min-height: 40px; margin: 0 0 4px; padding: 10px; color: #66748d; font-size: 12px; font-weight: 700; letter-spacing: .04em; line-height: 18px; text-align: left; background: transparent; border: 1px solid transparent; border-radius: 9px; cursor: pointer; transition: .2s ease; }
.space-station__menu-title:hover { color: #5368dc; background: #f5f7ff; }
.space-station__menu-title :deep(svg) { color: #6178e8; font-size: 14px; }
.space-station__group-arrow { margin-left: auto; color: #9aa7ba !important; font-size: 11px !important; transition: transform .2s ease; }
.space-station__menu-title.is-collapsed .space-station__group-arrow { transform: rotate(-90deg); }
.space-station__menu-group > button:not(.space-station__menu-title) { position: relative; display: flex; align-items: center; gap: 9px; width: 100%; padding: 10px 10px; color: #68758d; font-size: 12px; text-align: left; background: transparent; border: 1px solid transparent; border-radius: 9px; cursor: pointer; transition: .2s ease; }
.space-station__menu-group > button:not(.space-station__menu-title):hover { color: #425ad2; background: #f5f7ff; transform: translateX(2px); }
.space-station__menu-group > button:not(.space-station__menu-title).is-active { color: #4058d4; font-weight: 600; background: #edf0ff; border-color: #e0e5ff; box-shadow: inset 3px 0 #6076e9; }
.space-station__menu-group > button:not(.space-station__menu-title) :deep(svg) { color: currentColor; font-size: 15px; }
.space-station__menu-arrow { margin-left: auto; color: #b6bfd0 !important; font-size: 10px !important; }

.space-station__content { min-width: 0; padding: 24px 28px 32px; }
.space-station__content-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin: 13px 0 16px; }
.space-station__content-head > div > span { color: #71809b; font-size: 10px; font-weight: 700; letter-spacing: .14em; }
.space-station__content-head h1 { margin: 5px 0 3px; color: #182540; font-size: 24px; }
.space-station__content-head p { margin: 0; color: #78849a; font-size: 13px; }
.space-station__signal { display: inline-flex; align-items: center; gap: 8px; padding: 8px 11px; color: #438b80; font-size: 10px; font-weight: 700; letter-spacing: .08em; background: #e8f8f4; border: 1px solid #c8eee6; border-radius: 999px; }
.space-station__signal i { width: 6px; height: 6px; background: #49bda7; border-radius: 50%; box-shadow: 0 0 0 4px rgba(73,189,167,.12); }
.space-station__page-tabs { display: flex; align-items: center; gap: 5px; min-height: 46px; margin-bottom: 12px; padding: 5px; overflow-x: auto; background: #fff; border: 1px solid #e0e7f0; border-radius: 11px; box-shadow: 0 5px 16px rgba(28,45,81,.04); }
.space-station__page-tabs button { flex: 0 0 auto; padding: 8px 15px; color: #71809a; font-size: 12px; white-space: nowrap; background: transparent; border: 1px solid transparent; border-radius: 8px; cursor: pointer; transition: .2s ease; }
.space-station__page-tabs button:hover { color: #425ad2; background: #f5f7ff; }
.space-station__page-tabs button.is-active { color: #4058d4; font-weight: 600; background: #edf0ff; border-color: #dce3ff; box-shadow: 0 3px 8px rgba(65,88,212,.08); }
.space-station__module { min-height: calc(100vh - 188px); overflow: auto; background: #fff; border: 1px solid #e0e7f0; border-radius: 16px; box-shadow: 0 12px 35px rgba(28,45,81,.07); }
.space-station__module :deep(.app-container) { min-height: calc(100vh - 190px); }
.space-station__module :deep(.ask-data-page) { height: calc(100vh - 190px); min-height: 560px; border-radius: 16px; }

@media (max-width: 900px) {
  .space-station__header { display: flex; flex-wrap: wrap; gap: 10px; padding: 12px 16px; }
  .space-station__header-actions { margin-left: auto; }
  .space-station__body { display: block; }
  .space-station__sidebar { position: relative; top: 0; height: auto; padding: 14px 16px; border-right: 0; border-bottom: 1px solid #e1e7f0; }
  .space-station__menu-groups { display: flex; gap: 16px; overflow-x: auto; }
  .space-station__menu-group { flex: 0 0 auto; min-width: 190px; }
  .space-station__menu-group + .space-station__menu-group { margin-top: 0; }
  .space-station__content { padding: 20px 16px 28px; }
}
</style>
