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
    const { conversationToken } = useStore()
    if (!conversationToken.getApplicationId()) {
      if (to.params.applicationId) {
        conversationToken.setApplicationId(to.params.applicationId as string)
      }
      if (from.params.applicationId) {
        conversationToken.setApplicationId(from.params.applicationId as string)
      }
    }
    next()
  },
)

export default router
