interface CreateProcessorVO {
  name: string,
  desc: string,
  protocol: string
}
interface QueryProcessorVO {
  name?: string
  desc?: string,
  protocol?: string,
  currentPage: number,
  pageSize: number
}
interface EditProcessorVO {
  name?: string
  desc?: string,
  meta?: any,
  workflow?: any
}
export type {
  CreateProcessorVO,
  QueryProcessorVO,
  EditProcessorVO
}
