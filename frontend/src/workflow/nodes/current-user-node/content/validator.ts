import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (!data.sessionCacheId) {
    return { valid: false, errors: { sessionCacheId: '请选择会话缓存连接' } }
  }

  if (!data.credentialField || String(data.credentialField).trim() === '') {
    return { valid: false, errors: { credentialField: '请输入凭证字段名' } }
  }

  for (const [key, label] of [
    ['roles', '角色'],
    ['permissions', '权限']
  ] as const) {
    const segment = data[key]
    if (!segment?.enabled) continue
    if (segment.source === 'cache') {
      if (!segment.cacheId) {
        return { valid: false, errors: { [`${key}.cacheId`]: `请选择${label}缓存连接` } }
      }
    } else if (!segment.field || String(segment.field).trim() === '') {
      return { valid: false, errors: { [`${key}.field`]: `请输入${label}在用户对象中的字段名` } }
    }
  }

  return { valid: true, errors: {} }
}
