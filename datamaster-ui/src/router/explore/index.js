import Layout from '@/layout/index.vue';

const exploreRouter = [
  {
    path: '/explore',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/explore/index.vue'),
        name: 'DataExploreStation',
        meta: { title: '开始数据探索', fullScreen: true, noCache: true }
      }
    ]
  },
  {
    path: '/space',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/explore/space.vue'),
        name: 'SpaceWorkstation',
        meta: { title: '空间工作站', fullScreen: true, noCache: true }
      }
    ]
  }
];

export default exploreRouter;
