import { cloneDeep } from 'lodash'
import { loopStartNode } from '@/workflow/common/data'
import { randomId } from '@/utils/common'
import type { InputDef, OutputDef } from './node-catalog'

/**
 * 节点目录构件库：供各节点目录下的 catalog.ts 复用的输入片段与规范化函数。
 * 具体节点条目定义在各节点目录（nodes/<节点>/catalog.ts），由 catalog-registry 自动收集。
 */

export function triple(key: string, label: string, type: InputDef['type'], options: Partial<InputDef> = {}): InputDef {
  return { key, label, type, triple: true, ...options }
}

/** 工具类节点共有的 tool_call 模式开关（agent 场景由 AI 工具调用驱动整个节点） */
export function toolModeInputs(): InputDef[] {
  return [
    {
      key: 'location',
      label: '执行模式',
      type: 'string',
      enum: ['customize', 'tool_call'],
      default: 'customize',
      description: '直接执行用 customize；agent 场景用 tool_call（节点参数从 AI 工具调用解析）'
    },
    {
      key: 'reference',
      label: '工具调用对象引用',
      type: 'array',
      reference: true,
      required: 'location="tool_call" 时',
      description: '指向单个工具调用对象，通常是遍历 toolCalls 的循环 ["循环节点ID","item"]'
    }
  ]
}

/** 工具类节点的隐藏输出：完整工具调用结果，供 context-push 回写 agent 上下文 */
export const HIDDEN_TOOL_OUTPUT: OutputDef = {
  value: 'tool',
  label: '工具调用结果',
  type: 'object',
  hidden: true,
  description: '{ toolName, content 执行结果文本, functionArguments 调用参数JSON, status }，agent 场景用 context-push 引用它回写上下文'
}

export function createLoopStartNode(): Record<string, any> {
  const node = cloneDeep(loopStartNode)
  node.x = 200
  node.y = 200
  return node
}

export function inferDataType(defaultValue?: string): 'string' | 'number' | 'boolean' | 'array' | 'dict' {
  const value = (defaultValue ?? '').trim()
  if (value.startsWith('[')) return 'array'
  if (value.startsWith('{')) return 'dict'
  if (value === 'true' || value === 'false') return 'boolean'
  if (value !== '' && !Number.isNaN(Number(value))) return 'number'
  return 'string'
}

/** loop-break/loop-continue 共用：条件项补 id/variable/value 缺省 */
export function normalizeConditions(nodeData: Record<string, any>): Record<string, any> {
  if (!Array.isArray(nodeData.conditions)) nodeData.conditions = []
  for (const condition of nodeData.conditions) {
    if (!condition.id) condition.id = randomId()
    if (!Array.isArray(condition.variable)) condition.variable = []
    if (condition.value === undefined) condition.value = ''
  }
  if (!nodeData.logic) nodeData.logic = 'and'
  return nodeData
}

export const CONDITION_INPUT_DESC =
  '条件数组（空数组 = 无条件触发）。元素 { variable: [节点ID,字段], compare: 比较符(同 judge-node), value: string }，id 可省略自动生成'

/**
 * response 各画布变体共用的规范化：
 * 校验器 schema 里 jsonFields/headers 元素的 value/location 是必需键（z.any() 不含 undefined），
 * AI 只写 reference 时必须补上空 value，否则校验永远不通过
 */
export function normalizeResponseNodeData(nodeData: Record<string, any>): Record<string, any> {
  const normalizeItems = (items: any) => {
    if (!Array.isArray(items)) return
    for (const item of items) {
      if (!item || typeof item !== 'object') continue
      if (item.value === undefined) item.value = ''
      if (!item.location) item.location = 'customize'
      if (item.reference === undefined) item.reference = []
    }
  }
  normalizeItems(nodeData.jsonFields)
  normalizeItems(nodeData.headers)
  for (const key of ['jsonObject', 'plainText']) {
    const obj = nodeData[key]
    if (obj && typeof obj === 'object') {
      if (!obj.location) obj.location = 'customize'
      if (obj.value === undefined) obj.value = ''
    }
  }
  return nodeData
}

/** 响应体配置（response 各画布变体共用的 inputs 片段） */
export const RESPONSE_BODY_INPUTS: InputDef[] = [
  { key: 'contentType', label: '响应类型', type: 'string', enum: ['jsonFields', 'jsonObject', 'plainText'], default: 'jsonFields' },
  {
    key: 'jsonFields',
    label: '字段配置',
    type: 'array',
    default: [],
    description: 'contentType=jsonFields 时：元素 { field 字段名, location:"reference"|"customize", reference:[节点ID,字段], value 固定值, required: boolean }'
  },
  {
    key: 'jsonObject',
    label: 'JSON对象',
    type: 'object',
    default: { location: 'customize', value: '', reference: [] },
    description: 'contentType=jsonObject 时：{ location, value JSON字符串, reference }'
  },
  {
    key: 'plainText',
    label: '文本',
    type: 'object',
    default: { location: 'customize', value: '', reference: [] },
    description: 'contentType=plainText 时：{ location:"reference"|"customize", value, reference:[节点ID,字段] }'
  }
]
