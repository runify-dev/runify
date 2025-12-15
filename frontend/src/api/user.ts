import { Result, type Page } from '@/request/Result'
import { get, post, del } from '@/request/admin/index'
import type {
  User,
  LoginPojo,
  UserQueryPojo
} from '@/api/type/user'
import type { Ref } from 'vue'
const login: (
  LoginPojo: LoginPojo,
  loading?: Ref<boolean>
) => Promise<Result<string>> = (loginPojo, loading) => {
  return post(`/login`, loginPojo, undefined, loading)
}
const page: (query: UserQueryPojo, currentPage: number, pageSize: number, loading?: Ref<boolean>) => Promise<Result<Page<User>>> = (query, currentPage, pageSize, loading) => {
  return get(`/user/${currentPage}/${pageSize}`, query, loading)
}

const profile: (
  loading?: Ref<boolean>
) => Promise<Result<User>> = (loading) => {
  return get(`/user`, undefined, loading)
}

const logout: (
  loading?: Ref<boolean>
) => Promise<Result<User>> = (loading) => {
  return get(`/logout`, undefined, loading)
}

const createUser: (
  body: any,
  loading?: Ref<boolean>
) => Promise<Result<User>> = (body, loading) => {
  return post(`/user`, body, {}, loading)
}
const deleteUser: (
  userId: string,
  loading?: Ref<boolean>
) => Promise<Result<boolean>> = (userId, loading) => {
  return del(`/user/${userId}`, {}, {}, loading)
}
export default { login, profile, logout, page, createUser, deleteUser }
