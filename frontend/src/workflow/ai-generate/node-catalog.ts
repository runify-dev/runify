import { cloneDeep } from 'lodash'
import {
  aiChatNode,
  knowledgeSearchNode,
  judgeNode,
  extractNode,
  variableAssignNode,
  jsonResponseNode,
  javaScriptNode,
  terminalNode,
  readFileNode,
  listDirNode,
  globNode,
  grepNode,
  createFileNode,
  applyPatchNode,
  loopNode,
  loopStartNode,
  loopBreakNode,
  loopContinueNode,
  contextQueryNode,
  contextSaveNode,
  contextManageNode,
  contextPushNode,
  fileUploadNode,
  fileDownloadNode,
  listSkillsNode,
  downloadSkillsNode,
  runSkillNode
} from '@/workflow/common/data'
import { randomId } from '@/utils/common'
import { AGENT_TOOL_DEFINITIONS, AGENT_TOOL_NODE_MAP, AGENT_TOOL_NAMES } from './agent-tools'

/**
 * AI 生成工作流的静态节点目录
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

/** 提示词中变量引用指令的通用说明（ai-chat 的 system/user 等模板文本用） */
export const PROMPT_VARIABLE_DOC =
  '提示词文本中引用上游变量的格式（独占一行）：:::variable {value="节点ID.字段" label="显示名"}:::\n' +
  '例如：:::variable {value="start-node.question" label="用户问题"}:::'

// ─────────────────────────── 通用构件 ───────────────────────────

function triple(key: string, label: string, type: InputDef['type'], options: Partial<InputDef> = {}): InputDef {
  return { key, label, type, triple: true, ...options }
}

