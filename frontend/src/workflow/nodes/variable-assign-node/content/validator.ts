import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}
  const variables = data.variables || []

  // 检查变量不为空
  for (let i = 0; i < variables.length; i++) {
    const v = variables[i]
    if (!v.variable || !Array.isArray(v.variable) || v.variable.length === 0) {
      return { valid: false, errors: { [`variables.${i}.variable`]: '请选择变量' } }
    }
    if (v.type === 'reference' && (!v.reference || !Array.isArray(v.reference) || v.reference.length === 0)) {
      return { valid: false, errors: { [`variables.${i}.reference`]: '请选择引用变量' } }
    }
    if (v.type === 'constant' && !v.value?.trim()) {
      return { valid: false, errors: { [`variables.${i}.value`]: '请输入常量值' } }
    }
  }

  return { valid: true, errors: {} }
}
