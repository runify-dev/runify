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

export default { createProcessor, pageProcessor, getProcessor, editProcessor, deploy, undeploy, deleteProcessor }
