<template>
  <div class="space-station">
    <header class="space-station__header">
      <button type="button" class="space-station__brand" @click="goHome">
        <span class="space-station__brand-mark"><svg-icon icon-class="dashboard-3-line" /></span>
        <span><strong>DataMaster Exploration</strong><small>{{ isSpaceWorkspace ? '空间数据工作站' : '管理工作站' }}</small></span>
      </button>
      <div class="space-station__header-actions">
        <template v-if="isSpaceWorkspace">
          <span>当前空间</span>
          <a-select v-if="spaces.length" :value="userStore.spaceId ? String(userStore.spaceId) : undefined" :loading="spacesLoading" :options="spaceOptions" placeholder="选择空间" @change="switchSpace" />
          <strong v-else>{{ userStore.spaceName || '未命名空间' }}</strong>
        </template>
        <a-button type="text" @click="goHome">返回首页</a-button>
        <a-dropdown :trigger="['click']">
          <button type="button" class="space-station__user"><span>{{ userStore.nickName || userStore.name || '用户' }}</span><DownOutlined /></button>
          <template #overlay>
            <a-menu @click="handleUserCommand">
              <a-menu-item key="profile">个人中心</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </header>

    <main class="space-station__body">
      <aside class="space-station__sidebar">
        <div class="space-station__sidebar-head"><span>{{ isSpaceWorkspace ? '当前空间' : '平台管理' }}</span><strong>{{ isSpaceWorkspace ? (userStore.spaceName || '未命名空间') : '管理工作站' }}</strong></div>
        <div v-if="menuGroups.length" class="space-station__menu-groups">
          <section v-for="group in menuGroups" :key="group.id" class="space-station__menu-group">
            <button type="button" class="space-station__menu-title" :class="{ 'is-collapsed': collapsedGroups.has(group.id) }" :aria-expanded="!collapsedGroups.has(group.id)" @click="toggleGroup(group.id)">
              <svg-icon :icon-class="group.iconName" /><span>{{ group.title }}</span><DownOutlined class="space-station__group-arrow" />
            </button>
            <template v-if="!collapsedGroups.has(group.id)">
              <template v-for="item in group.items" :key="item.id">
                <button type="button" class="space-station__menu-item" :class="{ 'is-active': selectedItem?.id === item.id }" @click="selectItem(item)">
                  <svg-icon :icon-class="item.iconName" /><span>{{ item.title }}</span><RightOutlined class="space-station__menu-arrow" />
                </button>
                <div v-if="item.pages.length > 1" class="space-station__subpages">
                  <button v-for="page in item.pages" :key="page.path" type="button" :class="{ 'is-active': selectedPage?.path === page.path }" @click="navigateToPage(page)">{{ page.title }}</button>
                </div>
              </template>
            </template>
          </section>
        </div>
        <a-empty v-else description="暂无可用菜单" :image-style="{ height: '72px' }" />
      </aside>

      <section class="space-station__content">
        <div class="space-station__intro">
          <p :title="pageDescription">{{ pageDescription }}</p>
          <a-button v-if="isDetailPage" type="text" size="small" @click="goBack"><template #icon><LeftOutlined /></template>返回{{ selectedItem?.title || '列表' }}</a-button>
        </div>
        <section class="space-station__module"><slot /></section>
      </section>
    </main>
  </div>
</template>

