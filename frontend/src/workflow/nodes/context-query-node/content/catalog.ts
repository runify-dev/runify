import { contextQueryNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'context-query-node',
    label: '上下文查询',
    summary: '读取会话持久化上下文（历史消息/摘要/便签），通常放在 start 之后作为循环的种子数据',
    inputs: [],
    outputs: [
      { value: 'history', label: '历史上下文', type: 'array', description: '历史消息数组（不含当前轮）' },
      { value: 'full', label: '完整历史', type: 'array', description: '含当前轮的消息数组' },
      { value: 'summary', label: '摘要', type: 'object', description: '{ text 摘要文本, covered 覆盖条数, seedCovered }' },
      { value: 'facts', label: '便签', type: 'array' },
      { value: 'historyUpto', label: '载入边界', type: 'number' }
    ],
    notes: ['无需配置（会话标识从运行参数自动获取），nodeData 传 {} 即可'],
    template: contextQueryNode
  }
}