/** 工具类节点共有的 tool_call 模式开关（agent 场景由 AI 工具调用驱动整个节点） */
function toolModeInputs(): InputDef[] {
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
const HIDDEN_TOOL_OUTPUT: OutputDef = {
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

function inferDataType(defaultValue?: string): 'string' | 'number' | 'boolean' | 'array' | 'dict' {
  const value = (defaultValue ?? '').trim()
  if (value.startsWith('[')) return 'array'
  if (value.startsWith('{')) return 'dict'
  if (value === 'true' || value === 'false') return 'boolean'
  if (value !== '' && !Number.isNaN(Number(value))) return 'number'
  return 'string'
}

function normalizeConditions(nodeData: Record<string, any>): Record<string, any> {
  if (!Array.isArray(nodeData.conditions)) nodeData.conditions = []
  for (const condition of nodeData.conditions) {
    if (!condition.id) condition.id = randomId()
    if (!Array.isArray(condition.variable)) condition.variable = []
    if (condition.value === undefined) condition.value = ''
  }
  if (!nodeData.logic) nodeData.logic = 'and'
  return nodeData
}

const CONDITION_INPUT_DESC =
  '条件数组（空数组 = 无条件触发）。元素 { variable: [节点ID,字段], compare: 比较符(同 judge-node), value: string }，id 可省略自动生成'

// ─────────────────────────── 节点定义 ───────────────────────────

const aiChat: NodeCatalogEntry = {
  type: 'ai-chat-node',
  label: 'AI 对话',
  summary: '调用大语言模型生成回答，回答实时流式展示给用户',
  inputs: [
    { key: 'modelId', label: '模型', type: 'string', required: true, description: '模型资源 id，先调用 get_models 获取' },
    { key: 'system', label: '系统提示词', type: 'string', default: '', description: '支持变量指令（见变量引用说明）' },
    { key: 'user', label: '用户提示词', type: 'string', default: '', required: 'enableContext=false 时', description: '支持变量指令' },
    { key: 'enableContext', label: '自定义上下文', type: 'boolean', default: false, description: 'true 时使用 contextVariable 指定的消息数组作为上下文；false 时用对话历史' },
    { key: 'contextVariable', label: '上下文变量', type: 'array', reference: true, required: 'enableContext=true 时', description: '消息数组来源，如 [外层循环ID,"compress_context"]' },
    { key: 'contextNumber', label: '上下文轮次', type: 'number', default: 0, description: '限制历史轮次，0=全部' },
    { key: 'images', label: '图片引用', type: 'array', default: [], description: '元素为 [节点ID,字段]，一般留空' },
    { key: 'videos', label: '视频引用', type: 'array', default: [], description: '元素为 [节点ID,字段]，一般留空' },
    {
      key: 'tools',
      label: '工具配置',
      type: 'object',
      default: { location: 'customize', reference: [], tools: [] },
      description:
        'agent 场景给模型定义可调用函数：{ location:"customize", reference:[], tools:[...] }。' +
        '★ 使用系统预置标准工具时每项只写 {"type":"function","function":{"name":"标准名"}}，系统自动替换为完整标准定义。' +
        `标准名（禁止改名/自编参数）：${AGENT_TOOL_NAMES.join(', ')}。` +
        '仅业务自定义函数（不由工具节点执行）才需要完整写 name/description/parameters'
    }
  ],
  outputs: [
    { value: 'content', label: '回答结果', type: 'string' },
    { value: 'reasoningContent', label: '思考过程', type: 'string' },
    { value: 'refusal', label: '拒绝原因文本', type: 'string' },
    { value: 'isRefusal', label: '是否拒绝回答', type: 'boolean' },
    {
      value: 'toolCalls',
      label: '工具调用',
      type: 'array',
      description: '元素 { id, functionName 函数名, functionArguments 参数JSON字符串, type }；agent 场景用 foreach 循环遍历它'
    },
    { value: 'finishReason', label: '结束原因', type: 'string', description: '"stop" 正常结束 / "tool_calls" 模型发起了工具调用' }
  ],
  notes: [PROMPT_VARIABLE_DOC],
  template: aiChatNode,
  normalizeNodeData: (nodeData) => {
    // 标准工具函数定义以内置模板为准整体替换（后端工具节点按固定函数名/参数解析，防止模型自编导致错乱）
    const tools = nodeData.tools?.tools
    if (Array.isArray(tools)) {
      nodeData.tools.tools = tools.map((tool: any) => {
        const canonical = AGENT_TOOL_DEFINITIONS[tool?.function?.name]
        return canonical ? cloneDeep(canonical) : tool
      })
    }
    return nodeData
  }
}

const knowledgeSearch: NodeCatalogEntry = {
  type: 'knowledge-search-node',
  label: '知识检索',
  summary: '在指定知识库中做语义检索，返回命中片段列表',
  inputs: [
    { key: 'knowledgeIds', label: '知识库', type: 'array', required: true, description: '知识库 id 列表，先调用 get_knowledge_bases 获取' },
    ...toolModeInputs(),
    triple('keyword', '检索关键词', 'string', { required: 'location="customize" 时', default: '', description: '通常引用 ["start-node","question"]' }),
    triple('pageNo', '页码', 'number', { default: 1 }),
    triple('pageSize', '每页条数', 'number', { default: 10 })
  ],
  outputs: [
    {
      value: 'hits',
      label: '结果列表',
      type: 'array',
      description: '元素 { id, knowledgeId, score 相关度分, content 片段内容, title 标题 }'
    },
    { value: 'total', label: '总数', type: 'number' },
    { value: 'topScore', label: '最高分', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: knowledgeSearchNode
}

const judge: NodeCatalogEntry = {
  type: 'judge-node',
  label: '条件判断',
  summary: '按条件分支执行，每个分支有独立出口锚点（连线时用 sourceBranchId 指定分支）',
  inputs: [
    {
      key: 'branches',
      label: '分支',
      type: 'array',
      required: true,
      description:
        '必须至少一个 if 分支和一个 else 分支（else 必须保留且在最后）。元素 { type:"if"|"elseif"|"else", logic:"and"|"or", ' +
        'conditions:[{ variable:[节点ID,字段], compare, value }] }（else 不需要 logic/conditions；id 可省略自动生成）。' +
        'compare 可选值：eq/not_eq/ge/gt/le/lt/contain/not_contain/is_null/is_not_null/len_eq/len_ge/len_gt/len_le/len_lt/' +
        'is_true/is_not_true/start_with/end_with/regex/wildcard（is_null/is_not_null/is_true/is_not_true 不需要 value）'
    }
  ],
  outputs: [],
  notes: ['连线：add_edge 时用 sourceBranchId 指定从哪个分支引出（分支 id 通过 get_workflow 或 add_node 返回的 nodeData 查看）'],
  template: judgeNode,
  defaults: () => ({
    branches: [
      {
        id: randomId(),
        type: 'if',
        logic: 'and',
        conditions: [{ id: randomId(), variable: [], compare: 'eq', value: '' }]
      },
      { id: randomId(), type: 'else', logic: 'and', conditions: [] }
    ]
  }),
  normalizeNodeData: (nodeData) => {
    const branches = Array.isArray(nodeData.branches) ? nodeData.branches : []
    for (const branch of branches) {
      if (!branch.id) branch.id = randomId()
      if (branch.type !== 'else') {
        if (!branch.logic) branch.logic = 'and'
        if (!Array.isArray(branch.conditions)) branch.conditions = []
        for (const condition of branch.conditions) {
          if (!condition.id) condition.id = randomId()
          if (!Array.isArray(condition.variable)) condition.variable = []
          if (condition.value === undefined) condition.value = ''
        }
      }
    }
    if (branches.length > 0 && !branches.some((b: any) => b.type === 'else')) {
      branches.push({ id: randomId(), type: 'else', logic: 'and', conditions: [] })
    }
    return { ...nodeData, branches }
  }
}

const extract: NodeCatalogEntry = {
  type: 'extract-node',
  label: '参数提取',
  summary: '用 JSONPath 从上游变量（JSON/对象）中提取字段，产出可被下游引用的新变量',
  inputs: [
    { key: 'sourceReference', label: '源变量', type: 'array', reference: true, required: true },
    {
      key: 'rules',
      label: '提取规则',
      type: 'array',
      required: true,
      description: '元素 { name 字段名(成为输出字段), description 说明, path JSONPath 如 "$.functionName" 或 "$[0].id" }'
    }
  ],
  outputs: [],
  notes: ['输出字段由 rules 动态生成：每条规则一个输出字段（value=rule.name，label=rule.description）'],
  template: extractNode,
  applyFieldList: (properties) => {
    const rules = properties.nodeData?.rules ?? []
    properties.field_list = rules
      .filter((r: any) => r.name && String(r.name).trim())
      .map((r: any) => ({ label: r.description || r.name, value: r.name }))
  }
}

const variableAssign: NodeCatalogEntry = {
  type: 'variable-assign-node',
  label: '变量赋值',
  summary: '给开始节点定义的全局变量或循环变量赋值（引用上游变量或常量）',
  inputs: [
    {
      key: 'variables',
      label: '赋值项',
      type: 'array',
      default: [],
      description:
        '元素 { variable: 目标变量路径(全局变量 ["global",名] 或循环变量 ["循环ID",名]), type:"reference"|"constant", ' +
        'reference:[节点ID,字段](type=reference 时), dataType:"string"|"array"|"dict"|"number"|"boolean"(type=constant 时), ' +
        'value: 常量值(array/dict 用 JSON 字符串) }'
    }
  ],
  outputs: [],
  template: variableAssignNode
}

const response: NodeCatalogEntry = {
  type: 'response-node',
  label: '数据响应',
  summary: '向用户输出结构化/文本响应内容（引用上游变量或固定值）',
  inputs: [
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
    },
    { key: 'chunk', label: '分块输出', type: 'boolean', default: false, description: '保持默认 false' }
  ],
  outputs: [],
  notes: ['典型用法：把 ai-chat 回答之外的结构化数据（如检索结果）响应给前端'],
  template: jsonResponseNode
}

const javaScript: NodeCatalogEntry = {
  type: 'java-script-node',
  label: 'JavaScript 执行',
  summary: '执行一段 JavaScript 代码做数据加工/计算，返回结果供下游引用',
  inputs: [
    { key: 'mode', label: '模式', type: 'string', enum: ['script', 'function'], default: 'script', description: '推荐 script：代码直接执行，最后一个表达式的值即结果' },
    triple('code', 'JavaScript 代码', 'string', { required: 'codeLocation="customize" 时', default: '' }),
    { key: 'functionName', label: '函数名', type: 'string', default: '', required: 'mode="function" 时' },
    { key: 'allowIO', label: '允许 IO', type: 'boolean', default: false, description: '是否允许 IO/进程访问' },
    {
      key: 'parameters',
      label: '注入参数',
      type: 'array',
      default: [],
      description:
        'script 模式注入为顶层变量。元素 { field 变量名, location:"reference"|"customize", reference:[节点ID,字段], value 固定值, type/description 可选 }'
    }
  ],
  outputs: [{ value: 'result', label: '结果', type: 'any', description: '代码最后一个表达式的值' }],
  notes: [
    '示例：parameters=[{field:"hits",location:"reference",reference:["知识检索节点ID","hits"]}]，code="hits.map(h => h.content).join(\'\\n\')"'
  ],
  template: javaScriptNode
}

const terminal: NodeCatalogEntry = {
  type: 'terminal-node',
  label: '终端执行',
  summary: '在会话工作目录执行 shell 命令，输出 stdout/stderr/退出码',
  inputs: [
    { key: 'runtime', label: '运行时', type: 'string', default: 'local', description: '保持 "local"' },
    ...toolModeInputs(),
    triple('code', 'shell 命令', 'string', { required: 'location="customize" 时', default: '' }),
    triple('timeout', '超时秒数', 'number', { description: '1-3600，可选' })
  ],
  outputs: [
    { value: 'result', label: '执行结果', type: 'string' },
    { value: 'stdout', label: '标准输出', type: 'string' },
    { value: 'stderr', label: '错误输出', type: 'string' },
    { value: 'exitCode', label: '退出码', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  notes: ['文件路径均为会话工作目录内的相对路径'],
  template: terminalNode
}

const readFile: NodeCatalogEntry = {
  type: 'read-file-node',
  label: '读取文件',
  summary: '读取工作目录中的文件内容',
  inputs: [
    ...toolModeInputs(),
    triple('path', '文件相对路径', 'string', { required: 'location="customize" 时', default: '' }),
    triple('offset', '起始行', 'number'),
    triple('limit', '读取行数', 'number')
  ],
  outputs: [
    { value: 'content', label: '带行号内容', type: 'string' },
    { value: 'rawContent', label: '原始内容', type: 'string' },
    { value: 'totalLines', label: '总行数', type: 'number' },
    { value: 'lines', label: '读取行数', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: readFileNode
}

const listDir: NodeCatalogEntry = {
  type: 'list-dir-node',
  label: '目录列表',
  summary: '列出工作目录下的目录树',
  inputs: [
    ...toolModeInputs(),
    triple('path', '目录相对路径', 'string', { required: 'location="customize" 时', default: '', description: '"." 表示根目录' }),
    triple('depth', '遍历深度', 'number')
  ],
  outputs: [
    { value: 'content', label: '目录树', type: 'string' },
    { value: 'summary', label: '摘要', type: 'string' },
    { value: 'files', label: '文件数', type: 'number' },
    { value: 'dirs', label: '目录数', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: listDirNode
}

const glob: NodeCatalogEntry = {
  type: 'glob-node',
  label: '文件搜索',
  summary: '按 glob 模式搜索工作目录中的文件名',
  inputs: [
    ...toolModeInputs(),
    triple('pattern', 'glob 模式', 'string', { required: 'location="customize" 时', default: '', description: '如 "**/*.md"' }),
    triple('path', '搜索起始目录', 'string', { default: '' }),
    triple('maxResults', '结果上限', 'number')
  ],
  outputs: [
    { value: 'content', label: '文件列表', type: 'string' },
    { value: 'summary', label: '摘要', type: 'string' },
    { value: 'files', label: '文件数', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: globNode
}

const grep: NodeCatalogEntry = {
  type: 'grep-node',
  label: '内容搜索',
  summary: '按正则在工作目录文件内容中搜索',
  inputs: [
    ...toolModeInputs(),
    triple('pattern', '搜索正则', 'string', { required: 'location="customize" 时', default: '' }),
    triple('path', '搜索路径', 'string', { required: 'location="customize" 时', default: '' }),
    triple('filePattern', '文件名过滤', 'string', { default: '' }),
    triple('contextLines', '上下文行数', 'number'),
    triple('maxResults', '结果上限', 'number')
  ],
  outputs: [
    { value: 'content', label: '搜索结果', type: 'string', description: '格式 "文件:行号: 内容"' },
    { value: 'summary', label: '摘要', type: 'string' },
    { value: 'matches', label: '匹配数', type: 'number' },
    { value: 'files', label: '文件数', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: grepNode
}

const createFile: NodeCatalogEntry = {
  type: 'create-file-node',
  label: '创建文件',
  summary: '在工作目录创建/写入文件',
  inputs: [
    ...toolModeInputs(),
    triple('path', '文件相对路径', 'string', { required: 'location="customize" 时', default: '' }),
    triple('content', '文件内容', 'string', { default: '' })
  ],
  outputs: [{ value: 'result', label: '执行结果', type: 'string' }, HIDDEN_TOOL_OUTPUT],
  template: createFileNode
}

const applyPatch: NodeCatalogEntry = {
  type: 'apply-patch-node',
  label: '数据修补',
  summary: '对工作目录文件应用 unified diff 补丁',
  inputs: [
    ...toolModeInputs(),
    triple('path', '目标文件相对路径', 'string', { default: '' }),
    triple('patch', '补丁内容', 'string', { required: 'location="customize" 时', default: '' })
  ],
  outputs: [
    { value: 'result', label: '执行结果', type: 'string' },
    { value: 'stdout', label: '标准输出', type: 'string' },
    { value: 'stderr', label: '错误输出', type: 'string' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: applyPatchNode
}

const loop: NodeCatalogEntry = {
  type: 'loop-node',
  label: '循环',
  summary: '循环执行子画布（数组遍历/无限/指定次数），子画布内节点用 parentLoopId 添加',
  inputs: [
    { key: 'loopType', label: '循环类型', type: 'string', enum: ['foreach', 'infinite', 'count'], required: true, default: 'foreach' },
    { key: 'loopVariable', label: '遍历数组', type: 'array', reference: true, required: 'loopType="foreach" 时', description: '要遍历的数组变量，如 [aiChatID,"toolCalls"]' },
    { key: 'loopCount', label: '循环次数', type: 'number', required: 'loopType="count" 时' },
    {
      key: 'loopVariables',
      label: '循环作用域变量',
      type: 'array',
      default: [],
      description:
        '元素 { name, label, dataType, defaultValue }。dataType 必填且必须与值实际类型一致："string"|"number"|"boolean"|"array"|"dict"；' +
        'defaultValue 为字符串形式（数组 "[]"、字典 "{}"）。示例：{"name":"context","label":"上下文","dataType":"array","defaultValue":"[]"}。' +
        '定义后成为本循环输出字段，循环体内用 variable-assign 对 ["循环ID",name] 赋值实现跨轮传递状态'
    },
    { key: 'children', label: '子画布', type: 'object', description: '系统管理：添加本节点时自动初始化（含入口 loop-start-node），不要手动传' }
  ],
  outputs: [
    { value: 'item', label: '当前项', type: 'any', description: 'foreach 时为数组当前元素' },
    { value: 'index', label: '当前索引', type: 'number' }
  ],
  notes: [
    '子画布：add_node/add_edge/update_node/delete_node 传 parentLoopId=本节点 id；第一条边必须从 "loop-start-node" 引出；循环可嵌套',
    '循环体内引用当前项 [本循环ID,"item"]；用 loop-break-node 跳出、loop-continue-node 跳过本轮',
    'loopVariables 定义的变量也是本循环的输出字段'
  ],
  template: loopNode,
  defaults: () => ({ children: { nodes: [createLoopStartNode()], edges: [] } }),
  normalizeNodeData: (nodeData) => {
    if (!nodeData.children || !Array.isArray(nodeData.children.nodes)) {
      nodeData.children = { nodes: [createLoopStartNode()], edges: [] }
    } else if (!nodeData.children.nodes.some((n: any) => n.type === 'loop-start-node')) {
      nodeData.children.nodes.unshift(createLoopStartNode())
    }
    if (!Array.isArray(nodeData.children.edges)) nodeData.children.edges = []
    if (!Array.isArray(nodeData.loopVariables)) nodeData.loopVariables = []
    for (const variable of nodeData.loopVariables) {
      if (!variable || typeof variable !== 'object') continue
      if (!variable.label) variable.label = variable.name
      if (variable.defaultValue != null && typeof variable.defaultValue !== 'string') {
        variable.defaultValue = JSON.stringify(variable.defaultValue)
      }
      if (!['string', 'number', 'boolean', 'array', 'dict'].includes(variable.dataType)) {
        variable.dataType = inferDataType(variable.defaultValue)
      }
    }
    return nodeData
  },
  applyFieldList: (properties) => {
    const loopVariables = properties.nodeData?.loopVariables ?? []
    const customFields = loopVariables
      .filter((v: any) => v.name && String(v.name).trim())
      .map((v: any) => ({ label: v.label || v.name, value: v.name }))
    properties.field_list = [
      { label: '当前项', value: 'item' },
      { label: '当前索引', value: 'index' },
      ...customFields
    ]
    properties.loopFieldList = customFields
  }
}

const loopBreak: NodeCatalogEntry = {
  type: 'loop-break-node',
  label: '跳出循环',
  summary: '（仅循环子画布内）满足条件时跳出整个循环',
  inputs: [
    { key: 'conditions', label: '条件', type: 'array', default: [], description: CONDITION_INPUT_DESC },
    { key: 'logic', label: '逻辑', type: 'string', enum: ['and', 'or'], default: 'and' }
  ],
  outputs: [],
  template: loopBreakNode,
  normalizeNodeData: normalizeConditions
}

const loopContinue: NodeCatalogEntry = {
  type: 'loop-continue-node',
  label: '循环跳过',
  summary: '（仅循环子画布内）满足条件时跳过本轮，进入下一轮',
  inputs: [
    { key: 'conditions', label: '条件', type: 'array', default: [], description: CONDITION_INPUT_DESC },
    { key: 'logic', label: '逻辑', type: 'string', enum: ['and', 'or'], default: 'and' }
  ],
  outputs: [],
  template: loopContinueNode,
  normalizeNodeData: normalizeConditions
}

const contextQuery: NodeCatalogEntry = {
  type: 'context-query-node',
  label: '上下文查询',
  summary: '读取会话持久化上下文（历史消息/摘要/便签），通常放在 start 之后作为循环的种子数据',
  inputs: [],
  outputs: [
    { value: 'history', label: '历史上下文', type: 'array', description: '历史消息数组（不含当前轮）' },
    { value: 'full', label: '完整历史', type: 'array', description: '含当前轮的消息数组' },
    { value: 'summary', label: '摘要', type: 'object', description: '{ text 摘要文本, covered 覆盖条数, seedCovered }' },
    { value: 'facts', label: '便签', type: 'array' },
    { value: 'historyUpto', label: '载入边界', type: 'number' }
  ],
  notes: ['无需配置（会话标识从运行参数自动获取），nodeData 传 {} 即可'],
  template: contextQueryNode
}

const contextSave: NodeCatalogEntry = {
  type: 'context-save-node',
  label: '上下文写入',
  summary: '把摘要/便签写回会话持久化存储，通常放在外层循环结束之后',
  inputs: [
    { key: 'summaryReference', label: '摘要来源', type: 'array', reference: true, required: '与 factsReference 至少配置一个', description: '如 [外层循环ID,"summary"]' },
    { key: 'factsReference', label: '便签来源', type: 'array', reference: true, required: '与 summaryReference 至少配置一个', description: '如 [外层循环ID,"facts"]' }
  ],
  outputs: [
    { value: 'savedSummary', label: '摘要已写入', type: 'boolean' },
    { value: 'savedFacts', label: '便签写入数', type: 'number' }
  ],
  template: contextSaveNode
}

const contextManage: NodeCatalogEntry = {
  type: 'context-manage-node',
  label: '上下文压缩',
  summary: '按 token 预算压缩上下文（可选 LLM 摘要），agent 循环中放在 ai-chat 之前',
  inputs: [
    { key: 'sourceSeedVariable', label: '待压缩上下文（入参）', type: 'array', reference: true, required: true, description: '首轮种子，如 context-query 的 history 或循环变量 [外层循环ID,"context"]' },
    { key: 'sourceVariable', label: '压缩结果写回（出参）', type: 'array', reference: true, description: '不能与 sourceSeedVariable 相同，如 [外层循环ID,"compress_context"]，供 ai-chat 的 contextVariable 使用' },
    { key: 'summarySeedVariable', label: '摘要入参', type: 'array', reference: true },
    { key: 'summaryVariable', label: '摘要出参', type: 'array', reference: true },
    { key: 'factsSeedVariable', label: '便签入参', type: 'array', reference: true },
    { key: 'factsVariable', label: '便签出参', type: 'array', reference: true },
    { key: 'budget', label: 'token 预算', type: 'number', default: 32000, description: '≥1000' },
    { key: 'highRatio', label: '高水位', type: 'number', default: 0.85 },
    { key: 'lowRatio', label: '低水位', type: 'number', default: 0.6 },
    { key: 'keepRecentItems', label: '保留最近条数', type: 'number', default: 10 },
    { key: 'stripMultimodal', label: '剥离多模态', type: 'boolean', default: true },
    { key: 'enableSummarizer', label: '启用 LLM 摘要', type: 'boolean', default: false },
    { key: 'summarizerModelId', label: '摘要模型', type: 'string', default: '', required: 'enableSummarizer=true 时', description: 'get_models 获取' },
    { key: 'summarizerMethod', label: '摘要方式', type: 'string', default: 'fc', description: '保持 "fc"' },
    { key: 'factSections', label: '便签分区', type: 'array', default: ['convention', 'preference', 'env', 'goal', 'todo'], description: '保持默认' },
    { key: 'reservedTokens', label: '预留 token', type: 'number', default: 3000, description: '保持默认' },
    { key: 'tokenEncoding', label: '编码', type: 'string', default: 'cl100k', description: '保持默认' }
  ],
  outputs: [
    { value: 'messages', label: '消息', type: 'array', description: '压缩后的消息数组' },
    { value: 'summary', label: '摘要', type: 'object', description: '{ text, covered, seedCovered }' },
    { value: 'facts', label: '便签', type: 'array' },
    { value: 'stats', label: '统计', type: 'object', description: '压缩统计信息' }
  ],
  template: contextManageNode
}

const contextPush: NodeCatalogEntry = {
  type: 'context-push-node',
  label: '推送上下文',
  summary: '把消息/工具结果追加进循环上下文变量（agent 循环中回写工具执行结果）',
  inputs: [
    {
      key: 'items',
      label: '推送项',
      type: 'array',
      default: [],
      description:
        '元素 { variable: 目标上下文变量 [外层循环ID,"context"], mode:"reference"|"custom", ' +
        'reference:[节点ID,字段](mode=reference 时；工具结果引用工具节点的隐藏字段 tool，如 [工具节点ID,"tool"]), ' +
        'content: JSON 字符串(mode=custom 时), role:"system"|"user"|"assistant"|"tool"(工具结果一般用 "user") }'
    }
  ],
  outputs: [],
  template: contextPushNode
}

const fileUpload: NodeCatalogEntry = {
  type: 'file-upload-node',
  label: '文件上传',
  summary: '把工作目录文件上传到文件库，返回下载 URL（产出文件交付给用户的必经通道）',
  inputs: [...toolModeInputs(), triple('path', '文件相对路径', 'string', { required: 'location="customize" 时', default: '' })],
  outputs: [
    { value: 'url', label: '下载地址', type: 'string', description: './api/storage/file/{id}，必须原样使用' },
    { value: 'fileId', label: '文件 id', type: 'string' },
    { value: 'fileName', label: '文件名', type: 'string' },
    { value: 'fileSize', label: '文件大小', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  notes: ['工作流产出的文件必须经本节点（或 file_upload 工具调用）上传后，用返回的 url 交付给用户下载'],
  template: fileUploadNode
}

const fileDownload: NodeCatalogEntry = {
  type: 'file-download-node',
  label: '文件下载',
  summary: '按文件 id 下载文件到工作目录',
  inputs: [
    ...toolModeInputs(),
    triple('fileId', '文件 id', 'string', { required: 'location="customize" 时', default: '' }),
    triple('path', '保存路径', 'string', { default: '' })
  ],
  outputs: [
    { value: 'filePath', label: '本地路径', type: 'string' },
    { value: 'fileName', label: '文件名', type: 'string' },
    { value: 'fileSize', label: '文件大小', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: fileDownloadNode
}

const listSkills: NodeCatalogEntry = {
  type: 'list-skills-node',
  label: '技能列表',
  summary: '列出可用技能',
  inputs: [...toolModeInputs()],
  outputs: [
    {
      value: 'skills',
      label: '技能列表',
      type: 'array',
      description: '元素 { id, name, description, installed 是否已安装, hasUpdate, local 本地路径, parameters }'
    },
    { value: 'summary', label: '摘要', type: 'string' },
    { value: 'skills_count', label: '技能数', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: listSkillsNode
}

const downloadSkills: NodeCatalogEntry = {
  type: 'download-skills-node',
  label: '技能下载',
  summary: '按技能 id 下载安装技能',
  inputs: [...toolModeInputs(), triple('skillId', '技能 id', 'string', { required: 'location="customize" 时', default: '' })],
  outputs: [
    { value: 'skillId', label: '技能ID', type: 'string' },
    { value: 'skillName', label: '技能名称', type: 'string' },
    { value: 'files', label: '文件数', type: 'number' },
    { value: 'status', label: '安装状态', type: 'string' },
    { value: 'local', label: '本地路径', type: 'string' },
    { value: 'content', label: '技能内容', type: 'string' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: downloadSkillsNode
}

const runSkill: NodeCatalogEntry = {
  type: 'run-skill-node',
  label: '技能执行',
  summary: '执行已安装技能的命令（自动注入技能环境变量）',
  inputs: [
    { key: 'runtime', label: '运行时', type: 'string', default: 'local', description: '保持 "local"' },
    ...toolModeInputs(),
    triple('skillId', '技能 id', 'string', { required: 'location="customize" 时', default: '' }),
    triple('command', '执行命令', 'string', { required: 'location="customize" 时', default: '' })
  ],
  outputs: [
    { value: 'result', label: '执行结果', type: 'string' },
    { value: 'stdout', label: '标准输出', type: 'string' },
    { value: 'stderr', label: '错误输出', type: 'string' },
    { value: 'exitCode', label: '退出码', type: 'number' },
    HIDDEN_TOOL_OUTPUT
  ],
  template: runSkillNode
}

// ─────────────────────────── 目录聚合 ───────────────────────────

export const nodeCatalog: Record<string, NodeCatalogEntry> = {
  [aiChat.type]: aiChat,
  [knowledgeSearch.type]: knowledgeSearch,
  [judge.type]: judge,
  [extract.type]: extract,
  [variableAssign.type]: variableAssign,
  [response.type]: response,
  [javaScript.type]: javaScript,
  [terminal.type]: terminal,
  [readFile.type]: readFile,
  [listDir.type]: listDir,
  [glob.type]: glob,
  [grep.type]: grep,
  [createFile.type]: createFile,
  [applyPatch.type]: applyPatch,
  [loop.type]: loop,
  [loopBreak.type]: loopBreak,
  [loopContinue.type]: loopContinue,
  [contextQuery.type]: contextQuery,
  [contextSave.type]: contextSave,
  [contextManage.type]: contextManage,
  [contextPush.type]: contextPush,
  [fileUpload.type]: fileUpload,
  [fileDownload.type]: fileDownload,
  [listSkills.type]: listSkills,
  [downloadSkills.type]: downloadSkills,
  [runSkill.type]: runSkill
}

export const catalogTypes = Object.keys(nodeCatalog)

// 给每个有标准函数的工具节点标注其 tool_call 函数名（与 ai-chat 标准 tools 定义绑定）
for (const [functionName, nodeType] of Object.entries(AGENT_TOOL_NODE_MAP)) {
  const entry = nodeCatalog[nodeType]
  if (entry) {
    entry.notes = [
      ...(entry.notes ?? []),
      `tool_call 模式标准函数名：${functionName}。参数由后端按该函数的标准定义解析，在 ai-chat 的 tools 里必须按标准名引用，禁止改名或自编参数`
    ]
  }
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
  type: string,
  patch: Record<string, any> | undefined
): { patch: Record<string, any>; warnings: string[] } {
  const entry = nodeCatalog[type]
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
export function buildCatalogSummary(): string {
  return Object.values(nodeCatalog)
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
  type: string,
  name: string | undefined,
  nodeDataPatch: Record<string, any> | undefined
): { properties: Record<string, any>; warnings: string[] } {
  const entry = nodeCatalog[type]
  if (!entry) {
    throw new Error(`不支持的节点类型: ${type}`)
  }
  const properties = cloneDeep(entry.template.properties)
  if (name) {
    properties.name = name
  }
  const { patch, warnings } = sanitizeNodeDataPatch(type, nodeDataPatch)
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
export function refreshNodeProperties(type: string, properties: Record<string, any>): void {
  const entry = nodeCatalog[type]
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
