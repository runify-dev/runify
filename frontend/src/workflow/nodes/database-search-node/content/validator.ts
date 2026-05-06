import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  pool: z.array(z.any()).min(1, { error: '请选择数据库连接池' }),
  template: z.string().min(1, { error: '请输入SQL' }),
  parameters: z.array(z.any()).optional()
})

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
