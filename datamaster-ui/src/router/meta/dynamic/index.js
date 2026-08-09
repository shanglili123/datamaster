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
    {
        path: '/meta/unreleased/structured/db/detail',
        component: Layout,
        hidden: true,
        permissions: ['cat:table:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/unreleased/structured/database/detail/index.vue'),
                name: 'UnreleasedStructuredDatabaseDetail',
                meta: { title: '库元数据详情', activeMenu: '/meta/catalog/management' }
            }
        ]
    },
    tableRoute(
        '/meta/unreleased/structured/table/add',
        'UnreleasedStructuredTableAdd',
        '新增表元数据',
        'cat:table:add',
        tableHandleComponent
    ),
    tableRoute(
        '/meta/unreleased/structured/table/edit',
        'UnreleasedStructuredTableEdit',
        '修改表元数据',
        'cat:table:edit',
        tableHandleComponent
    ),
    tableRoute(
        '/meta/unreleased/structured/table/detail',
        'UnreleasedStructuredTableDetail',
        '表元数据详情',
        'cat:table:detail',
        tableDetailComponent
    ),
    {
        path: '/meta/unreleased/structured/column/add',
        component: Layout,
        hidden: true,
        permissions: ['cat:table:add'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/unreleased/structured/column/add/index.vue'),
                name: 'UnreleasedStructuredColumnAdd',
                meta: { title: '新增字段元数据', activeMenu: '/meta/catalog/management' }
            }
        ]
    },
    {
        path: '/meta/unreleased/structured/column/detail',
        component: Layout,
        hidden: true,
        permissions: ['cat:table:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/meta/unreleased/structured/column/detail/index.vue'),
                name: 'UnreleasedStructuredColumnDetail',
                meta: { title: '字段元数据详情', activeMenu: '/meta/catalog/management' }
            }
        ]
    }
];
