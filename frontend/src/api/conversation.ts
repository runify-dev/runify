import { type Page, Result } from '@/request/Result'
import { get, post, put, del, postStream } from '@/request/chat/index'
import type { Ref } from 'vue'

const conversation: (conversationId: string, conversation: any) => Promise<any> = (
  conversationId,
  conversation
) => {
  return postStream(`/conversation/api/conversation/${conversationId}/chat`, conversation)
}

const createConversation = (name: string) => {
  return post(`/conversation`, { name }, {})
}
const config = (applicationId: string) => {
  return get('/config', { applicationId })
}

const anonymousLogin: (
  applicationId: string,
  visitorId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, visitorId, loading) => {
  return post(
    `/anonymousLogin`,
    {
      visitorId,
      applicationId
    },
    loading
  )
}

const pageConversation: (query: any, loading?: Ref<boolean>) => Promise<Result<Page<any>>> = (
  query,
  loading
) => {
  return get(`/conversation`, query, loading)
}

const pageConversationMessage: (
  conversationId: string,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (conversationId, query, loading) => {
  return get(`/conversation/${conversationId}/message`, query, loading)
}
const modifyName: (
  conversationId: string,
  name: string,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (conversationId, name: string, loading) => {
  return post(`/conversation/${conversationId}/modify-name`, { name: name }, {}, loading)
}
const delConversation: (
  conversationId: string,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (conversationId, loading) => {
  return del(`/conversation/${conversationId}`, undefined, undefined, loading)
}
export default {
  conversation,
  createConversation,
  anonymousLogin,
  config,
  pageConversation,
  pageConversationMessage,
  modifyName,
  delConversation
}
