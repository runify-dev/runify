import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (!data.sourceReference || !Array.isArray(data.sourceReference) || data.sourceReference.length === 0) {
    return { valid: false, errors: { sourceReference: '请选择要提取便签的源消息变量' } }
  }

  // 便签写回变量非必填：不配时便签仍输出到节点自身上下文（[本节点, facts]），
  // 只是不做跨迭代累积到父层变量（循环累积场景才需要配）。
  if (!String(data.modelId ?? '').trim()) {
    return { valid: false, errors: { modelId: '请选择提取模型' } }
  }

  return { valid: true, errors: {} }
}
