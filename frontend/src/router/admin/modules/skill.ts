import {PermissionConstants} from '@/permission/data'
import {
  buildBasePermission,
  buildBaseResourcePermission
} from '@/permission/common'
import {ROOT_FOLDER_ID} from "@/constants/common"

const skillRouter = {
  path: '/skill',
  name: 'skill',
  meta: {
    title: '技能',
    icon: 'pi-bolt',
    activeMenu: 'skill',
    permission: (to: any) => {
      return [buildBasePermission(PermissionConstants.SKILL_READ)]
    },
    fallbackRouteNames: ['model']
  },
  component: () => import('@/views/skill/index.vue'),
  redirect: `/skill/folders/${ROOT_FOLDER_ID}`,
  children: [
    {
      path: 'folders/:id',
      name: 'skillFolders',
      meta: {
        title: '技能',
        activeMenu: 'skill',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.SKILL_READ, String(to.params.id))]
        },
        fallbackRouteNames: ['skillDetails']
      },
      component: () => import('@/views/skill/List.vue')
    },
    {
      path: 'resources/:id/files/:fileId',
      name: 'skillDetails',
      meta: {
        title: '技能详情',
        activeMenu: 'skill',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.SKILL_READ, String(to.params.id))]
        },
        fallbackRouteNames: ['skill403']
      },
      component: () => import('@/views/skill/Details.vue')
    },
    {
      path: 'resources/:id/setting',
      name: 'skillSetting',
      meta: {
        title: '技能设置',
        activeMenu: 'skill',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.SKILL_EDIT, String(to.params.id))]
        },
        fallbackRouteNames: ['skill403']
      },
      component: () => import('@/views/skill/Setting.vue')
    },
    {
      path: 'store',
      name: 'skillStore',
      meta: {
        title: '技能商店',
        activeMenu: 'skill',
      },
      component: () => import('@/views/skill/Store.vue')
    },
    {
      path: '403',
      name: 'skill403',
      meta: {
        title: '技能',
        activeMenu: 'skill',
      },
      component: () => import('@/views/error/403/index.vue')
    }
  ]
}

export default skillRouter
