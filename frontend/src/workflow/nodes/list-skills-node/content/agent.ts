import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { listSkillsNode } from '@/workflow/common/data'

/**
 * list-skills-node（技能列表）的 AI 生成配置（命令式，手写）。
 * customize 模式无可配置字段（仅列出可用技能）；tool_call 模式作为 agent 工具被调用。
 * nodeData 字段与表单 content/index.vue 一致：location / reference。
 */

const BASE = listSkillsNode.properties.nodeData
/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  {
    value: 'skills',
    label: '技能列表',
    type: 'array',
    items: [
      { value: 'id', type: 'string' },
      { value: 'name', type: 'string' },
      { value: 'description', type: 'string' },
      { value: 'installed', type: 'boolean', label: '是否已安装' },
      { value: 'hasUpdate', type: 'boolean', label: '是否有更新' },
      { value: 'local', type: 'string', label: '本地路径' },
      { value: 'parameters', type: 'array' }
    ]
  },
  { value: 'summary', label: '摘要', type: 'string' },
  { value: 'skills_count', label: '技能数', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

/** 技能列表节点数据（与后端 ListSkillsNode / 表单 content/index.vue 对齐） */
interface ListSkillsNodeData {
  location?: 'tool_call' | 'customize'
  reference?: string[]
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = ['location', 'reference'] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              location: {
                type: 'string',
                enum: ['tool_call', 'customize'],
                description: '来源模式：tool_call=作为 agent 工具被调用（配 reference）；customize=独立执行（无需其它字段）'
              },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description:
                  'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。本工具无入参，其值为 { id, functionArguments:"{}" }'
              }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '列出可用技能',
      prompt:
        'list-skills-node（技能列表）：用 update 配置，字段与节点数据一致：\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：作为 ai-chat 的工具被调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值为工具调用 { id, functionArguments }（本工具无入参，functionArguments 为空对象 "{}"）。\n' +
        '  · "customize"（独立执行）：无需配置任何字段，直接列出可用技能。\n' +
        '- 输出字段：skills 技能列表、summary 摘要、skills_count 技能数。',
      tools: [
        {
          name: 'update',
          description: '配置当前技能列表节点。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: ListSkillsNodeData = {
              ...cloneDeep(BASE),
              ...(node.properties.nodeData ?? {})
            }
            for (const key of UPDATE_KEYS) {
              if (args[key] !== undefined) (next as any)[key] = cloneDeep(args[key])
            }
            next.location = next.location ?? 'customize'
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            // 落库后自动校验（内联轻校验：customize 无必填项；tool_call 需 reference）：通过即 stop 收尾
            const data = node.properties?.nodeData ?? {}
            if (
              data.location === 'tool_call' &&
              (!Array.isArray(data.reference) || data.reference.length === 0)
            ) {
              return { field_list: FIELD_LIST, errors: { reference: '请选择引用变量' } }
            }
            return { status: 'stop', data: { field_list: FIELD_LIST } }
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
  type: 'list-skills-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
