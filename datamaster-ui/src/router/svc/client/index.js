
/* Layout */
import Layout from '@/layout/index.vue';

// 示例模块动态路由，基于用户权限动态去加载
export default [
    {
        path: '/svc/client/clientDetail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/svc/client/detail/index.vue'),
                name: 'clientDetail',
                meta: { title: '应用管理详情', activeMenu: '/svc/client' }
            }
        ]
    },

];

