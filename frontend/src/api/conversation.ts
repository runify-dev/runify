import {type Page, Result} from '@/request/Result'
import {get, post, put, del, postStream} from '@/request/chat/index'
import type {Ref} from 'vue'
import {type LoginVO, type QueryApplicationVO} from "@/api/type/conversation"

const conversation: (applicationId: string, conversationId: string, conversation: any) => Promise<any> = (
  applicationId,
  conversationId,
  conversation
) => {
  return postStream(`/conversation/api/application/${applicationId}/conversation/${conversationId}/chat`, conversation)
}
const queryApplication: (query: QueryApplicationVO) => Promise<any> = (query) => {
  return get(`/application`, query)
}


const createConversation = (applicationId: string, name: string) => {
  return post(`/application/${applicationId}/conversation`, {name}, {})
}
const config = (applicationId: string) => {
  return get(`/application/${applicationId}/config`, {applicationId})
}

const anonymousLogin: (
  visitorId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (visitorId, loading) => {
  return post(
    `/anonymousLogin`,
    {
      visitorId
    },
    loading
  )
}

const login: (
  login: LoginVO,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (login, loading) => {
  return post(
    `/login`,
    login,
    loading
  )
}


const pageConversation: (applicationId: string, query: any, loading?: Ref<boolean>) => Promise<Result<Page<any>>> = (
  applicationId,
  query,
  loading
) => {
  return get(`/application/${applicationId}/conversation`, query, loading)
}

const pageConversationMessage: (
  applicationId: string,
  conversationId: string,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (applicationId, conversationId, query, loading) => {
  return get(`/application/${applicationId}/conversation/${conversationId}/message`, query, loading)
}
const modifyName: (
  applicationId: string,
  conversationId: string,
  name: string,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (applicationId, conversationId, name: string, loading) => {
  return post(`/application/${applicationId}/conversation/${conversationId}/modify-name`, {name: name}, {}, loading)
}
const delConversation: (
  applicationId: string,
  conversationId: string,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (applicationId, conversationId, loading) => {
  return del(`/application/${applicationId}/conversation/${conversationId}`, undefined, undefined, loading)
}

const resumeStream = (applicationId: string, conversationId: string, index: number) => {
  return postStream(`/conversation/api/application/${applicationId}/conversation/${conversationId}/resume-stream`, {}, {'Last-Event-ID': index})
}
const cancel = (applicationId: string, conversationId: string) => {
  return post(`/application/${applicationId}/conversation/api/conversation/${conversationId}/cancel`, {}, {})
}
const statusStream = (applicationId: string, conversationId: string) => {
  return get(`/application/${applicationId}/conversation/${conversationId}/status`)
}
const userProfile = () => {
  return get('/userProfile')
}
const getApplication = (applicationId: string) => {
  return get(`/application/${applicationId}`)
}
const authProfile = (applicationId: string) => {
  return get(`/application/${applicationId}/auth-profile`)
}
// 我的便签：当前登录用户在该应用下的 user 档便签（匿名返回空）
const getMySections = (applicationId: string, loading?: Ref<boolean>) => {
  return get(`/application/${applicationId}/section-fact`, {}, loading)
}
export default {
  queryApplication,
  login,
  conversation,
  createConversation,
  anonymousLogin,
  config,
  pageConversation,
  pageConversationMessage,
  modifyName,
  delConversation,
  resumeStream,
  statusStream,
  cancel,
  userProfile,
  getApplication,
  authProfile,
  getMySections
}
