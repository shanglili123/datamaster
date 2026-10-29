
import auth from '@/plugins/auth';
import router, { constantRoutes, dynamicRoutes } from '@/router';
import { getRouters } from '@/api/system/menu.js';
import Layout from '@/layout/index';
import ParentView from '@/components/ParentView';
import InnerLink from '@/layout/components/InnerLink';
import { isHttp } from '@/utils/validate';

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue');

const usePermissionStore = defineStore('permission', {
    state: () => ({
        routes: [],
        addRoutes: [],
        defaultRoutes: [],
        homeRoutes: [],
        topbarRouters: [],
        sidebarRouters: [],
        menuMode: 'home'
    }),
    actions: {
        setMenuMode(mode) {
            this.menuMode = mode;
        },
        setRoutes(routes) {
            this.addRoutes = routes;
            this.routes = constantRoutes.concat(routes);
        },
        setDefaultRoutes(routes) {
            this.defaultRoutes = constantRoutes.concat(routes);
        },
        setHomeRoutes(routes) {
            this.homeRoutes = routes;
        },
        setTopbarRoutes(routes) {
            this.topbarRouters = routes;
            this.sidebarRouters = buildSidebarRoutesForCurrentPath(routes);
        },
        setSidebarRouters(routes) {
            this.sidebarRouters = routes;
        },
        async generateRoutes() {
            const res = await getRouters();
            // 菜单展示直接沿用后端树，路由注册的扁平化只影响 Router，不改目录。
            const sourceRoutes = filterAsyncRouter(JSON.parse(JSON.stringify(res.data || [])));
            const rewriteRoutes = buildRouterRoutes(sourceRoutes).map((route) => mergeStaticChildRoutes(route));
            filterDynamicRoutes(dynamicRoutes).forEach((route) => router.addRoute(route));
            this.setRoutes(rewriteRoutes);
            this.setMenuMode('home');
            this.setSidebarRouters([]);
            this.setDefaultRoutes(sourceRoutes);
            this.setHomeRoutes(sourceRoutes);
            this.setTopbarRoutes(sourceRoutes);
            return rewriteRoutes;
        },
        updateTopbarRoutes(routes) {
            const sourceRoutes = filterAsyncRouter(JSON.parse(JSON.stringify(routes || [])));
            const rewriteRoutes = buildRouterRoutes(sourceRoutes).map((route) => mergeStaticChildRoutes(route));
            addRoutesToRouter(sourceRoutes);
            this.setMenuMode('space');
            this.setRoutes(rewriteRoutes);
            this.setSidebarRouters(sourceRoutes);
            this.setDefaultRoutes(sourceRoutes);
            this.setTopbarRoutes(sourceRoutes);
        },
        resetHomeMenus(routes = this.defaultRoutes) {
            const sourceRoutes = this.homeRoutes && this.homeRoutes.length ? this.homeRoutes : routes;
            const homeRoutes = cloneMenuRoutes(sourceRoutes);
            this.setMenuMode('home');
            this.setSidebarRouters([]);
            this.setTopbarRoutes(homeRoutes);
        }
    }
});

// 已解析的菜单包含异步组件函数，不能再用 JSON 克隆，否则返回首页后会丢失组件引用。
function cloneMenuRoutes(routes) {
    return (routes || []).map((route) => ({
        ...route,
        meta: route.meta ? { ...route.meta } : route.meta,
        ...(route.children ? { children: cloneMenuRoutes(route.children) } : {})
    }));
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap) {
    return asyncRouterMap.filter((route) => {
        if (route.component) {
            // Layout ParentView 组件特殊处理
            if (route.component === 'Layout') {
                route.component = Layout;
            } else if (route.component === 'ParentView') {
                route.component = ParentView;
            } else if (route.component === 'InnerLink') {
                route.component = InnerLink;
            } else {
                route.component = loadView(route.component);
            }
        }
        if (route.children != null && route.children && route.children.length) {
            route.children = filterAsyncRouter(route.children);
        } else {
            delete route['children'];
            delete route['redirect'];
        }
        return true;
    });
}

