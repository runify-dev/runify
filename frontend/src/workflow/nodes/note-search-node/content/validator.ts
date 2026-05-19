import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  // 目录校验
  if (!data.folderIds || (Array.isArray(data.folderIds) && data.folderIds.length === 0)) {
    return { valid: false, errors: { folderIds: '请选择目录' } }
  }

  // tool_call 模式
  if (data.location === 'tool_call') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
    return { valid: true, errors: {} }
  }

  // customize 模式
  if (data.keywordLocation === 'reference') {
    if (!data.keywordReference || !Array.isArray(data.keywordReference) || data.keywordReference.length === 0) {
      return { valid: false, errors: { keywordReference: '请选择引用变量' } }
    }
  } else {
    if (!data.keyword || String(data.keyword).trim() === '') {
      return { valid: false, errors: { keyword: '请输入检索关键词' } }
    }
  }

  return { valid: true, errors: {} }
}
