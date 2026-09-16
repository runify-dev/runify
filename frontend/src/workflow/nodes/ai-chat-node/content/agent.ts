import {cloneDeep} from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getModelsTool, getNodeFieldOptionsTool, getTemplateVariablesTool, validateRef } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import agentTemplate from '@/views/application/template/agent/template.json'
import {validate} from './validator'

/**
 * ai-chat-node 的 AI 生成配置（命令式）。
 * nodeData 契约以 content/index.vue 表单为准：
 *   modelId / system / user / enableContext / contextVariable / contextNumber / images / videos / tools
 * 输出 field_list 固定 6 项（与表单 setField 一致）。模板无默认 nodeData，故 update 负责整体写入。
 */

/** 递归收集图（含循环子画布）中的所有节点 */
function collectNodes(nodes: any[], result: any[] = []): any[] {
  for (const node of nodes ?? []) {
    result.push(node)
    const children = node?.properties?.nodeData?.children
    if (children?.nodes?.length) collectNodes(children.nodes, result)
  }
  return result
}

/**
 * 标准工具函数完整定义（name → {type,function:{name,description,parameters}}），提取自内置 agent 模板。
 * 后端各工具节点在 tool_call 模式下按固定函数名/参数名解析 functionArguments，故定义必须与模板完全一致，
 * 禁止改名/改参数结构。模型只写 {function:{name}}，update 时按名换成这里的完整定义。
 */
const AGENT_TOOL_DEFINITIONS: Record<string, any> = (() => {
  const result: Record<string, any> = {}
  const aiChat = collectNodes((agentTemplate as any).nodes).find(
    (node) => node.type === 'ai-chat-node' && node.properties?.nodeData?.tools?.tools?.length
  )
  for (const tool of aiChat?.properties?.nodeData?.tools?.tools ?? []) {
    const name = tool?.function?.name
    if (name) result[name] = tool
  }
  return result
})()
const AGENT_TOOL_NAMES = Object.keys(AGENT_TOOL_DEFINITIONS)

const DEFAULT_NODE_DATA = {
  modelId: '',
  system: '',
  user: '',
  enableContext: false,
  contextVariable: [] as any[],
  contextNumber: 0,
  images: [] as any[],
  videos: [] as any[],
  tools: {location: 'customize', reference: [] as any[], tools: [] as any[]}
}

/**
 * 出参签名（带类型）：本节点写进 context、可被 [节点ID,字段] 引用的字段。出参单一来源，field_list 由它派生。
 * field_list 只有 label/value，表达不了 toolCalls 是数组、isRefusal 是布尔——type + 结构说明补上，供组合层接线。
 */
