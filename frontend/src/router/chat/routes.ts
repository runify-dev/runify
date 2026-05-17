import type {RouteRecordRaw} from 'vue-router'

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
    path: "/",
    name: 'home',
    component: () => import('@/views/conversation/HomePage.vue')
  }
]
