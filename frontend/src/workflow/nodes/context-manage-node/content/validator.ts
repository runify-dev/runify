import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  // 上下文初始值（入参）必填：压缩的主功能就是压上下文，必须有待处理内容。
  // 变化值是出参（写回压缩结果），可空；摘要、便签的入参也均可留空。
  if (!Array.isArray(data.sourceSeedVariable) || data.sourceSeedVariable.length === 0) {
    return { valid: false, errors: { sourceSeedVariable: '请选择待压缩的上下文（初始值为入参，必填）' } }
  }
  // 出参不能和入参指向同一变量，否则下一轮会把压缩产物当输入二次压缩
  if (
    Array.isArray(data.sourceVariable) &&
    data.sourceVariable.length > 0 &&
    JSON.stringify(data.sourceVariable) === JSON.stringify(data.sourceSeedVariable)
  ) {
    return { valid: false, errors: { sourceVariable: '变化值（出参）不能与初始值（入参）相同' } }
  }

  if (data.enableSummarizer && !String(data.summarizerModelId ?? '').trim()) {
    return { valid: false, errors: { summarizerModelId: '启用 LLM 摘要需要选择模型' } }
  }

  if (data.budget != null && (typeof data.budget !== 'number' || data.budget < 1000)) {
    return { valid: false, errors: { budget: '预算不能小于 1000 token' } }
  }

  if (
    data.highRatio != null &&
    data.lowRatio != null &&
    Number(data.lowRatio) >= Number(data.highRatio)
  ) {
    return { valid: false, errors: { lowRatio: '低水位必须小于高水位' } }
  }

  return { valid: true, errors: {} }
}
