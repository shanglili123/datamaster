
import auth from '@/plugins/auth';
import router, { constantRoutes, dynamicRoutes } from '@/router';
import { getRouters } from '@/api/system/menu.js';
import Layout from '@/layout/index';
import ParentView from '@/components/ParentView';
import InnerLink from '@/layout/components/InnerLink';
import { normalizeModuleRoute, normalizeModuleRoutePath } from '@/utils/moduleRoute';
import { isHttp } from '@/utils/validate';

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue');

const homeRoute = {
    path: '/index',
    name: 'Index',
    meta: { title: '首页', icon: 'dashboard', affix: true }
};

const homeMenuTitles = ['首页', '系统管理', '日志管理'];

const usePermissionStore = defineStore('permission', {
    state: () => ({
        routes: [],
        addRoutes: [],
        defaultRoutes: [],
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
        setTopbarRoutes(routes) {
            this.topbarRouters = routes;
        },
        setSidebarRouters(routes) {
            this.sidebarRouters = routes;
        },
        generateRoutes() {
            return new Promise((resolve) => {
                // 向后端请求路由数据
                getRouters().then((res) => {
                    const sdata = JSON.parse(JSON.stringify(res.data));
                    const rdata = JSON.parse(JSON.stringify(res.data));
                    const defaultData = JSON.parse(JSON.stringify(res.data));
                    const sidebarRoutes = normalizeMenuTree(filterSystemTool(filterAsyncRouter(sdata)));
                    const rewriteRoutes = normalizeMenuTree(filterSystemTool(filterAsyncRouter(rdata, true)));
                    const defaultRoutes = normalizeMenuTree(filterSystemTool(filterAsyncRouter(defaultData)));
                    const asyncRoutes = filterDynamicRoutes(dynamicRoutes);
                    asyncRoutes.forEach((route) => {
                        router.addRoute(route);
                    });
                    this.setRoutes(rewriteRoutes);
                    this.setMenuMode('home');
                    this.setSidebarRouters([]);
                    this.setDefaultRoutes(filterHomeMenus(sidebarRoutes));
                    this.setTopbarRoutes(filterHomeMenus(defaultRoutes));
                    resolve(rewriteRoutes);
                });
            });
        },
        updateTopbarRoutes(routes) {
            const sdata = JSON.parse(JSON.stringify(routes));
            const rdata = JSON.parse(JSON.stringify(routes));
            const defaultData = JSON.parse(JSON.stringify(routes));
            const sidebarRoutes = normalizeMenuTree(filterSystemTool(filterAsyncRouter(sdata)));
            const rewriteRoutes = normalizeMenuTree(filterSystemTool(filterAsyncRouter(rdata, true)));
            const defaultRoutes = normalizeMenuTree(filterSystemTool(filterAsyncRouter(defaultData)));

            addRoutesToRouter(sidebarRoutes);
            this.setMenuMode('project');
            this.setRoutes(rewriteRoutes);
            this.setSidebarRouters(constantRoutes.concat(sidebarRoutes));
            this.setDefaultRoutes(sidebarRoutes);
            this.setTopbarRoutes(defaultRoutes);
        },
        resetHomeMenus(routes = this.defaultRoutes) {
            const homeRoutes = filterHomeMenus(normalizeMenuTree(JSON.parse(JSON.stringify(routes))));
            this.setMenuMode('home');
            this.setSidebarRouters([]);
            this.setTopbarRoutes(homeRoutes);
        }
    }
});

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, type = false) {
    return asyncRouterMap.filter((route) => {
        if (type && route.children) {
            route.children = filterChildren(route.children);
        }
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
            route.children = filterAsyncRouter(route.children, type);
        } else {
            delete route['children'];
            delete route['redirect'];
        }
        return true;
    });
}

