import { defineStore } from 'pinia'
import type { User } from '@/api/type/user'
import UserApi from '@/api/user'
export interface userStateTypes {
  user?: User
  token?: string
}
const useUserStore = defineStore('user', {
  state: (): userStateTypes => ({
    user: undefined,
    token: undefined
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

    profile() {
      if (this.user) {
        return Promise.resolve(this.user)
      }
      return UserApi.profile().then(async (ok: any) => {
        this.user = ok.data
        return ok.data
      })
    },

    login(username: string, password: string) {
      return UserApi.login({ username, password }).then((ok: any) => {
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
