import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}
  const hasSummary = Array.isArray(data.summaryReference) && data.summaryReference.length > 0
  const hasFacts = Array.isArray(data.factsReference) && data.factsReference.length > 0
  if (!hasSummary && !hasFacts) {
    return { valid: false, errors: { summaryReference: '至少配置摘要或便签中的一个引用' } }
  }
  return { valid: true, errors: {} }
}
