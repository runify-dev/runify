import { z } from 'zod'
import type { ValidationResult } from './type'

export function parseZodResult(result: z.ZodSafeParseResult<any>): ValidationResult {
  if (result.success) {
    return { valid: true, errors: {} }
  }
  const errors: Record<string, string> = {}
  for (const issue of result.error.issues) {
    const key = issue.path.join('.') || '_root'
    if (!errors[key]) {
      errors[key] = issue.message
    }
  }
  return { valid: false, errors }
}
