import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (data.location === 'tool_call') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
    return { valid: true, errors: {} }
  }

  if (data.patternLocation === 'reference') {
    if (!data.patternReference || !Array.isArray(data.patternReference) || data.patternReference.length === 0) {
      return { valid: false, errors: { patternReference: '请选择 glob 模式变量' } }
    }
  }
  if (data.patternLocation === 'customize' || !data.patternLocation) {
    if (!data.pattern || String(data.pattern).trim() === '') {
      return { valid: false, errors: { pattern: '请输入 glob 模式' } }
    }
  }

  return { valid: true, errors: {} }
}
