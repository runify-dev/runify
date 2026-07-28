import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}
  if (!data.toolId || String(data.toolId).trim() === '') {
    return { valid: false, errors: { toolId: '请选择工具' } }
  }
  return { valid: true, errors: {} }
}
