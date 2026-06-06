
/* Layout */
import Layout from '@/layout/index.vue'

// 数据质量模块动公共路由
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
                meta: { title: '数据质量新增', activeMenu: '/ast/quality/qualityTask' }
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
                meta: { title: '数据质量配置', activeMenu: '/ast/quality/qualityTask' }
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
                meta: { title: '数据质量详情', activeMenu: '/ast/quality/qualityTask' }
            }
        ]
    },

    {
        path: '/ast/quality/qualityTaskLog/detail',
        component: Layout,
        redirect: 'detail',
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ast/quality/qualityTaskLog/detail/index.vue'),
                name: 'qualityTaskLogDetail',
                meta: { title: '质量任务日志详情', activeMenu: '/ast/quality/qualityTaskLog' }
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

