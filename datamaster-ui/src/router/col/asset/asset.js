
/* Layout */
import Layout from '@/layout/index.vue';

// 数据资产模块动公共路由
export default [
    {
        path: '/col/asset/detail',
        component: Layout,
        hidden: true,
        meta: { fullScreen: true, noCache: true },
        children: [
            {
                path: '',
                // 详情页也使用空间工作站壳，避免从资产列表进入时回到旧版全局菜单。
                component: () => import('@/views/explore/space.vue'),
                name: 'colDaAssetDetail',
                meta: { title: '数据资产详情', fullScreen: true, noCache: true }
            }
        ]
    },

];

