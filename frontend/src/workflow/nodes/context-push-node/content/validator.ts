import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}
  const items = data.items || []

  for (let i = 0; i < items.length; i++) {
    const item = items[i]

    // 校验变量
    if (!item.variable || !Array.isArray(item.variable) || item.variable.length === 0) {
      return { valid: false, errors: { [`items.${i}.variable`]: '请选择变量' } }
    }

    // 校验引用
    if (item.mode === 'reference') {
      if (!item.reference || !Array.isArray(item.reference) || item.reference.length === 0) {
        return { valid: false, errors: { [`items.${i}.reference`]: '请选择引用变量' } }
      }
    }

    // 校验自定义内容
    if (item.mode === 'custom') {
      if (!item.content?.trim()) {
        return { valid: false, errors: { [`items.${i}.content`]: '请输入内容' } }
      }

      // Tool 角色需要校验 JSON 格式
      if (item.role === 'tool') {
        try {
          const parsed = JSON.parse(item.content)
          if (!parsed.toolName) {
            return { valid: false, errors: { [`items.${i}.content`]: '缺少 toolName 字段' } }
          }
          if (!parsed.functionArguments) {
            return { valid: false, errors: { [`items.${i}.content`]: '缺少 functionArguments 字段' } }
          }
        } catch {
          return { valid: false, errors: { [`items.${i}.content`]: 'JSON 格式不正确' } }
        }
      }
    }
  }

  return { valid: true, errors: {} }
}
