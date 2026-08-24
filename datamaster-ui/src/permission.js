import router from "./router";
import { message } from "ant-design-vue";
import NProgress from "nprogress";
import "nprogress/nprogress.css";
import { getToken } from "@/utils/auth";
import { isHttp } from "@/utils/validate";
import { isRelogin } from "@/utils/request";
import useUserStore from "@/store/system/user";
import useSettingsStore from "@/store/system/settings";
import usePermissionStore from "@/store/system/permission";
import { getRoutersDpp } from "@/api/system/menu";
import { currentUser } from "@/api/tax/space/space";
import { isSpaceModuleRoute } from "@/utils/moduleRoute";

NProgress.configure({ showSpinner: false });

const whiteList = ["/login", "/register", "/sso/login", "/sso"];

router.beforeEach((to, from, next) => {
  NProgress.start();

  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title);

    if (to.path === "/login") {
      next();
      NProgress.done();
    } else if (whiteList.indexOf(to.path) !== -1) {
      next();
    } else {
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true;
        useUserStore()
          .getInfo()
          .then(() => {
            isRelogin.show = false;
            usePermissionStore()
              .generateRoutes()
              .then(async (accessRoutes) => {
                accessRoutes.forEach((route) => {
                  if (!isHttp(route.path)) {
                    router.addRoute(route);
                  }
                });
                await ensureSpaceRoutes(to);
                retryNavigation(to, next);
              });
          })
          .catch((err) => {
            useUserStore()
              .logOut()
              .then(() => {
                message.error(err);
                next({ path: "/" });
              });
          });
      } else {
        const permissionStore = usePermissionStore();
        if (isSpaceModuleRoute(to.path) && permissionStore.menuMode !== "space") {
          ensureSpaceRoutes(to)
            .then(() => retryNavigation(to, next))
            .catch(() => next());
        } else {
          next();
        }
      }
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next();
    } else {
      next(`/login?redirect=${to.fullPath}`);
      NProgress.done();
    }
  }
});

/**
 * 路由表在刷新/切换空间时会被后端菜单重建（removeRoute + addRoute），
 * 原始路由的 name 可能已被替换，直接 next({ ...to, replace: true }) 会按
 * 名称匹配失败抛出 No match，导致初始导航中断、页面空白。
 * 这里改为按路径重试；若目标确实已不存在，则回退首页而不是让导航崩溃。
 */
function retryNavigation(target, next) {
  let resolvedLocation = null;
  try {
    const resolved = router.resolve(target.fullPath);
    if (resolved.matched.length > 0) {
      resolvedLocation = resolved;
    }
  } catch (error) {
    // 目标路由无法解析（如已被菜单重建移除）
    resolvedLocation = null;
  }
  if (resolvedLocation) {
    next({
      path: resolvedLocation.path,
      query: resolvedLocation.query,
      hash: resolvedLocation.hash,
      replace: true
    });
  } else {
    next({ path: "/", replace: true });
  }
}

async function ensureSpaceRoutes(to) {
  if (!isSpaceModuleRoute(to.path)) return;

  let spaceId = localStorage.getItem("dataMasterSpaceId");

  const userStore = useUserStore();
  const permissionStore = usePermissionStore();
  if (!spaceId) {
    const spaceResponse = await currentUser();
    const firstSpace = spaceResponse?.data?.[0];
    if (!firstSpace?.id) return;

    spaceId = firstSpace.id;
    localStorage.setItem("dataMasterSpaceId", spaceId);
    userStore.spaceCode = firstSpace.code || firstSpace.spaceCode || "";
  }

  userStore.spaceId = spaceId;

  const response = await getRoutersDpp(spaceId);
  permissionStore.updateTopbarRoutes(response?.data || []);
}

router.afterEach(() => {
  NProgress.done();
});
