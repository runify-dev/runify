import type { ValidationResult } from '@/workflow/common/type'

export function validate(_nodeData: Record<string, any> | undefined): ValidationResult {
  // 无配置项：会话标识从运行参数取
  return { valid: true, errors: {} }
}
