import {PermissionConstants} from '@/permission/data'
import {
  AggregatePermission,
  Compare,
  Role,
  buildBasePermission,
  buildBaseResourcePermission
} from '@/permission/common'
import {ROOT_FOLDER_ID} from "@/constants/common"

const noteRouter = {
  path: '/application',
  name: 'application',
  meta: {
    title: '应用',
    icon: 'app-application',
    activeMenu: 'application',
    permission: (to: any) => {
      return [buildBasePermission(PermissionConstants.APPLICATION_READ)]
    },
    fallbackRouteNames: ['note']
  },
  component: () => import('@/views/application/index.vue'),
  redirect: `/application/folders/${ROOT_FOLDER_ID}`,
  children: [
    {
      path: 'folders/:id',
      name: 'applicationFolders',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'application',
        permission: (to: any) => {
          return [
            buildBaseResourcePermission(PermissionConstants.APPLICATION_READ, String(to.params.id))
          ]
        },
        fallbackRouteNames: ['applicationDetails']
      },
      component: () => import('@/views/application/List.vue')
    },
    {
      path: 'resources/:id',
      name: 'applicationDetails',
      meta: {
        title: 'common.fileUpload.document',
        activeMenu: 'application',
        permission: (to: any) => {
          return [
            buildBaseResourcePermission(PermissionConstants.APPLICATION_READ, String(to.params.id))
          ]
        },
        fallbackRouteNames: ['application403']
      },
      component: () => import('@/views/application/Details.vue'),
      redirect: {name: 'applicationOverview'},
      children: [
        {
          path: 'overview',
          name: 'applicationOverview',
          meta: {
            title: '概览',
            activeMenu: 'application',
            permission: (to: any) => {
              return [
                buildBaseResourcePermission(
                  PermissionConstants.APPLICATION_OVERVIEW_READ,
                  String(to.params.id)
                )
              ]
            },
            fallbackRouteNames: ['applicationSetting']
          },
          component: () => import('@/views/application/Overview.vue')
        },
        {
          path: 'setting',
          name: 'applicationSetting',
          meta: {
            title: '设置',
            activeMenu: 'application',
            permission: (to: any) => {
              return [
                buildBaseResourcePermission(
                  PermissionConstants.APPLICATION_SETTING_READ,
                  String(to.params.id)
                )
              ]
            },
            fallbackRouteNames: ['applicationConversationLog']
          },
          component: () => import('@/views/application/Setting.vue')
        },
        {
          path: 'conversation-log',
          name: 'applicationConversationLog',
          meta: {
            title: '对话日志',
            activeMenu: 'application',
            permission: (to: any) => {
              return [
                buildBaseResourcePermission(
                  PermissionConstants.APPLICATION_CONVERSATION_LOG_READ,
                  String(to.params.id)
                )
              ]
            },
            fallbackRouteNames: ['application403']
          },
          component: () => import('@/views/application/ConversationLog.vue')
        },
        {
          path: '403',
          name: 'application403',
          meta: {
            title: 'common.fileUpload.document',
            activeMenu: 'application',
          },
          component: () => import('@/views/error/403/index.vue')
        }
      ]
    }

  ]
}

export default noteRouter
