import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  runtime: z.string(),
  location: z.string(),
  reference: z.array(z.string()).optional(),
  codeLocation: z.string().optional(),
  codeReference: z.array(z.string()).optional(),
  code: z.string().optional(),
  timeoutLocation: z.string().optional(),
  timeoutReference: z.array(z.string()).optional(),
  timeout: z.number().min(1).max(3600).optional()
})

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  // 代码校验
  if (data.location === 'tool_call') {
    if (!data.reference || !Array.isArray(data.reference) || data.reference.length === 0) {
      return { valid: false, errors: { reference: '请选择引用变量' } }
    }
  } else {
    if (data.codeLocation === 'reference') {
      if (!data.codeReference || !Array.isArray(data.codeReference) || data.codeReference.length === 0) {
        return { valid: false, errors: { codeReference: '请选择代码变量' } }
      }
    } else {
      if (!data.code || typeof data.code !== 'string' || data.code.trim().length === 0) {
        return { valid: false, errors: { code: '请输入代码' } }
      }
    }
  }

  // 超时校验
  if (data.timeoutLocation === 'reference') {
    if (!data.timeoutReference || !Array.isArray(data.timeoutReference) || data.timeoutReference.length === 0) {
      return { valid: false, errors: { timeoutReference: '请选择超时变量' } }
    }
  }

  return { valid: true, errors: {} }
}
