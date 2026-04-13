import type { RouteRecordRaw } from 'vue-router'

export const routes: Array<RouteRecordRaw> = [
  {
    path: '/a/:applicationId/c/:conversationId',
    name: 'conversation',
    component: () => import('@/views/conversation/index.vue')
  },
  {
    path: '/a/:applicationId',
    name: 'conversation-new',
    component: () => import('@/views/conversation/index.vue')
  },
  {
    path: '/login/a/:applicationId/c/:conversationId',
    name: 'login',
    component: () => import('@/views/conversation/login/index.vue')
  },
  {
    path: '/login/a/:applicationId',
    name: 'login-new',
    component: () => import('@/views/conversation/login/index.vue')
  }
]
