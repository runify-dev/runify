import { jsonResponseNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { RESPONSE_BODY_INPUTS, normalizeResponseNodeData } from '@/workflow/ai-generate/catalog-library'

/** 应用画布版数据响应：无状态码/响应头概念，多 chunk 分块输出开关 */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.APPLICATION],
  entry: {
    type: 'response-node',
    label: '数据响应',
    summary: '向用户输出结构化/文本响应内容（引用上游变量或固定值）',
    inputs: [
      ...RESPONSE_BODY_INPUTS,
      { key: 'chunk', label: '分块输出', type: 'boolean', default: false, description: '保持默认 false' }
    ],
    outputs: [],
    notes: ['典型用法：把 ai-chat 回答之外的结构化数据（如检索结果）响应给前端'],
    template: jsonResponseNode,
    normalizeNodeData: normalizeResponseNodeData
  }
}
