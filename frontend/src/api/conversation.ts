import { Result } from '@/request/Result'
import { get, post, put, del, postStream } from '@/request/chat/index'
import type { Ref } from 'vue'

const conversation: (
  conversationId: string,
  conversation: any,
) => Promise<any> = (conversationId, conversation,) => {
  return postStream(`/conversation/api/conversation/${conversationId}/chat`, conversation)
}


const createConversation = (name: string) => {
  return post(`/conversation`, { name }, {})
}
const config = (applicationId: string) => {
  return get('/config', { applicationId },)
}

const anonymousLogin: (
  applicationId: String,
  visitorId: String,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, visitorId, loading) => {
  return post(`/anonymousLogin`, {
    visitorId,
    applicationId
  }, loading)
}

export default {
  conversation,
  createConversation,
  anonymousLogin,
  config
}
