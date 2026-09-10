export const SPACE_MODULE_ROUTE_PREFIXES = [
    '/space',
    '/spaceBase',
    '/col',
    '/svc',
    '/mdl',
    '/cat',
    '/ast',
    '/std',
    '/tax',
    '/meta',
    '/ai',
    '/ont'
];

export function isSpaceModuleRoute(path) {
    const routePath = typeof path === 'string' && !isExternalPath(path) ? path : '';
    return SPACE_MODULE_ROUTE_PREFIXES.some(
        (prefix) => routePath === prefix || routePath.startsWith(`${prefix}/`)
    );
}

function isExternalPath(path) {
    return /^(https?:|mailto:|tel:)/i.test(path);
}
