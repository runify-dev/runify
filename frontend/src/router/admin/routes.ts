import type { RouteRecordRaw } from 'vue-router'
import { PermissionConstants } from '@/permission/data'
import {
  AggregatePermission,
  Compare,
  Role,
  buildBasePermission,
  buildBaseResourcePermission
} from '@/permission/common'
const modules: any = import.meta.glob('./modules/*.ts', { eager: true })
const routeList: any = Object.keys(modules).map((key) => modules[key].default)
export const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'home',
    redirect: 'application',
    component: () => import('@/layout-plus/index.vue'),
    children: routeList
  },
  {
    path: '/system-management',
    name: 'system-management',
    redirect: '/system-management/user-management',
    component: () => import('@/views/system-management/index.vue'),
    children: [
      {
        path: 'user-management',
        name: 'user-management',
        meta: {
          title: '用户管理', activeMenu: 'system-management',
          permission: (to: any) => {
            return [
              buildBasePermission(
                PermissionConstants.USER_MANAGEMENT_READ,
              )
            ]
          },
          fallbackRouteNames: ['role-management']
        },
        component: () => import('@/views/user-management/index.vue')
      },
      {
        path: 'role-management',
        name: 'role-management',
        meta: {
          title: '角色管理', activeMenu: 'system-management',
          permission: (to: any) => {
            return [
              buildBasePermission(
                PermissionConstants.ROLE_MANAGEMENT_READ,
              )
            ]
          },
          fallbackRouteNames: ['system-management-403']
        },
        component: () => import('@/views/role-management/index.vue')
      },
      {
        path: '403',
        name: 'system-management-403',
        meta: {
          title: 'common.fileUpload.document',
          activeMenu: 'system-management',
        },
        component: () => import('@/views/error/403/index.vue')
      }
    ]
  },
  {
    path: '/doc',
    name: 'doc',
    component: () => import('@/views/openapi/index.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue')
  },
  { path: '/403', name: '403', component: () => import('@/views/error/403/index.vue') },
  { path: '/404', name: '404', component: () => import('@/views/error/404/index.vue') }
]
