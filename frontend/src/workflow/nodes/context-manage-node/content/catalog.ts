import { contextManageNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'context-manage-node',
    label: '上下文压缩',
    summary: '按 token 预算压缩上下文（可选 LLM 摘要），agent 循环中放在 ai-chat 之前',
    inputs: [
      { key: 'sourceSeedVariable', label: '待压缩上下文（入参）', type: 'array', reference: true, required: true, description: '首轮种子，如 context-query 的 history 或循环变量 [外层循环ID,"context"]' },
      { key: 'sourceVariable', label: '压缩结果写回（出参）', type: 'array', reference: true, description: '不能与 sourceSeedVariable 相同，如 [外层循环ID,"compress_context"]，供 ai-chat 的 contextVariable 使用' },
      { key: 'summarySeedVariable', label: '摘要入参', type: 'array', reference: true },
      { key: 'summaryVariable', label: '摘要出参', type: 'array', reference: true },
      { key: 'factsSeedVariable', label: '便签入参', type: 'array', reference: true },
      { key: 'factsVariable', label: '便签出参', type: 'array', reference: true },
      { key: 'budget', label: 'token 预算', type: 'number', default: 32000, description: '≥1000' },
      { key: 'highRatio', label: '高水位', type: 'number', default: 0.85 },
      { key: 'lowRatio', label: '低水位', type: 'number', default: 0.6 },
      { key: 'keepRecentItems', label: '保留最近条数', type: 'number', default: 10 },
      { key: 'stripMultimodal', label: '剥离多模态', type: 'boolean', default: true },
      { key: 'enableSummarizer', label: '启用 LLM 摘要', type: 'boolean', default: false },
      { key: 'summarizerModelId', label: '摘要模型', type: 'string', default: '', required: 'enableSummarizer=true 时', description: 'get_models 获取' },
      { key: 'summarizerMethod', label: '摘要方式', type: 'string', default: 'fc', description: '保持 "fc"' },
      { key: 'factSections', label: '便签分区', type: 'array', default: ['convention', 'preference', 'env', 'goal', 'todo'], description: '保持默认' },
      { key: 'reservedTokens', label: '预留 token', type: 'number', default: 3000, description: '保持默认' },
      { key: 'tokenEncoding', label: '编码', type: 'string', default: 'cl100k', description: '保持默认' }
    ],
    outputs: [
      { value: 'messages', label: '消息', type: 'array', description: '压缩后的消息数组' },
      { value: 'summary', label: '摘要', type: 'object', description: '{ text, covered, seedCovered }' },
      { value: 'facts', label: '便签', type: 'array' },
      { value: 'stats', label: '统计', type: 'object', description: '压缩统计信息' }
    ],
    template: contextManageNode
  }
}
