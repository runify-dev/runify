import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (data.location === 'tool_call') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
    return { valid: true, errors: {} }
  }

  if (data.skillIdLocation === 'reference') {
    if (!data.skillIdReference || !Array.isArray(data.skillIdReference) || data.skillIdReference.length === 0) {
      return { valid: false, errors: { skillIdReference: '请选择技能ID变量' } }
    }
  } else {
    if (!data.skillId || String(data.skillId).trim() === '') {
      return { valid: false, errors: { skillId: '请输入技能ID' } }
    }
  }

  return { valid: true, errors: {} }
}
