import router from "./router";
import { ElMessage } from "element-plus";
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
      next({ path: "/" });
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
                next({ ...to, replace: true });
              });
          })
          .catch((err) => {
            useUserStore()
              .logOut()
              .then(() => {
                ElMessage.error(err);
                next({ path: "/" });
              });
          });
      } else {
        const permissionStore = usePermissionStore();
        if (isSpaceModuleRoute(to.path) && permissionStore.menuMode !== "space") {
          ensureSpaceRoutes(to)
            .then(() => next({ ...to, replace: true }))
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