function addRoutesToRouter(routes) {
    buildRouterRoutes(routes).forEach((route) => {
        if (isHttp(route.path)) return;
        // /explore 是独立的数据探索流程工作站，不能被空间动态菜单重新注册或
        // 替换成空间工作站组件。空间菜单只负责 /space 及业务模块路由。
        if (route.name === 'DataExploreStation' || normalizeRoutePath(route.path) === '/explore') return;
        mergeStaticChildRoutes(route);
        if (route.name && router.hasRoute(route.name)) {
            router.removeRoute(route.name);
        }
        router.addRoute(route);
    });
}

// 后端空间菜单可能缺少部分子菜单（如带参数的隐藏详情页 workspace/:ontologyId），
// 直接 removeRoute + addRoute 重建会把前端静态路由中已注册的这些子页面一并删除，
// 导致刷新时按名称/路径重定向失败（No match）。注册前先把静态定义中缺失的子路由补回。
function mergeStaticChildRoutes(route) {
    if (!route) return route;
    const staticRoute = constantRoutes.find((item) => item && (
        (route.name && item.name === route.name) || normalizeRoutePath(item.path) === normalizeRoutePath(route.path)
    ));
    if (!staticRoute || !Array.isArray(staticRoute.children)) return route;

    const children = Array.isArray(route.children) ? route.children : [];
    const knownPaths = new Set(children.map((child) => child && child.path));
    staticRoute.children.forEach((child) => {
        if (child && child.path && !knownPaths.has(child.path)) {
            children.push({ ...child });
        }
    });
    if (children.length) {
        route.children = children;
    }
    return route;
}

function buildRouterRoutes(routes) {
    return (routes || []).map((route) => {
        if (!route || !route.children || !route.children.length) return route;

        const registerRoute = cloneRouteRecord(route, route.path);
        registerRoute.children = [];

        const firstChildPath = findFirstNavigablePath(route.children);
        if (firstChildPath && (!registerRoute.redirect || registerRoute.redirect === 'noRedirect' || registerRoute.redirect === 'noredirect')) {
            registerRoute.redirect = createAbsoluteRedirect(route.path, firstChildPath);
        }

        route.children.forEach((child) => {
            flattenRouteForRegister(child, '', registerRoute.children, route.path);
        });

        if (!registerRoute.children.length) {
            delete registerRoute.children;
        }
        return registerRoute;
    });
}

function buildSidebarRoutesForCurrentPath(routes) {
    const currentPath = getCurrentBrowserPath();
    const topPath = getTopRoutePath(currentPath);
    if (!topPath) return [];

    const matchedTopRoute = (routes || []).find((route) =>
        normalizeRoutePath(route.path) === topPath
    );
    if (!matchedTopRoute || !matchedTopRoute.children || !matchedTopRoute.children.length) {
        return [];
    }

    const currentL2 = findMatchingL2(matchedTopRoute, currentPath);
    if (!currentL2 || !currentL2.children || !currentL2.children.length) return [];

    const l2Path = normalizeRoutePath(matchedTopRoute.path) + '/' + currentL2.path;
    return currentL2.children.map((child) => {
        const cloned = cloneRouteRecord(child, toChildSidebarPath(l2Path, child.path));
        cloned.parentPath = l2Path;
        if (child.children && child.children.length) {
            cloned.children = child.children;
        }
        return cloned;
    });
}

