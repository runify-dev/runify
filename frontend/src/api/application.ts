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
const getConversationContext: (
  applicationId: string,
  conversationId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, conversationId, loading) => {
  return get(`/application/${applicationId}/conversation/${conversationId}/context`, {}, loading)
}

const getSections = (applicationId: string, loading?: Ref<boolean>) => {
  return get(`/application/${applicationId}/section`, {}, loading)
}

const saveSections = (applicationId: string, sections: any[], loading?: Ref<boolean>) => {
  return put(`/application/${applicationId}/section`, sections, {}, loading)
}

// 我的便签（后台侧）：当前登录管理员在该应用的 user 档便签，调试/管理端对话页用
const getMySections = (applicationId: string, loading?: Ref<boolean>) => {
  return get(`/application/${applicationId}/section-fact`, {}, loading)
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

const modifyName: (
  applicationId: string,
  conversationId: string,
  name: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, conversationId, name, loading) => {
  return put(
    `/application/${applicationId}/conversation/${conversationId}/modify-name`,
    { name },
    undefined,
    loading
  )
}

const delConversation: (
  applicationId: string,
  conversationId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, conversationId, loading) => {
  return del(
    `/application/${applicationId}/conversation/${conversationId}`,
    undefined,
    loading
  )
}

// 发布：把当前画布工作流(草稿)快照为一个新版本,最新版本即线上生效版本
const publish: (
  applicationId: string,
  data: { workflow: any; remark?: string },
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, data, loading) => {
  return post(`/application/resources/${applicationId}/publish`, data, undefined, loading)
}

// 发布历史列表(不含 snapshot)
const listVersions: (
  applicationId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, loading) => {
  return get(`/application/resources/${applicationId}/versions`, undefined, loading)
}

// 取单个版本(含 snapshot,用于回滚回填画布)
const getVersion: (
  applicationId: string,
  versionId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (applicationId, versionId, loading) => {
  return get(`/application/resources/${applicationId}/versions/${versionId}`, undefined, loading)
}

export default {
  edit,
  chat,
  pageConversation,
  mineConversation,
  createConversation,
  pageConversationMessage,
  getConversationContext,
  getSections,
  saveSections,
  getMySections,
  statusStream,
  resumeStream,
  getApplicationInfo,
  getOverview,
  cancel,
  modifyName,
  delConversation,
  publish,
  listVersions,
  getVersion
}
