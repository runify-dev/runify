import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { fileDownloadNode } from '@/workflow/common/data'

/**
 * file-download-node（文件下载）的 AI 生成配置（命令式，手写）。
 * 本节点无 content/validator.ts，validate 用内联轻校验（customize 模式下 fileId 必填）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 */

const BASE = fileDownloadNode.properties.nodeData
/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  { value: 'filePath', label: '本地路径', type: 'string' },
  { value: 'fileName', label: '文件名', type: 'string' },
  { value: 'fileSize', label: '文件大小', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 文件下载节点数据（与后端 FileDownloadNode / 表单 content/index.vue 对齐）。
 * location=tool_call：参数取自 reference 指向的工具调用；
 * location=customize：fileId/path 各由其 xxxLocation（customize 字面量 / reference 引用）决定。
 */
interface FileDownloadNodeData {
  location?: 'tool_call' | 'customize'
  reference?: string[]
  fileIdLocation?: 'customize' | 'reference'
  fileIdReference?: string[]
  fileId?: string
  pathLocation?: 'customize' | 'reference'
  pathReference?: string[]
  path?: string
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'location',
  'reference',
  'fileIdLocation',
  'fileIdReference',
  'fileId',
  'pathLocation',
  'pathReference',
  'path'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              location: {
                type: 'string',
                enum: ['tool_call', 'customize'],
                description:
                  '来源模式：tool_call=作为 agent 工具，参数取自 reference 指向的工具调用；customize=独立配置 fileId/path'
              },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description:
                  'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"file_id":"...","path":"..."}\' }（functionArguments 为含 file_id/path 的 JSON 字符串）'
              },
              fileIdLocation: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: '文件 id 来源：customize=用 fileId 字面量；reference=用 fileIdReference 引用'
              },
              fileIdReference: {
                type: 'array',
                items: { type: 'string' },
                description: 'fileIdLocation=reference 时的文件 id 引用 [节点ID,字段]'
              },
              fileId: {
                type: 'string',
                description: 'fileIdLocation=customize 时要下载的文件 id 字面量'
              },
              pathLocation: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: '保存路径来源：customize=用 path 字面量；reference=用 pathReference 引用'
              },
              pathReference: {
                type: 'array',
                items: { type: 'string' },
                description: 'pathLocation=reference 时的保存路径引用 [节点ID,字段]'
              },
              path: {
                type: 'string',
                description: 'pathLocation=customize 时的保存相对路径字面量（可选，默认原文件名）'
              }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '按文件 id 下载文件到工作目录',
      prompt:
        'file-download-node（文件下载）：用 update 配置，字段与节点数据一致：\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：参数取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"file_id":"...","path":"..."}\' }（functionArguments 为含 file_id/path 的 JSON 字符串）。此模式不填 fileId/path。\n' +
        '  · "customize"（独立配置）：fileId 必填（要下载的文件 id）；path 可选（保存相对路径，默认原文件名）。两者各自可字面量或引用 —— xxxLocation="customize" 填 xxx；xxxLocation="reference" 填 xxxReference（[节点ID,字段]，get_node_field_options 取）。\n' +
        '- 输出字段：filePath 本地路径、fileName 文件名、fileSize 文件大小。',
      tools: [
        {
          name: 'update',
          description: '配置当前文件下载节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: FileDownloadNodeData = {
              ...cloneDeep(BASE),
              ...(node.properties.nodeData ?? {})
            }
            for (const key of UPDATE_KEYS) {
              if (args[key] !== undefined) (next as any)[key] = cloneDeep(args[key])
            }
            next.location = next.location ?? 'customize'
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            // 落库后自动校验（内联轻校验：非 tool_call/reference 模式下 fileId 必填）：通过即 stop 收尾
            const data = node.properties?.nodeData ?? {}
            if (
              data.location !== 'tool_call' &&
              data.fileIdLocation !== 'reference' &&
              !String(data.fileId ?? '').trim()
            ) {
              return { field_list: FIELD_LIST, errors: { fileId: '请填写文件 id' } }
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
  type: 'file-download-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
