import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { contextPushNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * context-push-node（推送上下文）的 AI 生成配置（命令式，手写）。
 * agent 循环中把消息/工具结果追加进循环上下文变量。
 */

const BASE = contextPushNode.properties.nodeData

const outputs: OutputDef[] = []
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              items: {
                type: 'array',
                description:
                  '推送项数组。元素 { variable:[外层循环ID,"context"], mode:"reference"|"custom", reference:[节点ID,字段], content, role }。'
              }
            },
            required: ['items']
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '把消息/工具结果追加进循环上下文变量（agent 循环中回写工具执行结果）',
      prompt:
        'context-push-node（推送上下文）：用 update 配置 items。\n' +
        '- 元素 { variable: 目标上下文变量 [外层循环ID,"context"], mode:"reference"|"custom", ' +
        'reference:[节点ID,字段]（mode=reference 时；工具结果引用工具节点的隐藏字段 tool，如 [工具节点ID,"tool"]）, ' +
        'content: JSON 字符串（mode=custom 时）, role:"system"|"user"|"assistant"|"tool"（工具结果一般用 "user"） }。\n' +
        '- 本节点无输出字段。',
      tools: [
        {
          name: 'update',
          description: '配置当前推送上下文节点的推送项 items。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            if (args.items !== undefined) next.items = cloneDeep(args.items)
            node.properties.nodeData = next
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { ok: true, errors: _vr.errors }
            return { status: 'stop', data: { ok: true } }
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
  type: 'context-push-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
