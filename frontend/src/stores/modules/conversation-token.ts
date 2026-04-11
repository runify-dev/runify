import { defineStore } from 'pinia'
export interface ConversationTokenStore {
  token: any,
  appplicationId: string | undefined
}
const useConversationTokenStore = defineStore("conversationToken", {
  state: (): ConversationTokenStore => ({
    token: {},
    appplicationId: undefined
  }),
  actions: {
    getApplicationId() {
      return this.appplicationId;
    },
    setApplicationId(applicationId: string) {
      this.appplicationId = applicationId;
    },
    getToken(): String | undefined {
      if (!this.appplicationId) {
        return
      }
      if (this.appplicationId in this.token) {
        return this.token[this.appplicationId]
      }
      const t = localStorage.getItem(`${this.appplicationId}-token`)
      if (t) {
        this.token[this.appplicationId] = t;
      }
      return t ? t : undefined
    },
    setToken(applicationId: string, token: string) {
      localStorage.setItem(`${applicationId}-token`, token)
    }
  }
})

export default useConversationTokenStore
