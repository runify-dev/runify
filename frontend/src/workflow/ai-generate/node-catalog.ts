import { cloneDeep } from 'lodash'

/**
 * AI 生成工作流的节点目录框架（与具体画布无关）：
 * 只定义条目结构与派生逻辑，具体节点条目在 catalog-library.ts（共享库），
 * 各画布的目录由 profiles/<画布>/catalog.ts 组装（type → entry，画布内 type 唯一）。
 *
 * 每个节点的输入（nodeData 字段）与输出（field_list）以结构化方式声明：
 * - 文档（get_node_schema 下发）由 renderDoc 自动渲染，格式统一
 * - defaultNodeData 由 inputs 自动派生（defaults 补充无法派生的部分）
 * - add_node/update_node 的 nodeData patch 按 inputs 白名单校验，未知字段以 warning 回喂模型
 * - field_list 默认由 outputs 派生（动态输出的节点提供自定义 applyFieldList）
 * 字段结构以各节点 content/index.vue 与后端 Node 实现为准
 */

export interface InputDef {
  /** nodeData 字段名（triple=true 时同时展开 keyLocation/keyReference 两个伴生字段） */
  key: string
  label: string
  type: 'string' | 'number' | 'boolean' | 'array' | 'object'
  /** true=必填；字符串=条件必填的说明 */
  required?: boolean | string
  default?: any
  enum?: readonly string[]
  /** 参数三件套：key + keyLocation("customize"|"reference") + keyReference([节点ID,字段]) */
  triple?: boolean
  /** 值本身是变量引用路径 [节点ID, 字段] */
  reference?: boolean
  description?: string
}

export interface OutputDef {
  value: string
  label: string
  type: string
  /** 复合结构的元素/字段说明 */
  description?: string
  /** 不进 field_list，但运行时可被引用（如工具节点的 tool 字段） */
  hidden?: boolean
}

export interface NodeCatalogEntry {
  type: string
  label: string
  summary: string
  /** false = 画布固有节点（如处理器 start-node）：可查文档/update_node，不可 add_node */
  addable?: boolean
  inputs: InputDef[]
  outputs: OutputDef[]
  /** 追加到文档末尾的用法说明/约束/示例 */
  notes?: string[]
  template: Record<string, any>
  /** inputs 无法派生的额外默认值（每次调用生成新对象） */
  defaults?: () => Record<string, any>
  /** 补齐 AI 省略的内部字段（如 judge 分支 id、loop children） */
  normalizeNodeData?: (nodeData: Record<string, any>) => Record<string, any>
  /** 动态输出节点自定义刷新 field_list；缺省由 outputs 派生 */
  applyFieldList?: (properties: Record<string, any>) => void
}

/** 画布目录：type → entry（一个画布内每种节点类型只有一个条目） */
export type NodeCatalog = Record<string, NodeCatalogEntry>

/**
 * 节点目录声明：各节点目录下 catalog.ts 的导出约定（与 validator.ts 同级同风格），
 * 由 catalog-registry 自动收集、按画布过滤。画布差异节点导出 NodeCatalogDef[]（每画布一个变体）。
 */
export interface NodeCatalogDef {
  /** 支持的画布类型（WorkflowType 值）；省略 = 所有画布 */
  workflowTypes?: string[]
  entry: NodeCatalogEntry
}

/** 提示词中变量引用指令的通用说明（ai-chat 的 system/user 等模板文本用） */
export const PROMPT_VARIABLE_DOC =
  '提示词文本中引用上游变量的格式（独占一行）：:::variable {value="节点ID.字段" label="显示名"}:::\n' +
  '例如：:::variable {value="start-node.question" label="用户问题"}:::'

/** 由条目列表组装画布目录（重复 type 视为组装错误，立即抛出） */
export function buildCatalog(entries: NodeCatalogEntry[]): NodeCatalog {
  const catalog: NodeCatalog = {}
  for (const entry of entries) {
    if (catalog[entry.type]) {
      throw new Error(`目录组装错误：节点类型重复 ${entry.type}`)
    }
    catalog[entry.type] = entry
  }
  return catalog
}

// ─────────────────────────── 派生逻辑 ───────────────────────────

/** input 展开后的全部 nodeData 字段名（triple → 3 个） */
function expandInputKeys(input: InputDef): string[] {
  return input.triple ? [input.key, `${input.key}Location`, `${input.key}Reference`] : [input.key]
}

function typeDefault(type: InputDef['type']): any {
  switch (type) {
    case 'string':
      return ''
    case 'array':
      return []
    case 'object':
      return {}
    case 'boolean':
      return false
    default:
      return null
  }
}

/** 由 inputs（+defaults）派生完整默认 nodeData */
function buildDefaultNodeData(entry: NodeCatalogEntry): Record<string, any> {
  const data: Record<string, any> = {}
  for (const input of entry.inputs) {
    if (input.triple) {
      data[`${input.key}Location`] = 'customize'
      data[`${input.key}Reference`] = []
      data[input.key] = cloneDeep(input.default ?? (input.type === 'number' ? null : typeDefault(input.type)))
    } else {
      data[input.key] = cloneDeep(input.default ?? typeDefault(input.type))
    }
  }
  return { ...data, ...(entry.defaults?.() ?? {}) }
}

