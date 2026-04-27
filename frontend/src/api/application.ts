import { Result, type Page } from '@/request/Result'
import { get, post, put, del, postStream } from '@/request/admin/index'
import type { Ref } from 'vue'

const edit: (
  applicationId: string,
  workflow: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, workflow, loading) => {
  return put(`/application/resources/${applicationId}`, { workflow: workflow }, undefined, loading)
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
const createConversation = (applicationId: string, name: string) => {
  return post(`/application/${applicationId}/conversation`, { name }, {})
}

const resumeStream = (applicationId: string, conversationId: string, index: number) => {
  return postStream(`/admin/api/application/${applicationId}/conversation/${conversationId}/resume-stream`, {}, { 'Last-Event-ID': index })
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

export default {
  edit,
  chat,
  pageConversation,
  createConversation,
  pageConversationMessage,
  statusStream,
  resumeStream,
  getApplicationInfo
}
