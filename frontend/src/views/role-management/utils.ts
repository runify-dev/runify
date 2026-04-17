import type {
  PermissionGroupBlock,
  PermissionItem,
  PermissionRow,
  RoleItem
} from './types'

export function isBuiltinRole(role: RoleItem): boolean {
  return role.id === 'ADMIN' || role.id === 'USER' || !!role.internal
}

export function inheritedRoleLabel(type?: string | null): string {
  if (!type) return '-'
  if (type === 'ADMIN') return '系统管理员'
  if (type === 'USER') return '普通用户'
  return type
}

export function normalizePermissionLabel(item: PermissionItem): string {
  return item.permissionGroupLabel?.trim() || item.permission
}

export function buildPermissionRows(data: PermissionItem[]): PermissionRow[] {
  const rowMap = new Map<string, PermissionRow>()

  for (const item of data) {
    const rowKey = `${item.group}::${item.subGroup}`

    if (!rowMap.has(rowKey)) {
      rowMap.set(rowKey, {
        rowKey,
        group: item.group,
        groupLabel: item.groupLabel,
        subGroup: item.subGroup,
        subGroupLabel: item.subGroupLabel,
        permissions: []
      })
    }

    rowMap.get(rowKey)!.permissions.push({
      key: `${rowKey}::${item.permission}`,
      label: normalizePermissionLabel(item),
      permission: item.permission
    })
  }

  return [...rowMap.values()]
}

export function buildPermissionGroups(rows: PermissionRow[]): PermissionGroupBlock[] {
  const map = new Map<string, PermissionGroupBlock>()

  for (const row of rows) {
    const groupKey = row.group || row.groupLabel

    if (!map.has(groupKey)) {
      map.set(groupKey, {
        groupKey,
        groupLabel: row.groupLabel,
        rows: []
      })
    }

    map.get(groupKey)!.rows.push(row)
  }

  return [...map.values()]
}

export function cloneSet<T>(source: Set<T>): Set<T> {
  return new Set(Array.from(source))
}
