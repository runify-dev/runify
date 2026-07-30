import { Result, type Page } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'
import type { Ref } from 'vue'
import type { CreateProcessorVO, QueryProcessorVO, EditProcessorVO } from '@/api/type/processor'
const createProcessor: (
  projectId: string,
  createProcessorVO: CreateProcessorVO,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (projectId, createProcessorVO, loading) => {
  return post(`/project/${projectId}/processor`, createProcessorVO, {}, loading)
}

const pageProcessor: (
  projectId: string,
  query: QueryProcessorVO,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (projectId, query, loading) => {
  return get(`/project/${projectId}/processor`, query, loading)
}

const getProcessor: (projectId: string,
  processorId: string,
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, loading) => {
    return get(`/project/${projectId}/processor/${processorId}`, {}, loading)
  }
const editProcessor: (
  projectId: string,
  processorId: string,
  body: EditProcessorVO,
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, body, loading) => {
    return put(`/project/${projectId}/processor/${processorId}`, body, {}, loading)
  }

const deploy: (
  projectId: string,
  processorId: string,
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, loading) => {
    return post(`/project/${projectId}/processor/${processorId}/deploy`, {}, {}, loading)
  }


const undeploy: (
  projectId: string,
  processorId: string,
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, loading) => {
    return post(`/project/${projectId}/processor/${processorId}/undeploy`, {}, {}, loading)
  }

const deleteProcessor: (
  projectId: string,
  processorId: string,
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, loading) => {
    return del(`/project/${projectId}/processor/${processorId}`, {}, {}, loading)
  }

// 发布：把当前画布工作流(草稿)快照为一个新版本;部署时端点用最新已发布版本
const publish: (
  projectId: string,
  processorId: string,
  body: { workflow: any; remark?: string },
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, body, loading) => {
    return post(`/project/${projectId}/processor/${processorId}/publish`, body, {}, loading)
  }

// 发布历史列表(不含 snapshot)
const listVersions: (
  projectId: string,
  processorId: string,
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, loading) => {
    return get(`/project/${projectId}/processor/${processorId}/versions`, {}, loading)
  }

// 取单个版本(含 snapshot,用于回滚回填画布)
const getVersion: (
  projectId: string,
  processorId: string,
  versionId: string,
  loading?: Ref<boolean>)
  => Promise<Result<any>> = (projectId, processorId, versionId, loading) => {
    return get(`/project/${projectId}/processor/${processorId}/versions/${versionId}`, {}, loading)
  }

export default { createProcessor, pageProcessor, getProcessor, editProcessor, deploy, undeploy, deleteProcessor, publish, listVersions, getVersion }
