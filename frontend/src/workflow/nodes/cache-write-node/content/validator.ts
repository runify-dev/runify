import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (!data.cacheId) {
    return { valid: false, errors: { cacheId: '请选择缓存连接' } }
  }

  if (data.keyLocation === 'reference') {
    if (!data.keyReference || !Array.isArray(data.keyReference) || data.keyReference.length === 0) {
      return { valid: false, errors: { keyReference: '请选择Key变量' } }
    }
  }

  if (data.keyLocation === 'customize' || !data.keyLocation) {
    if (!data.key || String(data.key).trim() === '') {
      return { valid: false, errors: { key: '请输入Key' } }
    }
  }

  if (data.valueLocation === 'reference') {
    if (!data.valueReference || !Array.isArray(data.valueReference) || data.valueReference.length === 0) {
      return { valid: false, errors: { valueReference: '请选择Value变量' } }
    }
  }

  if (data.valueLocation === 'customize' || !data.valueLocation) {
    if (!data.value || String(data.value).trim() === '') {
      return { valid: false, errors: { value: '请输入Value' } }
    }
  }

  return { valid: true, errors: {} }
}
