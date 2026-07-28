import { jsonResponseNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { RESPONSE_BODY_INPUTS, normalizeResponseNodeData } from '@/workflow/ai-generate/catalog-library'

/** 工具版数据响应：可切「对话/处理器」两种模式（toolResponseMode）；status/headers 仅处理器模式用，非必填 */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.TOOL],
  entry: {
    type: 'response-node',
    label: '数据响应',
    summary: '工具向调用方输出响应内容；被处理器调用可出 HTTP 响应，被对话调用可出结构化/文本内容',
    inputs: [
      {
        key: 'status',
        label: 'HTTP 状态码（处理器模式）',
        type: 'number',
        default: 200,
        description: '仅处理器模式使用，非必填'
      },
      {
        key: 'headers',
        label: '响应头（处理器模式）',
        type: 'array',
        default: [],
        description: '仅处理器模式使用，元素 { field, location:"customize"|"reference", value, reference:[节点ID,字段] }'
      },
      ...RESPONSE_BODY_INPUTS
    ],
    outputs: [],
    notes: ['工具的数据返回走 outputSchema（变量赋值→global）；本节点用于产生对话/HTTP 响应内容'],
    template: jsonResponseNode,
    normalizeNodeData: normalizeResponseNodeData
  }
}
