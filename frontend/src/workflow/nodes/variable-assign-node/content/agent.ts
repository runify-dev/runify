import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool, validateRef } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { validate as validateNode } from './validator'

/**
 * variable-assign-node（变量赋值）的 AI 生成配置（命令式，手写）。
 * nodeData：variables（给全局变量或循环变量赋值）。无输出字段。
 */

const outputs: OutputDef[] = []
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              variables: {
                type: 'array',
                description:
                  '赋值项数组。元素 { variable:目标变量路径, type:"reference"|"constant", reference:[节点ID,字段], dataType, value }。'
              }
            },
            required: ['variables']
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '给开始节点定义的全局变量或循环变量赋值（引用上游变量或常量）',
      prompt:
        'variable-assign-node（变量赋值）：用 update 配置 variables。\n' +
        '- 元素 { variable: 目标变量路径（全局变量 ["global",名] 或循环变量 ["循环ID",名]）, type:"reference"|"constant", ' +
        'reference:[节点ID,字段]（type=reference 时）, dataType:"string"|"array"|"dict"|"number"|"boolean"（type=constant 时）, ' +
        'value: 常量值（array/dict 用 JSON 字符串） }。\n' +
        '- 本节点无输出字段。',
      tools: [
        {
          name: 'update',
          description: '配置当前变量赋值节点的赋值项 variables。',
          parameters: input,
          apply: (args) => {
            // 读引用硬校验：type=reference 的赋值项，其来源 reference 必须是 get_node_field_options 里的真实字段
            // （目标 variable 是写入端——可能是新建的全局/循环变量，不在此拦）。
            if (Array.isArray(args.variables)) {
              for (const v of args.variables) {
                if (v?.type === 'reference') {
                  const err = validateRef(bindings, v.reference, `赋值项 ${JSON.stringify(v.variable)} 的来源`)
                  if (err) return { error: err }
                }
              }
            }
            node.properties.nodeData = {
              ...(node.properties.nodeData ?? {}),
              variables: Array.isArray(args.variables) ? cloneDeep(args.variables) : []
            }
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
  type: 'variable-assign-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
