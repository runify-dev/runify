import { Result } from '@/request/Result'
import { get, post, put, del, postStream } from '@/request/index'
import type { Ref } from 'vue'


const edit: (
  folderId: string,
  applicationId: String,
  workflow: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (folderId, applicationId, workflow, loading) => {
  return put(`/application/folder/${folderId}/resource/${applicationId}`, { 'workflow': workflow }, undefined, loading)
}


const chat: (
  folderId: string,
  applicationId: String,
  conversation: any,
) => Promise<any> = (folderId, applicationId, conversation) => {
  return postStream(`/api/application/folder/${folderId}/resource/${applicationId}/conversation`, conversation)
}



export default {
  edit,
  chat
}
