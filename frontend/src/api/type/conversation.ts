interface QueryConversationVO {
  currentPage: number
  pageSize: number
}

interface QueryApplicationVO {
  currentPage: number
  pageSize: number
  name?: string
}
interface LoginVO {
  username: string
  password: string
}

export type {QueryConversationVO,QueryApplicationVO,LoginVO}
