import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { runSkillNode } from '@/workflow/common/data'

/**
 * run-skill-node（技能执行）的 AI 生成配置（命令式，手写）。
 * 无 content/validator.ts，validate 内联轻校验（customize 模式下 skillId + command 必填）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 */

const BASE = runSkillNode.properties.nodeData
/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  { value: 'result', label: '执行结果', type: 'string' },
  { value: 'stdout', label: '标准输出', type: 'string' },
  { value: 'stderr', label: '错误输出', type: 'string' },
  { value: 'exitCode', label: '退出码', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 技能执行节点数据（与后端 RunSkillNode / 表单 content/index.vue 对齐）。
 * location=tool_call：参数取自 reference 指向的工具调用；
 * location=customize：skillId/command 各由其 xxxLocation（customize 字面量 / reference 引用）决定。
 */
interface RunSkillNodeData {
  runtime?: string
  location?: 'tool_call' | 'customize'
  reference?: string[]
  skillIdLocation?: 'customize' | 'reference'
  skillIdReference?: string[]
  skillId?: string
  commandLocation?: 'customize' | 'reference'
  commandReference?: string[]
  command?: string
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'runtime',
  'location',
  'reference',
  'skillIdLocation',
  'skillIdReference',
  'skillId',
  'commandLocation',
  'commandReference',
  'command'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              runtime: {
                type: 'string',
                enum: ['local', 'docker'],
                description: '运行环境：local=本地终端（默认）；docker=hush-toolbox 容器'
              },
              location: {
                type: 'string',
                enum: ['tool_call', 'customize'],
                description:
                  '来源模式：tool_call=作为 agent 工具，参数取自 reference 指向的工具调用；customize=独立配置 skillId/command'
              },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description:
                  'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"skill_id":"...","command":"..."}\' }（functionArguments 为含 skill_id/command 的 JSON 字符串）'
              },
              skillIdLocation: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: '技能 id 来源：customize=用 skillId 字面量；reference=用 skillIdReference 引用'
              },
              skillIdReference: {
                type: 'array',
                items: { type: 'string' },
                description: 'skillIdLocation=reference 时的技能 id 引用 [节点ID,字段]'
              },
              skillId: {
                type: 'string',
                description: 'skillIdLocation=customize 时的技能 id 字面量，如 "apply_patch"'
              },
              commandLocation: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: '执行命令来源：customize=用 command 字面量；reference=用 commandReference 引用'
              },
              commandReference: {
                type: 'array',
                items: { type: 'string' },
                description: 'commandLocation=reference 时的执行命令引用 [节点ID,字段]'
              },
              command: {
                type: 'string',
                description: 'commandLocation=customize 时的执行命令字面量（留空运行默认脚本）'
              }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '执行已安装技能的命令（自动注入技能环境变量）',
      prompt:
        'run-skill-node（技能执行）：用 update 配置，字段与节点数据一致：\n' +
        '- runtime：运行环境，local=本地终端（默认）/ docker=hush-toolbox 容器。\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：参数取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"skill_id":"...","command":"..."}\' }（functionArguments 为含 skill_id/command 的 JSON 字符串）。此模式不填 skillId/command。\n' +
        '  · "customize"（独立配置）：skillId 必填（技能 id）、command 必填（执行命令，留空运行默认脚本）。两者各自可字面量或引用 —— xxxLocation="customize" 填 xxx；xxxLocation="reference" 填 xxxReference（[节点ID,字段]，get_node_field_options 取）。\n' +
        '- 输出字段：result 执行结果、stdout、stderr、exitCode。',
      tools: [
        {
          name: 'update',
          description: '配置当前技能执行节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: RunSkillNodeData = {
              ...cloneDeep(BASE),
              ...(node.properties.nodeData ?? {})
            }
            for (const key of UPDATE_KEYS) {
              if (args[key] !== undefined) (next as any)[key] = cloneDeep(args[key])
            }
            next.runtime = next.runtime ?? 'local'
            next.location = next.location ?? 'customize'
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            // 落库后自动校验（内联轻校验）：通过即 stop 收尾
            const data = node.properties?.nodeData ?? {}
            if (data.location !== 'tool_call') {
              if (data.skillIdLocation !== 'reference' && !String(data.skillId ?? '').trim()) {
                return { field_list: FIELD_LIST, errors: { skillId: '请填写技能 id' } }
              }
              if (data.commandLocation !== 'reference' && !String(data.command ?? '').trim()) {
                return { field_list: FIELD_LIST, errors: { command: '请填写执行命令' } }
              }
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
  type: 'run-skill-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
