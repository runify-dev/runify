import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  applyFieldList(ctx.model.properties)
}

/** 角色/权限段关闭时不输出对应变量,避免下游引用到永远为空的字段 */
export function applyFieldList(properties: Record<string, any>) {
  const nodeData = properties.nodeData ?? {}
  const fieldList: Array<{ label: string; value: string }> = [
    { label: '是否已登录', value: 'authenticated' },
    { label: '用户信息', value: 'user' }
  ]
  if (nodeData.roles?.enabled) {
    fieldList.push({ label: '角色', value: 'roles' })
  }
  if (nodeData.permissions?.enabled) {
    fieldList.push({ label: '权限', value: 'permissions' })
  }
  properties.field_list = fieldList
}
