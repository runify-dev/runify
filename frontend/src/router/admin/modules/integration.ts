import {PermissionConstants} from '@/permission/data'
import {
  buildBasePermission,
  buildBaseResourcePermission
} from '@/permission/common'
import {ROOT_FOLDER_ID} from "@/constants/common"

const integrationRouter = {
  path: '/integration',
  name: 'integration',
  meta: {
    title: '集成',
    icon: 'app-integration',
    activeMenu: 'integration',
    permission: (to: any) => {
      return [buildBasePermission(PermissionConstants.INTEGRATION_READ)]
    },
    fallbackRouteNames: ['note']
  },
  component: () => import('@/views/integration/index.vue'),
  redirect: `/integration/folders/${ROOT_FOLDER_ID}`,

  children: [
    {
      path: 'folders/:id',
      name: 'integrationFolders',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'integration',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.INTEGRATION_READ, String(to.params.id))]
        }
      },
      component: () => import('@/views/integration/List.vue')
    },

    {
      path: 'resources/:id',
      name: 'integrationDetails',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'integration',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.INTEGRATION_READ, String(to.params.id))]
        }
      },
      component: () => import('@/views/integration/Details.vue')
    }
  ]
}

export default integrationRouter