const outputs: OutputDef[] = [
  {value: 'content', label: '回答结果', type: 'string', description: '模型生成的最终回答文本'},
  {
    value: 'reasoningContent',
    label: '思考过程',
    type: 'string',
    description: 'thinking 模型的推理内容'
  },
  {value: 'refusal', label: '拒绝原因文本', type: 'string', description: '模型拒绝回答时的说明'},
  {value: 'isRefusal', label: '是否拒绝回答', type: 'boolean'},
  {
    value: 'toolCalls',
    label: '工具调用',
    type: 'array',
    description: 'agent 场景用 foreach 循环遍历它，每个元素是一条工具调用',
    items: [
      {value: 'id', type: 'string', label: '调用ID'},
      {value: 'functionName', type: 'string', label: '函数名'},
      {value: 'functionArguments', type: 'string', label: '参数', description: 'JSON 字符串'},
      {value: 'type', type: 'string', label: '类型'}
    ]
  },
  {
    value: 'finishReason',
    label: '结束原因',
    type: 'string',
    enum: ['stop', 'tool_calls', 'length', 'content_filter', 'error', 'cancelled'],
    description: '"stop" 正常结束 / "tool_calls" 模型发起了工具调用（agent 循环据此判定是否继续）'
  }
]
const input: NodeInputSchema = {
  type: 'object',
  properties: {
    modelId: {type: 'string', description: '模型资源 id（get_models 获取）'},
    system: {type: 'string', description: '系统提示词（支持变量指令）'},
    user: {
      type: 'string',
      description: '用户提示词（支持变量指令）；enableContext=false 时必填'
    },
    enableContext: {
      type: 'boolean',
      description: 'true 用 contextVariable 作为上下文，false 用对话历史'
    },
    contextVariable: {type: 'array', description: '上下文消息数组来源 [节点ID,字段]'},
    contextNumber: {type: 'number', description: '历史轮次限制，0=全部'},
    images: {type: 'array', description: '图片引用，元素 [节点ID,字段]，一般留空'},
    videos: {type: 'array', description: '视频引用，元素 [节点ID,字段]，一般留空'},
    tools: {
      type: 'object',
      description:
        '工具配置 { location:"customize", reference:[], tools:[{"type":"function","function":{"name":"标准名"}}] }'
    }
  }
}
const FIELD_LIST = deriveFieldList(outputs)

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description:
        '调用大语言模型生成回答，回答实时流式展示给用户；可配置系统/用户提示词、自定义上下文与工具（agent 场景）',
      prompt:
        'ai-chat-node（AI 对话）：只配本节点字段（已绑定目标节点，update/validate 无需传 nodeId）。\n' +
        '- modelId 必填：调 get_models 选一个合适的真实模型 id。\n' +
        '- ★ 引用上游输出前先查：写 system/user 里的 :::variable::: 前先调 get_template_variables 拿可用变量 value；填 contextVariable/images/videos 的 [节点ID,字段] 前先调 get_node_field_options 拿真实节点 id 与字段，切勿臆造。\n' +
        '- system / user 支持变量指令 :::variable {value="节点ID.字段" label="显示名"}:::（value 直接取自 get_template_variables）；enableContext=false 时 user 必填。\n' +
        '- enableContext=true 时用 contextVariable（[节点ID,字段] 指向消息数组，如 [外层循环ID,"compress_context"]）作为上下文，contextNumber 限制轮次(0=全部)。\n' +
        '- tools：agent 场景给模型定义可调用函数 { location:"customize", reference:[], tools:[...] }。\n' +
        `  使用系统预置标准工具时每项只写 {"type":"function","function":{"name":"标准名"}}，系统自动替换为完整定义。标准名：${AGENT_TOOL_NAMES.join(', ')}。\n` +
        '- 输出字段：content 回答、reasoningContent 思考、refusal/isRefusal 拒绝、toolCalls 工具调用、finishReason（"stop"/"tool_calls"）。',
      tools: [
        {
          name: 'update',
          description:
            '配置当前 AI 对话节点：模型、系统/用户提示词、自定义上下文、附件、工具。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            // 读引用硬校验：上下文来源与图/视频引用必须是 get_node_field_options 里的真实字段
            // （contextVariable 常指向 [循环ID,压缩上下文]，引用不存在多半是循环变量还没声明）。
            if (args.contextVariable !== undefined) {
              const err = validateRef(bindings, args.contextVariable, 'contextVariable 上下文来源')
              if (err) return {error: err}
            }
            for (const key of ['images', 'videos'] as const) {
              if (Array.isArray(args[key])) {
                for (const r of args[key]) {
                  const err = validateRef(bindings, r, key)
                  if (err) return {error: err}
                }
              }
            }
            const next = {...cloneDeep(DEFAULT_NODE_DATA), ...(node.properties.nodeData ?? {})}
            for (const key of [
              'modelId',
              'system',
              'user',
              'enableContext',
              'contextVariable',
              'contextNumber',
              'images',
              'videos'
            ]) {
              if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            }
            if (args.tools !== undefined) {
              const t = args.tools ?? {}
              const list = Array.isArray(t.tools) ? t.tools : []
              next.tools = {
                location: t.location ?? 'customize',
                reference: Array.isArray(t.reference) ? t.reference : [],
                // 标准工具函数以内置定义整体替换（后端按固定函数名/参数解析，防模型自编）
                tools: list.map((tool: any) => {
                  const canonical = AGENT_TOOL_DEFINITIONS[tool?.function?.name]
                  return canonical ? cloneDeep(canonical) : tool
                })
              }
            }
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            // 落库后自动校验：通过即返回 stop 收尾（无需 AI 再调 validate），不通过返回 errors 让其续修
            const result = validate(node.properties.nodeData)
            if (!result.valid) return {field_list: FIELD_LIST, errors: result.errors}
            return {status: 'stop', data: {field_list: FIELD_LIST}}
          }
        },
        getModelsTool,
        getNodeFieldOptionsTool(bindings),
        getTemplateVariablesTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'ai-chat-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
