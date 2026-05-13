import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (data.location === 'reference') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择 patch 变量' } }
    }
  }

  if (data.location === 'customize' || !data.location) {
    if (!data.patch || String(data.patch).trim() === '') {
      return { valid: false, errors: { patch: '请输入 patch 内容' } }
    }
  }

  return { valid: true, errors: {} }
}
