import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  functionName: z.string().min(1, { error: '请输入函数名称' }),
  code: z.string().min(1, { error: '请输入JavaScript代码' }),
  parameters: z.array(z.any()).optional()
})

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
