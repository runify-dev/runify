interface CreateDatabaseCollectionPoolVO {
  name: string
  desc: string
  protocol: string
  meta: any
}

interface QueryDatabaseCollectionPoolVO {
  name?: string
  desc?: string,
  protocol?: string,

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
