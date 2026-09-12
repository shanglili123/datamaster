import Layout from '@/layout/index.vue'

const aiRouter = [
  {
    path: '/ai',
    component: Layout,
    redirect: '/ai/ask',
    alwaysShow: true,
    name: 'Ai',
    meta: { title: '数据智能体', icon: 'message' },
    children: [
      {
        path: 'ask',
        component: () => import('@/views/ai/chat/index/index.vue'),
        name: 'Ask',
        meta: { title: '决策智能体', icon: 'message' }
      },
      {
        path: 'skill',
        component: () => import('@/views/ai/skill/index.vue'),
        name: 'Skill',
        meta: { title: 'Skill管理', icon: 'skill' }
      }
    ]
  }
]

export default aiRouter
