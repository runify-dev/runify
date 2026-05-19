import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (data.location === 'tool_call') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
    return { valid: true, errors: {} }
  }

  if (data.fileIdLocation === 'reference') {
    if (!data.fileIdReference || !Array.isArray(data.fileIdReference) || data.fileIdReference.length === 0) {
      return { valid: false, errors: { fileIdReference: '请选择文件ID变量' } }
    }
  } else {
    if (!data.fileId || String(data.fileId).trim() === '') {
      return { valid: false, errors: { fileId: '请输入文件ID' } }
    }
  }

  return { valid: true, errors: {} }
}
