import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'
import { WorkflowType } from '@/workflow/common/data'

export const schema = z
  .object({
    loopType: z.string().min(1, { error: '请选择循环类型' }),
    loopCount: z.number().min(1).optional(),
    loopVariable: z.array(z.string()).optional()
  })
  .refine((data) => {
    if (data.loopType === 'count' && (data.loopCount == null || data.loopCount < 1)) return false
    return true
  }, { message: '请输入有效的循环次数', path: ['loopCount'] })
  .refine((data) => {
    if (data.loopType === 'foreach' && (!data.loopVariable || data.loopVariable.length === 0)) return false
    return true
  }, { message: '请选择循环变量', path: ['loopVariable'] })

export function validate(
  nodeData: Record<string, any> | undefined,
  validators?: Map<string, (nodeData: Record<string, any>, validators?: Map<string, any>, workflowType?: string) => ValidationResult>,
  workflowType?: string
): ValidationResult {
  const selfResult = parseZodResult(schema.safeParse(nodeData ?? {}))
  if (!selfResult.valid) {
    return selfResult
  }

  const children = nodeData?.children?.nodes as Array<{ id: string; type: string; properties?: Record<string, any> }> | undefined
  if (!children?.length || !validators) {
    return selfResult
  }

  // 父级 APPLICATION/PROCESSOR → 子循环 APPLICATION_LOOP/PROCESSOR_LOOP
  const childWorkflowType = workflowType?.startsWith(WorkflowType.PROCESSOR)
    ? WorkflowType.PROCESSOR_LOOP
    : WorkflowType.APPLICATION_LOOP

  for (const child of children) {
    const childValidator = validators.get(child.type)
    if (!childValidator) continue
    const childResult = childValidator(child.properties?.nodeData ?? {}, validators, childWorkflowType)
    if (!childResult.valid) {
      return {
        valid: false,
        errors: childResult.errors,
        failedNodeId: childResult.failedNodeId ?? child.id,
        failedPath: childResult.failedPath ?? []
      }
    }
  }

  return selfResult
}
