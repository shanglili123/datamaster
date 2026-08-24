import Layout from '@/layout/index.vue'

const ontRouter = [
  {
    path: '/ont',
    component: Layout,
    redirect: '/ont/ontology',
    alwaysShow: true,
    name: 'Ont',
    meta: { title: '本体模型', icon: 'mind-map' },
    children: [
      {
        path: 'ontology',
        component: () => import('@/views/ont/ontology/index.vue'),
        name: 'OntologyList',
        meta: { title: '本体管理', icon: 'organization-chart' }
      },
      {
        path: 'workspace/:ontologyId',
        component: () => import('@/views/ont/workspace/index.vue'),
        name: 'OntWorkspace',
        meta: { title: '本体工作台', activeMenu: '/ont/ontology', hidden: true }
      },
      {
        path: 'concept/:ontologyId',
        component: () => import('@/views/ont/concept/index.vue'),
        name: 'ConceptManage',
        meta: { title: '概念管理', icon: 'node-tree', activeMenu: '/ont/ontology', hidden: true }
      },
      {
        path: 'function',
        component: () => import('@/views/ont/function/index.vue'),
        name: 'OntFunction',
        meta: { title: '函数管理', icon: 'tool' }
      }
    ]
  }
]

export default ontRouter
