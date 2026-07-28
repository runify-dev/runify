import { ROOT_FOLDER_ID } from '@/constants/common'

const toolRouter = {
  path: '/tool',
  name: 'tool',
  meta: {
    title: '工具',
    icon: 'app-wrench',
    activeMenu: 'tool'
  },
  component: () => import('@/views/tool/index.vue'),
  redirect: `/tool/folders/${ROOT_FOLDER_ID}`,
  children: [
    {
      path: 'folders/:id',
      name: 'toolFolders',
      meta: {
        title: '工具',
        activeMenu: 'tool'
      },
      component: () => import('@/views/tool/List.vue')
    },
    {
      path: 'resources/:id',
      name: 'toolDetails',
      meta: {
        title: '工具详情',
        activeMenu: 'tool'
      },
      component: () => import('@/views/tool/Details.vue')
    },
    {
      path: 'folders/:folderId/create',
      name: 'toolCreate',
      meta: {
        title: '新建工具',
        activeMenu: 'tool'
      },
      component: () => import('@/views/tool/Create.vue')
    }
  ]
}

export default toolRouter
