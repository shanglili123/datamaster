
/* Layout */
import Layout from '@/layout/index.vue';

// 数据资产模块动公共路由
export default [
    {
        path: '/col/asset/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/col/asset/detail/index.vue'),
                name: 'colDaAssetDetail',
                meta: { title: '数据资产详情', activeMenu: '/col/asset' }
            }
        ]
    },

];

