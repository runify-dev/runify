// src/router/createPermissionBeforeEach.ts
import type {
  NavigationGuardWithThis,
  RouteLocationNormalized,
  RouteLocationRaw,
  RouteRecordName,
  RouteRecordRaw,
  Router
} from 'vue-router'

type MaybePromise<T> = T | Promise<T>

type PermissionValue<P> = P | P[]
type PermissionResolver<P> =
  | PermissionValue<P>
  | ((to: RouteLocationNormalized) => PermissionValue<P> | null | undefined)

type MetaWithPermission<P> = {
  permission?: PermissionResolver<P>
  fallbackRouteNames?: RouteRecordName[]
}

type PermissionChecker<P> = (permissions: P[]) => MaybePromise<boolean>

type ResolvedRoute = ReturnType<Router['resolve']>
type RouteLike = RouteLocationNormalized | ResolvedRoute

interface UserStoreLike {
  user?: unknown
  getToken: () => string | null | undefined
  profile: () => Promise<unknown>
  logout?: () => Promise<unknown> | unknown
}

interface CreatePermissionBeforeEachOptions<P> {
  router: Router
  routes: RouteRecordRaw[]
  hasPermission: PermissionChecker<P>
  getUserStore: () => UserStoreLike

  /**
   * 免登录路由名称
   * 默认 ['login']
   */
  notAuthRouteNameList?: RouteRecordName[]

  /**
   * 登录路由
   * 默认 { path: '/login' }
   */
  loginRoute?: RouteLocationRaw

  /**
   * 403 路由
   * 默认 { name: '403' }
   */
  forbiddenRoute?: RouteLocationRaw
}

function buildRouteNameSet(routes: RouteRecordRaw[]): Set<RouteRecordName> {
  const set = new Set<RouteRecordName>()

  const walk = (items: RouteRecordRaw[]) => {
    for (const item of items) {
      if (item.name != null) {
        set.add(item.name)
      }
      if (item.children?.length) {
        walk(item.children)
      }
    }
  }

  walk(routes)
  return set
}

function normalizePermissions<P>(value: PermissionValue<P> | null | undefined): P[] {
  if (value == null) {
    return []
  }
  return Array.isArray(value) ? value : [value]
}

function buildLoginLocation(
  loginRoute: RouteLocationRaw,
  to: RouteLocationNormalized
): RouteLocationRaw {
  if (typeof loginRoute === 'string') {
    return {
      path: loginRoute,
      query: {
        redirect: to.fullPath
      }
    }
  }

  const query =
    'query' in loginRoute && loginRoute.query && typeof loginRoute.query === 'object'
      ? loginRoute.query
      : undefined

  return {
    ...loginRoute,
    query: {
      ...query,
      redirect: to.fullPath
    }
  }
}

function resolvePermissionsFromMatched<P>(to: RouteLike): P[] {
  const result: P[] = []

  for (const record of to.matched) {
    const meta = record.meta as MetaWithPermission<P>
    const permissionConfig: any = meta.permission
    if (!permissionConfig) {
      continue
    }

    const resolved =
      typeof permissionConfig === 'function'
        ? permissionConfig(to as RouteLocationNormalized)
        : permissionConfig

    result.push(...normalizePermissions(resolved))
  }

  return result
}

async function canAccessRoute<P>(
  to: RouteLike,
  hasPermission: PermissionChecker<P>
): Promise<boolean> {
  const permissions = resolvePermissionsFromMatched<P>(to)

  // 没有 permission，直接放行
  if (permissions.length === 0) {
    return true
  }

  return await hasPermission(permissions)
}

function resolveFallbackRouteNames<P>(to: RouteLike): RouteRecordName[] {
  for (let i = to.matched.length - 1; i >= 0; i--) {
    const meta = to.matched[i].meta as MetaWithPermission<P>
    if (meta.fallbackRouteNames?.length) {
      return meta.fallbackRouteNames
    }
  }

  return []
}

function buildFallbackLocation(to: RouteLike, routeName: RouteRecordName): RouteLocationRaw {
  return {
    name: routeName,
    params: to.params,
    query: to.query,
    hash: to.hash
  }
}

async function findFirstAccessibleFallback<P>(
  router: Router,
  to: RouteLike,
  routeNameSet: Set<RouteRecordName>,
  hasPermission: PermissionChecker<P>,
  visited: Set<RouteRecordName>
): Promise<RouteLocationRaw | null> {
  const fallbackRouteNames = resolveFallbackRouteNames<P>(to)
  if (fallbackRouteNames.length === 0) {
    return null
  }

  for (const fallbackName of fallbackRouteNames) {
    if (!routeNameSet.has(fallbackName)) {
      continue
    }

    if (visited.has(fallbackName)) {
      continue
    }

    const nextVisited = new Set(visited)
    nextVisited.add(fallbackName)

    let target: ResolvedRoute
    try {
      target = router.resolve(buildFallbackLocation(to, fallbackName))
    } catch {
      continue
    }

    // fallback 到自己，直接跳过
    if (target.fullPath === to.fullPath) {
      continue
    }

    const ok = await canAccessRoute<P>(target, hasPermission)
    if (ok) {
      return buildFallbackLocation(to, fallbackName)
    }

    const deepFallback = await findFirstAccessibleFallback<P>(
      router,
      target,
      routeNameSet,
      hasPermission,
      nextVisited
    )

    if (deepFallback) {
      return deepFallback
    }
  }

  return null
}

export function createPermissionBeforeEach<P>(
  options: CreatePermissionBeforeEachOptions<P>
): NavigationGuardWithThis<undefined> {
  const {
    router,
    routes,
    hasPermission,
    getUserStore,
    loginRoute = { path: '/login' },
    forbiddenRoute = { name: '403' },
    notAuthRouteNameList = ['login']
  } = options

  // 初始化一次，后面直接复用
  const routeNameSet = buildRouteNameSet(routes)
  const notAuthNameSet = new Set(notAuthRouteNameList.map(String))

  return async (to) => {
    const user = getUserStore()

    // 1. 免登录路由跳过 token / profile 校验
    const isNotAuthRoute = notAuthNameSet.has(String(to.name ?? ''))

    if (!isNotAuthRoute) {
      // 2. URL 上带 token，先写入本地
      if (to.query?.token != null) {
        localStorage.setItem('token', String(to.query.token))
      }

      // 3. 检查 token
      const token = user.getToken()
      if (!token) {
        return buildLoginLocation(loginRoute, to)
      }

      // 4. 用户信息不存在则拉取 profile
      if (!user.user) {
        try {
          await user.profile()
        } catch {
          if (user.logout) {
            await user.logout()
          }
          return buildLoginLocation(loginRoute, to)
        }
      }
    }

    // 5. 当前路由权限通过，直接进入
    const ok = await canAccessRoute<P>(to, hasPermission)
    if (ok) {
      return true
    }

    // 6. 当前路由没权限，按 fallbackRouteNames 递归查找
    const visited = new Set<RouteRecordName>()
    if (to.name != null) {
      visited.add(to.name)
    }

    const fallback = await findFirstAccessibleFallback<P>(
      router,
      to,
      routeNameSet,
      hasPermission,
      visited
    )

    if (fallback) {
      return fallback
    }

    // 7. fallback 也没有，直接 403
    return forbiddenRoute
  }
}
