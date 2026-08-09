<template>
  <a-menu
    :selected-keys="[activeMenu]"
    mode="horizontal"
    @click="handleSelect"
    class="topnav-menu"
    :style="{ borderBottom: 'none', flex: 1, minWidth: 0 }"
  >
    <template v-for="(item, index) in visibleTopMenus" :key="item.path || index">
      <a-menu-item :key="item.path">
        <template #icon v-if="item.meta && item.meta.icon && item.meta.icon !== '#'">
          <svg-icon :icon-class="item.meta.icon" />
        </template>
        {{ item.meta && item.meta.title }}
      </a-menu-item>
    </template>
    <a-sub-menu v-if="overflowTopMenus.length > 0" key="__more" title="更多菜单">
      <a-menu-item v-for="item in overflowTopMenus" :key="item.path">
        <template #icon v-if="item.meta && item.meta.icon && item.meta.icon !== '#'">
          <svg-icon :icon-class="item.meta.icon" />
        </template>
        {{ item.meta && item.meta.title }}
      </a-menu-item>
    </a-sub-menu>
  </a-menu>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isHttp } from '@/utils/validate'
import { getNormalPath } from '@/utils/anivia'
import { parseRouteQuery } from '@/utils/routeQuery'
import useAppStore from '@/store/system/app'
import useSettingsStore from '@/store/system/settings'
import usePermissionStore from '@/store/system/permission'
import useTagsViewStore from '@/store/system/tagsView'
import defaultSettings from '@/settings'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()

const theme = computed(() => settingsStore.theme)
const routers = computed(() => permissionStore.topbarRouters || [])

const visibleNumber = ref(null)
let resizeObserver = null
let mutationObserver = null

const topMenus = computed(() => {
  let menus = []
  routers.value.forEach((menu) => {
    if (menu.hidden !== true) {
      if (menu.path === '/') {
        if (menu.children && menu.children[0]) {
          menus.push(menu.children[0])
        }
      } else {
        menus.push(menu)
      }
    }
  })
  return menus.filter((menu) => !isHomeTopMenu(menu))
})

function isHomeTopMenu(menu) {
  const title = menu && menu.meta && menu.meta.title
  return title === '首页' || menu?.path === '/index' || menu?.path === 'index'
}

const visibleTopMenus = computed(() => {
  if (visibleNumber.value === null) return topMenus.value
  return topMenus.value.slice(0, visibleNumber.value)
})

const overflowTopMenus = computed(() => {
  if (visibleNumber.value === null) return []
  return topMenus.value.slice(visibleNumber.value)
})

function normalizeMenuPath(path) {
  if (!path) return path
  return path.startsWith('/') ? path : `/${path}`
}

// 从 routers 中查找当前路由所属的顶级菜单
function findTopLevelParentForActiveMenu(routers, activeMenuPath) {
  const parts = activeMenuPath.split('/').filter(Boolean)
  if (parts.length < 2) return null
  const trailingSegments = parts.slice(1)
  for (const topRoute of routers) {
    if (topRoute.hidden) continue
    if (routeHasTrailingSegments(topRoute, '', trailingSegments)) {
      return normalizeMenuPath(topRoute.path)
    }
  }
  return null
}

function routeHasTrailingSegments(route, parentResolvedPath, targetSegments) {
  const routePath = route.path || ''
  if (isHttp(routePath)) return false
  const resolved = parentResolvedPath
    ? (routePath.startsWith('/') ? routePath : getNormalPath(parentResolvedPath + '/' + routePath))
    : (routePath.startsWith('/') ? routePath : routePath)
  const resolvedParts = resolved.split('/').filter(Boolean)
  if (resolvedParts.length >= targetSegments.length) {
    const trailing = resolvedParts.slice(-targetSegments.length)
    if (trailing.every((p, i) => p === targetSegments[i])) return true
  }
  if (route.children) {
    for (const child of route.children) {
      if (routeHasTrailingSegments(child, resolved, targetSegments)) return true
    }
  }
  return false
}

const activeMenu = computed(() => {
  routers.value.length // 依赖追踪
  const path = route.path
  let activePath = path

  if (path === '/index') {
    const firstMenu = topMenus.value[0]
    if (firstMenu) activePath = firstMenu.path
  } else if (path.lastIndexOf('/') > 0) {
    const activeMenuMeta = route.meta && route.meta.activeMenu
    if (activeMenuMeta) {
      const topLevelPath = findTopLevelParentForActiveMenu(routers.value, activeMenuMeta)
      if (topLevelPath) {
        activePath = topLevelPath
      } else {
        const tmpPath = path.substring(1)
        activePath = '/' + tmpPath.substring(0, tmpPath.indexOf('/'))
      }
    } else {
      const tmpPath = path.substring(1)
      activePath = '/' + tmpPath.substring(0, tmpPath.indexOf('/'))
    }
  }

  // 侧边栏显示控制
  const navbarLogoRoutes = defaultSettings.navbarLogoRoutes || []
  const isLogoRoute = navbarLogoRoutes.some((logoPath) => path.startsWith(logoPath))
  if (isLogoRoute || path === '/index') {
    appStore.toggleSideBarHide(true)
  } else if (path.lastIndexOf('/') > 0) {
    if (!route.meta.link) appStore.toggleSideBarHide(false)
  } else {
    appStore.toggleSideBarHide(true)
  }

  activeRoutes(activePath)
  return normalizeMenuPath(activePath)
})

