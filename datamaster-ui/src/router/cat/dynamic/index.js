/* Layout */
import Layout from '@/layout/index.vue';

export default [
    {
        path: '/cat/meta/task/detail',
        component: Layout,
        hidden: true,
        permissions: ['mc:task:structured:detail'],
        children: [
            {
                path: '',
                component: () => import('@/views/cat/task/structured/detail/index.vue'),
                name: 'McTaskStructuredDetail',
                meta: { title: '元数据采集详情', activeMenu: '/cat/meta/task' }
            }
        ]
    }
];
