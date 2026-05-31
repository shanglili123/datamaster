
/* Layout */
import Layout from '@/layout/index.vue';

// 示例模块动公共路由
export default [
    {
        path: '/cat/safety/desensitizationRules/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '', // 使用动态路由参数
                component: () => import('@/views/dg/safety/desensitizationRules/detail/index.vue'),
                name: 'DataElemCodeDetail',
                meta: {
                    title: '脱敏规则详情',
                    activeMenu: '/cat/safety/desensitizationRules'
                }
            },
        ]
    },

];

