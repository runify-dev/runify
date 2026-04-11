import {
  createRouter,
  createWebHistory,
  type NavigationGuardNext,
  type RouteLocationNormalized,
  type RouteRecordRaw,
  type RouteRecordName
} from 'vue-router'
import { routes } from '@/router/admin/routes'
const router = createRouter({
  history: createWebHistory(window.RUNIFY_APP.admin.baseURL ? window.RUNIFY_APP.admin.baseURL : import.meta.env.BASE_URL),
  routes: routes
})

export const getChildRouteListByPathAndName = (name?: RouteRecordName | any) => {
  return getChildRouteList(routes, name)
}

export const getChildRouteList: (
  routeList: Array<RouteRecordRaw>,
  name: RouteRecordName | null | undefined
) => Array<RouteRecordRaw> = (routeList, name) => {
  for (let index = 0; index < routeList.length; index++) {
    const route = routeList[index]
    if (name === route.name) {
      return route.children || []
    }
    if (route.children && route.children.length > 0) {
      const result = getChildRouteList(route.children, name)
      if (result && result?.length > 0) {
        return result
      }
    }
  }
  return []
}

export default router
