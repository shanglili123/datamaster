
/* Layout */
import Layout from '@/layout/index.vue';

// 数据资研发模块公共路由
export default [
    {
        path: '/col/task/integratioTask/edit',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/col/task/integratioTask/edit/index.vue'),
                name: 'integratioTaskEdit',
                meta: { title: '数据集成任务配置任务', activeMenu: '/col/task/integratioTask' }
            },
        ]
    },
    {
        path: '/col/task/integratioTask/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/col/task/integratioTask/detail/index.vue'),
                name: 'integratioTaskDetail',
                meta: { title: '数据集成任务详情', activeMenu: '/col/task/integratioTask' }
            }
        ]
    },

];

