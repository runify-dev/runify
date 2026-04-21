import { Role, Permission, AggregatePermission, type PermissionFactory } from "@/permission/common"
import { isFunction } from "@/utils/common"
import useStore from '@/stores'
function isRole(value: any): value is Role {
  return Object.values(Role).includes(value);
}
const hasPermissionChild = (
  permission: Role | string | Permission | AggregatePermission | PermissionFactory,
) => {
  const { user } = useStore()
  const permissions: Map<string, bigint> = user.getPermissions()
  const roles: Set<string> = user.getRoles()

  if (!permission) {
    return true
  }
  if (isFunction(permission)) {
    permission = (permission as PermissionFactory)()
  }
  if (isRole(permission)) {
    return roles.has(Role[permission])
  }
  if (permission instanceof Permission) {
    return permissions.has(permission.toPermissionKey()) && ((permissions.get(permission.toPermissionKey()) as bigint) & permission.bit()) > 0

  }

  if (permission instanceof AggregatePermission) {
    permission.hasPermission(roles, permissions,)
  }

  if (typeof permission === 'string') {
    return permissions.has(permission)
  }

  return false
}

export const hasPermission = (permission:
  | Array<Role | string | Permission | AggregatePermission | PermissionFactory>
  | Role
  | string
  | Permission
  | AggregatePermission
  | PermissionFactory,
  compare: 'OR' | 'AND',
) => {
  if (permission instanceof Array) {
    return compare === 'OR'
      ? permission.some((p) => hasPermissionChild(p))
      : permission.every((p) => hasPermissionChild(p))
  } else {
    return hasPermissionChild(permission)
  }
}
