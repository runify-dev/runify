import { jsonResponseNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { RESPONSE_BODY_INPUTS, normalizeResponseNodeData } from '@/workflow/ai-generate/catalog-library'

/** 处理器版数据响应：HTTP 响应（校验器强制 status 必填，另有响应头） */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.PROCESSOR],
  entry: {
    type: 'response-node',
    label: 'HTTP 响应',
    summary: '把处理结果作为 HTTP 响应返回给调用方（状态码、响应头、响应体），处理器工作流的收尾节点',
    inputs: [
      { key: 'status', label: 'HTTP 状态码', type: 'number', required: true, default: 200 },
      {
        key: 'headers',
        label: '响应头',
        type: 'array',
        default: [{ field: 'content-type', value: 'application/json; charset=utf-8', location: 'customize' }],
        description:
          '元素 { field 头名, location:"customize"|"reference", value 固定值, reference:[节点ID,字段], required: boolean }；' +
          '默认已含 content-type: application/json，一般无需改动'
      },
      ...RESPONSE_BODY_INPUTS
    ],
    outputs: [],
    notes: ['处理器工作流必须以本节点结尾，否则 HTTP 调用方收不到响应内容'],
    template: jsonResponseNode,
    normalizeNodeData: normalizeResponseNodeData
  }
}
