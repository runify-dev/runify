import { PermissionConstants } from '@/permission/data'
import {
  AggregatePermission,
  Compare,
  Role,
  buildBasePermission,
  buildBaseResourcePermission
} from '@/permission/common'
const knowledgeRouter = {
  path: '/note',
  name: 'note',
  meta: {
    title: '笔记',
    icon: 'app-document',
    activeMenu: 'note',
    permission: (to: any) => {
      return [buildBasePermission(PermissionConstants.NOTE_READ)]
    },
    fallbackRouteNames: ['model']
  },
  component: () => import('@/views/note/index.vue'),
  redirect: '/note/folders/root',
  children: [
    {
      path: 'folders/:id',
      name: 'noteFolders',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'note',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.NOTE_READ, String(to.params.id))]
        },
        fallbackRouteNames: ['noteDetails']
      },
      component: () => import('@/views/note/List.vue')
    },

    {
      path: 'resources/:id',
      name: 'noteDetails',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'note',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.NOTE_READ, String(to.params.id))]
        },
        fallbackRouteNames: ['note403']
      },
      component: () => import('@/views/note/Details.vue')
    },
    {
      path: '403',
      name: 'note403',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'application',
      },
      component: () => import('@/views/error/403/index.vue')
    }
  ]
}

export default knowledgeRouter
