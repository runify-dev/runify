import { loopContinueNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { normalizeConditions, CONDITION_INPUT_DESC } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'loop-continue-node',
    label: '循环跳过',
    summary: '（仅循环子画布内）满足条件时跳过本轮，进入下一轮',
    inputs: [
      { key: 'conditions', label: '条件', type: 'array', default: [], description: CONDITION_INPUT_DESC },
      { key: 'logic', label: '逻辑', type: 'string', enum: ['and', 'or'], default: 'and' }
    ],
    outputs: [],
    template: loopContinueNode,
    normalizeNodeData: normalizeConditions
  }
}
