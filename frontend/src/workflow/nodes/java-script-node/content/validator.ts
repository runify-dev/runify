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
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
