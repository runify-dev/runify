import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  pool: z.array(z.any()).min(1, { error: '请选择数据库连接池' }),
  location: z.string(),
  reference: z.array(z.string()).optional(),
  template: z.string().optional(),
  parameters: z.array(z.any()).optional()
})

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (data.location === 'reference') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
  }

  if (data.location === 'customize' || !data.location) {
    if (!data.template || String(data.template).trim() === '') {
      return { valid: false, errors: { template: '请输入SQL' } }
    }
  }

  if (Array.isArray(data.parameters)) {
    for (let i = 0; i < data.parameters.length; i++) {
      const value = data.parameters[i]?.value
      const empty =
        value === undefined ||
        value === null ||
        (typeof value === 'string' && value.trim() === '') ||
        (Array.isArray(value) && value.length === 0)
      if (empty) {
        return { valid: false, errors: { [`parameters.${i}.value`]: '请填写参数值' } }
      }
    }
  }

  return { valid: true, errors: {} }
}
