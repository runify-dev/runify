import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getModelsTool, getNodeFieldOptionsTool, getTemplateVariablesTool, validateRef } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { contextManageNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * context-manage-node（上下文压缩）的 AI 生成配置（命令式，手写）。
 * agent 循环中放在 ai-chat 之前，按 token 预算压缩上下文（可选 LLM 摘要）。
 */

const BASE = contextManageNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'messages', label: '消息', type: 'array', description: '压缩后的消息数组' },
  {
    value: 'summary',
    label: '摘要',
    type: 'object',
    fields: [
      { value: 'text', type: 'string', label: '摘要文本' },
      { value: 'covered', type: 'number', label: '覆盖条数' },
      { value: 'seedCovered', type: 'number', label: '种子覆盖条数' }
    ]
  },
  { value: 'facts', label: '便签', type: 'array' },
  { value: 'stats', label: '统计', type: 'object', description: '压缩统计信息' }
]
const FIELD_LIST = deriveFieldList(outputs)

const KEYS = [
  'sourceSeedVariable',
  'sourceVariable',
  'summarySeedVariable',
  'summaryVariable',
  'factsSeedVariable',
  'factsVariable',
  'budget',
  'highRatio',
  'lowRatio',
  'keepRecentItems',
  'stripMultimodal',
  'enableSummarizer',
  'summarizerModelId',
  'summarizerMethod',
  'factSections',
  'reservedTokens',
  'tokenEncoding'
]

const input: NodeInputSchema = {
            type: 'object',
            properties: {
              sourceSeedVariable: { type: 'array', description: '待压缩上下文入参 [节点ID,字段]' },
              sourceVariable: {
                type: 'array',
                description: '压缩结果写回出参 [节点ID,字段]（不能与入参相同）'
              },
              summarySeedVariable: { type: 'array', description: '摘要入参 [节点ID,字段]' },
              summaryVariable: { type: 'array', description: '摘要出参 [节点ID,字段]' },
              factsSeedVariable: { type: 'array', description: '便签入参 [节点ID,字段]' },
              factsVariable: { type: 'array', description: '便签出参 [节点ID,字段]' },
              budget: { type: 'number', description: 'token 预算（默认 32000，≥1000）' },
              highRatio: { type: 'number', description: '高水位（默认 0.85）' },
              lowRatio: { type: 'number', description: '低水位（默认 0.6）' },
              keepRecentItems: { type: 'number', description: '保留最近条数（默认 10）' },
              stripMultimodal: { type: 'boolean', description: '剥离多模态（默认 true）' },
              enableSummarizer: { type: 'boolean', description: '启用 LLM 摘要（默认 false）' },
              summarizerModelId: {
                type: 'string',
                description: 'enableSummarizer=true 时必填，get_models 获取'
              },
              summarizerMethod: { type: 'string', description: '摘要方式，保持 "fc"' },
              factSections: { type: 'array', description: '便签分区（保持默认）' },
              reservedTokens: { type: 'number', description: '预留 token（保持默认 3000）' },
              tokenEncoding: { type: 'string', description: '编码（保持默认 cl100k）' }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '按 token 预算压缩上下文（可选 LLM 摘要），agent 循环中放在 ai-chat 之前',
      prompt:
        'context-manage-node（上下文压缩）：用 update 配置。\n' +
        '- sourceSeedVariable 必填：待压缩上下文入参（首轮种子，如 context-query 的 history 或 [外层循环ID,"context"]）。\n' +
        '- sourceVariable：压缩结果写回出参（不能与入参相同，如 [外层循环ID,"compress_context"]，供 ai-chat 的 contextVariable 用）。\n' +
        '- summary/facts 的 Seed/出参变量成对可选；budget(默认32000,≥1000)/highRatio/lowRatio/keepRecentItems 控制压缩。\n' +
        '- enableSummarizer=true 时 summarizerModelId 必填（get_models）。其余 factSections/reservedTokens/tokenEncoding 保持默认。\n' +
        '- 输出字段：messages 压缩后消息、summary 摘要、facts 便签、stats 统计。',
      tools: [
        {
          name: 'update',
          description:
            '配置当前上下文压缩节点：入/出参变量、token 预算与水位、可选 LLM 摘要。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            // 读引用硬校验：种子入参必须是 get_node_field_options 里的真实字段（写回出参可能是循环作用域新变量，不在此拦）
            for (const [key, label] of [
              ['sourceSeedVariable', '待压缩上下文入参'],
              ['summarySeedVariable', '摘要入参'],
              ['factsSeedVariable', '便签入参']
            ] as const) {
              if (args[key] !== undefined) {
                const err = validateRef(bindings, args[key], label)
                if (err) return { error: err }
              }
            }
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of KEYS) if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: FIELD_LIST, errors: _vr.errors }
            return { status: 'stop', data: { field_list: FIELD_LIST } }
          }
        },
        getModelsTool,
        getNodeFieldOptionsTool(bindings),
        getTemplateVariablesTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'context-manage-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
