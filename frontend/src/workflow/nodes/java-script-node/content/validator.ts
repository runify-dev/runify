import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  mode: z.enum(['script', 'function']).default('script'),
  codeLocation: z.enum(['reference', 'customize']).default('customize'),
  codeReference: z.array(z.string()).optional(),
  functionName: z.string().optional(),
  code: z.string().optional(),
  parameters: z.array(z.any()).optional()
}).refine(
  (data) => data.mode !== 'function' || (data.functionName && data.functionName.length > 0),
  { message: '请输入函数名称', path: ['functionName'] }
).refine(
  (data) => {
    if (data.codeLocation === 'reference') {
      return data.codeReference && data.codeReference.length > 0
    }
    return data.code && data.code.length > 0
  },
  { message: '请填写JavaScript代码或选择引用变量', path: ['codeLocation'] }
)

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}
  const result = parseZodResult(schema.safeParse(data))
  if (!result.valid) {
    return result
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

  return result
}
