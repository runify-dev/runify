import { Result } from '@/request/Result'
import { get, post } from '@/request/index'
import type {
    User,
    LoginPojo
} from '@/api/type/user'
import type { Ref } from 'vue'
const login: (
    LoginPojo: LoginPojo,
    loading?: Ref<boolean>
) => Promise<Result<string>> = (loginPojo, loading) => {
    return post(`/login`, loginPojo, undefined, loading)
}

const profile: (
    loading?: Ref<boolean>
) => Promise<Result<User>> = (loading) => {
    return get(`/user`, undefined, loading)
}
export default { login, profile }