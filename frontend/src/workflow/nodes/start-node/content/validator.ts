import type { ValidationResult } from '@/workflow/common/type'
import { validate as validateProcessor } from './processor/validator'

const validators: Record<string, (nodeData: any) => ValidationResult> = {
  PROCESSOR: validateProcessor
}

export function validate(
  nodeData: Record<string, any> | undefined,
  _validators?: Map<string, any>,
  workflowType?: string
): ValidationResult {
  if (!workflowType) return { valid: true, errors: {} }
  return validators[workflowType]?.(nodeData) ?? { valid: true, errors: {} }
}
