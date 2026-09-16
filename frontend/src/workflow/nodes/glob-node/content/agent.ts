import {cloneDeep} from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import {globNode} from '@/workflow/common/data'
import {validate as validateNode} from './validator'

/**
 * glob-node（文件搜索）的 AI 生成配置（命令式，手写）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 */

const BASE = globNode.properties.nodeData

/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  {value: 'content', label: '文件列表', type: 'string'},
  {value: 'summary', label: '摘要', type: 'string'},
  {value: 'files', label: '文件数', type: 'number'}
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 文件搜索节点数据（与后端 GlobNode / 表单 content/index.vue 对齐）。
 * location=tool_call：搜索参数取自 reference 指向的工具调用；
 * location=customize：pattern/path/maxResults 各由其 xxxLocation（customize 字面量 / reference 引用）决定。
 */
interface GlobNodeData {
  location?: 'tool_call' | 'customize'
  reference?: string[]
  patternLocation?: 'customize' | 'reference'
  patternReference?: string[]
  pattern?: string
  pathLocation?: 'customize' | 'reference'
  pathReference?: string[]
  path?: string
  maxResultsLocation?: 'customize' | 'reference'
  maxResultsReference?: string[]
  maxResults?: number | null
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'location',
  'reference',
  'patternLocation',
  'patternReference',
  'pattern',
  'pathLocation',
  'pathReference',
  'path',
  'maxResultsLocation',
  'maxResultsReference',
  'maxResults'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
  type: 'object',
  properties: {
    location: {
      type: 'string',
      enum: ['tool_call', 'customize'],
      description:
        '来源模式：tool_call=作为 agent 工具，搜索参数取自 reference 指向的工具调用；customize=独立配置 pattern/path/maxResults'
    },
    reference: {
      type: 'array',
      items: {type: 'string'},
      description:
        'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"pattern":"...","path":"."}\' }（functionArguments 为含 pattern/path 的 JSON 字符串）'
    },
    patternLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description: 'glob 模式来源：customize=用 pattern 字面量；reference=用 patternReference 引用'
    },
    patternReference: {
      type: 'array',
      items: {type: 'string'},
      description: 'patternLocation=reference 时的 glob 模式引用路径 [节点ID,字段]'
    },
    pattern: {
      type: 'string',
      description: 'patternLocation=customize 时的 glob 模式字面量，如 "**/*.md"'
    },
    pathLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description: '搜索起始目录来源：customize=用 path 字面量；reference=用 pathReference 引用'
    },
    pathReference: {
      type: 'array',
      items: {type: 'string'},
      description: 'pathLocation=reference 时的搜索目录引用路径 [节点ID,字段]'
    },
    path: {
      type: 'string',
      description: 'pathLocation=customize 时的搜索起始目录字面量（可选，如 "src/"）'
    },
    maxResultsLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description: '结果上限来源：customize=用 maxResults 数值；reference=用 maxResultsReference 引用'
    },
    maxResultsReference: {
      type: 'array',
      items: {type: 'string'},
      description: 'maxResultsLocation=reference 时的结果上限引用路径 [节点ID,字段]'
    },
    maxResults: {
      type: 'number',
      description: 'maxResultsLocation=customize 时的结果上限（可选，默认 1000）'
    }
  }
}

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '按 glob 模式搜索工作目录中的文件名，返回匹配文件列表',
      prompt:
        'glob-node（文件搜索）：用 update 配置，字段与节点数据一致：\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：搜索参数取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"pattern":"**/*.ts","path":"."}\' }（functionArguments 为含 pattern/path 的 JSON 字符串）。此模式不填 pattern/path/maxResults。\n' +
        '  · "customize"（独立配置）：pattern 必填（glob 模式，如 "**/*.md"）；path、maxResults 可选。三者各自可字面量或引用 —— xxxLocation="customize" 填 xxx（pattern/path 字符串、maxResults 数值）；xxxLocation="reference" 填 xxxReference（[节点ID,字段]，get_node_field_options 取）。\n' +
        '- 输出字段：content 文件列表、summary 摘要、files 文件数。',
      tools: [
        {
          name: 'update',
          description: '配置当前文件搜索节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: GlobNodeData = {
              ...cloneDeep(BASE),
              ...(node.properties.nodeData ?? {})
            }
            for (const key of UPDATE_KEYS) {
              if (args[key] !== undefined) (next as any)[key] = cloneDeep(args[key])
            }
            next.location = next.location ?? 'customize'
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: FIELD_LIST, errors: _vr.errors }
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
  type: 'glob-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
