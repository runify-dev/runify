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

  return { valid: true, errors: {} }
}
