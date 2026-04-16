import { Result, type Page } from '@/request/Result'
import { get, post, del } from '@/request/admin/index'
import type {
  User,
  LoginPojo,
  UserQueryPojo
} from '@/api/type/user'
import type { Ref } from 'vue'
const listRoles: (
  loading?: Ref<boolean>
) => Promise<Result<any>> = (loading) => {
  return get(`/role`, undefined, loading)
}
const listPermission: (
  roleId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (roleId, loading) => {
  return get(`/role/${roleId}/permissions`, undefined, loading)
}

const modifyPermissions: (
  roleId: string,
  body: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (roleId, body, loading) => {
  return post(`/role/${roleId}/permissions`, body, undefined, loading)
}
const createRole: (
  body: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (body, loading) => {
  return post(`/role`, body, undefined, loading)
}
export default { listRoles, listPermission, modifyPermissions, createRole }
