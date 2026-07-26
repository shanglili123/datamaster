export const PROJECT_MODULE_ROUTE_PREFIXES = [
    '/projectBase',
    '/col',
    '/svc',
    '/mdl',
    '/cat',
    '/ast',
    '/std',
    '/tax',
    '/meta',
    '/ai'
];

export function isProjectModuleRoute(path) {
    const routePath = typeof path === 'string' && !isExternalPath(path) ? path : '';
    return PROJECT_MODULE_ROUTE_PREFIXES.some(
        (prefix) => routePath === prefix || routePath.startsWith(`${prefix}/`)
    );
}

function isExternalPath(path) {
    return /^(https?:|mailto:|tel:)/i.test(path);
}
