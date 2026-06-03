
/* Layout */
import Layout from '@/layout/index.vue';

// 数据资产模块动公共路由
export default [
    {
        path: '/svc/api/detail',
        component: Layout,
        redirect: 'detail',
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/svc/api/detail/index.vue'),
                name: 'dsApiDetail',
                meta: { title: 'API服务详情', activeMenu: '/svc/api' }
            }
        ]
    },

    {
        path: '/svc/api/edit',
        component: Layout,
        redirect: 'edit',
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/svc/api/edit/index.vue'),
                name: 'dsApiEdit',
                meta: { title: 'API服务修改', activeMenu: '/svc/api' }
            }
        ]
    },
    {
        path: '/svc/api/add',
        component: Layout,
        redirect: 'add',
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/svc/api/edit/index.vue'),
                name: 'dsApiAdd',
                meta: { title: 'API服务新增', activeMenu: '/svc/api' }
            }
        ]
    },

];

