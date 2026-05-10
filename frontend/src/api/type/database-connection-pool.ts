interface CreateDatabaseCollectionPoolVO {
  name: string
  desc: string
  dataSourceType: string
  provider: string
  meta: any
}

interface QueryDatabaseCollectionPoolVO {
  name?: string
  desc?: string
  provider?: string
}
interface QueryPageDatabaseCollectionPoolVO extends QueryDatabaseCollectionPoolVO {
  currentPage: number,
  pageSize: number
}
export type {
  CreateDatabaseCollectionPoolVO,
  QueryDatabaseCollectionPoolVO,
  QueryPageDatabaseCollectionPoolVO

}
