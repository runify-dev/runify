import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { terminalNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * terminal-node（终端执行）的 AI 生成配置（命令式，手写）。
 * nodeData：runtime(保持 local) + 三元组 code / timeout（timeout 模板无、后端接受）。
 */

const BASE = terminalNode.properties.nodeData

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
 * 终端执行节点数据（与后端 com.run.workflow.nodes.terminal.entity.TerminalNodeData 对齐）。
 * 顶层 location 决定命令来源：
 * - tool_call：作为 agent 工具，命令参数取自 reference 指向的工具调用 item（[你所在这层循环的真实ID,"item"]）；
 * - customize：独立执行，code 由 codeLocation（customize 字面量 / reference 引用）决定。
 */
interface TerminalNodeData {
  runtime?: string
  location?: 'tool_call' | 'customize'
  /** location=tool_call 时的引用路径 [节点ID, 字段]，指向一个「工具调用」结构（含 functionArguments） */
  reference?: string[]
  codeLocation?: 'customize' | 'reference'
  codeReference?: string[]
  code?: string
  timeoutLocation?: 'customize' | 'reference'
  timeoutReference?: string[]
  timeout?: number
}

/** update 会写入的 nodeData 字段（与表单 content/index.vue 一致，逐个显式透传） */
const UPDATE_KEYS = [
  'runtime',
  'location',
  'reference',
  'codeLocation',
  'codeReference',
  'code',
  'timeoutLocation',
  'timeoutReference',
  'timeout'
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
        '命令来源模式：tool_call=作为 agent 工具，命令取自 reference 指向的工具调用；customize=独立执行，命令由 codeLocation 决定'
    },
    reference: {
      type: 'array',
      items: { type: 'string' },
      description:
        'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"command":"..."}\' }（functionArguments 为含 command 的 JSON 字符串）或直接一个命令字符串'
    },
    codeLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description:
        'location=customize 时命令来源：customize=用 code 字面量；reference=用 codeReference 引用上游变量'
    },
    codeReference: {
      type: 'array',
      items: { type: 'string' },
      description:
        'codeLocation=reference 时的命令引用路径 [节点ID,字段]（get_node_field_options 取）'
    },
    code: {
      type: 'string',
      description: 'codeLocation=customize 时的 shell 命令字面量'
    },
    timeoutLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description: '超时来源：customize=用 timeout 数值；reference=用 timeoutReference 引用'
    },
    timeoutReference: {
      type: 'array',
      items: { type: 'string' },
      description: 'timeoutLocation=reference 时的超时引用路径 [节点ID,字段]'
    },
    timeout: {
      type: 'number',
      description: 'timeoutLocation=customize 时的超时秒数（1-3600，缺省 30）'
    }
  }
}

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '在会话工作目录执行 shell 命令，输出 stdout/stderr/退出码',
      prompt:
        'terminal-node（终端执行）：用 update 配置，字段与节点数据一致：\n' +
        '- runtime：运行环境，local=本地终端（默认）/ docker=hush-toolbox 容器。\n' +
        '- location：命令来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：命令取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"command":"..."}\' } 或直接命令字符串。此模式不填 code/codeLocation。\n' +
        '  · "customize"（独立执行）：命令由 codeLocation 决定 —— codeLocation="customize" 填 code（shell 命令字面量）；codeLocation="reference" 填 codeReference（[节点ID,字段] 引用上游，get_node_field_options 取）。\n' +
        '- timeout：超时（可选，1-3600 秒，缺省 30）。timeoutLocation="customize" 填 timeout 数值；="reference" 填 timeoutReference（[节点ID,字段]）。\n' +
        '- 文件路径均为会话工作目录内相对路径。输出字段：result / stdout / stderr / exitCode。',
      tools: [
        {
          name: 'update',
          description: '配置当前终端执行节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: TerminalNodeData = {
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
  type: 'terminal-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