function findMatchingL2(topRoute, currentPath) {
    const topPath = normalizeRoutePath(topRoute.path);
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

function getCurrentBrowserPath() {
    if (typeof window === 'undefined') return '';
    const hashPath = window.location.hash && window.location.hash.startsWith('#/')
        ? window.location.hash.slice(1).split('?')[0]
        : '';
    return hashPath || window.location.pathname || '';
}

function getTopRoutePath(path) {
    if (!path || path === '/' || path === '/index') return '';
    const segments = path.split('/').filter(Boolean);
    return segments.length > 0 ? `/${segments[0]}` : '';
}

function normalizeRoutePath(path) {
    if (!path) return '';
    return path.startsWith('/') ? path : `/${path}`;
}

function toChildSidebarPath(parentPath, childPath) {
    if (!childPath) return childPath;
    if (isHttp(childPath) || childPath.startsWith('/')) return childPath;
    return joinRoutePath(normalizeRoutePath(parentPath), childPath);
}

function flattenRouteForRegister(route, parentPath, targetRoutes, rootPath = '') {
    if (!route) return;

    const currentPath = joinRoutePath(parentPath, route.path);
    const children = route.children || [];
    const hasChildren = children.length > 0;
    const hasOwnPage = isPageRoute(route);

    if (hasOwnPage) {
        targetRoutes.push(cloneRouteRecord(route, stripLeadingSlash(currentPath)));
    }

    if (hasChildren) {
        if (!hasOwnPage) {
            const firstChildPath = findFirstNavigablePath(children, currentPath);
            if (firstChildPath) {
                const redirectRoute = cloneRouteRecord(route, stripLeadingSlash(currentPath));
                redirectRoute.redirect = createAbsoluteRedirect(rootPath, firstChildPath);
                delete redirectRoute.component;
                targetRoutes.push(redirectRoute);
            }
        }
        children.forEach((child) => {
            flattenRouteForRegister(child, currentPath, targetRoutes, rootPath);
        });
        return;
    }

    if (!hasOwnPage && route.component) {
        targetRoutes.push(cloneRouteRecord(route, stripLeadingSlash(currentPath)));
    }
}

function findFirstNavigablePath(routes, parentPath = '') {
    for (const route of routes || []) {
        if (!route || route.hidden) continue;
        const currentPath = joinRoutePath(parentPath, route.path);
        if (isPageRoute(route)) return currentPath;
        if (route.children && route.children.length) {
            const childPath = findFirstNavigablePath(route.children, currentPath);
            if (childPath) return childPath;
        }
    }
    return '';
}

function cloneRouteRecord(route, path) {
    const clonedRoute = {
        ...route,
        path
    };
    delete clonedRoute.children;
    return clonedRoute;
}

function isPageRoute(route) {
    return route.component && route.component !== Layout && route.component !== ParentView;
}

function joinRoutePath(parentPath, path) {
    if (!path) return parentPath || '';
    if (isHttp(path)) return path;
    if (path.startsWith('/')) return path;
    if (!parentPath) return path;
    const parent = parentPath.endsWith('/')
        ? parentPath.slice(0, -1)
        : parentPath;
    return `${parent}/${path}`.replace(/\/+/g, '/');
}

function toAbsoluteRoutePath(rootPath, path) {
    if (!path || path.startsWith('/')) return path;
    return joinRoutePath(rootPath, path);
}

function createAbsoluteRedirect(rootPath, path) {
    return (to) => {
        const matchedRoot = to.matched && to.matched[0] && to.matched[0].path;
        const basePath = matchedRoot && matchedRoot !== '/' ? matchedRoot : rootPath;
        return toAbsoluteRoutePath(basePath, path);
    };
}

function stripLeadingSlash(path) {
    return path ? path.replace(/^\/+/, '') : path;
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
    const res = [];
    routes.forEach((route) => {
        if (route.permissions) {
            if (auth.hasPermiOr(route.permissions)) {
                res.push(route);
            }
        } else if (route.roles) {
            if (auth.hasRoleOr(route.roles)) {
                res.push(route);
            }
        }
    });
    return res;
}

export const loadView = (view) => {
    let res;
    for (const path in modules) {
        const dir = path.split('views/')[1].split('.vue')[0];
        if (dir === view) {
            res = () => modules[path]();
        }
    }
    return res;
};

export default usePermissionStore;
