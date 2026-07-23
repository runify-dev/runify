import { loopBreakNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { normalizeConditions, CONDITION_INPUT_DESC } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'loop-break-node',
    label: '跳出循环',
    summary: '（仅循环子画布内）满足条件时跳出整个循环',
    inputs: [
      { key: 'conditions', label: '条件', type: 'array', default: [], description: CONDITION_INPUT_DESC },
      { key: 'logic', label: '逻辑', type: 'string', enum: ['and', 'or'], default: 'and' }
    ],
    outputs: [],
    template: loopBreakNode,
    normalizeNodeData: normalizeConditions
  }
}