function handleSelect({ key }) {
  const targetKey = normalizeMenuPath(key)
  const matchedRoute = routers.value.find((item) => normalizeMenuPath(item.path) === targetKey)

  if (isHttp(key)) {
    window.open(key, '_blank')
    return
  }

  const hasChildren = matchedRoute && matchedRoute.children && matchedRoute.children.length > 0

  if (!hasChildren) {
    // 无子路由，直接跳转
    appStore.toggleSideBarHide(true)
    router.push({ path: targetKey })
  } else {
    // 有子路由，更新 sidebarRouters 并跳转
    activeRoutes(key)
    router.push({ path: targetKey })
    appStore.toggleSideBarHide(false)
  }
}

function activeRoutes(key) {
  const activeKey = normalizeMenuPath(key)
  const topMenusAll = topMenus.value
  const matched = topMenusAll.find((item) => normalizeMenuPath(item.path) === activeKey)
  if (!matched || !matched.children || matched.children.length === 0) {
    permissionStore.setSidebarRouters([])
    appStore.toggleSideBarHide(true)
    return []
  }
  const routes = matched.children
    .filter((child) => !child.hidden)
    .map((child) => {
      const cloned = JSON.parse(JSON.stringify(child))
      cloned.parentPath = activeKey
      if (cloned.path && !cloned.path.startsWith('/') && !isHttp(cloned.path)) {
        cloned.path = getNormalPath(activeKey + '/' + cloned.path)
      }
      return cloned
    })
  permissionStore.setSidebarRouters(routes)
  return routes
}

// 计算可见菜单数量
function calculateVisibleMenus() {
  const el = proxy?.$el
  if (!el) return

  const navbar = el.closest('.navbar')
  if (!navbar) return

  const rightMenu = navbar.querySelector('.navbar-right')
  const hamburger = navbar.querySelector('.hamburger-container')

  const navbarRect = navbar.getBoundingClientRect()
  const hamburgerWidth = hamburger ? hamburger.getBoundingClientRect().width : 50
  const leftWidth = navbarRect.left + hamburgerWidth

  const rightWidth = rightMenu ? rightMenu.getBoundingClientRect().width + 100 : 606
  const bodyWidth = document.body.getBoundingClientRect().width
  const menuWidth = 140

  const availableWidth = bodyWidth - leftWidth - rightWidth
  if (availableWidth < 0) {
    visibleNumber.value = 0
    return
  }

  const rawCount = Math.floor(availableWidth / menuWidth)
  const totalCount = topMenus.value.length

  if (totalCount <= rawCount) {
    visibleNumber.value = totalCount
  } else {
    visibleNumber.value = Math.max(0, rawCount - 1)
  }
}

watch(
  [() => route.path, () => topMenus.value.length],
  () => {
    nextTick(() => {
      calculateVisibleMenus()
    })
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('resize', calculateVisibleMenus)
  nextTick(() => {
    calculateVisibleMenus()
    const el = proxy?.$el
    if (el) {
      const navbar = el.closest('.navbar')
      if (navbar) {
        resizeObserver = new ResizeObserver(() => {
          calculateVisibleMenus()
        })
        resizeObserver.observe(navbar)

        const rightMenu = navbar.querySelector('.navbar-right')
        if (rightMenu) {
          resizeObserver.observe(rightMenu)
          Array.from(rightMenu.children).forEach((child) => {
            resizeObserver.observe(child)
          })
          mutationObserver = new MutationObserver(() => {
            calculateVisibleMenus()
          })
          mutationObserver.observe(rightMenu, { childList: true })
        }
      }
    }
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', calculateVisibleMenus)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (mutationObserver) {
    mutationObserver.disconnect()
    mutationObserver = null
  }
})
</script>

<style lang="scss" scoped>
.topnav-menu {
  flex: 1;
  min-width: 0;
  line-height: 60px;
  height: 60px;

  :deep(.ant-menu-item),
  :deep(.ant-menu-submenu-title) {
    font-size: 15px;
    height: 42px;
    line-height: 42px;
    margin: 9px 4px;
    padding: 0 16px;
    border-radius: 6px;
  }

  :deep(.ant-menu-item-selected),
  :deep(.ant-menu-submenu-selected > .ant-menu-submenu-title) {
    background: v-bind(theme) !important;
    color: #fff !important;

    &::after {
      display: none;
    }
  }

  :deep(.ant-menu-item:hover),
  :deep(.ant-menu-submenu-title:hover) {
    color: #fff !important;
    background: v-bind(theme) !important;
  }
}
</style>
