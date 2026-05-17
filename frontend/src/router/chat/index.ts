import {
  createRouter,
  createWebHistory,
  type NavigationGuardNext,
  type RouteLocationNormalized,
} from 'vue-router'
import { routes } from '@/router/chat/routes'
import useStore from '@/stores/converstaion/index'
console.log('chat')
const router = createRouter({
  history: createWebHistory(window.RUNIFY_APP.chat.baseURL ? window.RUNIFY_APP.chat.baseURL : import.meta.env.BASE_URL),
  routes: routes
})
router.beforeEach(
  async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    next()
  },
)

export default router
