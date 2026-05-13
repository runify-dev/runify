import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (data.location === 'tool_call') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
    return { valid: true, errors: {} }
  }

  if (data.pathLocation === 'reference') {
    if (!data.pathReference || !Array.isArray(data.pathReference) || data.pathReference.length === 0) {
      return { valid: false, errors: { pathReference: '请选择目录路径变量' } }
    }
  }
  if (data.pathLocation === 'customize' || !data.pathLocation) {
    if (!data.path || String(data.path).trim() === '') {
      return { valid: false, errors: { path: '请输入目录路径' } }
    }
  }

  return { valid: true, errors: {} }
}
