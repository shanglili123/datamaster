/* Layout */
import Layout from '@/layout/index.vue';

const tableHandleComponent = () => import('@/views/meta/catalog/table/handle/index.vue');
const tableDetailComponent = () => import('@/views/meta/catalog/table/detail/index.vue');
const probeTaskInstanceDetailComponent = () => import('@/views/ast/quality/probeTaskInstance/detail/index.vue');

function tableRoute(path, name, title, permission, component) {
    return {
        path,
        component: Layout,
        hidden: true,
        permissions: [permission],
        children: [
            {
                path: '',
                component,
                name,
                meta: { title, activeMenu: '/meta/catalog/management' }
            }
        ]
    };
}

export default [
    {
        path: '/meta/task/detail',
        component: Layout,
        hidden: true,
        permissions: ['cat:task:structured:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/task/structured/detail/index.vue'),
                name: 'McTaskStructuredDetail',
                meta: { title: '元数据采集详情', activeMenu: '/meta/catalog/task' }
            }
        ]
    },
    tableRoute(
        '/meta/catalog/table/add',
        'CatalogTableAdd',
        '新增表元数据',
        'cat:table:add',
        tableHandleComponent
    ),
    tableRoute(
        '/meta/catalog/table/edit',
        'CatalogTableEdit',
        '修改表元数据',
        'cat:table:edit',
        tableHandleComponent
    ),
    tableRoute(
        '/meta/catalog/table/detail',
        'CatalogTableDetail',
        '表元数据详情',
        'cat:table:detail',
        tableDetailComponent
    ),
    {
        path: '/meta/probeResult/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: probeTaskInstanceDetailComponent,
                name: 'ProbeResultDetail',
                meta: { title: '质量探查报告', activeMenu: '/meta/probeResult' }
            }
        ]
    },
    {
        path: '/meta/catalog/column/add',
        component: Layout,
        hidden: true,
        permissions: ['cat:table:add'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/catalog/column/add/index.vue'),
                name: 'CatalogColumnAdd',
                meta: { title: '新增字段元数据', activeMenu: '/meta/catalog/management' }
            }
        ]
    }
];
