import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (!data.sourceReference || !Array.isArray(data.sourceReference) || data.sourceReference.length === 0) {
    return { valid: false, errors: { sourceReference: '请选择源变量' } }
  }

  if (!data.rules || !Array.isArray(data.rules) || data.rules.length === 0) {
    return { valid: false, errors: { rules: '请添加至少一条提取规则' } }
  }

  for (let i = 0; i < data.rules.length; i++) {
    const rule = data.rules[i]
    if (!rule.name || rule.name.trim() === '') {
      return { valid: false, errors: { [`rule_${i}_name`]: '字段名不能为空' } }
    }
    if (!rule.path || rule.path.trim() === '') {
      return { valid: false, errors: { [`rule_${i}_path`]: 'JSONPath 不能为空' } }
    }
  }

  return { valid: true, errors: {} }
}
