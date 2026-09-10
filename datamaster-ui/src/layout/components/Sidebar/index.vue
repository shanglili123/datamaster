<template>
  <div :class="{ 'has-logo': showLogo }" class="sidebar-shell">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <a-menu
      :selected-keys="activeMenu ? [activeMenu] : []"
      :open-keys="isCollapse ? [] : openKeys"
      :inline-collapsed="isCollapse"
      :mode="isCollapse ? 'vertical' : 'inline'"
      theme="dark"
      @click="handleMenuClick"
      @openChange="handleOpenChange"
      class="sidebar-menu"
    >
      <template v-for="(route, index) in topbarRouters" :key="route.routePath + index">
        <a-menu-item v-if="!route.visibleChildren.length" :key="route.routePath">
          <template #icon v-if="route.meta && route.meta.icon && route.meta.icon !== '#'">
            <svg-icon :icon-class="route.meta.icon" />
          </template>
          <span>{{ route.meta && route.meta.title }}</span>
        </a-menu-item>
        <a-sub-menu v-else :key="route.routePath">
          <template #title>
            <span v-if="route.meta && route.meta.icon && route.meta.icon !== '#'">
              <svg-icon :icon-class="route.meta.icon" />
            </span>
            <span>{{ route.meta && route.meta.title }}</span>
          </template>
          <!-- 三级菜单不在侧边栏展开，由内容区顶部页签承载；此处统一渲染为二级菜单项 -->
          <a-menu-item v-for="child in route.visibleChildren" :key="child.routePath">
            {{ child.meta && child.meta.title }}
          </a-menu-item>
        </a-sub-menu>
      </template>
    </a-menu>
  </div>
</template>

<script setup>
import Logo from './Logo'
import useAppStore from '@/store/system/app'
import useSettingsStore from '@/store/system/settings'
import usePermissionStore from '@/store/system/permission'
import { isHttp } from '@/utils/validate'
import { getNormalPath } from '@/utils/anivia'
import { parseRouteQuery } from '@/utils/routeQuery'
import { useRouter, useRoute } from 'vue-router'

const route = useRoute();
const router = useRouter();
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()

const topbarRouters = computed(() => {
  const routers = permissionStore.topbarRouters || []
  return decorateRoutes(routers.filter(r => !isHomeMenu(r)))
})

const showLogo = computed(() => settingsStore.sidebarLogo)
const isCollapse = computed(() => !appStore.sidebar.opened)

const openKeys = ref([])

// 当前高亮：优先精确匹配菜单叶子路径；命中三级叶子时回退高亮其二级父菜单
const activeMenu = computed(() => {
  const path = route.path
  if (path === '/index' || path === '/') return ''
  const matched = findMenu(path)
  const l2 = findSecondLevelAncestor(path)
  // 三级页面命中叶子时，侧边栏只渲染到二级，需高亮其二级父菜单
  if (l2 && (!matched || matched.routePath !== l2)) return l2
  if (matched) return matched.routePath
  const segments = path.split('/').filter(Boolean)
  if (segments.length > 0) return '/' + segments[0]
  return path
})

// 导航变化时展开对应的一、二级子菜单
watch(activeMenu, (key) => {
  openKeys.value = key ? findAncestors(key) : []
}, { immediate: true })

function isHomeMenu(menu) {
  const title = menu && menu.meta && menu.meta.title
  return title === '首页' || menu?.path === '/index' || menu?.path === 'index'
}

// 给菜单树补充解析后的完整路径与可见子菜单，用于递归渲染
function decorateRoutes(routes, parentPath = '') {
  return (routes || [])
    .filter(r => !r.hidden)
    .map(r => {
      const routePath = resolveMenuPath(r.path, parentPath)
      const visibleChildren = decorateRoutes(r.children, routePath)
      return { ...r, routePath, visibleChildren }
    })
}

function resolveMenuPath(path, parentPath) {
  if (!path) return ''
  if (isHttp(path)) return path
  if (path.startsWith('/')) return path
  return getNormalPath(parentPath + '/' + path)
}

// 递归查找完整路径对应的菜单项
function findMenu(path, routes = topbarRouters.value) {
  for (const menu of routes) {
    if (menu.routePath === path) return menu
    if (menu.visibleChildren && menu.visibleChildren.length) {
      const found = findMenu(path, menu.visibleChildren)
      if (found) return found
    }
  }
  return null
}

// 查找路径所属的二级菜单路径（用于三级页面高亮二级菜单）
function findSecondLevelAncestor(path) {
  for (const top of topbarRouters.value) {
    for (const child of top.visibleChildren || []) {
      if (path === child.routePath || path.startsWith(child.routePath + '/')) {
        return child.routePath
      }
    }
  }
  return null
}

// 计算某个完整路径的所有祖先菜单路径（用于展开子菜单）
function findAncestors(path) {
  const ancestors = []
  const segments = path.split('/').filter(Boolean)
  let acc = ''
  for (const seg of segments) {
    acc += '/' + seg
    if (acc === path) break
    ancestors.push(acc)
  }
  return ancestors
}

function handleOpenChange(keys) {
  openKeys.value = keys
}

function handleMenuClick({ key }) {
  const menu = findMenu(key)
  if (!menu) {
    router.push({ path: key })
    return
  }
  if (isHttp(key)) {
    window.open(key, '_blank')
    return
  }
  const children = menu.visibleChildren
  if (children && children.length > 0) {
    // 兼容：若一级/二级菜单本身可点击，跳转到第一个可见子页面
    const firstChild = children[0]
    const childPath = firstChild.routePath || firstChild.path || ''
    if (firstChild.query) {
      const query = parseRouteQuery(firstChild.query)
      router.push(query ? { path: childPath, query } : { path: childPath })
    } else {
      router.push({ path: childPath })
    }
  } else {
    if (menu.query) {
      const query = parseRouteQuery(menu.query)
      router.push(query ? { path: key, query } : { path: key })
    } else {
      router.push({ path: key })
    }
  }
}
</script>

<style lang="scss" scoped>
.has-logo {
  .sidebar-menu {
    border-inline-end: none !important;
  }
}

.sidebar-menu {
  border-inline-end: none !important;

  :deep(.ant-menu-submenu-arrow) {
    color: #7487aa;
    transition: color 0.2s ease, transform 0.2s ease;
  }

  :deep(.ant-menu-submenu-open > .ant-menu-submenu-title .ant-menu-submenu-arrow),
  :deep(.ant-menu-submenu-title:hover .ant-menu-submenu-arrow) {
    color: #9db3ff;
  }

  :deep(.ant-menu-item),
  :deep(.ant-menu-submenu-title) {
    position: relative;
    overflow: hidden;
  }

  :deep(.ant-menu-item::after) {
    right: auto;
    left: 0;
    width: 3px;
    height: 18px;
    margin-top: -9px;
    background: #8ee7d7;
    border: 0;
    border-radius: 0 3px 3px 0;
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  :deep(.ant-menu-item-selected::after) {
    opacity: 1;
  }
}
</style>
