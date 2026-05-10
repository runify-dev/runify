import { Result, type Page } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'
import { TreeCommonAPI } from '@/api/tree'
import type { Ref } from 'vue'
import type { CreateDatabaseCollectionPoolVO, QueryPageDatabaseCollectionPoolVO } from '@/api/type/database-connection-pool'

const treeAPI = new TreeCommonAPI('datasource')

const getDataSourceTypes: (loading?: Ref<boolean>) => Promise<Result<Array<any>>> = (loading) => {
  return get('/database-collection-pool/types', {}, loading)
}

const getProviders: (type: string, loading?: Ref<boolean>) => Promise<Result<Array<any>>> = (type, loading) => {
  return get(`/database-collection-pool/types/${type}/providers`, {}, loading)
}

const getProviderForm: (provider: string, loading?: Ref<boolean>) => Promise<Result<Array<any>>> = (provider, loading) => {
  return get(`/database-collection-pool/providers/${provider}/form`, {}, loading)
}

const getTables: (resourceId: string, loading?: Ref<boolean>) => Promise<Result<Array<any>>> = (resourceId, loading) => {
  return get(`/datasource/resources/${resourceId}/tables`, {}, loading)
}

const getColumns: (resourceId: string, tableName: string, loading?: Ref<boolean>) => Promise<Result<Array<any>>> = (resourceId, tableName, loading) => {
  return get(`/datasource/resources/${resourceId}/tables/${tableName}/columns`, {}, loading)
}

const create: (
  projectId: string,
  data: CreateDatabaseCollectionPoolVO,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (projectId, data, loading) => {
  return post(`/project/${projectId}/database-collection-pool`, data, undefined, loading)
}

const edit: (
  projectId: string,
  id: string,
  data: CreateDatabaseCollectionPoolVO,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (projectId, id, data, loading) => {
  return put(`/project/${projectId}/database-collection-pool/${id}`, data, undefined, loading)
}

const deleteById: (
  projectId: string,
  id: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (projectId, id, loading) => {
  return del(`/project/${projectId}/database-collection-pool/${id}`, undefined, undefined, loading)
}

const page: (
  projectId: string,
  query: QueryPageDatabaseCollectionPoolVO,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (projectId, query, loading) => {
  return get(`/project/${projectId}/database-collection-pool`, query, loading)
}

export default { ...treeAPI, getDataSourceTypes, getProviders, getProviderForm, getTables, getColumns, create, edit, deleteById, page }
