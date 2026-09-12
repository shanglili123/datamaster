
/* Layout */
import Layout from '@/layout/index.vue';

// 数据资产模块动公共路由
export default [
    {
        path: '/ast/asset/detail',
        component: Layout,
        hidden: true,
        meta: { fullScreen: true, noCache: true },
        children: [
            {
                path: '',
                component: () => import('@/views/explore/space.vue'),
                name: 'daDaAssetDetail',
                meta: { title: '资产数据详情', fullScreen: true, noCache: true }
            }
        ]
    },
];

