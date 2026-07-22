/* Layout */
import Layout from '@/layout/index.vue';

const tableHandleComponent = () => import('@/views/meta/unreleased/structured/table/handle/index.vue');
const tableDetailComponent = () => import('@/views/meta/unreleased/structured/table/detail/index.vue');

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
                meta: { title, activeMenu: '/cat/meta/management' }
            }
        ]
    };
}

export default [
    {
        path: '/meta/unreleased/structured/db/detail',
        component: Layout,
        hidden: true,
        permissions: ['mc:metadata:table:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/unreleased/structured/database/detail/index.vue'),
                name: 'UnreleasedStructuredDatabaseDetail',
                meta: { title: '库元数据详情', activeMenu: '/cat/meta/management' }
            }
        ]
    },
    tableRoute(
        '/cat/meta/management/add',
        'UnreleasedStructuredTableAdd',
        '新增表元数据',
        'mc:metadata:table:add',
        tableHandleComponent
    ),
    tableRoute(
        '/cat/meta/management/edit',
        'UnreleasedStructuredTableEdit',
        '修改表元数据',
        'mc:metadata:table:edit',
        tableHandleComponent
    ),
    tableRoute(
        '/cat/meta/management/detail',
        'UnreleasedStructuredTableDetail',
        '表元数据详情',
        'mc:metadata:table:detail',
        tableDetailComponent
    ),
    tableRoute(
        '/meta/unreleased/structured/table/add',
        'UnreleasedStructuredTableAddLegacy',
        '新增表元数据',
        'mc:metadata:table:add',
        tableHandleComponent
    ),
    tableRoute(
        '/meta/unreleased/structured/table/edit',
        'UnreleasedStructuredTableEditLegacy',
        '修改表元数据',
        'mc:metadata:table:edit',
        tableHandleComponent
    ),
    tableRoute(
        '/meta/unreleased/structured/table/detail',
        'UnreleasedStructuredTableDetailLegacy',
        '表元数据详情',
        'mc:metadata:table:detail',
        tableDetailComponent
    ),
    {
        path: '/meta/unreleased/structured/column/add',
        component: Layout,
        hidden: true,
        permissions: ['mc:metadata:table:add'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/unreleased/structured/column/add/index.vue'),
                name: 'UnreleasedStructuredColumnAdd',
                meta: { title: '新增字段元数据', activeMenu: '/cat/meta/management' }
            }
        ]
    },
    {
        path: '/meta/unreleased/structured/column/detail',
        component: Layout,
        hidden: true,
        permissions: ['mc:metadata:table:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/unreleased/structured/column/detail/index.vue'),
                name: 'UnreleasedStructuredColumnDetail',
                meta: { title: '字段元数据详情', activeMenu: '/cat/meta/management' }
            }
        ]
    }
];
