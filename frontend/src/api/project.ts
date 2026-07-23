import { Result } from '@/request/Result'
import { get, put } from '@/request/admin/index'
import type { Ref } from 'vue'

/**
 * 获取项目统一异常配置(结构:{ validationError: ResponseNodeData })
 */
const getErrorResponse: (projectId: string, loading?: Ref<boolean>) => Promise<Result<any>> = (
  projectId,
  loading
) => {
  return get(`/project/resources/${projectId}/error-response`, {}, loading)
}

/**
 * 修改项目统一异常配置
 */
const editErrorResponse: (
  projectId: string,
  body: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (projectId, body, loading) => {
  return put(`/project/resources/${projectId}/error-response`, body, {}, loading)
}

export default {
  getErrorResponse,
  editErrorResponse
}
