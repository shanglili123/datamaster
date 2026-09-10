<template>
  <div class="navbar" ref="navbar">
    <div class="navbar-left">
      <hamburger
        v-if="showSidebarToggle"
        id="hamburger-container"
        :is-active="appStore.sidebar.opened"
        class="hamburger-container"
        @toggleClick="toggleSideBar"
      />
      <breadcrumb v-if="showBreadcrumb" />
    </div>
    <div class="navbar-right">
      <template v-if="appStore.device !== 'mobile'">
        <div style="display: flex; align-items: center; white-space: nowrap" v-if="showSpaceSelector">
          <span style="font-size: 13px; color: #ef4444; margin-right: 2px">*</span>
          <span style="font-size: 13px; color: #606266; margin-right: 8px">所属空间</span>
          <a-select
            style="width: 130px"
            v-model:value="userStore.spaceId"
            @change="spaceIdChange"
            placeholder="请选择所属空间"
            allow-clear
            :get-popup-container="() => $el || document.body"
          >
            <a-select-option
              v-for="item in spaceOptions"
              :key="item.id"
              :value="item.id"
            >
              <a-tooltip v-if="item.name.length > 6" :title="item.name">
                <div class="ellipsis-option">{{ item.name }}</div>
              </a-tooltip>
              <template v-else>
                <div class="ellipsis-option">{{ item.name }}</div>
              </template>
            </a-select-option>
          </a-select>
        </div>
      </template>
      <div class="avatar-container">
        <a-dropdown :trigger="['click']">
          <div class="avatar-wrapper">
            <img
              :src="userAvatar"
              class="user-avatar"
              alt="avatar"
            />
            <span class="nickName">{{ userStore.nickName }}</span>
            <DownOutlined style="font-size: 12px; margin-left: 4px;" />
          </div>
          <template #overlay>
            <a-menu @click="handleCommand">
              <a-menu-item key="profile">
                <router-link to="/user/profile">个人中心</router-link>
              </a-menu-item>
              <a-menu-item key="setLayout" v-if="settingsStore.showSettings">
                布局设置
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout">
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup name="Navbar">
import { useWindowSize } from "@vueuse/core";
import { Modal } from "ant-design-vue";
import Hamburger from "@/components/Hamburger";
import Breadcrumb from "@/components/Breadcrumb/index.vue";
import useAppStore from "@/store/system/app";
import useUserStore from "@/store/system/user";
import useSettingsStore from "@/store/system/settings";
import useTagsViewStore from "@/store/system/tagsView";
import defaultSettings from "@/settings";
import {
  getNum,
  listMessage,
  readAll,
} from "@/api/system/system/message/message";
import { onMounted, ref, watch } from "vue";
import { currentUser } from "@/api/tax/space/space";
import usePermissionStore from "@/store/system/permission";
import { getRoutersDpp } from "@/api/system/menu";
import defaultAvatar from "@/assets/images/defaultAvatar.svg";
import { isSpaceModuleRoute } from "@/utils/moduleRoute";

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const { proxy } = getCurrentInstance();
const visitedViews = computed(() => useTagsViewStore().visitedViews);
const isOnlyLogoRoute = computed(() => {
  if (isHomeShellPath(route.path)) return true;
  const navbarLogoRoutes = defaultSettings.navbarLogoRoutes || [];
  return navbarLogoRoutes.some((logoPath) => route.path.startsWith(logoPath));
});
const permissionStore = usePermissionStore();
const userAvatar = computed(() => userStore.avatar || defaultAvatar);

const needUpdate = ref(false);
const currentVersion = ref("");
const latestVersion = ref("");

const { width } = useWindowSize();
const showSpaceSelector = computed(
  () =>
    width.value >= 1200 &&
    isSpaceWorkspacePath(route.path) &&
    spaceOptions.value.length > 0
);
const showSidebarToggle = computed(() => !isHomeShellPath(route.path));
const showBreadcrumb = computed(
  () => !isHomeShellPath(route.path) && route.path !== "/"
);
const spaceOptions = ref([]);

function isSpaceWorkspacePath(path) {
  return isSpaceModuleRoute(path);
}

function isHomeShellPath(path) {
  return path === "/" || path === "/index";
}

function resetHomeShell() {
  userStore.spaceCode = "";
  userStore.spaceName = "";
  permissionStore.resetHomeMenus();
  appStore.toggleSideBarHide(true);
}

function loadSpaceMenus(spaceId, options = { navigate: true }) {
  const space = spaceOptions.value.find(
    (item) => String(item.id) === String(spaceId)
  );
  if (space) {
    userStore.spaceCode = space.code;
    userStore.spaceName = space.name || "";
  }
  localStorage.setItem("dataMasterSpaceId", spaceId);

  getRoutersDpp(spaceId).then((res) => {
    const routes = res.data || [];
    permissionStore.updateTopbarRoutes(routes);

    if (!options.navigate) return;

    const targetPath = isSpaceWorkspacePath(router.currentRoute.value.path)
      ? router.currentRoute.value.path
      : findFirstRoutePath(permissionStore.addRoutes);

    if (targetPath) {
      proxy.$tab.closeAllPage();
      router.push({
        path: targetPath,
        query: router.currentRoute.value.query,
      });
    }
  });
}

