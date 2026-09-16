import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { downloadSkillsNode } from '@/workflow/common/data'

/**
 * download-skills-node（技能下载）的 AI 生成配置（命令式，手写）。
 * 无 content/validator.ts，validate 内联轻校验（customize 模式下 skillId 必填）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 */

const BASE = downloadSkillsNode.properties.nodeData
/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  { value: 'skillId', label: '技能ID', type: 'string' },
  { value: 'skillName', label: '技能名称', type: 'string' },
  { value: 'files', label: '文件数', type: 'number' },
  { value: 'status', label: '安装状态', type: 'string' },
  { value: 'local', label: '本地路径', type: 'string' },
  { value: 'content', label: '技能内容', type: 'string' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 技能下载节点数据（与后端 DownloadSkillsNode / 表单 content/index.vue 对齐）。
 * location=tool_call：skillId 取自 reference 指向的工具调用；
 * location=customize：skillId 由 skillIdLocation（customize 字面量 / reference 引用）决定。
 */
interface DownloadSkillsNodeData {
  location?: 'tool_call' | 'customize'
  reference?: string[]
  skillIdLocation?: 'customize' | 'reference'
  skillIdReference?: string[]
  skillId?: string
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'location',
  'reference',
  'skillIdLocation',
  'skillIdReference',
  'skillId'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              location: {
                type: 'string',
                enum: ['tool_call', 'customize'],
                description:
                  '来源模式：tool_call=作为 agent 工具，skillId 取自 reference 指向的工具调用；customize=独立配置 skillId'
              },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description:
                  'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"skill_id":"..."}\' }（functionArguments 为含 skill_id 的 JSON 字符串）'
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
                description: 'skillIdLocation=customize 时要下载的技能 id 字面量'
              }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '按技能 id 下载安装技能',
      prompt:
        'download-skills-node（技能下载）：用 update 配置，字段与节点数据一致：\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：skillId 取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"skill_id":"..."}\' }（functionArguments 为含 skill_id 的 JSON 字符串）。此模式不填 skillId。\n' +
        '  · "customize"（独立配置）：skillId 必填（要下载的技能 id），可字面量或引用 —— skillIdLocation="customize" 填 skillId；="reference" 填 skillIdReference（[节点ID,字段]，get_node_field_options 取）。\n' +
        '- 输出字段：skillId、skillName、files、status、local、content。',
      tools: [
        {
          name: 'update',
          description: '配置当前技能下载节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: DownloadSkillsNodeData = {
              ...cloneDeep(BASE),
              ...(node.properties.nodeData ?? {})
            }
            for (const key of UPDATE_KEYS) {
              if (args[key] !== undefined) (next as any)[key] = cloneDeep(args[key])
            }
            next.location = next.location ?? 'customize'
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            // 落库后自动校验（内联轻校验：非 tool_call/reference 模式下 skillId 必填）：通过即 stop 收尾
            const data = node.properties?.nodeData ?? {}
            if (
              data.location !== 'tool_call' &&
              data.skillIdLocation !== 'reference' &&
              !String(data.skillId ?? '').trim()
            ) {
              return { field_list: FIELD_LIST, errors: { skillId: '请填写技能 id' } }
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
  type: 'download-skills-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
