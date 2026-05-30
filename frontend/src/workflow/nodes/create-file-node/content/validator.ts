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
      return { valid: false, errors: { pathReference: '请选择文件路径变量' } }
    }
  }
  if (data.pathLocation === 'customize' || !data.pathLocation) {
    if (!data.path || String(data.path).trim() === '') {
      return { valid: false, errors: { path: '请输入文件路径' } }
    }
  }

  if (data.contentLocation === 'reference') {
    if (!data.contentReference || !Array.isArray(data.contentReference) || data.contentReference.length === 0) {
      return { valid: false, errors: { contentReference: '请选择文件内容变量' } }
    }
  }
  if (data.contentLocation === 'customize' || !data.contentLocation) {
    if (!data.content || String(data.content).trim() === '') {
      return { valid: false, errors: { content: '请输入文件内容' } }
    }
  }

  return { valid: true, errors: {} }
}
