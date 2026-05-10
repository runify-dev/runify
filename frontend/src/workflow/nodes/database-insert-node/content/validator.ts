import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (!data.poolId) {
    return { valid: false, errors: { poolId: '请选择数据库连接池' } }
  }

  if (data.location === 'reference') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
  }

  if (data.location === 'customize' || !data.location) {
    if (!data.template || String(data.template).trim() === '') {
      return { valid: false, errors: { template: '请输入SQL' } }
    }
  }

  return { valid: true, errors: {} }
}