function filterChildren(childrenMap, lastRouter = false) {
    let children = [];
    childrenMap.forEach((el) => {
        if (el.children && el.children.length) {
            if (el.component === 'ParentView' && !lastRouter) {
                el.children.forEach((c) => {
                    c.path = el.path + '/' + c.path;
                    if (c.children && c.children.length) {
                        children = children.concat(filterChildren(c.children, c));
                        return;
                    }
                    children.push(c);
                });
                return;
            }
        }
        if (lastRouter) {
            el.path = lastRouter.path + '/' + el.path;
            if (el.children && el.children.length) {
                children = children.concat(filterChildren(el.children, el));
                return;
            }
        }
        children = children.concat(el);
    });
    return children;
}

function filterSystemTool(routes) {
    return routes
        .filter((route) => {
            const path = route.path || '';
            const title = route.meta && route.meta.title;
            return !(
                path === 'tool' ||
                path === '/tool' ||
                path.startsWith('tool/') ||
                path.startsWith('/tool') ||
                title === '系统工具' ||
                path === 'ai' ||
                path === '/ai' ||
                title === '智能问数'
            );
        })
        .map((route) => {
            if (route.children && route.children.length) {
                route.children = filterSystemTool(route.children);
            }
            return route;
        });
}

function addRoutesToRouter(routes) {
    buildRouterRoutes(routes).forEach((route) => {
        if (isHttp(route.path)) return;
        if (route.name && router.hasRoute(route.name)) {
            router.removeRoute(route.name);
        }
        router.addRoute(route);
    });
}

