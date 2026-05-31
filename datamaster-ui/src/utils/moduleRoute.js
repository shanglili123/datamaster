const OLD_TO_NEW_PREFIX = {
    da: 'ast',
    dp: 'std',
    dpp: 'col',
    ds: 'svc',
    dm: 'mdl',
    mc: 'cat',
    dg: 'cat',
    att: 'tax'
};

export const PROJECT_MODULE_ROUTE_PREFIXES = [
    '/projectBase',
    '/col',
    '/svc',
    '/mdl',
    '/cat',
    '/ast',
    '/std',
    '/tax',
    '/meta'
];

export function normalizeModuleRoutePath(path) {
    if (!path || typeof path !== 'string' || isExternalPath(path)) {
        return path;
    }

    const hasLeadingSlash = path.startsWith('/');
    const normalizedPath = hasLeadingSlash ? path.slice(1) : path;
    const segments = normalizedPath.split('/');
    const firstSegment = segments[0];
    const newPrefix = OLD_TO_NEW_PREFIX[firstSegment];

    if (!newPrefix) return path;

    segments[0] = newPrefix;
    return `${hasLeadingSlash ? '/' : ''}${segments.join('/')}`;
}

export function normalizeModuleRouteTree(routes) {
    return (routes || []).map((route) => normalizeModuleRoute(route));
}

export function normalizeModuleRoute(route) {
    if (!route) return route;

    route.path = normalizeModuleRoutePath(route.path);
    route.redirect = normalizeModuleRoutePath(route.redirect);
    route.parentPath = normalizeModuleRoutePath(route.parentPath);

    if (route.meta) {
        route.meta.activeMenu = normalizeModuleRoutePath(route.meta.activeMenu);
    }

    if (route.children && route.children.length) {
        route.children = normalizeModuleRouteTree(route.children);
    }

    return route;
}

export function isProjectModuleRoute(path) {
    const normalizedPath = normalizeModuleRoutePath(path) || '';
    return PROJECT_MODULE_ROUTE_PREFIXES.some(
        (prefix) => normalizedPath === prefix || normalizedPath.startsWith(`${prefix}/`)
    );
}

function isExternalPath(path) {
    return /^(https?:|mailto:|tel:)/i.test(path);
}
