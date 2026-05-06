import type { ValidationResult } from '@/workflow/common/type'
import { validate as validateApplication } from './application/validator'
import { validate as validateProcessor } from './processor/validator'
import { WorkflowType } from '@/workflow/common/data'

const validatorMap: Record<string, (nodeData: Record<string, any> | undefined) => ValidationResult> = {
  [WorkflowType.APPLICATION]: validateApplication, 
   [WorkflowType.APPLICATION_LOOP]: validateApplication,
  [WorkflowType.PROCESSOR]: validateProcessor
}

export function validate(
  nodeData: Record<string, any> | undefined,
  _validators?: Map<string, any>,
  workflowType?: string
): ValidationResult {
  const validator = workflowType ? validatorMap[workflowType] : undefined
  if (validator) {
    return validator(nodeData)
  }
  // 默认使用 processor 校验
  return validateProcessor(nodeData)
}
