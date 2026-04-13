import { Result } from '@/request/Result'
import { get, post, put, del, postStream } from '@/request/admin/index'
import type { Ref } from 'vue'


const edit: (
  applicationId: String,
  workflow: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, workflow, loading) => {
  return put(`/application/resources/${applicationId}`, { 'workflow': workflow }, undefined, loading)
}


const chat: (
  applicationId: String,
  conversationId: string,
  conversation: any,
) => Promise<any> = (applicationId, conversationId, conversation,) => {
  return postStream(`/admin/api/application/${applicationId}/conversation/${conversationId}`, conversation)
}

const pageConversation: (
  applicationId: String,
  currentPage: number,
  pageSize: number,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, currentPage, pageSize, query, loading) => {
  return get(`/application/${applicationId}/conversation`, { ...query, pageSize, currentPage }, loading)
}

const pageConversationMessage: (
  applicationId: String,
  conversationId: String,
  currentPage: number,
  pageSize: number,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, conversationId, currentPage, pageSize, query, loading) => {
  return get(`/application/${applicationId}/conversation/${conversationId}/message`, { ...query, pageSize, currentPage }, loading)
}
const createConversation = (applicationId: String, name: string) => {
  return post(`/application/${applicationId}/conversation`, { name }, {})
}

export default {
  edit,
  chat,
  pageConversation,
  createConversation,
  pageConversationMessage
}
