import { contextSaveNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'context-save-node',
    label: '上下文写入',
    summary: '把摘要/便签写回会话持久化存储，通常放在外层循环结束之后',
    inputs: [
      { key: 'summaryReference', label: '摘要来源', type: 'array', reference: true, required: '与 factsReference 至少配置一个', description: '如 [外层循环ID,"summary"]' },
      { key: 'factsReference', label: '便签来源', type: 'array', reference: true, required: '与 summaryReference 至少配置一个', description: '如 [外层循环ID,"facts"]' }
    ],
    outputs: [
      { value: 'savedSummary', label: '摘要已写入', type: 'boolean' },
      { value: 'savedFacts', label: '便签写入数', type: 'number' }
    ],
    template: contextSaveNode
  }
}
