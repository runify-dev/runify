import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool, validateRef } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { contextSaveNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * context-save-node（上下文写入）的 AI 生成配置（命令式，手写）。
 * 把摘要/便签写回会话持久化存储，通常放外层循环结束之后。summaryReference/factsReference 至少配一个。
 */

const BASE = contextSaveNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'savedSummary', label: '摘要已写入', type: 'boolean' },
  { value: 'savedFacts', label: '便签写入数', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

const input: NodeInputSchema = {
            type: 'object',
            properties: {
              summaryReference: { type: 'array', description: '摘要来源 [节点ID,字段]' },
              factsReference: { type: 'array', description: '便签来源 [节点ID,字段]' }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '把摘要/便签写回会话持久化存储，通常放在外层循环结束之后',
      prompt:
        'context-save-node（上下文写入）：用 update 配置。\n' +
        '- summaryReference 摘要来源、factsReference 便签来源，均为 [节点ID,字段]（如 [外层循环ID,"summary"]），二者至少配一个。\n' +
        '- 输出字段：savedSummary 摘要已写入、savedFacts 便签写入数。',
      tools: [
        {
          name: 'update',
          description: '配置当前上下文写入节点：摘要来源、便签来源。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            // 读引用硬校验：来源必须是 get_node_field_options 里的真实字段——通常是循环累积的 [循环ID,"summary"/"facts"]，
            // 不是 context-query 的种子；若引用不存在多半是循环变量还没声明。
            for (const [key, label] of [
              ['summaryReference', '摘要来源'],
              ['factsReference', '便签来源']
            ] as const) {
              if (args[key] !== undefined) {
                const err = validateRef(bindings, args[key], label)
                if (err) return { error: err }
              }
            }
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of ['summaryReference', 'factsReference']) {
              if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            }
            node.properties.nodeData = next
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
  type: 'context-save-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
