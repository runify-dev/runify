import { Result } from '@/request/Result'
import { get, post, put, del, postStream } from '@/request/index'
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
  return postStream(`/api/application/${applicationId}/conversation/${conversationId}`, conversation)
}

const pageConversation: (
  applicationId: String,
  currentPage: number,
  pageSize: number,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, currentPage, pageSize, query, loading) => {
  return get(`/application/${applicationId}/conversation/${currentPage}/${pageSize}`, query, loading)
}

const createConversation = (applicationId: String, name: string) => {
  return post(`/application/${applicationId}/conversation`, { name }, {})
}

export default {
  edit,
  chat,
  pageConversation,
  createConversation
}
