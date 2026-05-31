
/* Layout */
import Layout from '@/layout/index.vue';

// 标准
export default [
    {
        path: '/mdl/document/national/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/dp/document/detail/index.vue'),
                name: 'national',
                meta: { title: '国家标准详情', activeMenu: '/mdl/document/national' }
            },
        ]
    },
    {
        path: '/mdl/document/industry/detail',
        component: Layout,
        children: [

            {
                path: '',
                component: () => import('@/views/dp/document/detail/index.vue'),
                name: 'industrylocal',
                meta: { title: '行业标准详情', activeMenu: '/mdl/document/industry' }
            },

        ]
    },
    {
        path: '/mdl/document/provincial/detail',
        component: Layout,
        children: [


            {
                path: '',
                component: () => import('@/views/dp/document/detail/index.vue'),
                name: 'provincial',
                meta: { title: '地方标准详情', activeMenu: '/mdl/document/provincial' }
            },

        ]
    },
    {
        path: '/mdl/document/group/detail',
        component: Layout,
        children: [
            {
                path: '',
                component: () => import('@/views/dp/document/detail/index.vue'),
                name: 'groupDetail',
                meta: { title: '团体标准详情', activeMenu: '/mdl/document/group' }
            },

        ]
    },
    {
        path: '/mdl/document/search/detail',
        component: Layout,
        children: [
            {
                path: '',
                component: () => import('@/views/dp/document/detail/index.vue'),
                name: 'search',
                meta: { title: '标准检索详情', activeMenu: '/mdl/document/search' }
            },
        ]
    },
]
//     }
// ];

