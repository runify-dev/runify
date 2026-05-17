import { Result, type Page } from '@/request/Result'
import { get, post, put, del, postStream } from '@/request/admin/index'
import type { Ref } from 'vue'

const edit: (
  applicationId: string,
  data: { name?: string; desc?: string; icon?: string; allowAnonymousAccess?: boolean; workflow?: any },
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, data, loading) => {
  return put(`/application/resources/${applicationId}`, data, undefined, loading)
}

const chat: (applicationId: string, conversationId: string, conversation: any) => Promise<any> = (
  applicationId,
  conversationId,
  conversation
) => {
  return postStream(
    `/admin/api/application/${applicationId}/conversation/${conversationId}`,
    conversation
  )
}

const pageConversation: (
  applicationId: string,
  currentPage: number,
  pageSize: number,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, currentPage, pageSize, query, loading) => {

  return get(
    `/application/${applicationId}/conversation`,
    { ...query, pageSize, currentPage },
    loading
  )
}

const pageConversationMessage: (
  applicationId: string,
  conversationId: string,
  currentPage: number,
  pageSize: number,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (
  applicationId,
  conversationId,
  currentPage,
  pageSize,
  query,
  loading
) => {
    return get(
      `/application/${applicationId}/conversation/${conversationId}/message`,
      { ...query, pageSize, currentPage },
      loading
    )
  }
const createConversation = (applicationId: string, name: string, executeType?: string) => {
  return post(`/application/${applicationId}/conversation`, { name, executeType }, {})
}

const resumeStream = (applicationId: string, conversationId: string, index: number) => {
  return postStream(`/admin/api/application/${applicationId}/conversation/${conversationId}/resume-stream`, {}, { 'Last-Event-ID': index })
}
const cancel=(applicationId: string, conversationId: string,)=>{
  return post(`/application/${applicationId}/conversation/${conversationId}/cancel`,{}, {})
}
const statusStream = (applicationId: string, conversationId: string) => {
  return get(`/application/${applicationId}/conversation/${conversationId}/status`)
}
const getApplicationInfo: (
  applicationId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, loading) => {
  return get(`/application/resources/${applicationId}`, undefined, loading)
}

const getOverview: (
  applicationId: string,
  days?: number,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, days = 7, loading) => {
  return get(`/application/${applicationId}/overview`, { days }, loading)
}

const mineConversation: (
  applicationId: string,
  currentPage: number,
  pageSize: number,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, currentPage, pageSize, loading) => {
  return get(
    `/application/${applicationId}/conversation/mine`,
    { pageSize, currentPage },
    loading
  )
}

export default {
  edit,
  chat,
  pageConversation,
  mineConversation,
  createConversation,
  pageConversationMessage,
  statusStream,
  resumeStream,
  getApplicationInfo,
  getOverview,
  cancel
}
