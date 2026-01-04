import { Result, type Page } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'
import type { Ref } from 'vue'
import type { CreateDatabaseCollectionPoolVO, QueryDatabaseCollectionPoolVO, QueryPageDatabaseCollectionPoolVO } from '@/api/type/database-connection-pool'
const create: (
  projectId: string,
  createDatabaseCollectionPoolVO: CreateDatabaseCollectionPoolVO,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (projectId, createDatabaseCollectionPoolVO, loading) => {
  return post(`/project/${projectId}/database-collection-pool`, createDatabaseCollectionPoolVO, {}, loading)
}

const query: (
  projectId: string,
  query: QueryDatabaseCollectionPoolVO,
  loading?: Ref<boolean>
) => Promise<Result<Array<any>>> = (projectId, query, loading) => {
  return get(`/project/${projectId}/database-collection-pool`, query, loading)
}

const page: (
  projectId: string,
  query: QueryPageDatabaseCollectionPoolVO,
  loading?: Ref<boolean>
) => Promise<Result<Page<any>>> = (projectId, query, loading) => {
  return get(`/project/${projectId}/database-collection-pool`, query, loading)
}


export default { create, query, page }
