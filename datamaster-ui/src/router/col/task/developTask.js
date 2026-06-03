
/* Layout */
import Layout from '@/layout/index.vue';

// 数据资研发模块公共路由
export default [

    {
        path: '/col/task/developTask/edit',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/col/task/developTask/detail/index.vue'),
                name: 'developTaskEdit',
                meta: { title: '数据开发配置转换', activeMenu: '/col/task/developTask' }
            },
        ]
    },
    {
        path: '/col/task/developTask/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/col/task/developTask/detail/index.vue'),
                name: 'developTaskDetail',
                meta: { title: '数据开发详情', activeMenu: '/col/task/developTask' }
            }
        ]
    },
];