function findFirstRoutePath(routes, parentPath = "") {
  for (const route of routes || []) {
    if (route.hidden) continue;
    const currentPath = joinRoutePath(parentPath, route.path);
    if (route.children && route.children.length) {
      const childPath = findFirstRoutePath(route.children, currentPath);
      if (childPath) return childPath;
    }
    if (currentPath && currentPath !== "/" && currentPath !== "/index") {
      return currentPath;
    }
  }
  return "";
}

function joinRoutePath(parentPath, path) {
  if (!path) return parentPath;
  if (/^https?:\/\//.test(path)) return path;
  if (path.startsWith("/")) return path;
  const parent = parentPath.endsWith("/")
    ? parentPath.slice(0, -1)
    : parentPath;
  return `${parent}/${path}`.replace(/\/+/g, "/");
}

onMounted(() => {
  listSpace();
});

watch(
  () => route.path,
  (path) => {
    if (isHomeShellPath(path)) {
      resetHomeShell();
    }
  },
  { immediate: true }
);

onBeforeUnmount(() => {});

function spaceIdChange() {
  const space = spaceOptions.value.find(
    (item) => item.id === userStore.spaceId
  );
  if (space) {
    userStore.spaceCode = space.code;
    userStore.spaceName = space.name || "";
  }
  if (userStore.spaceId) {
    localStorage.setItem("dataMasterSpaceId", userStore.spaceId);
    location.reload();
  } else {
    userStore.spaceCode = "";
    userStore.spaceName = "";
    localStorage.removeItem("dataMasterSpaceId");
  }
}

const listSpace = () => {
  if (userStore.id) {
    currentUser().then((response) => {
      spaceOptions.value = response.data || [];
      if (!spaceOptions.value.length) {
        userStore.spaceId = null;
        userStore.spaceCode = "";
        userStore.spaceName = "";
        localStorage.removeItem("dataMasterSpaceId");
        return;
      }

      const dataMasterSpaceId = localStorage.getItem("dataMasterSpaceId");
      if (!dataMasterSpaceId) {
        userStore.spaceId = "";
        userStore.spaceCode = "";
        userStore.spaceName = "";
        return;
      }

      const space = spaceOptions.value.find(
        (item) => String(item.id) === String(dataMasterSpaceId)
      );
      if (!space) {
        userStore.spaceId = "";
        userStore.spaceCode = "";
        userStore.spaceName = "";
        localStorage.removeItem("dataMasterSpaceId");
        return;
      }

      userStore.spaceId = space.id;
      userStore.spaceCode = space.code;
      userStore.spaceName = space.name || "";
      if (isSpaceWorkspacePath(route.path)) {
        loadSpaceMenus(space.id, { navigate: false });
      }
    });
  }
};

function toggleSideBar() {
  appStore.toggleSideBar();
}

function handleCommand({ key }) {
  switch (key) {
    case "setLayout":
      setLayout();
      break;
    case "logout":
      logout();
      break;
    default:
      break;
  }
}

function logout() {
  Modal.confirm({
    title: '提示',
    content: '确定注销并退出系统吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      userStore.logOut().then(() => {
        location.href = "/index";
      });
    }
  });
}

const emits = defineEmits(["setLayout"]);

function setLayout() {
  emits("setLayout");
}
</script>

<style lang="scss" scoped>
.ellipsis-option {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.navbar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(222, 230, 242, 0.9);
  padding: 0 18px 0 10px;
  box-shadow: 0 8px 24px rgba(27, 48, 91, 0.045);
  backdrop-filter: blur(16px);

  .navbar-left {
    display: flex;
    align-items: center;
    flex: 1;
    overflow: hidden;

    :deep(.breadcrumb) {
      color: #75829a;
      font-size: 13px;
    }

    :deep(.breadcrumb .no-redirect),
    :deep(.breadcrumb .redirect) {
      transition: color 0.2s ease;
    }

    :deep(.breadcrumb .redirect:hover) {
      color: #4b67df;
    }
  }

  .navbar-right {
    display: flex;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;

    .avatar-container {
      display: flex;
      align-items: center;
      height: 100%;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        height: 36px;
        padding: 0 6px 0 4px;
        border: 1px solid transparent;
        border-radius: 18px;
        cursor: pointer;
        transition: border-color 0.16s ease, background-color 0.16s ease;

        &:hover {
          background: #f5f8ff;
          border-color: #dfe6f7;
        }

        .user-avatar {
          width: 28px;
          height: 28px;
          border-radius: 14px;
          object-fit: cover;
          background: #f2f3f5;
          border: 1px solid #eef0f3;
        }

        .nickName {
          max-width: 80px;
          overflow: hidden;
          color: var(--dm-text-regular, #374151);
          display: inline-block;
          font-size: 13px;
          font-weight: 500;
          line-height: 20px;
          margin-left: 6px;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }

  .hamburger-container {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    line-height: 40px;
    margin: 10px 8px 0 10px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.3s;

    &:hover {
      background: #f3f6fb;
    }
  }
}
</style>
