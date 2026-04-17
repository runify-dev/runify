import type { RouteRecordRaw } from 'vue-router'
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
        meta: { title: '用户管理', activeMenu: 'system-management' },
        component: () => import('@/views/user-management/index.vue')
      },
      {
        path: 'role-management',
        name: 'role-management',
        meta: { title: '角色管理', activeMenu: 'system-management' },
        component: () => import('@/views/role-management/index.vue')
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
  }
]
