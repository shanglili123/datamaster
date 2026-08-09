
/* Layout */
import Layout from '@/layout/index.vue'

// 质量探查模块动公共路由
export default [
    {
        path: '/ast/quality/qualityTask/add',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ast/quality/qualityTask/add/add.vue'),
                name: 'qualityTaskAdd',
                meta: { title: '质量探查新增', activeMenu: '/meta/catalog/qualityTask' }
            }
        ]
    },
    {
        path: '/ast/quality/qualityTask/edit',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ast/quality/qualityTask/add/add.vue'),
                name: 'qualityTaskEdit',
                meta: { title: '质量探查配置', activeMenu: '/meta/catalog/qualityTask' }
            },
        ]
    },
    {
        path: '/ast/quality/qualityTask/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ast/quality/qualityTask/add/add.vue'),
                name: 'qualityTaskDetail',
                meta: { title: '质量探查详情', activeMenu: '/meta/catalog/qualityTask' }
            }
        ]
    },

    {
        path: '/ast/quality/probeTaskInstance/detail',
        component: Layout,
        redirect: 'detail',
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ast/quality/probeTaskInstance/detail/index.vue'),
                name: 'probeTaskInstanceDetail',
                meta: { title: '探查任务实例详情', activeMenu: '/ast/quality/probeTaskInstance' }
            }
        ]
    },
    {
        path: '/ast/quality/errorStorageConfig',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ast/quality/errorStorageConfig/index.vue'),
                name: 'errorStorageConfig',
                meta: { title: '错误明细存储配置', activeMenu: '/ast/quality/qualityTask' }
            }
        ]
    },
]

