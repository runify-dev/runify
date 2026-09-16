import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { fileUploadNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * file-upload-node（文件上传）的 AI 生成配置（命令式，手写）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 */

const BASE = fileUploadNode.properties.nodeData
/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  { value: 'url', label: '下载地址', type: 'string', description: './api/storage/file/{id}，必须原样使用' },
  { value: 'fileId', label: '文件ID', type: 'string' },
  { value: 'fileName', label: '文件名', type: 'string' },
  { value: 'fileSize', label: '文件大小', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 文件上传节点数据（与后端 FileUploadNode / 表单 content/index.vue 对齐）。
 * location=tool_call：path 取自 reference 指向的工具调用；
 * location=customize：path 由 pathLocation（customize 字面量 / reference 引用）决定，fileName 为可选字面量。
 */
interface FileUploadNodeData {
  location?: 'tool_call' | 'customize'
  reference?: string[]
  pathLocation?: 'customize' | 'reference'
  pathReference?: string[]
  path?: string
  fileName?: string
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'location',
  'reference',
  'pathLocation',
  'pathReference',
  'path',
  'fileName'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              location: {
                type: 'string',
                enum: ['tool_call', 'customize'],
                description:
                  '来源模式：tool_call=作为 agent 工具，path 取自 reference 指向的工具调用；customize=独立配置 path'
              },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description:
                  'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"path":"..."}\' }（functionArguments 为含 path 的 JSON 字符串）'
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
                description: 'pathLocation=customize 时要上传的文件相对路径字面量'
              },
              fileName: {
                type: 'string',
                description: '上传后的文件名（可选，留空自动取原文件名）'
              }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '把工作目录文件上传到文件库，返回下载 URL（产出文件交付给用户的必经通道）',
      prompt:
        'file-upload-node（文件上传）：用 update 配置，字段与节点数据一致：\n' +
        '- location：来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：path 取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"path":"..."}\' }（functionArguments 为含 path 的 JSON 字符串）。此模式不填 path。\n' +
        '  · "customize"（独立配置）：path 必填（要上传的文件相对路径），可字面量或引用 —— pathLocation="customize" 填 path；="reference" 填 pathReference（[节点ID,字段]，get_node_field_options 取）。fileName 可选（留空自动取原文件名）。\n' +
        '- 输出字段：url 下载地址（./api/storage/file/{id}，须原样使用）、fileId、fileName、fileSize。\n' +
        '- 工作流产出的文件必须经本节点上传后用返回 url 交付用户下载。',
      tools: [
        {
          name: 'update',
          description: '配置当前文件上传节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: FileUploadNodeData = {
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
  type: 'file-upload-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
