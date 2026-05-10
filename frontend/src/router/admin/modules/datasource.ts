const datasourceRouter = {
  path: '/datasource',
  name: 'datasource',
  meta: {
    title: '数据源',
    icon: 'app-database',
    activeMenu: 'datasource'
  },
  component: () => import('@/views/datasource/index.vue'),
  redirect: '/datasource/folders/root',
  children: [
    {
      path: 'folders/:id',
      name: 'datasourceFolders',
      meta: {
        title: '数据源',
        activeMenu: 'datasource'
      },
      component: () => import('@/views/datasource/List.vue')
    },
    {
      path: 'resources/:id',
      name: 'datasourceDetails',
      meta: {
        title: '数据源详情',
        activeMenu: 'datasource'
      },
      component: () => import('@/views/datasource/Details.vue')
    },
    {
      path: 'folders/:folderId/create',
      name: 'datasourceCreate',
      meta: {
        title: '新建数据源',
        activeMenu: 'datasource'
      },
      component: () => import('@/views/datasource/Create.vue')
    }
  ]
}

export default datasourceRouter
