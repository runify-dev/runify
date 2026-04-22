import { PermissionConstants } from '@/permission/data'
import {
  AggregatePermission,
  Compare,
  Role,
  buildBasePermission,
  buildBaseResourcePermission
} from '@/permission/common'
const modelRouter = {
  path: '/model',
  name: 'model',
  meta: {
    title: '模型',
    icon: 'app-model',
    activeMenu: 'model',
    permission: (to: any) => {
      return [buildBasePermission(PermissionConstants.MODEL_READ)]
    },
    fallbackRouteNames: ['note']
  },
  component: () => import('@/views/model/index.vue'),
  redirect: '/model/folders/root',

  children: [
    {
      path: 'folders/:id',
      name: 'modelFolders',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'model',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.MODEL_READ, String(to.params.id))]
        }
      },
      component: () => import('@/views/model/List.vue')
    },

    {
      path: 'resources/:id',
      name: 'modelDetails',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'model',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.MODEL_READ, String(to.params.id))]
        }
      },
      component: () => import('@/views/model/Details.vue')
    },
    {
      path: 'resources/:id/edit',
      name: 'modelEdit',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'model',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.MODEL_EDIT, String(to.params.id))]
        }
      },
      component: () => import('@/views/model/Edit.vue')
    }
  ]
}

export default modelRouter
