
/* Layout */
import Layout from '@/layout/index.vue';

// 业务分类模块公共路由
export default [
    {
        path: '/mdl/businessCategory/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/dm/businessCategory/detail/index.vue'),
                name: 'BusinessLayerDetail',
                meta: {
                    title: '业务分类详情',
                    activeMenu: '/mdl/businessCategory'
                }
            },
        ]
    },
];

