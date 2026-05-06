import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'
import { getConditions, getConditionErrors, type JudgeBranch } from '../type'

const conditionSchema = z.object({
  id: z.string(),
  variable: z.array(z.string()).min(1, { error: '变量值不可为空' }),
  compare: z.string().min(1, { error: '条件不可为空' }),
  value: z.string().optional()
})

const branchSchema = z.object({
  id: z.string(),
  type: z.enum(['if', 'elseif', 'else']),
  logic: z.enum(['and', 'or']).optional(),
  conditions: z.array(conditionSchema).optional()
})

export const schema = z
  .object({
    branches: z.array(branchSchema).min(1, { error: '条件分支配置不完整' })
  })
  .check((ctx) => {
    const branches = ctx.value.branches as JudgeBranch[]
    if (!branches.some((b) => b.type === 'else')) {
      ctx.issues.push({
        code: 'custom',
        input: ctx.value,
        message: '必须保留否则分支',
        path: ['branches']
      })
    }
    for (const branch of branches) {
      if (branch.type === 'else') continue
      for (const condition of getConditions(branch)) {
        const errors = getConditionErrors(condition)
        if (errors.variable) {
          ctx.issues.push({
            code: 'custom',
            input: condition.variable,
            message: '变量值不可为空',
            path: ['branches', branch.id, condition.id, 'variable']
          })
        }
        if (errors.compare) {
          ctx.issues.push({
            code: 'custom',
            input: condition.compare,
            message: '条件不可为空',
            path: ['branches', branch.id, condition.id, 'compare']
          })
        }
        if (errors.value) {
          ctx.issues.push({
            code: 'custom',
            input: condition.value,
            message: '变量值不可为空',
            path: ['branches', branch.id, condition.id, 'value']
          })
        }
      }
    }
  })

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? { branches: [] }))
}
