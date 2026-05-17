import {defineStore} from 'pinia'
import conversationAPI from '@/api/conversation'
import FingerprintJS from '@fingerprintjs/fingerprintjs'

interface UserProfile {
  id: string
  type: 'ANONYMOUS' | 'USER'
  user?: {
    id: string
    email: string
    phone: string
    icon: string
    nickname: string
    username: string
    createTime: string
    updateTime: string
  }
}

export interface ConversationTokenStore {
  token: string | null
  profile: UserProfile | null
  anonymousTried: boolean
  lastAuthWasAnonymous: boolean
}

const useConversationTokenStore = defineStore("conversationToken", {
  state: (): ConversationTokenStore => ({
    token: localStorage.getItem(`conversation-token`),
    profile: null,
    anonymousTried: false,
    lastAuthWasAnonymous: false
  }),
  getters: {
    getToken: (state) => () => state.token,
    isLogged: (state) => !!state.token,
    displayName: (state) => {
      if (!state.profile) return ''
      if (state.profile.type === 'ANONYMOUS') return '匿名用户'
      return state.profile.user?.nickname || state.profile.user?.username || '用户'
    },
    isAnonymous: (state) => state.profile?.type === 'ANONYMOUS'
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(`conversation-token`, token)
    },
    clearToken() {
      this.token = null
      this.profile = null
      this.lastAuthWasAnonymous = false
      localStorage.removeItem(`conversation-token`)
    },
    fetchProfile(): Promise<void> {
      return conversationAPI.userProfile().then((res) => {
        this.profile = res.data
      }).catch((err): Promise<void> => {
        if (err?.response?.status === 401) {
          return this.handle401()
        }
        return Promise.reject(err)
      })
    },
    login(username: string, password: string) {
      return conversationAPI.login({ username, password }).then((res) => {
        if (res.code !== 200) {
          return Promise.reject(res.message || '登录失败')
        }
        this.setToken(res.data)
        this.lastAuthWasAnonymous = false
        return this.fetchProfile()
      })
    },
    tryAnonymousLogin(): Promise<void> {
      return FingerprintJS.load()
        .then((fp) => fp.get())
        .then((result) => conversationAPI.anonymousLogin(result.visitorId))
        .then((res) => {
          this.anonymousTried = true
          this.setToken(res.data)
          this.lastAuthWasAnonymous = true
          return this.fetchProfile()
        })
        .catch((): Promise<void> => {
          this.anonymousTried = true
          return Promise.reject()
        })
    },
    handle401(): Promise<void> {
      const wasAnonymous = this.lastAuthWasAnonymous
      this.token = null
      this.profile = null
      this.lastAuthWasAnonymous = false
      localStorage.removeItem(`conversation-token`)
      if (wasAnonymous) {
        return this.tryAnonymousLogin()
      }
      return Promise.reject('need-login')
    }
  }
})

export default useConversationTokenStore
