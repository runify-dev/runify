import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getModelsTool, getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { factExtractNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * fact-extract-node（AI 便签提取）的 AI 生成配置（命令式，手写；无旧 catalog，据 index.vue/validator 编写）。
 * 用 LLM 从源消息里抽取便签（结构化记忆），可选写回父层变量做跨迭代累积。
 */

const BASE = factExtractNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'facts', label: '便签', type: 'array' },
  { value: 'extracted', label: '本轮抽取数', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

const input: NodeInputSchema = {
            type: 'object',
            properties: {
              sourceReference: { type: 'array', description: '源消息变量 [节点ID,字段]' },
              modelId: { type: 'string', description: '提取模型资源 id（get_models 获取）' },
              factsVariable: {
                type: 'array',
                description: '便签写回父层变量 [外层循环ID,字段]（可选）'
              },
              method: { type: 'string', description: '提取方式，保持 "fc"' }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '用 LLM 从源消息中抽取便签（结构化记忆），可选写回父层变量做跨迭代累积',
      prompt:
        'fact-extract-node（AI 便签提取）：用 update 配置。\n' +
        '- sourceReference 必填：源消息变量 [节点ID,字段]。modelId 必填：提取模型（get_models 获取）。\n' +
        '- factsVariable 可选：便签写回父层变量 [外层循环ID,字段]（循环累积场景才配）。method 保持 "fc"。\n' +
        '- 输出字段：facts 便签、extracted 本轮抽取数。',
      tools: [
        {
          name: 'update',
          description:
            '配置当前 AI 便签提取节点：源消息、提取模型、可选便签写回变量。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of ['sourceReference', 'modelId', 'factsVariable', 'method']) {
              if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            }
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
  type: 'fact-extract-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
