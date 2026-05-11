import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  pathLocation: z.enum(['reference', 'customize']).default('customize'),
  pathReference: z.array(z.string()).optional(),
  path: z.string().optional(),
  fileName: z.string().optional()
}).refine(
  (data) => {
    if (data.pathLocation === 'reference') {
      return data.pathReference && data.pathReference.length > 0
    }
    return data.path && data.path.length > 0
  },
  { message: '请选择或输入文件路径', path: ['pathLocation'] }
)

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
