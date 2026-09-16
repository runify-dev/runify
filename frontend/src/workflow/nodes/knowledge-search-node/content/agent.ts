import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList, TOOL_OUTPUT } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getKnowledgeBasesTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { knowledgeSearchNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * knowledge-search-node（知识检索）的 AI 生成配置（命令式，手写）。
 * nodeData 字段与表单 content/index.vue 一致，逐个显式透传（不做「数组=引用/字符串=字面」推断）。
 * ★ 真实字段是 knowledgeIds（模板里的 folderIds 为过期字段，以 index.vue/validator/后端为准）。
 * 顶层 location 决定来源：
 * - tool_call：作为 agent 工具，检索参数取自 reference 指向的工具调用 item（[你所在这层循环的真实ID,"item"]）；
 * - customize：独立检索，keyword 由 keywordLocation（customize 字面量 / reference 引用）决定，pageNo/pageSize 同理。
 * 两种模式都必须先设 knowledgeIds（在哪些知识库里搜）。
 */

const BASE = knowledgeSearchNode.properties.nodeData

/** 出参签名（带类型）：出参单一来源，field_list 由它派生。tool=工具类节点共有「工具执行」输出。 */
const outputs: OutputDef[] = [
  TOOL_OUTPUT,
  {
    value: 'hits',
    label: '结果列表',
    type: 'array',
    items: [
      { value: 'id', type: 'string' },
      { value: 'knowledgeId', type: 'string' },
      { value: 'score', type: 'number', label: '相关度分' },
      { value: 'content', type: 'string', label: '片段内容' },
      { value: 'title', type: 'string', label: '标题' }
    ]
  },
  { value: 'total', label: '总数', type: 'number' },
  { value: 'topScore', label: '最高分', type: 'number' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 知识检索节点数据（与后端 / 表单 content/index.vue 对齐）。
 * location=tool_call：检索参数取自 reference 指向的工具调用；
 * location=customize：keyword/pageNo/pageSize 各由其 xxxLocation（customize 字面量 / reference 引用）决定。
 */
interface KnowledgeSearchNodeData {
  knowledgeIds?: string[]
  location?: 'tool_call' | 'customize'
  reference?: string[]
  keywordLocation?: 'customize' | 'reference'
  keywordReference?: string[]
  keyword?: string
  pageNoLocation?: 'customize' | 'reference'
  pageNoReference?: string[]
  pageNo?: number
  pageSizeLocation?: 'customize' | 'reference'
  pageSizeReference?: string[]
  pageSize?: number
}

/** update 会写入的 nodeData 字段（与表单一致，逐个显式透传） */
const UPDATE_KEYS = [
  'knowledgeIds',
  'location',
  'reference',
  'keywordLocation',
  'keywordReference',
  'keyword',
  'pageNoLocation',
  'pageNoReference',
  'pageNo',
  'pageSizeLocation',
  'pageSizeReference',
  'pageSize'
] as const

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
  type: 'object',
  properties: {
    knowledgeIds: {
      type: 'array',
      items: { type: 'string' },
      description: '知识库 id 数组（必填，两种模式都要；先 get_knowledge_bases 获取）'
    },
    location: {
      type: 'string',
      enum: ['tool_call', 'customize'],
      description:
        '检索参数来源：tool_call=作为 agent 工具，参数取自 reference 指向的工具调用；customize=独立检索，keyword 由 keywordLocation 决定'
    },
    reference: {
      type: 'array',
      items: { type: 'string' },
      description:
        'location=tool_call 时必填：指向工具调用的引用路径 [节点ID,字段]，即你所在这层循环的当前项 [循环ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在循环层，用该层真实循环 id，勿引用错层）。其值须为 { id, functionArguments:\'{"keyword":"..."}\' }（functionArguments 为含 keyword 的 JSON 字符串）。此模式不填 keyword/keywordLocation'
    },
    keywordLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description:
        'location=customize 时检索词来源：customize=用 keyword 字面量；reference=用 keywordReference 引用上游变量'
    },
    keywordReference: {
      type: 'array',
      items: { type: 'string' },
      description: 'keywordLocation=reference 时的检索词引用路径 [节点ID,字段]（get_node_field_options 取）'
    },
    keyword: {
      type: 'string',
      description: 'keywordLocation=customize 时的检索关键词字面量'
    },
    pageNoLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description: '页码来源：customize=用 pageNo 数值；reference=用 pageNoReference 引用'
    },
    pageNoReference: {
      type: 'array',
      items: { type: 'string' },
      description: 'pageNoLocation=reference 时的页码引用 [节点ID,字段]'
    },
    pageNo: {
      type: 'number',
      description: 'pageNoLocation=customize 时的页码（可选，默认 1）'
    },
    pageSizeLocation: {
      type: 'string',
      enum: ['customize', 'reference'],
      description: '每页条数来源：customize=用 pageSize 数值；reference=用 pageSizeReference 引用'
    },
    pageSizeReference: {
      type: 'array',
      items: { type: 'string' },
      description: 'pageSizeLocation=reference 时的每页条数引用 [节点ID,字段]'
    },
    pageSize: {
      type: 'number',
      description: 'pageSizeLocation=customize 时的每页条数（可选，默认 10）'
    }
  }
}

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '在指定知识库中做语义检索，返回命中片段列表',
      prompt:
        'knowledge-search-node（知识检索）：用 update 配置，字段与节点数据一致：\n' +
        '- knowledgeIds 必填（两种模式都要）：知识库 id 数组，先 get_knowledge_bases 获取。\n' +
        '- location：检索参数来源模式。\n' +
        '  · "tool_call"（作为 agent 工具，标准做法）：检索参数取自 reference 指向的工具调用；reference=[你所在这层循环的真实ID,"item"]（先调 get_canvas_detail 看 youAreHere 确认所在层，勿引用错层），其值须为 { id, functionArguments:\'{"keyword":"..."}\' }。此模式不填 keyword/keywordLocation。\n' +
        '  · "customize"（独立检索）：检索词由 keywordLocation 决定 —— keywordLocation="customize" 填 keyword（字面量）；="reference" 填 keywordReference（[节点ID,字段] 引用上游，get_node_field_options 取）。pageNo/pageSize 可选，同样 customize 填数值 / reference 填 xxxReference。\n' +
        '- 输出字段：hits 结果列表（元素 {id,knowledgeId,score,content,title}）、total 总数、topScore 最高分。',
      tools: [
        {
          name: 'update',
          description: '配置当前知识检索节点的各字段。只传要设置的字段，其余保持不变。',
          parameters: input,
          apply: (args) => {
            const next: KnowledgeSearchNodeData = {
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
        getKnowledgeBasesTool,
        getCanvasDetailTool(bindings),
        getNodeFieldOptionsTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'knowledge-search-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
