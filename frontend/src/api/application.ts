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
  folderId: string,
  applicationId: String,
  conversation: any,
) => Promise<any> = (folderId, applicationId, conversation) => {
  return postStream(`/api/application/folder/${folderId}/resource/${applicationId}/conversation`, conversation)
}

const pageConversation: (
  folderId: string,
  applicationId: String,
  currentPage: number,
  pageSize: number,
  query: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (folderId, applicationId, currentPage, pageSize, query, loading) => {
  return get(`/application/folder/${folderId}/resource/${applicationId}/conversation/${currentPage}/${pageSize}`, query, loading)
}


export default {
  edit,
  chat,
  pageConversation
}