<script setup name="SpaceWorkstationLayout">
import { computed, onMounted, provide, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { DownOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue';
import { Modal, message } from 'ant-design-vue';
import useUserStore from '@/store/system/user';
import usePermissionStore from '@/store/system/permission';
import { currentUser } from '@/api/tax/space/space';
import { isSpaceModuleRoute } from '@/utils/moduleRoute';
import { parseRouteQuery } from '@/utils/routeQuery';
import { checkPermi } from '@/utils/permission';
import { normalizeDataQueryTarget, resolveDataQueryPath } from '@/utils/dataQueryNavigation';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const spaces = ref([]);
const spacesLoading = ref(false);
const collapsedGroups = ref(new Set());
const knownGroups = ref(new Set());
const isSpaceWorkspace = computed(() => isSpaceModuleRoute(route.path));
const spaceOptions = computed(() => spaces.value.map((space) => ({ value: String(space.id), label: space.name || space.spaceName || space.code || space.spaceCode })));
const menuSource = computed(() => isSpaceWorkspace.value ? permissionStore.topbarRouters : permissionStore.homeRoutes);

const iconMap = {
  data: 'da-database', database: 'da-database', table: 'table', message: 'message',
  'mind-map': 'mind-map', 'node-tree': 'node-tree', folder: 'file-list-2-line', scan: 'search',
  setting: 'settings-line', tool: 'tool', service: 'server', skill: 'skill', asset: 'money-dollar-box-line', catalog: 'tree-table',
};

function joinPath(parent, path) {
  if (/^https?:\/\//i.test(path || '')) return path;
  if (path?.startsWith('/')) return path;
  return `${parent || ''}/${path || ''}`.replace(/\/+/g, '/');
}

function hidden(record) {
  return !record || record.hidden || record.meta?.hidden || record.path === '/index' || record.path === 'index' || record.meta?.title === '首页';
}

function collectPages(record, parent = '') {
  if (hidden(record)) return [];
  const path = joinPath(parent, record.path);
  const children = (record.children || []).filter((child) => !hidden(child));
  if (children.length) return children.flatMap((child) => collectPages(child, path));
  if (!record.component || path === '/' || /\/:/.test(path)) return [];
  return [{ path, title: record.meta?.title || record.name || path, query: parseRouteQuery(record.query), name: record.name, meta: record.meta || {} }];
}

function createItem(record, parent) {
  const pages = collectPages(record, parent);
  if (!pages.length) return null;
  return {
    id: `${record.name || 'page'}:${joinPath(parent, record.path)}`,
    title: record.meta?.title || record.name || pages[0].title,
    iconName: iconMap[record.meta?.icon] || record.meta?.icon || 'file-list-2-line',
    pages,
  };
}

function createGroups(records, parent = '') {
  return (records || []).flatMap((record) => {
    if (hidden(record)) return [];
    const path = joinPath(parent, record.path);
    const children = (record.children || []).filter((child) => !hidden(child));
    if (path === '/' && !record.meta?.title) return createGroups(children, path);
    if (!isSpaceWorkspace.value && isSpaceModuleRoute(path)) return [];
    const items = (children.length ? children.map((child) => createItem(child, path)) : [createItem(record, parent)]).filter(Boolean);
    return items.length ? [{ id: `${record.name || 'group'}:${path}`, title: record.meta?.title || record.name || path, iconName: iconMap[record.meta?.icon] || record.meta?.icon || 'file-list-2-line', items }] : [];
  });
}

const menuGroups = computed(() => createGroups(menuSource.value));
const allPages = computed(() => menuGroups.value.flatMap((group) => group.items.flatMap((item) => item.pages)));
const normalizePath = (path) => String(path || '').replace(/\/$/, '').replace(/\/index$/, '');
const activePath = computed(() => normalizePath(route.meta?.activeMenu || route.path));
function pageScore(page) {
  const path = normalizePath(page.path);
  if (!path) return 0;
  if (path === activePath.value) return 10000 + path.length;
  if (activePath.value.startsWith(`${path}/`)) return 1000 + path.length;
  // 空间菜单会搬迁模块；静态详情的 activeMenu 仍可能是原来的列表路径。
  const segments = activePath.value.split('/').filter(Boolean);
  const pageSegments = path.split('/').filter(Boolean);
  if (segments.length && pageSegments[pageSegments.length - 1] === segments[segments.length - 1]) {
    return 100 + pageSegments.filter((segment, index) => segment === segments[index]).length;
  }
  return 0;
}
const selectedPage = computed(() => allPages.value.reduce((best, page) => pageScore(page) > pageScore(best || {}) ? page : best, null));
const selectedItem = computed(() => menuGroups.value.flatMap((group) => group.items).find((item) => item.pages.includes(selectedPage.value)));
const selectedGroup = computed(() => menuGroups.value.find((group) => group.items.includes(selectedItem.value)));
const isDetailPage = computed(() => Boolean(route.meta?.activeMenu) || !allPages.value.some((page) => normalizePath(page.path) === normalizePath(route.path)));
const pageTitle = computed(() => route.meta?.title || selectedPage.value?.title || '空间工作站');
const pageDescription = computed(() => {
  if (route.path.startsWith('/ont/workspace/')) return '管理本体概念、属性、关系、动作编排和对象实例，构建可执行的业务语义网络。';
  if (route.path === '/ont/ontology') return '创建和维护本体模型，并进入可视化工作台完成业务语义建模。';
  return `管理${pageTitle.value}相关数据、配置和业务流程。`;
});

function isActivePath(path) {
  return normalizePath(path) === activePath.value;
}
function toggleGroup(id) {
  const next = new Set(collapsedGroups.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  collapsedGroups.value = next;
}
watch(menuGroups, (groups) => {
  const valid = new Set(groups.map((group) => group.id));
  const next = new Set([...collapsedGroups.value].filter((id) => valid.has(id)));
  groups.forEach((group) => { if (!knownGroups.value.has(group.id)) next.add(group.id); });
  collapsedGroups.value = next;
  knownGroups.value = valid;
}, { immediate: true });
watch(() => selectedGroup.value?.id, (id) => {
  if (id) collapsedGroups.value = new Set([...collapsedGroups.value].filter((entry) => entry !== id));
}, { immediate: true });

function navigateToPage(page, replace = false) {
  if (!page) return;
  if (/^https?:\/\//i.test(page.path)) {
    window.open(page.path, '_blank', 'noopener,noreferrer');
    return;
  }
  const target = { path: page.path, query: parseRouteQuery(page.query) || {} };
  return (replace ? router.replace(target) : router.push(target)).catch(() => {
    message.error('页面打开失败，请刷新后重试');
  });
}
function selectItem(item) {
  navigateToPage(item.pages.find((page) => isActivePath(page.path)) || item.pages[0]);
}
watch(() => [route.path, allPages.value, permissionStore.menuMode], () => {
  if (route.path !== '/space' || permissionStore.menuMode !== 'space') return;
  const pages = allPages.value.filter((entry) => !/^https?:\/\//i.test(entry.path) && entry.path !== '/space');
  const page = pages.find((entry) => entry.title.includes('数据统计') || entry.path.toLowerCase().includes('statistics')) || pages[0];
  if (page) navigateToPage(page, true);
}, { immediate: true });

// 普通工作站使用真实路由，保留刷新、浏览器历史和业务页的离开守卫。
// /explore 自己提供同名协议和虚拟路由，独立维护流程内的详情与返回栈。
function resolvePagePath(path) {
  const direct = router.resolve(path);
  if (direct.matched.length && !direct.matched.some((record) => /pathMatch|catchAll/.test(record.path))) return path;
  return allPages.value.find((entry) => normalizePath(entry.path) === normalizePath(path))?.path;
}
function openPage({ path, query = {}, routeName, hash, replace = false } = {}) {
  if (!path || !path.startsWith('/') || path.startsWith('//')) return false;
  let targetPath;
  if (path === '/ast/dataQuery') {
    if (!checkPermi(['ast:dataQuery:list'])) {
      message.warning('暂无数据查询权限');
      return true;
    }
    query = normalizeDataQueryTarget(query);
    targetPath = resolveDataQueryPath(router, permissionStore);
    if (!targetPath) {
      message.warning('当前空间暂无可用的数据查询入口');
      return true;
    }
  } else {
    targetPath = resolvePagePath(path) || (routeName && router.hasRoute(routeName) ? router.resolve({ name: routeName, query }).path : null);
  }
  if (!targetPath) return false;
  const target = { path: targetPath, query, hash };
  (replace ? router.replace(target) : router.push(target)).catch(() => {
    message.error('页面打开失败，请刷新后重试');
  });
  return true;
}
function back() {
  const previous = router.options.history.state.back;
  if (typeof previous === 'string' && previous.startsWith('/') && !previous.startsWith('//')
    && previous.split('?')[0] !== route.path && !/^\/(login|register|redirect)(?:\/|\?|$)/.test(previous)
    && router.resolve(previous).matched.some((record) => !/pathMatch|catchAll/.test(record.path))) {
    router.back();
    return true;
  }
  if (selectedPage.value && normalizePath(selectedPage.value.path) !== normalizePath(route.path)) {
    navigateToPage(selectedPage.value);
    return true;
  }
  return false;
}
function goBack() {
  if (!back()) router.push(isSpaceWorkspace.value ? '/space' : '/index');
}
provide('spaceWorkstationNavigation', { openPage, back });

async function loadSpaces() {
  if (!isSpaceWorkspace.value) return;
  spacesLoading.value = true;
  try {
    const response = await currentUser();
    spaces.value = response?.data || [];
  } catch {
    message.warning('空间列表加载失败，可返回首页重新选择空间');
  } finally {
    spacesLoading.value = false;
  }
}
onMounted(loadSpaces);
watch(isSpaceWorkspace, (value) => { if (value && !spaces.value.length) loadSpaces(); });
function switchSpace(id) {
  if (String(id) === String(userStore.spaceId)) return;
  const space = spaces.value.find((entry) => String(entry.id) === String(id));
  if (!space) return;
  // 切换前保留当前空间，先让编辑器处理未保存内容；导航成功后刷新，清理旧空间缓存。
  router.push('/index').then((failure) => {
    if (failure) return;
    localStorage.setItem('dataMasterSpaceId', String(id));
    userStore.spaceId = id;
    userStore.spaceCode = space.code || space.spaceCode || '';
    userStore.spaceName = space.name || space.spaceName || '';
    window.location.assign('/space');
  });
}
function goHome() {
  router.push('/index');
}
function handleUserCommand({ key }) {
  if (key === 'profile') router.push('/user/profile');
  if (key === 'logout') Modal.confirm({ title: '退出登录', content: '确定退出当前账号吗？', onOk: () => userStore.logOut().then(() => { window.location.href = '/login'; }) });
}
</script>

<style lang="scss" scoped>
.space-station { display: flex; flex-direction: column; height: 100vh; height: 100dvh; min-height: 0; overflow: hidden; color: #15213b; background: #f4f7fb; }
.space-station__header { flex: 0 0 auto; position: sticky; top: 0; z-index: 20; display: flex; align-items: center; justify-content: space-between; gap: 20px; min-height: 76px; padding: 0 28px; color: #15213b; background: rgba(255,255,255,.96); border-bottom: 1px solid #e1e7f0; box-shadow: 0 8px 26px rgba(34,54,91,.08); backdrop-filter: blur(14px); }
.space-station__brand { display: inline-flex; align-items: center; gap: 10px; padding: 0; color: inherit; text-align: left; background: none; border: 0; cursor: pointer; }
.space-station__brand strong, .space-station__brand small { display: block; }
.space-station__brand strong { color: #1b2944; font-size: 14px; letter-spacing: .02em; }
.space-station__brand small { margin-top: 2px; color: #8290aa; font-size: 11px; }
.space-station__brand-mark { display: grid; width: 38px; height: 38px; place-items: center; color: #fff; font-size: 19px; background: linear-gradient(135deg,#5b6df6,#8d5ce5); border-radius: 11px; box-shadow: 0 8px 22px rgba(91,109,246,.35); }
.space-station__header-actions { display: flex; align-items: center; justify-content: flex-end; gap: 12px; color: #8a96aa; font-size: 12px; }
.space-station__header-actions strong { color: #273651; font-size: 15px; }
.space-station__header-actions :deep(.ant-select) { min-width: 150px; max-width: 240px; }
.space-station__user { display: flex; align-items: center; gap: 8px; padding: 8px 10px; color: #273651; background: #f5f7ff; border: 1px solid #e0e5ff; border-radius: 9px; cursor: pointer; }
.space-station__body { flex: 1; display: grid; grid-template-columns: 252px minmax(0,1fr); grid-template-rows: minmax(0,1fr); min-height: 0; overflow: hidden; }
.space-station__sidebar { display: flex; flex-direction: column; min-height: 0; padding: 22px 16px; overflow-y: auto; background: #fff; border-right: 1px solid #e1e7f0; }
.space-station__sidebar-head { padding: 0 8px 18px; border-bottom: 1px solid #edf0f5; }
.space-station__sidebar-head span, .space-station__sidebar-head strong { display: block; }
.space-station__sidebar-head span { color: #8995a9; font-size: 10px; letter-spacing: .1em; }
.space-station__sidebar-head strong { margin-top: 6px; color: #202d47; font-size: 18px; }
.space-station__menu-groups { margin-top: 16px; }
.space-station__menu-group + .space-station__menu-group { margin-top: 18px; }
.space-station__menu-title { display: flex; align-items: center; gap: 9px; width: 100%; min-height: 40px; margin: 0 0 4px; padding: 10px; color: #66748d; font-size: 12px; font-weight: 700; letter-spacing: .04em; line-height: 18px; text-align: left; background: transparent; border: 1px solid transparent; border-radius: 9px; cursor: pointer; transition: .2s ease; }
.space-station__menu-title:hover { color: #5368dc; background: #f5f7ff; }
.space-station__menu-title :deep(svg) { color: #6178e8; font-size: 14px; }
.space-station__group-arrow { margin-left: auto; color: #9aa7ba !important; font-size: 11px !important; transition: transform .2s ease; }
.space-station__menu-title.is-collapsed .space-station__group-arrow { transform: rotate(-90deg); }
.space-station__menu-group > button:not(.space-station__menu-title) { position: relative; display: flex; align-items: center; gap: 9px; width: 100%; padding: 10px; color: #68758d; font-size: 12px; text-align: left; background: transparent; border: 1px solid transparent; border-radius: 9px; cursor: pointer; transition: .2s ease; }
.space-station__menu-group > button:not(.space-station__menu-title):hover { color: #425ad2; background: #f5f7ff; }
.space-station__menu-group > button:not(.space-station__menu-title).is-active { color: #4058d4; font-weight: 600; background: #edf0ff; border-color: #e0e5ff; box-shadow: inset 3px 0 #6076e9; }
.space-station__menu-group > button:not(.space-station__menu-title) :deep(svg) { color: currentColor; font-size: 15px; }
.space-station__subpages { display: flex; flex-direction: column; gap: 2px; margin: 2px 0 8px 24px; padding-left: 10px; border-left: 1px solid #e1e7f0; }
.space-station__subpages button { padding: 7px 10px; color: #78849a; font-size: 12px; text-align: left; background: transparent; border: 0; border-radius: 6px; cursor: pointer; }
.space-station__subpages button:hover { color: #425ad2; background: #f5f7ff; }
.space-station__subpages button.is-active { color: #4058d4; font-weight: 600; background: #edf0ff; }
.space-station__menu-arrow { margin-left: auto; color: #b6bfd0 !important; font-size: 10px !important; }
.space-station__content { display: flex; flex-direction: column; min-width: 0; min-height: 0; padding: 10px 16px 16px; overflow: hidden; }
.space-station__intro { display: flex; flex: 0 0 auto; align-items: center; justify-content: space-between; gap: 12px; min-height: 28px; margin-bottom: 8px; }
.space-station__intro p { min-width: 0; margin: 0; overflow: hidden; color: #78849a; font-size: 13px; line-height: 22px; text-overflow: ellipsis; white-space: nowrap; }
.space-station__intro > .ant-btn { flex-shrink: 0; }
.space-station__module { position: relative; flex: 1; min-width: 0; min-height: 0; overflow: auto; overscroll-behavior: contain; background: #fff; border: 1px solid #e0e7f0; border-radius: 16px; box-shadow: 0 12px 35px rgba(28,45,81,.07); }
.space-station__module :deep(.app-main) { height: 100%; min-height: 0; background: #fff !important; }
.space-station__module :deep(.app-main-inner) { height: 100%; min-height: 0; }
.space-station__module :deep(.app-container) { min-height: 100%; }
.space-station__module :deep(.integration-task-editor) { height: 100%; min-height: 0; margin: 0; }
.space-station__module :deep(.ask-data-page) { height: 100%; min-height: 0; border-radius: 16px; }
@media (max-width: 900px) {
  .space-station__header { flex-wrap: wrap; gap: 10px; padding: 12px 16px; }
  .space-station__header-actions { flex-wrap: wrap; margin-left: auto; }
  .space-station__body { display: flex; flex-direction: column; }
  .space-station__sidebar { flex: 0 0 auto; max-height: 30%; padding: 14px 16px; border-right: 0; border-bottom: 1px solid #e1e7f0; }
  .space-station__menu-groups { display: flex; gap: 16px; overflow-x: auto; }
  .space-station__menu-group { flex: 0 0 auto; min-width: 190px; }
  .space-station__menu-group + .space-station__menu-group { margin-top: 0; }
  .space-station__content { flex: 1; padding: 8px 12px 12px; }
}
</style>