/** 渲染节点文档（get_node_schema 下发给模型） */
export function renderDoc(entry: NodeCatalogEntry): string {
  const lines: string[] = []
  if (entry.inputs.length) {
    lines.push('输入（nodeData 字段）：')
    for (const input of entry.inputs) {
      const required =
        input.required === true ? '必填' : typeof input.required === 'string' ? `必填条件：${input.required}` : '可选'
      let line = `- ${input.key}: ${input.type}，${input.label}（${required}`
      if (input.default !== undefined) line += `，默认 ${JSON.stringify(input.default)}`
      line += '）'
      if (input.enum) line += ` 可选值：${input.enum.join(' | ')}`
      if (input.reference) line += ' 值为变量引用路径 [节点ID,字段]'
      if (input.description) line += `——${input.description}`
      lines.push(line)
      if (input.triple) {
        lines.push(
          `  · 引用上游变量时：${input.key}Location="reference" + ${input.key}Reference=[节点ID,字段]；固定值时保持 ${input.key}Location="customize" 并填 ${input.key}`
        )
      }
    }
  } else {
    lines.push('输入（nodeData 字段）：无需配置，传 {} 即可')
  }
  if (entry.outputs.length) {
    lines.push('', '输出字段：')
    for (const output of entry.outputs) {
      let line = `- ${output.value} (${output.type}) ${output.label}`
      if (output.description) line += `——${output.description}`
      if (output.hidden) line += '【隐藏字段：不显示在 field_list，但可用 [节点ID,"' + output.value + '"] 引用】'
      lines.push(line)
    }
  } else {
    lines.push('', '输出字段：无（或由配置动态生成，见说明）')
  }
  if (entry.notes?.length) {
    lines.push('', '说明：')
    for (const note of entry.notes) lines.push(`- ${note}`)
  }
  return lines.join('\n')
}

/**
 * 按 inputs 白名单过滤 nodeData patch：
 * 未知字段丢弃并生成警告（回喂模型自纠），enum 取值非法也告警
 */
export function sanitizeNodeDataPatch(
  catalog: NodeCatalog,
  type: string,
  patch: Record<string, any> | undefined
): { patch: Record<string, any>; warnings: string[] } {
  const entry = catalog[type]
  if (!entry || !patch) return { patch: patch ?? {}, warnings: [] }
  const allowed = new Set(entry.inputs.flatMap(expandInputKeys))
  const clean: Record<string, any> = {}
  const unknownKeys: string[] = []
  for (const [key, value] of Object.entries(patch)) {
    if (allowed.has(key)) {
      clean[key] = value
    } else {
      unknownKeys.push(key)
    }
  }
  const warnings: string[] = []
  if (unknownKeys.length) {
    warnings.push(`未知字段已忽略：${unknownKeys.join(', ')}。可用字段：${[...allowed].join(', ')}`)
  }
  for (const input of entry.inputs) {
    const value = clean[input.key]
    if (input.enum && value !== undefined && !input.enum.includes(value)) {
      warnings.push(`${input.key} 取值 "${value}" 非法，可选值：${input.enum.join(' | ')}`)
    }
  }
  return { patch: clean, warnings }
}

/** 系统提示词用的一行式节点目录 */
export function buildCatalogSummary(catalog: NodeCatalog): string {
  return Object.values(catalog)
    .map((entry) => `- ${entry.type}（${entry.label}）：${entry.summary}`)
    .join('\n')
}

/** 缺省 field_list：由 outputs（非隐藏）派生 */
function defaultApplyFieldList(entry: NodeCatalogEntry, properties: Record<string, any>): void {
  properties.field_list = entry.outputs
    .filter((output) => !output.hidden)
    .map((output) => ({ label: output.label, value: output.value }))
}

/** 生成一个可 addNode 的完整 properties（模板 + 默认 nodeData + AI patch），返回校验警告 */
export function buildNodeProperties(
  catalog: NodeCatalog,
  type: string,
  name: string | undefined,
  nodeDataPatch: Record<string, any> | undefined
): { properties: Record<string, any>; warnings: string[] } {
  const entry = catalog[type]
  if (!entry) {
    throw new Error(`不支持的节点类型: ${type}`)
  }
  const properties = cloneDeep(entry.template.properties)
  if (name) {
    properties.name = name
  }
  const { patch, warnings } = sanitizeNodeDataPatch(catalog, type, nodeDataPatch)
  let nodeData = { ...buildDefaultNodeData(entry), ...cloneDeep(patch) }
  if (entry.normalizeNodeData) {
    nodeData = entry.normalizeNodeData(nodeData)
  }
  properties.nodeData = nodeData
  if (entry.applyFieldList) {
    entry.applyFieldList(properties)
  } else {
    defaultApplyFieldList(entry, properties)
  }
  return { properties, warnings }
}

/** update_node 后重新规范化 nodeData 并刷新 field_list */
export function refreshNodeProperties(
  catalog: NodeCatalog,
  type: string,
  properties: Record<string, any>
): void {
  const entry = catalog[type]
  if (!entry) return
  if (entry.normalizeNodeData && properties.nodeData) {
    properties.nodeData = entry.normalizeNodeData(properties.nodeData)
  }
  if (entry.applyFieldList) {
    entry.applyFieldList(properties)
  } else {
    defaultApplyFieldList(entry, properties)
  }
}
