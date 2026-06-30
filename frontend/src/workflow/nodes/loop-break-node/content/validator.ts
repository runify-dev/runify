import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'
import { getConditionErrors, type JudgeCondition } from '@/workflow/nodes/judge-node/type'

const conditionSchema = z.object({
  id: z.string(),
  variable: z.array(z.string()).min(1, { error: '变量值不可为空' }),
  compare: z.string({ error: '条件不可为空' }).min(1, { error: '条件不可为空' }),
  value: z.string().optional()
})

export const schema = z.object({
  conditions: z.array(conditionSchema).optional(),
  logic: z.enum(['and', 'or']).optional()
})

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
