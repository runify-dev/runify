import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  modelId: z.string({ error: '请选择模型' }).min(1, { error: '请选择模型' }),
  enableContext: z.boolean().optional(),
  user: z.string().optional()
}).refine(
  (data) => data.enableContext || (data.user && data.user.trim().length > 0),
  { message: '请输入用户提示词', path: ['user'] }
)

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
