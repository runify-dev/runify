import {PermissionConstants} from '@/permission/data'
import {
  buildBasePermission,
  buildBaseResourcePermission
} from '@/permission/common'
import {ROOT_FOLDER_ID} from "@/constants/common"

const knowledgeRouter = {
  path: '/knowledge',
  name: 'knowledge',
  meta: {
    title: '知识库',
    icon: 'pi-book',
    activeMenu: 'knowledge',
    permission: (to: any) => {
      return [buildBasePermission(PermissionConstants.KNOWLEDGE_READ)]
    },
    fallbackRouteNames: ['model']
  },
  component: () => import('@/views/knowledge/index.vue'),
  redirect: `/knowledge/folders/${ROOT_FOLDER_ID}`,
  children: [
    {
      path: 'folders/:id',
      name: 'knowledgeFolders',
      meta: {
        title: '知识库',
        activeMenu: 'knowledge',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.KNOWLEDGE_READ, String(to.params.id))]
        },
        fallbackRouteNames: ['knowledgeDocFolder']
      },
      component: () => import('@/views/knowledge/List.vue')
    },
    {
      path: 'resources/:id/folders/:folderId',
      name: 'knowledgeDocFolder',
      meta: {
        title: '知识库文件夹',
        activeMenu: 'knowledge',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.KNOWLEDGE_READ, String(to.params.id))]
        },
        fallbackRouteNames: ['knowledge403']
      },
      component: () => import('@/views/knowledge/Details.vue')
    },
    {
      path: 'resources/:id/documents/:documentId',
      name: 'knowledgeDocument',
      meta: {
        title: '知识库文档',
        activeMenu: 'knowledge',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.KNOWLEDGE_READ, String(to.params.id))]
        },
        fallbackRouteNames: ['knowledge403']
      },
      component: () => import('@/views/knowledge/Details.vue')
    },
    {
      path: 'resources/:id/setting',
      name: 'knowledgeSetting',
      meta: {
        title: '知识库设置',
        activeMenu: 'knowledge',
        permission: (to: any) => {
          return [buildBaseResourcePermission(PermissionConstants.KNOWLEDGE_EDIT, String(to.params.id))]
        },
        fallbackRouteNames: ['knowledge403']
      },
      component: () => import('@/views/knowledge/Setting.vue')
    },
    {
      path: '403',
      name: 'knowledge403',
      meta: {
        title: '知识库',
        activeMenu: 'knowledge',
      },
      component: () => import('@/views/error/403/index.vue')
    }
  ]
}

export default knowledgeRouter
