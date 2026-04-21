export enum Group {
  APPLICATION = "APPLICATION",
  NOTE = "NOTE",
  PROJECT = "PROJECT",
  MODEL = "MODEL",
  USER_MANAGEMENT = "USER_MANAGEMENT",
  ROLE_MANAGEMENT = "ROLE_MANAGEMENT",
  OVERVIEW = "OVERVIEW",
  SETTING = "SETTING",
  CONVERSATION_LOG = "CONVERSATION_LOG",
  FOLDER = "FOLDER"
}

export enum Operate {
  READ = "READ",
  EDIT = "EDIT",
  CREATE = "CREATE",
  DELETE = "DELETE",
  AUTHORIZATION = "AUTHORIZATION"
}

export enum Role {
  ADMIN = "ADMIN",
  USER = "USER"
}

export enum Compare {
  OR = "OR",
  AND = "AND"
}

export class Permission {
  group: Group
  subGroup: Group
  operate: Operate
  bitIndex: number
  resourceId?: string

  constructor(
    group: Group,
    subGroup: Group,
    operate: Operate,
    bitIndex: number,
    resourceId?: string
  ) {
    this.group = group
    this.subGroup = subGroup
    this.operate = operate
    this.bitIndex = bitIndex
    this.resourceId = resourceId
  }

  bit(): bigint {
    return 1n << BigInt(this.bitIndex)
  }

  toString(): string {
    const groupName = this.group
    const subGroupName = this.subGroup
    const operateName = this.operate

    return `${groupName}:${this.subGroup !== this.group ? `${subGroupName}:` : ''}${operateName}`
  }
  toPermissionKey(): string {
    if (this.resourceId) {
      return `${this.group}:${this.resourceId}`
    }
    return this.toString()

  }
  getResourcePermissionKey(resourceId: string): string {
    return `${this.group}:${resourceId}`
  }
}

export type PermissionFactory = () => Permission

export interface AggregatePermissionOptions {
  permissionCalls?: PermissionFactory[]
  permissionConstants?: Permission[]
  aggregatePermissions?: AggregatePermission[]
  roles?: Role[]
  compare?: Compare
}

export class AggregatePermission {
  permissionCalls: PermissionFactory[]
  permissionConstants: Permission[]
  aggregatePermissions: AggregatePermission[]
  roles: Role[]
  compare: Compare

  constructor(options: AggregatePermissionOptions = {}) {
    this.permissionCalls = options.permissionCalls ?? []
    this.permissionConstants = options.permissionConstants ?? []
    this.aggregatePermissions = options.aggregatePermissions ?? []
    this.roles = options.roles ?? []
    this.compare = options.compare ?? Compare.OR
  }

  static builder(): AggregatePermissionBuilder {
    return new AggregatePermissionBuilder()
  }

  hasPermission(
    userRoles: Set<string>,
    userPermissions: Map<string, bigint>
  ): boolean {
    if (
      this.permissionCalls.length === 0 &&
      this.permissionConstants.length === 0 &&
      this.aggregatePermissions.length === 0 &&
      this.roles.length === 0
    ) {
      return true
    }

    for (const permissionCall of this.permissionCalls) {
      const permission = permissionCall()
      const hasPerm = this.checkPermission(permission, userPermissions)

      if (hasPerm) {
        if (this.compare === Compare.OR) {
          return true
        }
      } else {
        if (this.compare === Compare.AND) {
          return false
        }
      }
    }

    for (const permission of this.permissionConstants) {
      const hasPerm = this.checkPermission(permission, userPermissions)

      if (hasPerm) {
        if (this.compare === Compare.OR) {
          return true
        }
      } else {
        if (this.compare === Compare.AND) {
          return false
        }
      }
    }

    for (const role of this.roles) {
      const hasPerm = this.checkRole(role, userRoles)

      if (hasPerm) {
        if (this.compare === Compare.OR) {
          return true
        }
      } else {
        if (this.compare === Compare.AND) {
          return false
        }
      }
    }

    for (const aggregatePermission of this.aggregatePermissions) {
      const hasPerm = aggregatePermission.hasPermission(userRoles, userPermissions)

      if (hasPerm) {
        if (this.compare === Compare.OR) {
          return true
        }
      } else {
        if (this.compare === Compare.AND) {
          return false
        }
      }
    }

    return this.compare === Compare.AND
  }

  private checkRole(role: Role, userRoles: Set<string>): boolean {
    if (userRoles.size === 0) {
      return false
    }
    return userRoles.has(Role[role])
  }

  private checkPermission(
    permission: Permission,
    userPermissions: Map<string, bigint>

  ): boolean {
    const key = permission.resourceId
      ? permission.getResourcePermissionKey(permission.resourceId)
      : Group[permission.group]
    const rawValue =
      userPermissions instanceof Map ? userPermissions.get(key) : userPermissions[key]
    if (rawValue == null) {
      return false
    }
    return (rawValue & permission.bit()) !== 0n
  }
}

export class AggregatePermissionBuilder {
  private _permissionCalls: PermissionFactory[] = []
  private _permissionConstants: Permission[] = []
  private _aggregatePermissions: AggregatePermission[] = []
  private _roles: Role[] = []
  private _compare: Compare = Compare.OR

  permissionCalls(permissionCalls: PermissionFactory[]): this {
    this._permissionCalls = permissionCalls
    return this
  }

  permissionConstants(permissionConstants: Permission[]): this {
    this._permissionConstants = permissionConstants
    return this
  }

  aggregatePermissions(aggregatePermissions: AggregatePermission[]): this {
    this._aggregatePermissions = aggregatePermissions
    return this
  }

  roles(roles: Role[]): this {
    this._roles = roles
    return this
  }

  compare(compare: Compare): this {
    this._compare = compare
    return this
  }

  addPermission(permission: PermissionFactory): this
  addPermission(permission: Permission): this
  addPermission(permission: AggregatePermission): this
  addPermission(permission: PermissionFactory | Permission | AggregatePermission): this {
    if (typeof permission === 'function') {
      this._permissionCalls.push(permission)
      return this
    }

    if (permission instanceof AggregatePermission) {
      this._aggregatePermissions.push(permission)
      return this
    }

    this._permissionConstants.push(permission)
    return this
  }

  addRole(role: Role): this {
    this._roles.push(role)
    return this
  }

  build(): AggregatePermission {
    return new AggregatePermission({
      permissionCalls: this._permissionCalls,
      permissionConstants: this._permissionConstants,
      aggregatePermissions: this._aggregatePermissions,
      roles: this._roles,
      compare: this._compare
    })
  }
}