function buildRouterRoutes(routes) {
    return (routes || []).map((route) => {
        if (!route || !route.children || !route.children.length) return route;

        const registerRoute = cloneRouteRecord(route, route.path);
        registerRoute.children = [];

        const firstChildPath = findFirstNavigablePath(route.children);
        if (firstChildPath) {
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

function normalizeMenuTree(routes) {
    const normalizedRoutes = routes
        .map((route) => normalizeMenuTitle(route))
        .filter(Boolean);

    mergeBasicManagement(normalizedRoutes);
    moveQualityMenusToMetadata(normalizedRoutes);
    moveQualityCatUnderQualityMenu(normalizedRoutes);
    moveDatasourceToMetadata(normalizedRoutes);
    reorganizeDevelopmentMenus(normalizedRoutes);
    reorderTopLevelRoutes(normalizedRoutes);
    return normalizedRoutes;
}

function reorganizeDevelopmentMenus(routes) {
    const dppRoute = routes.find((route) => isDevelopmentManagement(route));
    if (!dppRoute) return;

    const projectBaseRoute = detachProjectBaseRoute(routes, dppRoute) || createProjectBaseRoute();
    const projectBaseExistingChildren = projectBaseRoute.children || [];
    projectBaseRoute.children = [];

    const existingDevelopmentCategoryRoute = dppRoute.children
        ? detachFirstRoute(dppRoute.children, isDevelopmentCategoryManagement)
        : null;
    const developmentCategoryRoute = existingDevelopmentCategoryRoute || createDevelopmentCategoryRoute(projectBaseRoute);
    developmentCategoryRoute.children = uniqueRoutesByPathOrTitle(
        (developmentCategoryRoute.children || [])
            .filter((child) => !isIntegrationTaskCat(child) && !isDataDevCat(child))
            .map((route) => normalizeMovedChildPath(route))
    );

    const sourceRoutes = [projectBaseExistingChildren, routes];
    const projectRoutes = collectRoutes(sourceRoutes, isProjectManagement);
    const projectAssetRoutes = collectRoutes(sourceRoutes, isProjectAssetManagement);
    const ruleRoutes = collectRoutes(sourceRoutes, isRuleManagement);
    const categoryRoutes = collectRoutes(sourceRoutes, isCategoryManagement);
    const memberRoleRoutes = collectRoutes(sourceRoutes, isMemberRoleManagement);
    const qualityCatRoutes = collectRoutes(sourceRoutes, isQualityCat);
    const integrationCatRoutes = collectRoutes(sourceRoutes, isIntegrationTaskCat);
    const dataDevCatRoutes = collectRoutes(sourceRoutes, isDataDevCat);

    const projectRoute = projectRoutes[0];
    if (projectRoute) {
        projectRoute.alwaysShow = true;
        projectRoute.children = uniqueRoutesByPathOrTitle([
            ...(projectRoute.children || []),
            ...ruleRoutes
        ]).map((route) => normalizeMovedChildPath(route));
    }

    const categoryRoute = categoryRoutes[0];
    if (categoryRoute) {
        categoryRoute.alwaysShow = true;
        categoryRoute.children = uniqueRoutesByPathOrTitle([
            ...(categoryRoute.children || []),
            ...qualityCatRoutes,
            ...integrationCatRoutes,
            ...dataDevCatRoutes
        ]).map((route) => normalizeMovedChildPath(route));
    }

    projectBaseRoute.children = uniqueRoutesByPathOrTitle([
        projectRoute,
        projectAssetRoutes[0],
        categoryRoute,
        ...memberRoleRoutes,
        ...projectBaseExistingChildren
    ].filter(Boolean)).map((route) => normalizeMovedChildPath(route));

    normalizeProjectBaseRoute(projectBaseRoute);
    insertBeforeRoute(routes, dppRoute, projectBaseRoute);
    insertDevelopmentCategoryFirst(dppRoute, developmentCategoryRoute);
}

function createDevelopmentCategoryRoute(sourceRoute) {
    return {
        path: 'setting',
        name: 'ColDevelopmentCategory',
        component: ParentView,
        redirect: 'noRedirect',
        alwaysShow: true,
        meta: {
            title: '研发类目管理',
            icon: sourceRoute && sourceRoute.meta ? sourceRoute.meta.icon : 'briefcase-2-line'
        },
        children: []
    };
}

function detachProjectBaseRoute(routes, dppRoute) {
    return (
        detachFirstRoute(routes, isProjectBaseManagement) ||
        (dppRoute.children ? detachFirstRoute(dppRoute.children, isProjectBaseManagement) : null)
    );
}

function createProjectBaseRoute() {
    return {
        path: '/projectBase',
        name: 'ProjectBaseManagement',
        component: Layout,
        redirect: 'noRedirect',
        alwaysShow: true,
        meta: {
            title: '项目基础管理',
            icon: 'lifebuoy-line'
        },
        children: []
    };
}

function normalizeProjectBaseRoute(projectBaseRoute) {
    projectBaseRoute.path = '/projectBase';
    projectBaseRoute.name = 'ProjectBaseManagement';
    projectBaseRoute.component = Layout;
    projectBaseRoute.redirect = getProjectBaseRedirect(projectBaseRoute);
    projectBaseRoute.alwaysShow = true;
    projectBaseRoute.meta = projectBaseRoute.meta || {};
    projectBaseRoute.meta.title = '项目基础管理';
    projectBaseRoute.meta.icon = projectBaseRoute.meta.icon || 'lifebuoy-line';
}

function getProjectBaseRedirect(projectBaseRoute) {
    const firstChild = projectBaseRoute.children && projectBaseRoute.children[0];
    if (!firstChild || !firstChild.path) return 'noRedirect';
    return firstChild.path.startsWith('/')
        ? firstChild.path
        : `/projectBase/${firstChild.path}`;
}

function detachFirstRoute(routes, predicate) {
    if (!routes || !routes.length) return null;
    const index = routes.findIndex((route) => predicate(route));
    if (index === -1) return null;
    return routes.splice(index, 1)[0];
}

function collectRoutes(routeLists, predicate) {
    return uniqueRoutesByPathOrTitle(
        routeLists.flatMap((routes) => detachRoutes(routes, predicate))
    );
}

function detachRoutes(routes, predicate) {
    const matchedRoutes = [];
    if (!routes || !routes.length) return matchedRoutes;

    for (let index = routes.length - 1; index >= 0; index--) {
        const route = routes[index];
        if (predicate(route)) {
            matchedRoutes.unshift(routes.splice(index, 1)[0]);
            continue;
        }
        if (route.children && route.children.length) {
            matchedRoutes.push(...detachRoutes(route.children, predicate));
        }
    }
    return matchedRoutes;
}

function insertBeforeRoute(routes, targetRoute, routeToInsert) {
    const existingIndex = routes.findIndex((route) => route === routeToInsert || isProjectBaseManagement(route));
    if (existingIndex !== -1) {
        routes.splice(existingIndex, 1);
    }

    const targetIndex = routes.findIndex((route) => route === targetRoute);
    if (targetIndex === -1) {
        routes.unshift(routeToInsert);
        return;
    }
    routes.splice(targetIndex, 0, routeToInsert);
}

function uniqueRoutesByPathOrTitle(routes) {
    const seen = new Set();
    return routes.filter((route) => {
        const title = route.meta && route.meta.title;
        const key = `${route.path || ''}:${title || route.name || ''}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    });
}

function normalizeMovedChildPath(route) {
    if (!route || !route.path || !route.path.includes('/')) return route;
    route.path = route.path.split('/').filter(Boolean).pop();
    return route;
}

function insertDevelopmentCategoryFirst(dppRoute, developmentCategoryRoute) {
    dppRoute.children = (dppRoute.children || []).filter(
        (child) => !isDevelopmentCategoryManagement(child)
    );
    if (!developmentCategoryRoute.children || !developmentCategoryRoute.children.length) {
        return;
    }
    dppRoute.children.unshift(developmentCategoryRoute);
}

function moveQualityCatUnderQualityMenu(routes) {
    const metadataRoute = findRoute(routes, (route) => isMetadataManagement(route));
    if (metadataRoute && metadataRoute.children) {
        const qualityRoute = metadataRoute.children.find(
            (child) => isQualityParentMenu(child)
        );
        const qualityCatIndex = metadataRoute.children.findIndex(
            (child) => child.meta && child.meta.title === '数据质量类目'
        );
        if (qualityRoute) {
            if (qualityCatIndex !== -1) {
                const qualityCatRoute = metadataRoute.children.splice(qualityCatIndex, 1)[0];
                addQualityCatAsFirstChild(qualityRoute, qualityCatRoute);
                return;
            }
        }
        if (qualityCatIndex !== -1) {
            normalizeQualityCatAsMetadataChild(metadataRoute.children[qualityCatIndex]);
            return;
        }
    }

    const basicRoute = routes.find((route) => isBasicManagement(route));
    if (!basicRoute || !basicRoute.children) return;

    const catRoute = basicRoute.children.find(
        (child) => child.meta && child.meta.title === '类目管理'
    );
    if (!catRoute || !catRoute.children) return;

    const qualityCatIndex = catRoute.children.findIndex(
        (child) => child.meta && child.meta.title === '数据质量类目'
    );
    if (qualityCatIndex === -1) return;

    const daRoute = routes.find((route) => isDAManagement(route));
    if (!daRoute || !daRoute.children) return;

    const qualityRoute = daRoute.children.find(
        (child) => child.meta && child.meta.title === '数据质量'
    );
    if (!qualityRoute) return;

    const qualityCatRoute = catRoute.children.splice(qualityCatIndex, 1)[0];
    addQualityCatAsFirstChild(qualityRoute, qualityCatRoute);
}

function addQualityCatAsFirstChild(qualityRoute, qualityCatRoute) {
    qualityRoute.children = (qualityRoute.children || []).filter(
        (child) => !(child.meta && child.meta.title === '数据质量类目')
    );
    qualityCatRoute.path = 'qualityCat';
    normalizeQualityCatRoute(qualityCatRoute);
    qualityRoute.children.unshift(qualityCatRoute);
}

function normalizeQualityCatAsMetadataChild(qualityCatRoute) {
    qualityCatRoute.path = 'quality/qualityCat';
    normalizeQualityCatRoute(qualityCatRoute);
}

function normalizeQualityCatRoute(qualityCatRoute) {
    qualityCatRoute.name = 'QualityCatProject';
    qualityCatRoute.component = loadView('att/cat/qualityCat/index');
    qualityCatRoute.meta = qualityCatRoute.meta || {};
    qualityCatRoute.meta.title = '数据质量类目';
    qualityCatRoute.meta.activeMenu = '/cat/quality/qualityCat';
    delete qualityCatRoute.children;
    delete qualityCatRoute.redirect;
}

function isDAManagement(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return title === '数据资产' || path === '/da' || path === 'da' || path === '/ast' || path === 'ast';
}

function normalizeMenuTitle(route) {
    if (!route) return route;
    normalizeModuleRoute(route);
    if (route.meta) {
        if (route.meta.title === '系统监控') {
            route.meta.title = '日志管理';
        }
        if (route.meta.title === '数据治理') {
            route.meta.title = '元数据管理';
        }
        if (route.meta.title === '数据连接') {
            route.meta.title = '数据源管理';
        }
    }
    if (route.children && route.children.length) {
        route.children = route.children
            .map((child) => normalizeMenuTitle(child))
            .filter(Boolean);
    }
    return route;
}

function mergeBasicManagement(routes) {
    const systemRoute = routes.find((route) => isSystemManagement(route));
    const basicIndexes = [];

    routes.forEach((route, index) => {
        if (isBasicManagement(route)) {
            basicIndexes.push(index);
        }
    });

    if (!basicIndexes.length) return;

    let targetRoute = systemRoute;
    if (!targetRoute) {
        targetRoute = routes[basicIndexes[0]];
        targetRoute.meta = targetRoute.meta || {};
        targetRoute.meta.title = '系统管理';
    }

    targetRoute.children = targetRoute.children || [];
    basicIndexes
        .filter((index) => routes[index] !== targetRoute)
        .reverse()
        .forEach((index) => {
            const basicRoute = routes[index];
            if (basicRoute.children && basicRoute.children.length) {
                targetRoute.children.push(...basicRoute.children);
            }
            routes.splice(index, 1);
        });
}

function moveQualityMenusToMetadata(routes) {
    const metadataRoute = findRoute(routes, (route) => isMetadataManagement(route));
    if (!metadataRoute) return;

    const qualityRoutes = [];
    removeRoutes(routes, (route) => {
        if (route === metadataRoute) return false;
        if (isQualityMenu(route)) {
            qualityRoutes.push(route);
            return true;
        }
        return false;
    });

    if (!qualityRoutes.length) return;

    metadataRoute.children = metadataRoute.children || [];
    qualityRoutes.forEach((qualityRoute) => {
        if (!metadataRoute.children.some((child) => child.path === qualityRoute.path)) {
            metadataRoute.children.push(qualityRoute);
        }
    });
}

function filterHomeMenus(routes) {
    const homeRoutes = [homeRoute];
    routes.forEach((route) => {
        if (isHomeMenu(route) && !homeRoutes.some((item) => item.path === route.path)) {
            homeRoutes.push(route);
        }
    });
    return homeRoutes;
}

function removeRoutes(routes, predicate) {
    for (let index = routes.length - 1; index >= 0; index--) {
        const route = routes[index];
        if (predicate(route)) {
            routes.splice(index, 1);
            continue;
        }
        if (route.children && route.children.length) {
            removeRoutes(route.children, predicate);
        }
    }
}

function findRoute(routes, predicate) {
    for (const route of routes) {
        if (predicate(route)) return route;
        if (route.children && route.children.length) {
            const matched = findRoute(route.children, predicate);
            if (matched) return matched;
        }
    }
    return null;
}

function isHomeMenu(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return (
        homeMenuTitles.includes(title) ||
        path === '/index' ||
        path === 'index' ||
        path === '/system' ||
        path === 'system' ||
        path === '/monitor' ||
        path === 'monitor'
    );
}

function isSystemManagement(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return title === '系统管理' || path === '/system' || path === 'system';
}

function isDevelopmentManagement(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return title === '数据研发' || path === '/col' || path === 'col';
}

function isProjectBaseManagement(route) {
    const title = route.meta && route.meta.title;
    return title === '项目基础管理';
}

function isProjectManagement(route) {
    const title = route.meta && route.meta.title;
    return title === '项目管理';
}

function isProjectAssetManagement(route) {
    const title = route.meta && route.meta.title;
    const name = route.name || '';
    return title === '项目资产' || name === 'colAsset';
}

function isRuleManagement(route) {
    const title = route.meta && route.meta.title;
    return title === '规则管理';
}

function isCategoryManagement(route) {
    const title = route.meta && route.meta.title;
    return title === '类目管理';
}

function isDevelopmentCategoryManagement(route) {
    const title = route.meta && route.meta.title;
    const name = route.name || '';
    return title === '研发类目管理' || name === 'ColDevelopmentCategory';
}

function isMemberRoleManagement(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return title === '成员角色管理' || path === 'projectUserRel' || path.endsWith('/projectUserRel');
}

function isIntegrationTaskCat(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return title === '数据集成类目' || path === 'taskCat' || path.endsWith('/taskCat');
}

function isDataDevCat(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return title === '数据开发类目' || path === 'dataDevCat' || path.endsWith('/dataDevCat');
}

function isQualityCat(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return title === '数据质量类目' || path === 'qualityCat' || path.endsWith('/qualityCat');
}

function isBasicManagement(route) {
    const title = route.meta && route.meta.title;
    return title === '基础管理';
}

function isMetadataManagement(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return (
        title === '元数据管理' ||
        path === '/meta' ||
        path === 'meta' ||
        path === '/dg' ||
        path === 'dg'
    );
}

function isQualityMenu(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return (title && title.includes('数据质量')) || path.includes('/quality') || path.includes('quality');
}

function isQualityParentMenu(route) {
    const title = route.meta && route.meta.title;
    const path = route.path || '';
    return (
        title === '数据质量' ||
        path === 'quality' ||
        path === '/quality' ||
        path === '/cat/quality' ||
        path === 'cat/quality'
    );
}

function moveDatasourceToMetadata(routes) {
    const metadataRoute = findRoute(routes, (route) => isMetadataManagement(route));
    if (!metadataRoute) return;

    if (metadataRoute.children && metadataRoute.children.some((child) =>
        child.meta && child.meta.title === '数据源管理'
    )) return;

    let datasourceRoute = null;

    removeRoutes(routes, (route) => {
        if (route === metadataRoute) return false;
        if (route.meta && route.meta.title === '数据源管理') {
            datasourceRoute = route;
            return true;
        }
        return false;
    });

    if (!datasourceRoute) return;

    metadataRoute.children = metadataRoute.children || [];
    metadataRoute.children.unshift(datasourceRoute);
}

function reorderTopLevelRoutes(routes) {
    const metaIndex = routes.findIndex((route) => isMetadataManagement(route));
    if (metaIndex > 0) {
        const metaRoute = routes.splice(metaIndex, 1)[0];
        routes.unshift(metaRoute);
    }
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
    const normalizedView = normalizeModuleRoutePath(view);
    let res;
    for (const path in modules) {
        const dir = path.split('views/')[1].split('.vue')[0];
        if (dir === view || dir === normalizedView) {
            res = () => modules[path]();
        }
    }
    return res;
};

export default usePermissionStore;

