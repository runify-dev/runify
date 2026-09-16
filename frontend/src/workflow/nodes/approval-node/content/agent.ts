import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { approvalNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * approval-node（审批）的 AI 生成配置（命令式，手写；无旧 catalog）。
 * nodeData：location('customize'|'reference') + reference + prompt（审批提示文本）。
 */

const BASE = approvalNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'approved', label: '审批结果', type: 'boolean' }
]
const FIELD_LIST = deriveFieldList(outputs)

const input: NodeInputSchema = {
            type: 'object',
            properties: {
              prompt: { type: 'string', description: '审批提示文本（字面值）' },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description: '引用上游变量作为提示内容 [节点ID,字段]'
              }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '发起人工审批，输出审批结果（通过/拒绝）',
      prompt:
        'approval-node（审批）：用 update 配置。\n' +
        '- prompt：审批提示文本（字面值）；或用 reference [节点ID,字段] 引用上游变量作为提示内容。\n' +
        '- 输出字段：approved 审批结果（boolean）。',
      tools: [
        {
          name: 'update',
          description: '配置当前审批节点：审批提示文本或引用变量。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            if (args.reference !== undefined) {
              next.location = 'reference'
              next.reference = cloneDeep(args.reference)
            }
            if (args.prompt !== undefined) {
              next.location = 'customize'
              next.prompt = args.prompt
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
  type: 'approval-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
