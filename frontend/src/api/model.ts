import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
import type {
  MarkdownNode
} from '@/api/type/knowledge'
import type { Ref } from 'vue'
import { type KnowledgeEdit } from '@/api/type/knowledge'

const getProvider: (
  loading?: Ref<boolean>
) => Promise<Result<any>> = (loading) => {
  return get(`/model/provider`, {}, loading)
}
const getProviderModelList: (
  provider: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (provider, loading) => {
  return get(`/model/${provider}/template`, {}, loading)
}

const listModelType: (
  provider: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (provider, loading) => {
  return get(`/model/${provider}/type`, {}, loading)
}

const edit: (
  folderId: string,
  resourceId: string,
  model: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (folderId, resourceId, model, loading) => {
  return put(`/model/folder/${folderId}/resource/${resourceId}`, model, loading)
}
export default {
  getProvider,
  getProviderModelList,
  listModelType,
  edit
}
