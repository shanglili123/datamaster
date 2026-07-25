
/* Layout */
import Layout from '@/layout/index.vue';

// 标准数据
export default [
    {
        path: '/mdl/document/list',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/std/document/list/index.vue'),
                name: 'documentList',
                meta: { title: '标准数据集', activeMenu: '/mdl/document' }
            },
        ]
    },
    {
        path: '/mdl/document/docs',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/std/document/docs/index.vue'),
                name: 'documentDocs',
                meta: { title: '标准文档', activeMenu: '/mdl/document' }
            },
        ]
    },
    {
        path: '/mdl/document/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/std/document/detail/index.vue'),
                name: 'documentDetail',
                meta: { title: '标准详情', activeMenu: '/mdl/document' }
            },
        ]
    },
]
