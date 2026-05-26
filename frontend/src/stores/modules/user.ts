import {defineStore} from 'pinia'
import type {User} from '@/api/type/user'
import UserApi from '@/api/user'

export interface userStateTypes {
  user?: User
  token?: string,
  permissions: Map<string, bigint>,
  roles: Set<string>
}

const buildPermissionMap = (data: Record<string, string>): Map<string, bigint> => {
  const map = new Map<string, bigint>()
  if (!data) return map

  for (const [resourceId, hex] of Object.entries(data)) {
    if (hex) {
      map.set(resourceId, BigInt('0x' + hex))
    }
  }
  return map
}
const useUserStore = defineStore('user', {
  state: (): userStateTypes => ({
    user: undefined,
    token: undefined,
    permissions: new Map<string, bigint>(),
    roles: new Set<string>()
  }),
  actions: {
    getToken(): string | undefined {
      if (this.token) {
        return this.token
      }
      const token = localStorage.getItem('token')
      if (token) {
        this.token = token
      }
      return token ? token : undefined
    },
    resetProfile() {
      return UserApi.profile().then(async (ok: any) => {
        this.user = ok.data
        this.permissions = buildPermissionMap(ok.data.permissions)
        if (ok.data.roles) {
          ok.data.roles.forEach((role: any) => {
            if (role.internal) {
              this.roles.add(role.id)
            } else {
              this.roles.add("EXTENDS:" + role.type)
            }

          })
        }

        return ok.data
      })
    },
    profile() {
      if (this.user) {
        return Promise.resolve(this.user)
      }
      return this.resetProfile()
    },
    getPermissions() {
      return this.permissions
    },
    getRoles() {
      return this.roles
    },
    login(username: string, password: string) {
      return UserApi.login({username, password}).then((ok: any) => {
        this.token = ok.data
        localStorage.setItem('token', ok.data)
        return this.profile()
      })
    },
    logout() {
      this.user = undefined
      this.token = undefined
      localStorage.removeItem('token')
    }
  }
})

export default useUserStore
