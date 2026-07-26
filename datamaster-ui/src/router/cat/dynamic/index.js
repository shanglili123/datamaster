/* Layout */
import Layout from '@/layout/index.vue';

const tableHandleComponent = () => import('@/views/cat/unreleased/structured/table/handle/index.vue');
const tableDetailComponent = () => import('@/views/cat/unreleased/structured/table/detail/index.vue');

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
                meta: { title, activeMenu: '/dg/cat/management' }
            }
        ]
    };
}

export default [
    {
        path: '/cat/task/detail',
        component: Layout,
        hidden: true,
        permissions: ['mc:task:structured:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/cat/task/structured/detail/index.vue'),
                name: 'McTaskStructuredDetail',
                meta: { title: '元数据采集详情', activeMenu: '/dg/cat/task' }
            }
        ]
    },
    {
        path: '/cat/unreleased/structured/db/detail',
        component: Layout,
        hidden: true,
        permissions: ['mc:metadata:table:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/cat/unreleased/structured/database/detail/index.vue'),
                name: 'UnreleasedStructuredDatabaseDetail',
                meta: { title: '库元数据详情', activeMenu: '/dg/cat/management' }
            }
        ]
    },
    tableRoute(
        '/dg/cat/management/add',
        'UnreleasedStructuredTableAdd',
        '新增表元数据',
        'mc:metadata:table:add',
        tableHandleComponent
    ),
    tableRoute(
        '/dg/cat/management/edit',
        'UnreleasedStructuredTableEdit',
        '修改表元数据',
        'mc:metadata:table:edit',
        tableHandleComponent
    ),
    tableRoute(
        '/dg/cat/management/detail',
        'UnreleasedStructuredTableDetail',
        '表元数据详情',
        'mc:metadata:table:detail',
        tableDetailComponent
    ),
    tableRoute(
        '/cat/unreleased/structured/table/add',
        'UnreleasedStructuredTableAddLegacy',
        '新增表元数据',
        'mc:metadata:table:add',
        tableHandleComponent
    ),
    tableRoute(
        '/cat/unreleased/structured/table/edit',
        'UnreleasedStructuredTableEditLegacy',
        '修改表元数据',
        'mc:metadata:table:edit',
        tableHandleComponent
    ),
    tableRoute(
        '/cat/unreleased/structured/table/detail',
        'UnreleasedStructuredTableDetailLegacy',
        '表元数据详情',
        'mc:metadata:table:detail',
        tableDetailComponent
    ),
    {
        path: '/cat/unreleased/structured/column/add',
        component: Layout,
        hidden: true,
        permissions: ['mc:metadata:table:add'],
        children: [
            {
                path: '',
                component: () => import('@/views/cat/unreleased/structured/column/add/index.vue'),
                name: 'UnreleasedStructuredColumnAdd',
                meta: { title: '新增字段元数据', activeMenu: '/dg/cat/management' }
            }
        ]
    },
    {
        path: '/cat/unreleased/structured/column/detail',
        component: Layout,
        hidden: true,
        permissions: ['mc:metadata:table:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/cat/unreleased/structured/column/detail/index.vue'),
                name: 'UnreleasedStructuredColumnDetail',
                meta: { title: '字段元数据详情', activeMenu: '/dg/cat/management' }
            }
        ]
    }
];
