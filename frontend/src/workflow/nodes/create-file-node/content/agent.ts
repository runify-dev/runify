import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { createFileNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * create-file-node（创建文件）的 AI 生成配置（命令式，手写）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 */

const BASE = createFileNode.properties.nodeData

/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  { value: 'result', label: '执行结果', type: 'string' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 创建文件节点数据（与后端 CreateFileNode / 表单 content/index.vue 对齐）。
 * location=tool_call：参数取自 reference 指向的工具调用；
 * location=customize：path/content 各由其 xxxLocation（customize 字面量 / reference 引用）决定。
 */
interface CreateFileNodeData {
  location?: 'tool_call' | 'customize'
  reference?: string[]
  pathLocation?: 'customize' | 'reference'
  pathReference?: string[]
  path?: string
  contentLocation?: 'customize' | 'reference'
  contentReference?: string[]
  content?: string
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'location',
  'reference',
  'pathLocation',
  'pathReference',
  'path',
  'contentLocation',
  'contentReference',
  'content'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
  type: 'object',
  properties: {
    location: {
      type: 'string',
      enum: ['tool_call', 'customize'],
      description:
        '来源模式：tool_call=作为 agent 工具，参数取自 reference 指向的工具调用；customize=独立配置 path/content'
    },
    reference: {
      type: 'array',
      items: { type: 'string' },
      description:
        'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"path":"...","content":"..."}\' }（functionArguments 为含 path/content 的 JSON 字符串）'
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
      description: 'pathLocation=customize 时的文件相对路径字面量，如 "src/new-file.ts"'
    },
    contentLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description: '文件内容来源：customize=用 content 字面量；reference=用 contentReference 引用'
    },
    contentReference: {
      type: 'array',
      items: { type: 'string' },
      description: 'contentLocation=reference 时的文件内容引用 [节点ID,字段]'
    },
    content: {
      type: 'string',
      description: 'contentLocation=customize 时的文件内容字面量'
    }
  }
}

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '在工作目录创建/写入文件',
      prompt:
        'create-file-node（创建文件）：用 update 配置，字段与节点数据一致：\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：参数取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"path":"...","content":"..."}\' }（functionArguments 为含 path/content 的 JSON 字符串）。此模式不填 path/content。\n' +
        '  · "customize"（独立配置）：path 必填（文件相对路径）、content 必填（文件内容）。两者各自可字面量或引用 —— xxxLocation="customize" 填 xxx；xxxLocation="reference" 填 xxxReference（[节点ID,字段]，get_node_field_options 取）。\n' +
        '- 输出字段：result 执行结果。',
      tools: [
        {
          name: 'update',
          description: '配置当前创建文件节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: CreateFileNodeData = {
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
  type: 'create-file-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
