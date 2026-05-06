import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const httpSchema = z.object({
  method: z.string().min(1, { error: '请选择请求方式' }),
  path: z.string().min(1, { error: '请输入请求地址' })
})

const validators: Record<string, (nodeData: any) => ValidationResult> = {
  HTTP: (nodeData) => parseZodResult(httpSchema.safeParse(nodeData?.meta ?? {}))
}

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const protocol = nodeData?.protocol
  return validators[protocol]?.(nodeData) ?? { valid: true, errors: {} }
}
