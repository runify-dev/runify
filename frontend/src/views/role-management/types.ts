export interface RoleItem {
  id: string
  name: string
  internal?: boolean
  type?: 'ADMIN' | 'USER' | string | null
  createTime?: string
  updateTime?: string
}

export interface PermissionItem {
  groupLabel: string
  subGroupLabel: string
  permissionGroupLabel?: string | null
  group: string
  subGroup: string
  permission: string
  selected?: boolean
}

export interface PermissionOption {
  key: string
  label: string
  permission: string
}

export interface PermissionRow {
  rowKey: string
  group: string
  groupLabel: string
  subGroup: string
  subGroupLabel: string
  permissions: PermissionOption[]
}

export interface PermissionGroupBlock {
  groupKey: string
  groupLabel: string
  rows: PermissionRow[]
}

export interface UserItem {
  id: string
  username: string
  email: string
  nickname: string
  icon?: string
}

export interface PageResult<T> {
  records: Array<T>
  current: number
  size: number
  total: number
}

export type InheritRoleType = 'ADMIN' | 'USER'
export type MemberSearchField = 'global' | 'username' | 'nickname'
