import { cloneDeep } from 'lodash'
import { randomId } from '@/utils/common'
import { resolveNode, useAgentConfig } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { conditionItemSchema } from '@/workflow/nodes/judge-node/type'
import { validate as validateNode } from './validator'

/** loop-break-node（跳出循环）的 AI 生成配置（命令式，手写）。仅循环子画布内可用。 */

export const scope = 'child'

/** 补齐 conditions id/variable/value + logic 缺省（照旧 catalog 的 normalizeConditions） */
function normalizeConditions(data: Record<string, any>): Record<string, any> {
  if (!Array.isArray(data.conditions)) data.conditions = []
  for (const condition of data.conditions) {
    if (!condition.id) condition.id = randomId()
    if (!Array.isArray(condition.variable)) condition.variable = []
    if (!condition.location) condition.location = 'customize'
    if (!Array.isArray(condition.referenceValue)) condition.referenceValue = []
    if (condition.value === undefined) condition.value = ''
  }
  if (!data.logic) data.logic = 'and'
  return data
}

const CONDITION_DESC =
  '条件数组（空数组=无条件触发）。元素 { variable:[节点ID,字段](左值引用), compare 比较符(同 judge-node), ' +
  'location:"customize"|"reference"(右值来源), referenceValue:[节点ID,字段](reference 时), value(customize 时字面量) }。' +
  '凡引用上游字段先用 get_node_field_options 取真实 [节点ID,字段]，id 可省略'

const outputs: OutputDef[] = []
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              conditions: {
                type: 'array',
                description: '触发条件列表（空数组=无条件触发）',
                items: conditionItemSchema
              },
              logic: { type: 'string', enum: ['and', 'or'], description: '多条件间逻辑' }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '（仅循环子画布内）满足条件时跳出整个循环',
      prompt:
        'loop-break-node（跳出循环）：用 update 配置。\n' +
        `- conditions：${CONDITION_DESC}。logic："and"|"or"。\n` +
        '- 若条件涉及「模型调了哪个工具 / 当前项字段」，先调 get_canvas_detail：读对应 ai-chat 的 declaredTools 取真实工具名，按 youAreHere 认准当前循环层与其 item，勿臆造名字或引用错层。\n' +
        '- 本节点无输出字段。',
      tools: [
        {
          name: 'update',
          description: '配置当前跳出循环节点：触发条件与逻辑。',
          parameters: input,
          apply: (args) => {
            const next = { ...(node.properties.nodeData ?? {}) }
            if (args.conditions !== undefined) next.conditions = cloneDeep(args.conditions)
            if (args.logic !== undefined) next.logic = args.logic
            node.properties.nodeData = normalizeConditions(next)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { ok: true, errors: _vr.errors }
            return { status: 'stop', data: { ok: true } }
          }
        },
        getCanvasDetailTool(bindings),
        getNodeFieldOptionsTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'loop-break-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
