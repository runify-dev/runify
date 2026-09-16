import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { readFileNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * read-file-node（读取文件）的 AI 生成配置（命令式，手写）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 */

const BASE = readFileNode.properties.nodeData
/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  { value: 'question', label: '提问内容', type: 'string', description: '仅二进制文件存在，供下轮交互 push 上下文' },
  { value: 'content', label: '带行号内容', type: 'string' },
  { value: 'rawContent', label: '原始内容', type: 'string' },
  { value: 'totalLines', label: '总行数', type: 'number' },
  { value: 'lines', label: '读取行数', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 读取文件节点数据（与后端 ReadFileNode / 表单 content/index.vue 对齐）。
 * location=tool_call：读取参数取自 reference 指向的工具调用；
 * location=customize：path/offset/limit 各由其 xxxLocation（customize 字面量 / reference 引用）决定。
 */
interface ReadFileNodeData {
  location?: 'tool_call' | 'customize'
  reference?: string[]
  pathLocation?: 'customize' | 'reference'
  pathReference?: string[]
  path?: string
  offsetLocation?: 'customize' | 'reference'
  offsetReference?: string[]
  offset?: number | null
  limitLocation?: 'customize' | 'reference'
  limitReference?: string[]
  limit?: number | null
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'location',
  'reference',
  'pathLocation',
  'pathReference',
  'path',
  'offsetLocation',
  'offsetReference',
  'offset',
  'limitLocation',
  'limitReference',
  'limit'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              location: {
                type: 'string',
                enum: ['tool_call', 'customize'],
                description:
                  '来源模式：tool_call=作为 agent 工具，读取参数取自 reference 指向的工具调用；customize=独立配置 path/offset/limit'
              },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description:
                  'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"path":"...","offset":1,"limit":-1}\' }（functionArguments 为含 path/offset/limit 的 JSON 字符串）'
              },
              pathLocation: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: '文件路径来源：customize=用 path 字面量；reference=用 pathReference 引用'
              },
              pathReference: {
                type: 'array',
                items: { type: 'string' },
                description: 'pathLocation=reference 时的文件路径引用 [节点ID,字段]'
              },
              path: {
                type: 'string',
                description: 'pathLocation=customize 时的文件相对路径字面量，如 "src/api.ts"'
              },
              offsetLocation: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: '起始行来源：customize=用 offset 数值；reference=用 offsetReference 引用'
              },
              offsetReference: {
                type: 'array',
                items: { type: 'string' },
                description: 'offsetLocation=reference 时的起始行引用 [节点ID,字段]'
              },
              offset: {
                type: 'number',
                description: 'offsetLocation=customize 时的起始行（可选，从 0 起）'
              },
              limitLocation: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: '读取行数来源：customize=用 limit 数值；reference=用 limitReference 引用'
              },
              limitReference: {
                type: 'array',
                items: { type: 'string' },
                description: 'limitLocation=reference 时的读取行数引用 [节点ID,字段]'
              },
              limit: {
                type: 'number',
                description: 'limitLocation=customize 时的读取行数（可选，默认全部）'
              }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '读取工作目录中的文件内容，返回带行号内容/原始内容/行数',
      prompt:
        'read-file-node（读取文件）：用 update 配置，字段与节点数据一致：\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：读取参数取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"path":"src/api.ts","offset":1,"limit":-1}\' }（functionArguments 为含 path/offset/limit 的 JSON 字符串）。此模式不填 path/offset/limit。\n' +
        '  · "customize"（独立配置）：path 必填（文件相对路径，如 src/api.ts）；offset、limit 可选。三者各自可字面量或引用 —— xxxLocation="customize" 填 xxx（path 字符串、offset/limit 数值）；xxxLocation="reference" 填 xxxReference（[节点ID,字段]，get_node_field_options 取）。\n' +
        '- 输出字段：content 带行号内容、rawContent 原始内容、totalLines 总行数、lines 读取行数。',
      tools: [
        {
          name: 'update',
          description: '配置当前读取文件节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: ReadFileNodeData = {
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
  type: 'read-file-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
