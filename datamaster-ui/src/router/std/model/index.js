
/* Layout */
import Layout from '@/layout/index.vue';
export default [

    {
        path: '/mdl/model/create',
        component: Layout,
        redirect: 'create',
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/std/model/index.vue'),
                name: 'modelCreate',
                meta: { title: '逻辑模型', activeMenu: '/mdl/model/create' }
            }
        ]
    },

    {
        path: '/mdl/model/detail',
        component: Layout,
        redirect: 'detail',
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/std/model/detail/index.vue'),
                name: 'modelDetail',
                meta: { title: '逻辑模型详情', activeMenu: '/mdl/model/create' }
            }
        ]
    },
];

