import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { validate as validateNode } from './validator'

/**
 * context-query-node（上下文查询）的 AI 生成配置（命令式，手写）。
 * 无需配置（会话标识运行时自动获取，nodeData 传 {}）；通常放 start 之后作循环种子。
 */

/** 出参签名（带类型）：出参单一来源，field_list 由它派生。 */
const outputs: OutputDef[] = [
  { value: 'history', label: '历史上下文', type: 'array', description: '历史消息数组（不含当前轮）' },
  { value: 'full', label: '完整历史', type: 'array', description: '含当前轮的消息数组' },
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
  { value: 'historyUpto', label: '载入边界', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

/** 入参签名（无可配置字段） */
const input: NodeInputSchema = { type: 'object', properties: {} }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description:
        '读取会话持久化上下文（历史消息/摘要/便签），通常放 start 之后作为循环的种子数据',
      prompt:
        'context-query-node（上下文查询）：无需配置字段（会话标识运行时自动获取），直接 update 即可。\n' +
        '- 输出字段：history 历史消息（不含当前轮）、full 完整历史（含当前轮）、summary 摘要、facts 便签、historyUpto 载入边界。',
      tools: [
        {
          name: 'update',
          description: '归位当前上下文查询节点（无可配置字段）。',
          parameters: input,
          apply: (_args) => {
            node.properties.nodeData = { ...(node.properties.nodeData ?? {}) }
            node.properties.field_list = cloneDeep(FIELD_LIST)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: FIELD_LIST, errors: _vr.errors }
            return { status: 'stop', data: { field_list: FIELD_LIST } }
          }
        },
        getNodeFieldOptionsTool(bindings),
        getTemplateVariablesTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'context-query-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
