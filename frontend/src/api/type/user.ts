interface User {
  /**
   * 用户id
   */
  id: string
  /**
   * 用户名
   */
  username: string
  /**
   * 邮箱
   */
  email: string
  /**
   * 昵称
   */
  nickname: string
  /**
   * 图标
   */
  icon: string
  /**
   * 用户角色
   */
  role: string
  /**
   * 用户权限
   */
  permissions: Array<string>
}
interface LoginPojo {
  username: string
  password: string
}

interface UserQueryPojo {
  mixing?: string,
  username?: string,
  nickname?: string
}
export type {
  User,
  LoginPojo,
  UserQueryPojo
}
