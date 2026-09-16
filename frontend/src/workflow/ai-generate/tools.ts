import { TreeCommonAPI } from '@/api/tree'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import { ROOT_FOLDER_ID } from '@/constants/common'
import { useProvide } from '@/workflow/common/node'
import { generateAnchor } from '@/utils/common'
import { layoutCanvas } from '@/workflow/common/auto-layout'
import { WorkflowType } from '@/workflow/common/data'
import { createAgent } from './create-agent'
import { resolveNode, useAgentConfig, listNodeCatalog, findNodeTemplate } from './common'
import type {
  AgentTool,
  AgentBindings,
  OutputDef,
  NodeUseAgent,
  NodeAgentModule,
  NodeInputSchema
} from './type'

/**
 * ai-generate 的「Agent 工具」（AgentTool 构建器）集合：模型可调用的函数。
 * - 资源发现（模型/知识库/数据库连接池/库结构）与节点字段引用（get_node_field_options 等）——各节点配置子 agent 复用；
 * - 建图基元（list_nodes/add_node/add_edge/configure_node/get_node_anchors/validate/plan）——各画布父 agent 共用，
 *   都带 lf_id 跨画布操作（主画布 "main" / 循环体 = 该循环节点 id）。
 * 纯类型在 type.ts，公共函数在 common.ts，主流程（各画布父 agent 装配）在 impl/。
 */

// ============================================================
// 节点配置 agent 注册表（原 node-agents.ts）
// ============================================================
// 放在 tools.ts 而非 common.ts：这里用 import.meta.glob 急切加载所有节点 content/agent.ts，
// 而那些节点在模块顶层会引用 common 的 TOOL_OUTPUT/deriveFieldList——若 glob 与这些值同处 common，
// 节点会在 common 初始化完成前被加载 → TDZ（Cannot access 'TOOL_OUTPUT' before initialization）。
// tools.ts 不被任何节点在顶层引用（节点只在函数体里用这里的工具），故放这里无环。

/**
 * 扫描每个节点的 content/agent.ts。迁移完成的节点 default 导出 NodeAgentModule（含 type/useAgent/input/outputs）；
 * 未迁移的仍具名导出 useAgent，迁移期二者取其一（default 优先）。key = 文件夹名（= node.type）。
 */
const nodeModules = import.meta.glob<{ default?: NodeAgentModule; useAgent?: NodeUseAgent }>(
  '../nodes/*/content/agent.ts',
  { eager: true }
)

const nodeRegistry: Record<string, NodeAgentModule> = {}
for (const path in nodeModules) {
  const match = path.match(/nodes\/([^/]+)\/content\/agent\.ts$/)
  if (!match) continue
  const type = match[1]
  const m = nodeModules[path]
  if (m?.default) {
    nodeRegistry[type] = m.default
  } else if (m?.useAgent) {
    // 迁移期兜底：未迁移节点只有具名 useAgent，补成最小描述符（input/outputs 待迁移后补全）
    nodeRegistry[type] = { type, useAgent: m.useAgent, input: { type: 'object', properties: {} }, outputs: [] }
  }
}

/** 按 node.type 取该节点的 useAgent（configure_node 分发用）；无则 undefined */
export function nodeUseAgentFor(nodeType: string): NodeUseAgent | undefined {
  return nodeRegistry[nodeType]?.useAgent
}

/**
 * 取某节点的出参签名（已解析）：出参随配置变的节点（extract/loop）传 nodeData 得实时出参，
 * 不传则得基础出参（extract→[]、loop→item/index）。无该类型则空数组。
 */
export function nodeOutputsFor(nodeType: string, nodeData?: any): OutputDef[] {
  const m = nodeRegistry[nodeType]
  if (!m) return []
  return typeof m.outputs === 'function' ? m.outputs(nodeData ?? {}) : m.outputs
}

/** 按 node.type 取该节点的函数签名（入参 schema + 已解析出参），供组合层按数据流接线；无则 undefined */
export function nodeSignatureFor(
  nodeType: string
): { type: string; input: NodeInputSchema; outputs: OutputDef[] } | undefined {
  const m = nodeRegistry[nodeType]
  return m ? { type: m.type, input: m.input, outputs: nodeOutputsFor(nodeType) } : undefined
}

// ============================================================
// 资源发现工具（原 common-tools.ts）
// ============================================================

/**
 * 节点配置子 agent 复用的「资源发现」工具集（模型/知识库/数据库连接池/库结构）。
 * 全是纯 API 调用，不依赖画布上下文（lf/node），故各节点直接把这些常量塞进自己的 tools 即可。
 * apply 只吃模型给的 args（新架构零注入约定，见 type.ts 的 AgentTool）。
 */

/** 列出可用模型资源（供 modelId 选取） */
export const getModelsTool: AgentTool = {
  name: 'get_models',
  description: '列出当前可用的模型资源（id/name/modelName/modelType），配置 modelId 前调用。',
  parameters: { type: 'object', properties: {} },
  apply: async () => {
    const res = await new TreeCommonAPI('model').listResource(ROOT_FOLDER_ID)
    return (res.data ?? []).map((item: any) => ({
      id: item.id,
      name: item.name,
      modelName: item.modelName,
      modelType: item.modelType
    }))
  }
}

/** 列出可用知识库资源（供 knowledgeIds 选取） */
export const getKnowledgeBasesTool: AgentTool = {
  name: 'get_knowledge_bases',
  description: '列出当前可用的知识库资源（id/name），配置 knowledgeIds 前调用。',
  parameters: { type: 'object', properties: {} },
  apply: async () => {
    const res = await new TreeCommonAPI('knowledge').listResource(ROOT_FOLDER_ID)
    return (res.data ?? []).map((item: any) => ({ id: item.id, name: item.name }))
  }
}

/**
 * 列出数据库/缓存连接池资源。
 * provider 区分 SQL 库（mysql/postgresql，供 database-search/insert 的 poolId）与
 * 缓存（redis/local_cache，供 cache-query/write 的 cacheId）；kind 归一供模型判断。
 */
export const getDatabasePoolsTool: AgentTool = {
  name: 'get_database_pools',
  description:
    '列出当前可用的数据库/缓存连接池资源。kind="sql" 的连接才用于 database-search/insert 的 poolId、' +
    '才支持 get_database_tables/columns；kind="cache" 的连接用于 cache-query/write 的 cacheId。',
  parameters: { type: 'object', properties: {} },
  apply: async () => {
    const res = await new TreeCommonAPI('datasource').listResource(ROOT_FOLDER_ID)
    return (res.data ?? []).map((item: any) => {
      const provider = item.provider ?? ''
      return {
        id: item.id,
        name: item.name,
        provider,
        kind: provider === 'redis' || provider === 'local_cache' ? 'cache' : 'sql'
      }
    })
  }
}

/** 查某 SQL 连接池的表名列表 */
export const getDatabaseTablesTool: AgentTool = {
  name: 'get_database_tables',
  description: '查询指定 SQL 数据库连接池下的所有表名。',
  parameters: {
    type: 'object',
    properties: {
      poolId: {
        type: 'string',
        description: 'SQL 数据库连接池 id（get_database_pools 中 kind="sql" 的项）'
      }
    },
    required: ['poolId']
  },
  apply: async (args) => {
    if (!args.poolId) throw new Error('poolId 不能为空')
    const res = await databaseConnectionPoolAPI.getTables(args.poolId)
    return res.data ?? []
  }
}

/** 查某 SQL 连接池某表的列信息 */
export const getDatabaseColumnsTool: AgentTool = {
  name: 'get_database_columns',
  description: '查询指定 SQL 数据库连接池某张表的列信息（列名/类型）。',
  parameters: {
    type: 'object',
    properties: {
      poolId: {
        type: 'string',
        description: 'SQL 数据库连接池 id（get_database_pools 中 kind="sql" 的项）'
      },
      table: { type: 'string', description: '表名（get_database_tables 获取）' }
    },
    required: ['poolId', 'table']
  },
  apply: async (args) => {
    if (!args.poolId) throw new Error('poolId 不能为空')
    const res = await databaseConnectionPoolAPI.getColumns(args.poolId, args.table)
    return res.data ?? []
  }
}

/** SQL 类节点常用的数据库资源查询三件套（连接池 → 表 → 列） */
export const PROCESSOR_DB_TOOLS: AgentTool[] = [
  getDatabasePoolsTool,
  getDatabaseTablesTool,
  getDatabaseColumnsTool
]

// ============================================================
// 节点字段引用工具（原 common-tools.ts）
// ============================================================

/** 取本子 agent 绑定的目标节点 model（position 末段即 nodeId），与各节点 update/validate 同一取法 */
function boundNode(b: AgentBindings): any {
  return resolveNode(b.lf, b.position?.[b.position.length - 1] ?? '')
}

/**
 * 查本节点可引用的上游输出字段（含 global）。给 [节点ID,字段] 形式的引用（reference/上下文来源/附件等）用。
 * 与节点表单同源——走 useProvide(node)（common/node.ts），返回与人工在表单里看到一致的上游选项。
 * 用法同 getCanvasDetailTool 等：各节点 tools 里写 getNodeFieldOptionsTool(bindings)。
 */
export function getNodeFieldOptionsTool(b: AgentBindings): AgentTool {
  return {
    name: 'get_node_field_options',
    description:
      '列出本节点可引用的上游输出字段（含 global）。写 [节点ID,字段] 形式的上游引用前先调用，取真实节点 id 与字段，勿臆造；返回项 { nodeId, nodeName, fields:[{value,label}] }。',
    parameters: { type: 'object', properties: {} },
    apply: () => {
      const node = boundNode(b)
      if (!node) return { error: '当前节点未绑定，无法查询上游字段' }
      // useProvide(node).getNodeFieldOptions() 返回 cascader 选项树：分组 { value:节点ID, label:节点名, children:[{value:字段,label}] }
      const groups = useProvide(node).getNodeFieldOptions() as any[]
      return groups.map((g) => ({
        nodeId: g.value,
        nodeName: g.label,
        fields: (g.children ?? []).map((c: any) => ({ value: c.value, label: c.label }))
      }))
    }
  }
}

/**
 * 取本节点可引用的上游选项分组（与 get_node_field_options 同源）：[{ nodeId, fields:[字段value...] }]。
 * 供各节点在写 [节点ID,字段] 引用前做「引用硬校验」，堵住臆造/引用未声明变量。
 */
export function nodeFieldOptionGroups(b: AgentBindings): Array<{ nodeId: string; fields: string[] }> {
  const node = boundNode(b)
  if (!node) return []
  const groups = (useProvide(node).getNodeFieldOptions() as any[]) ?? []
  return groups.map((g) => ({
    nodeId: g.value,
    fields: (g.children ?? []).map((c: any) => c.value)
  }))
}

/**
 * 校验一个 [节点ID, 字段] 引用是否落在真实可选项内（与 get_node_field_options 同源）。
 * 通过返回 null；不通过返回一句给模型看的错误（并提示"可能是循环/全局变量尚未声明"）。
 * 非数组/不足两段（未填/半填）不在此拦，交给各自 required 校验；'global' 组随声明动态变化，暂不硬拦。
 */
export function validateRef(b: AgentBindings, ref: any, label = '引用'): string | null {
  if (!Array.isArray(ref) || ref.length < 2) return null
  const [nid, field] = ref
  if (nid === 'global') return null
  const groups = nodeFieldOptionGroups(b)
  const g = groups.find((o) => o.nodeId === nid)
  if (!g) {
    return (
      `${label} 的节点 id "${nid}" 不在可引用范围（get_node_field_options 里没有它）。` +
      '若它本应是某循环/开始节点上的变量，说明该变量还没声明——按 plan 的变量契约先在对应节点声明，再引用；切勿臆造。'
    )
  }
  if (!g.fields.includes(field)) {
    return (
      `${label} 里节点 "${nid}" 没有字段 "${field}"（可用字段：${g.fields.join(', ') || '无'}）。` +
      '请用 get_node_field_options 里的真实字段；若该字段本应是尚未声明的循环/全局变量，先声明再引用。'
    )
  }
  return null
}

// ============================================================
// 全景工具 get_canvas_detail（原 common-tools.ts）
// ============================================================

/** 全景图里字符串内容的预览长度：长内容（prompt/code/description 等）只留个意思，省 token */
const CANVAS_STR_PREVIEW = 20

/**
 * 递归裁剪：把「对象字段」里的长字符串截成 CANVAS_STR_PREVIEW 字的预览（供全景图省 token）。
 * 关键例外——【基元数组】原样保留：引用是 [节点id, 字段] 这样的字符串元组，截断会毁掉 id / 字段名，
 * 下游就没法照它引用了。故只截对象里的内容字符串；含对象的数组逐个递归，基元数组整体不动。
 * 纯读用途、深拷贝产出，不碰真实 nodeData。
 */
function trimStrings(v: any): any {
  if (typeof v === 'string') return v.length > CANVAS_STR_PREVIEW ? v.slice(0, CANVAS_STR_PREVIEW) + '…' : v
  if (Array.isArray(v)) {
    // 引用元组等基元数组原样保留；元素是对象才递归（如 ai-chat 的 tools、judge 的 branches）
    return v.map((x) => (x && typeof x === 'object' ? trimStrings(x) : x))
  }
  if (v && typeof v === 'object') {
    const out: Record<string, any> = {}
    for (const k in v) out[k] = trimStrings(v[k])
    return out
  }
  return v
}

/**
 * 把一个节点压成给全景图用的形态：只留 id / type / nodeData。
 * 坐标/样式/pointsList 等渲染字段在 node 顶层、不在 nodeData 里，故天然被排除；
 * 只额外剥掉 nodeData.children——循环体子画布由 walk 递归单独展开成 loopBodies，留在这里会重复膨胀。
 * 其余 nodeData 结构原样（含 loopVariables、ai-chat 的 tools、judge 的 branches 等，不漏字段），
 * 但对象里的长字符串按 trimStrings 截成预览（引用元组的 id/字段不截）。
 */
function plainNode(n: any): Record<string, any> {
  const { children, ...nodeData } = (n.properties?.nodeData ?? {}) as Record<string, any>
  return { id: n.id, type: n.type, nodeData: trimStrings(nodeData) }
}

/**
 * 「全景」工具：跨所有循环层，把整张工作流吐成 { id, type, nodeData } 的节点树 + 连线，并标出本子 agent
 * 当前所在层（youAreHere）。从根画布（rootLf）往下递归，对每个 loop-node 用 getLf(loopId) 展开其循环体
 * 子画布（getLf 由主画布透传，能解析任意深度 id）。节点直接给近乎完整的 nodeData（只剥渲染字段与 children），
 * 不再按类型挑字段，避免漏字段；故设 resultLimit=Infinity 不截断（否则全量 nodeData 会被默认上限截掉）。
 */
export function getCanvasDetailTool(b: AgentBindings): AgentTool {
  return {
    name: 'get_canvas_detail',
    // 全量 nodeData 可能较大：不截断，避免模型看到 …(truncated) 拿到残缺画布
    resultLimit: Infinity,
    description:
      '查看整张工作流的结构化详情（含所有循环层）：每层的 nodes（每个节点 { id, type, nodeData }——nodeData 是该节点近乎完整的配置数据，' +
      '含 ai-chat 的 tools、judge 的 branches、loop 的 loopType/loopVariable/loopVariables、工具节点的 location/reference 等，只去掉了坐标样式与循环体 children）' +
      '与 edges（连线 from→to，及 source_anchor_id＝源出口的完整锚点，内含走的分支与 success/fail）；并用 youAreHere 标出你当前所在的循环层。' +
      '配置引用/工具名/循环变量、或校验连线是否正确前先调它，只用这里的真实值，勿凭空编造节点 id、字段或工具名。',
    parameters: { type: 'object', properties: {} },
    apply: async () => {
      const rootLf = b.rootLf ?? b.lf
      const getLf = rootLf?.graphModel?.getLf ?? b.lf?.graphModel?.getLf
      const walk = async (lf: any, loopId: string | null, depth: number): Promise<any> => {
        const g = lf?.graphModel
        if (!g) return { loopId, depth, nodes: [], edges: [] }
        const nodes = (g.nodes ?? []).map(plainNode)
        // 连线：from→to，source_anchor_id 直接给完整出口锚点（内含分支与 success/fail，普通边即 ..._right_main_success）
        const edges = (g.edges ?? []).map((e: any) => ({
          from: e.sourceNodeId,
          to: e.targetNodeId,
          source_anchor_id: e.sourceAnchorId
        }))
        const loopBodies: Record<string, any> = {}
        if (typeof getLf === 'function' && depth < 6) {
          for (const lp of (g.nodes ?? []).filter((n: any) => n.type === 'loop-node')) {
            try {
              const sub = await getLf(lp.id)
              if (sub) loopBodies[lp.id] = await walk(sub, lp.id, depth + 1)
            } catch {
              /* 子画布未就绪就跳过该层，不阻断全景 */
            }
          }
        }
        return { loopId, depth, nodes, edges, loopBodies }
      }
      const tree = await walk(rootLf, null, 0)
      return {
        tree,
        youAreHere: b.loopContext
          ? { loopId: b.loopContext.selfLoopId, depth: b.loopContext.depth, outerLoopId: b.loopContext.outerLoopId }
          : '主画布（不在任何循环内）'
      }
    }
  }
}

/**
 * 查可插入提示词/模板文本的变量。给 :::variable {value="节点ID.字段" label="显示名"}::: 指令用。
 * 与 getNodeFieldOptionsTool 同源（同一上游选项树），此处扁平成指令要的「节点ID.字段」点号串。
 */
export function getTemplateVariablesTool(b: AgentBindings): AgentTool {
  return {
    name: 'get_template_variables',
    description:
      '列出可插入提示词/模板文本的变量。写 :::variable {value="节点ID.字段" label="显示名"}::: 前先调用；返回项 { value:"节点ID.字段", label:"节点名 / 字段名" }，value 直接填入指令。',
    parameters: { type: 'object', properties: {} },
    apply: () => {
      const node = boundNode(b)
      if (!node) return { error: '当前节点未绑定，无法查询模板变量' }
      const groups = useProvide(node).getTemplateVariables() as any[]
      const flat: Array<{ value: string; label: string }> = []
      for (const g of groups) {
        for (const c of g.children ?? []) {
          flat.push({ value: `${g.value}.${c.value}`, label: `${g.label} / ${c.label}` })
        }
      }
      return flat
    }
  }
}

// ============================================================
// 跨画布定位辅助（原 workflow-agent/shared.ts）
// ============================================================

/** 本画布类型 → 循环体子画布类型（LOOP 变体；已是 LOOP 则不变，幂等，供任意深度子画布） */
export function toLoopVariant(workflowType: string): string {
  switch (workflowType) {
    case WorkflowType.PROCESSOR:
    case WorkflowType.PROCESSOR_LOOP:
      return WorkflowType.PROCESSOR_LOOP
    case WorkflowType.TOOL:
    case WorkflowType.TOOL_LOOP:
      return WorkflowType.TOOL_LOOP
    default:
      return WorkflowType.APPLICATION_LOOP
  }
}

/**
 * 取某循环节点的循环体子画布 lf：用根 lf 的 getLf 展开（逐层展开祖先循环、等渲染完成，任意深度可解析）。
 */
export async function getSubLf(b: AgentBindings, loopId: string): Promise<any> {
  const root = b.rootLf ?? b.lf
  const getLf = root?.graphModel?.getLf
  if (typeof getLf !== 'function') throw new Error('当前宿主不支持子画布展开（缺 getLf）')
  const sub = await getLf(loopId)
  if (!sub) throw new Error(`子画布未就绪: ${loopId}`)
  return sub
}

/** 深度封顶（与 get_canvas_detail 一致）：防子画布环/异常撑爆递归 */
const MAX_CANVAS_DEPTH = 6

/**
 * 按 lf_id 解析目标画布 lf：空 / "main" → 主画布（rootLf）；否则 lf_id = 某 loop 节点 id → 取它的循环体子画布。
 * 一个 lf_id 唯一标识一张画布：主画布，或"某层循环的循环体"。跨画布建图基元（add_node/add_edge/…）据此选画布。
 */
export async function lfOf(b: AgentBindings, lfId?: string): Promise<{ lf?: any; error?: string }> {
  const root = b.rootLf ?? b.lf
  if (!lfId || lfId === 'main') return { lf: root }
  try {
    return { lf: await getSubLf(b, lfId) }
  } catch (e: any) {
    return { error: e?.message ?? `子画布未就绪: ${lfId}（lf_id 应为主画布传 "main"、循环体传该循环节点 id）` }
  }
}

/**
 * 按 lf_id 取目标画布 + 该画布的 workflowType（循环体走 LOOP 变体，主画布用父类型）。
 * add_node 找节点模板、configure_node 起子 agent 都要按对画布类型选节点变体（循环体才有 loop-break/continue）。
 */
export async function resolveCanvas(
  b: AgentBindings,
  lfId?: string
): Promise<{ lf?: any; workflowType?: string; error?: string }> {
  const { lf, error } = await lfOf(b, lfId)
  if (error) return { error }
  const isBody = !!lfId && lfId !== 'main'
  return { lf, workflowType: isBody ? toLoopVariant(b.workflowType) : b.workflowType }
}

/**
 * 在整棵画布树（主画布 + 各层循环体，深度封顶）里按 id 找到节点所在的 lf；找不到返回 null。
 * 节点 id 全局唯一，故 configure_node/get_node_anchors 即便 lf_id 传错/漏传，也能据此兜底定位到正确画布。
 */
export async function findLfOfNode(b: AgentBindings, nodeId: string): Promise<any | null> {
  const root = b.rootLf ?? b.lf
  const getLf = root?.graphModel?.getLf
  const walk = async (lf: any, depth: number): Promise<any | null> => {
    if (!lf?.graphModel) return null
    if (resolveNode(lf, nodeId)) return lf
    if (typeof getLf === 'function' && depth < MAX_CANVAS_DEPTH) {
      for (const lp of (lf.graphModel.nodes ?? []).filter((n: any) => n.type === 'loop-node')) {
        try {
          const sub = await getLf(lp.id)
          if (sub) {
            const hit = await walk(sub, depth + 1)
            if (hit) return hit
          }
        } catch {
          /* 子画布未就绪就跳过 */
        }
      }
    }
    return null
  }
  return walk(root, 0)
}

/**
 * 由 lf_id 推出「该画布」的循环层上下文 loopContext：主画布 → undefined；某循环体 → { depth, selfLoopId, outerLoopId }。
 * 供 configure_node 给循环体内节点的配置 agent 注入——工具节点据 selfLoopId 写 reference=[selfLoopId,"item"]。
 * 从根往下走一遍循环嵌套：最外层循环体 depth=0，逐层 +1；outerLoopId = 承载本循环节点的那层循环（主画布上则空）。
 */
export async function deriveLoopContext(
  b: AgentBindings,
  lfId?: string
): Promise<AgentBindings['loopContext']> {
  if (!lfId || lfId === 'main') return undefined
  const root = b.rootLf ?? b.lf
  const getLf = root?.graphModel?.getLf
  const map = new Map<string, { depth: number; selfLoopId: string; outerLoopId?: string }>()
  const walk = async (lf: any, canvasLoopId: string | undefined, canvasDepth: number) => {
    if (!lf?.graphModel) return
    for (const lp of (lf.graphModel.nodes ?? []).filter((n: any) => n.type === 'loop-node')) {
      map.set(lp.id, { depth: canvasDepth + 1, selfLoopId: lp.id, outerLoopId: canvasLoopId })
      if (typeof getLf === 'function' && canvasDepth + 1 < MAX_CANVAS_DEPTH) {
        try {
          const sub = await getLf(lp.id)
          if (sub) await walk(sub, lp.id, canvasDepth + 1)
        } catch {
          /* 子画布未就绪就跳过 */
        }
      }
    }
  }
  await walk(root, undefined, -1)
  return map.get(lfId)
}

// ============================================================
// 建边 / 出口锚点辅助（原 workflow-agent/shared.ts）
// ============================================================

/**
 * 组合源出口锚点：已是完整锚点串（以 `${sourceId}_` 开头）则原样用；否则把入参当分支段
 * （缺省 main）拼成 `${sourceId}_right_<branch>_success`。buildEdge 内部用；上层锚点已由 resolveSourcePort 解析好。
 */
function sourceAnchorOf(sourceId: string, token?: string): string {
  const v = token || 'main'
  if (v.startsWith(`${sourceId}_`)) return v
  return generateAnchor(sourceId, 'right', v, 'success')
}

/** 建边参数：源出口锚点缺省用主输出口，目标入口固定左主口——与菜单拖线（common/node.ts appendNode）一致 */
function buildEdge(sourceId: string, targetId: string, sourceAnchorId?: string) {
  return {
    sourceNodeId: sourceId,
    sourceAnchorId: sourceAnchorOf(sourceId, sourceAnchorId),
    targetNodeId: targetId,
    targetAnchorId: generateAnchor(targetId, 'left', 'main', 'success')
  }
}

/** 是否已存在同源锚→同目标的边（去重用，避免 add_node 建边后模型再 add_edge 造重复边） */
function edgeExists(lf: any, sourceId: string, sourceAnchorId: string, targetId: string): boolean {
  return (lf.graphModel?.edges ?? []).some(
    (e: any) =>
      e.sourceNodeId === sourceId &&
      e.targetNodeId === targetId &&
      e.sourceAnchorId === sourceAnchorId
  )
}

/** 取节点右侧所有输出口（正常 success 各分支 + 异常捕获 fail）；锚点由 getDefaultAnchor 按当前属性多态算出 */
function outputAnchorsOf(node: any): any[] {
  const anchors = (node.anchors ?? node.getDefaultAnchor?.() ?? []) as any[]
  return anchors.filter((a) => a.direction === 'right')
}

/**
 * 解析「上游出口」入参 source_anchor_id：一个值既定源节点、又定具体出口。接受两种写法——
 * - 节点 id：走该节点主干输出（main/success）；普通链式连接最常用，模型手里就有 id，无需拼锚点；
 * - 完整锚点（get_node_anchors 给的 source_anchor_id）：judge 某分支 / 异常输出等具体出口，含源节点。
 * 因完整锚点串本身已含源节点 id，故不再单收 source_node_id（消除冗余与「节点/锚点对不上」的歧义）。
 * 返回 { source: 源节点 model, anchor: 完整锚点 } 或 error（含可用出口指引）。
 */
function resolveSourcePort(lf: any, value: string): { source: any; anchor: string } | {
  error: string
} {
  if (!value) {
    return {
      error:
        'source_anchor_id 不能为空：接某节点的正常输出口就传该上游节点 id；接 judge 分支或异常输出口传 get_node_anchors 给的 source_anchor_id。'
    }
  }
  // 判定是完整锚点串还是裸 node_id：锚点 = id_direction_branch_status，
  // node id 无下划线（uuid/连字符），故 parts[0] 即 node_id；方向段∈枚举、末段∈{success,fail} 时才算锚点。
  const parts = value.split('_')
  const isAnchor =
    parts.length >= 4 &&
    ['left', 'right', 'top', 'bottom'].includes(parts[1]) &&
    ['success', 'fail'].includes(parts[parts.length - 1])
  const nodeId = isAnchor ? parts[0] : value
  const source = resolveNode(lf, nodeId)
  if (!source) {
    return {
      error:
        `源节点不存在: ${nodeId}${isAnchor ? `（从锚点 ${value} 解构得到）` : ''}。用 get_canvas_detail 查节点、get_node_anchors 查出口。`
    }
  }
  // 裸 node_id → 主干输出；完整锚点 → 原样
  const anchor = isAnchor ? value : generateAnchor(nodeId, 'right', 'main', 'success')
  // 校验该出口确为此节点真实输出口（挡下 judge 传 node_id 走主干、拼错锚点、分支未配等）——只查这一个节点，非扫描
  if (!outputAnchorsOf(source).some((a) => a.id === anchor)) {
    const list = outputAnchorsOf(source).map((a) => a.id).join(', ') || '（无输出口——多为 judge 只有分支出口，或尚未配置）'
    return {
      error:
        `节点 ${nodeId} 没有出口 "${anchor}"。请先 get_node_anchors(${nodeId}) 取目标出口的 source_anchor_id 原样传入。可用出口：${list}。`
    }
  }
  return { source, anchor }
}

// ============================================================
// 建图基元与委派工具（原 workflow-agent/shared.ts）
// ============================================================

/**
 * 从父 agent 的活历史（b.messages）裁出可交给子 agent 继承的「初始历史」快照。
 * 子 agent 以它作初始历史，从而看到父至此的全部推理/工具往来，直接明白自己为何被建、该配成什么样，
 * 不必从画布重新猜。这里只管一件事——裁掉「当前在途那轮」尚未回填完的 tool_calls：
 *   此刻父刚把带 tool_calls 的 assistant 压进历史、但本次委派对应的 tool 结果还没回填，直接继承会得到
 *   悬空 tool_calls（下一轮 400）。故剥掉尾部这轮已产出的 tool 结果、并把尾部那条 assistant 的 tool_calls
 *   去掉、只保留其正文（父此刻决定委派前的意图说明）。
 * system 不在这里管：父的 system 原样留在结果里也无妨，子的 useAgentConfig 会统一滤掉所有 system、
 * 再放子自己的那条（system 的唯一负责人在 useAgentConfig）。
 * 委派工具在 apply 一进来就同步快照（同轮工具顺序 await，快照期间 b.messages 不会再长）。
 */
export function inheritMessages(b: AgentBindings): any[] {
  const out = Array.isArray(b.messages) ? b.messages : []
  // 剥掉当前在途那轮已产出的 tool 结果（尾部连续的 tool 消息只可能属于尚未回填完的这轮）
  let end = out.length
  while (end > 0 && out[end - 1]?.role === 'tool') end--
  const trimmed = out.slice(0, end)
  // 尾部若是带 tool_calls 的 assistant（即触发本次委派的那条），去掉 tool_calls 只留正文；正文为空则整条丢弃
  const last = trimmed[trimmed.length - 1]
  if (last && last.role === 'assistant' && last.tool_calls) {
    if (last.content) trimmed[trimmed.length - 1] = { role: 'assistant', content: last.content }
    else trimmed.pop()
  }
  return trimmed
}

/**
 * 规划工具：模型列出/更新搭建步骤清单，供用户跟踪进度。
 * 无副作用——plan 本身不改画布，只把清单作为工具结果回传；消费方从事件流里读 args/结果渲染进度条。
 */
export function planTool(_b: AgentBindings): AgentTool {
  return {
    name: 'plan',
    description:
      '列出/更新你的搭建计划（步骤清单）。开工前先规划一遍，之后每推进一步就再调一次更新各步状态，便于用户跟踪进度。',
    parameters: {
      type: 'object',
      properties: {
        steps: {
          type: 'array',
          description:
            '步骤清单（全量覆盖），元素 { title 步骤简述, status:"pending"|"doing"|"done"（缺省 pending） }',
          items: {
            type: 'object',
            properties: {
              title: { type: 'string', description: '步骤简述' },
              status: {
                type: 'string',
                enum: ['pending', 'doing', 'done'],
                description: '步骤状态'
              }
            },
            required: ['title']
          }
        }
      },
      required: ['steps']
    },
    apply: (args) => {
      const steps = Array.isArray(args.steps) ? args.steps : []
      return {
        ok: true,
        steps: steps.map((s: any) => ({ title: s?.title ?? '', status: s?.status ?? 'pending' }))
      }
    }
  }
}

/**
 * 列出当前画布可用的节点类型（按分组），每个节点给出完整「函数签名」：
 * type/名称 + input（配置该节点的字段/类型/枚举）+ outputs（出参，含 type、enum、数组元素 items、对象字段 fields）。
 * 模型据此按数据流选型/接线：把上游 outputs 接到下游 input、按 outputs 的 value 写 [节点ID,字段] 引用。
 */
export function listNodesTool(b: AgentBindings): AgentTool {
  return {
    name: 'list_nodes',
    // 全量签名目录较大：不截断，避免模型看到 …(truncated) 反复重调（静态、每次只拉一次，token 一次性成本可接受）
    resultLimit: Infinity,
    description:
      '列出当前画布可用的节点类型（按分组）。每个节点含 type、名称、input（配置字段/类型/枚举）与 outputs（出参，带 type、enum、数组元素 items、对象字段 fields）——即节点的函数签名。规划/新增/接线前先调用它，按真实签名把上游出参接到下游入参、按 outputs 的 value 写引用；add_node 的 type、引用的字段都只能取这里返回的值，勿编造。',
    parameters: { type: 'object', properties: {} },
    apply: () => ({
      groups: listNodeCatalog(b.workflowType).map((g) => ({
        group: g.group,
        nodes: g.nodes.map((n) => {
          const sig = nodeSignatureFor(n.type)
          return {
            type: n.type,
            name: n.name,
            input: sig?.input ?? { type: 'object', properties: {} },
            outputs: sig?.outputs ?? []
          }
        })
      }))
    })
  }
}

/**
 * 查某节点可作为连线起点的「所有输出口」，每个给一个拿来即用的完整 source_anchor_id 与人话 label：
 * 正常输出（main/success）、judge 各分支（branch/success，含条件摘要）、异常输出（main/fail，仅开启异常捕获时有）。
 * 锚点由节点模型的 getDefaultAnchor() 多态算出（基类 common/node.ts + judge 覆写），本函数只读、零节点特判；
 * 模型不再拼锚点——挑目标行、把它的 source_anchor_id 原样填进 add_node/add_edge 即可。
 */
export function getNodeAnchorsTool(b: AgentBindings): AgentTool {
  return {
    name: 'get_node_anchors',
    description:
      '查看某节点可作连线起点的所有输出口——正常输出、judge 各分支（含条件摘要）、异常输出（开启异常捕获时）。' +
      '接正常输出口以外的出口（judge 判断分支、异常兜底）前调用它，挑目标行、把它的 source_anchor_id 原样填进 add_node/add_edge。' +
      'lf_id 指该节点所在画布：主画布传 "main"（或省略），循环体里的节点传所在循环节点的 id。',
    parameters: {
      type: 'object',
      properties: {
        nodeId: { type: 'string', description: '要查锚点的节点 id' },
        lf_id: { type: 'string', description: '该节点所在画布：主画布 "main"（或省略）；循环体内节点传所在循环节点 id' }
      },
      required: ['nodeId']
    },
    apply: async (args) => {
      const { lf, error } = await lfOf(b, args.lf_id)
      if (error) return { error }
      // 节点 id 全局唯一：lf_id 传错/漏传时，据 id 全树兜底定位到正确画布
      const targetLf = resolveNode(lf, args.nodeId) ? lf : (await findLfOfNode(b, args.nodeId)) ?? lf
      const node = resolveNode(targetLf, args.nodeId)
      if (!node) return { error: `节点不存在: ${args.nodeId}` }
      const outputs = outputAnchorsOf(node)
      const branches = node.properties?.nodeData?.branches
      const labelOf = (a: any): string => {
        // 异常捕获出口：节点执行失败时走这里
        if (a.status === 'fail') return '异常输出（该节点执行失败时走这条边兜底）'
        // judge 分支出口：拼出条件摘要
        if (Array.isArray(branches)) {
          const br = branches.find((x: any) => x.id === a.branch)
          if (br) {
            if (br.type === 'else') return '分支: 否则(else)'
            const conds = (br.conditions ?? [])
              .map((c: any) =>
                `${(c.variable ?? []).join('.')} ${c.compare ?? ''} ${c.value ?? ''}`.trim()
              )
              .join(` ${br.logic ?? 'and'} `)
            return `分支: ${br.type} ${conds}`.trim()
          }
        }
        return '正常输出'
      }
      return {
        // source_anchor_id = 完整锚点，原样填进 add_node/add_edge 的 source_anchor_id
        anchors: outputs.map((a) => ({
          source_anchor_id: a.id,
          status: a.status,
          label: labelOf(a)
        }))
      }
    }
  }
}

/** 新增节点并从 source_anchor_id 指定的上游出口接入一条入边（建节点即接上游），返回其 id */
export function addNodeTool(b: AgentBindings): AgentTool {
  return {
    name: 'add_node',
    description:
      '新增一个节点，并从上游出口接一条入边（建节点即接上游，杜绝孤立节点），返回新节点 id。' +
      'source_anchor_id 指定这条入边从上游哪个出口引出：接某节点的正常输出口——直接传该上游节点 id 即可（普通链式连接的常态）；接 judge 的某个分支、或某节点的异常输出口——先调 get_node_anchors(上游id) 取该出口的 source_anchor_id 原样传入（它已含源节点，无需再单给节点 id）。' +
      'lf_id 指往哪张画布建：主画布传 "main"（或省略）；往某个循环的循环体里建，传该循环节点 id——循环体首节点把 source_anchor_id 填 "loop-start-node"。',
    parameters: {
      type: 'object',
      properties: {
        type: { type: 'string', description: '节点类型' },
        source_anchor_id: {
          type: 'string',
          description:
            '这条入边从上游哪个出口引出：接某节点的正常输出口就直接传该节点 id；接 judge 的某个分支或某节点的异常输出口，传 get_node_anchors 给的该出口 source_anchor_id；往循环体建的首节点传 "loop-start-node"'
        },
        lf_id: {
          type: 'string',
          description: '往哪张画布建：主画布 "main"（或省略）；往某循环体建传该循环节点 id'
        }
      },
      required: ['type', 'source_anchor_id']
    },
    apply: async (args) => {
      const { lf: targetLf, workflowType, error } = await resolveCanvas(b, args.lf_id)
      if (error) return { error }
      // 照菜单点选路径（common/node.ts 的 appendNode）建节点：用模板 properties，找不到＝当前画布不支持
      const template = findNodeTemplate(workflowType!, args.type)
      if (!template) {
        return { error: `当前画布不支持的节点类型: ${args.type}（用 list_nodes 查看可用类型）` }
      }
      // 从 source_anchor_id 一并解析出源节点与完整出口锚点（节点 id→主干；完整锚点→具体出口）
      const resolved = resolveSourcePort(targetLf, args.source_anchor_id)
      if ('error' in resolved) return { error: resolved.error }
      const source = resolved.source

      const node = targetLf.addNode({
        type: args.type,
        properties: template.properties,
        x: (source.x ?? 0) + (template.properties?.width ?? 200) / 2 + 200,
        y: source.y ?? 0
      })
      targetLf.addEdge(buildEdge(source.id, node.id, resolved.anchor))
      layoutCanvas(targetLf)
      return { id: node?.id ?? null }
    }
  }
}

/** 连一条边：从 source_anchor_id 指定的上游出口 → target */
export function addEdgeTool(b: AgentBindings): AgentTool {
  return {
    name: 'add_edge',
    description:
      '连一条边，把上游某出口接到 target 节点（补合流/回连等额外入边；建节点时的入边 add_node 已自动接好，勿重复连）。' +
      'source_anchor_id 指定这条边从上游哪个出口引出：接某节点的正常输出口——直接传该节点 id 即可；接 judge 的某个分支、或某节点的异常输出口——先调 get_node_anchors 取该出口的 source_anchor_id 原样传入（它已含源节点）。' +
      'lf_id 指这条边所在画布（源与目标必须同画布）：主画布传 "main"（或省略）；循环体内传该循环节点 id。',
    parameters: {
      type: 'object',
      properties: {
        source_anchor_id: {
          type: 'string',
          description:
            '这条边从上游哪个出口引出：接某节点的正常输出口就直接传该节点 id；接 judge 的某个分支或某节点的异常输出口，传 get_node_anchors 给的该出口 source_anchor_id'
        },
        target: { type: 'string', description: '终点节点 id' },
        lf_id: { type: 'string', description: '这条边所在画布：主画布 "main"（或省略）；循环体内传该循环节点 id' }
      },
      required: ['source_anchor_id', 'target']
    },
    apply: async (args) => {
      const { lf, error } = await lfOf(b, args.lf_id)
      if (error) return { error }
      // 源/目标同画布：lf_id 传错/漏传时据 target 全树兜底定位
      const targetLf = resolveNode(lf, args.target) ? lf : (await findLfOfNode(b, args.target)) ?? lf
      if (!resolveNode(targetLf, args.target)) return { error: `终点节点不存在: ${args.target}` }
      // 从 source_anchor_id 一并解析出源节点与完整出口锚点（节点 id→主干；完整锚点→具体出口）
      const resolved = resolveSourcePort(targetLf, args.source_anchor_id)
      if ('error' in resolved) return { error: resolved.error }
      const source = resolved.source
      // 去重报错：同源锚→同目标已存在就报错（模型常在 add_node 建过边后又 add_edge 一次，造重复连线）
      if (edgeExists(targetLf, source.id, resolved.anchor, args.target)) {
        return {
          error:
            `该边已存在（${source.id} 的同一出口 → ${args.target}），未重复创建。` +
            'add_node 时已自动接了上游边，无需再 add_edge 连同一条；请勿重复连线。'
        }
      }
      targetLf.addEdge(buildEdge(source.id, args.target, resolved.anchor))
      return { ok: true }
    }
  }
}

/**
 * 所有节点配置 agent 的统一「开工指令」基线：子 agent 是本节点类型的专家，结合整体目标(已折进 system)+画布结构配置本节点。
 * 免得把同一段话抄进每个节点 prompt；节点自己的 prompt 只留字段级的具体规则。
 */
const CONFIGURE_NODE_BASE =
  '配置你绑定的这个节点：结合上面的整体目标判断它在这条链路里该干什么（父 agent 已把节点建好、接好上下游边，设计意图都在图里），' +
  '用 get_node_field_options 看清可引用的上游真实输出 [节点id,字段]，再用本节点的 update 工具把字段配好落库。' +
  'update 会自动校验：通过即代表配置完成、你的任务到此结束，无需再做任何事（不必再调别的工具、也不必额外总结）；' +
  '若 update 返回 errors（字段→错误消息）就按提示改对应字段再次 update，直到通过。只配这个节点，别碰别的节点。'

/** input 是否为空（未给/空对象）——空则回退到基线指令，由节点 agent 结合画布自行判断字段 */
function isEmptyInput(input: any): boolean {
  return (
    input === undefined ||
    input === null ||
    (typeof input === 'object' && !Array.isArray(input) && Object.keys(input).length === 0)
  )
}

/**
 * 拼「开工指令」：父 agent 给了 input（本节点各字段的意图/值）就据它配。
 * 关键约定——需要生成内容的字段（如 js 节点的 code、脚本、提示词）：input 里给的是「要生成什么」的描述，
 * 不是最终内容；子 agent 据描述生成真实内容再落库（父只表意图，内容由节点专家产）。
 */
function configureTask(input?: any): string {
  if (isEmptyInput(input)) return CONFIGURE_NODE_BASE
  return (
    `${CONFIGURE_NODE_BASE}\n\n父 agent 给出的本节点配置意图（input，按本节点 input 结构）：\n${JSON.stringify(input)}\n` +
    '据此配置：用本节点的工具把这些字段落库；引用类字段用 get_node_field_options 取真实 [节点id,字段] 对上。' +
    '注意 input 只表意图，两类字段父 agent 不会给最终值、由你负责定：' +
    '① 需要生成内容的字段（code、脚本、提示词等）——input 给的是「要生成什么」的描述，你据描述生成真实内容再落库；' +
    '② 需要选具体资源的字段（模型 model_id、知识库 knowledgeIds、数据库/缓存连接池 poolId/cacheId 等）——input 至多给个意图（如「用对话模型」），你用本节点的资源查询工具（get_models / get_knowledge_bases / get_database_pools 等）查出真实 id 再填，绝不能沿用 input 里的假 id。'
  )
}

/**
 * 委派型工具：把某节点的字段配置交给一个绑定该节点的子 agent。
 * 返回 Executor —— promise 是子 agent 跑到终态的结果，cancel 是子 agent 的 stop；
 * createAgent 会把 cancel 登记进本 agent 的停止表，父 stop() 即级联停子。
 * 子 agent 的事件 position = 父 position + nodeId，消费方据此嵌套展示。
 */
export function configureNodeTool(b: AgentBindings): AgentTool {
  return {
    name: 'configure_node',
    description:
      '把指定节点交给它自己的配置 agent 配置：该 agent 是本节点类型的专家。' +
      '用 input 传本节点需要的参数（一个 JSON 对象，按 list_nodes 里该节点的 input 结构来）：普通字段给值或引用意图；' +
      '★两类字段只表意图、别给最终值：需要「生成内容」的字段（js 的 code、脚本、提示词等）只写「要生成什么」的描述；' +
      '需要「选具体资源」的字段（模型 model_id、知识库 knowledgeIds、数据库/缓存连接池 poolId/cacheId 等）别猜真实 id，至多写意图——节点自己的 agent 有资源查询工具（get_models/get_knowledge_bases/get_database_pools 等），会查出真实 id 并生成内容后落库。' +
      '返回 { nodeId, result }：result 是配置 agent 配完后的结论说明（配置失败则返回 { nodeId, error }）。要接下游引用该节点输出时用 get_node_field_options 查。' +
      '前提：先把该节点 add_node 建好、接上边再配。循环节点也用本工具配——它自己的 agent 只配循环字段（loopType/遍历数组/循环变量）；' +
      '循环体里的节点由你继续用 add_node(lf_id=该循环id) 建、再 configure_node(lf_id=该循环id) 逐个配。' +
      'lf_id 指该节点所在画布：主画布传 "main"（或省略）；循环体内的节点传所在循环节点的 id。' +
      '多个已建好的节点要配时，尽管在同一轮里一次性发起多个 configure_node——同一轮的工具会并发执行，比逐个配快得多。',
    parameters: {
      type: 'object',
      properties: {
        nodeId: { type: 'string', description: '要配置的节点 id' },
        input: {
          type: 'object',
          description:
            '本节点的配置参数（按 list_nodes 里该节点的 input 结构给）。普通字段给值/引用意图；' +
            '需要生成内容的字段（js 的 code、脚本、提示词）只写「要生成什么」的描述、不要写真实代码/内容；' +
            '需要选资源的字段（model_id、knowledgeIds、poolId/cacheId 等）别猜真实 id，至多写意图——节点会用 get_models/get_knowledge_bases/get_database_pools 等查出真实 id。' +
            '可省略：省略时由该节点自己的 agent 结合画布自行判断字段。'
        },
        lf_id: { type: 'string', description: '该节点所在画布：主画布 "main"（或省略）；循环体内节点传所在循环节点 id' }
      },
      required: ['nodeId']
    },
    apply: (args) => {
      // 委派型工具：apply 保持同步返回 Executor（框架据此登记 cancel、级联停子）；子 agent 跑到终态是异步的，放进 promise。
      // 节点配置子 agent 不继承父对话历史——父已用 input 显式把该节点要配什么传下来，加上 context(整体目标)+画布结构足够，
      // 不必再灌父的全段往来（省 token、免串味）。子从空历史起，system=节点自身 prompt + context。
      let childRef: ReturnType<typeof createAgent> | null = null
      const promise = (async () => {
        // 按 lf_id 选中被配节点所在画布（循环体走 LOOP 变体）；节点 id 全局唯一，lf_id 传错/漏传时据 id 全树兜底
        const resolved = await resolveCanvas(b, args.lf_id)
        if (resolved.error) return { nodeId: args.nodeId, error: resolved.error }
        let targetLf = resolveNode(resolved.lf, args.nodeId) ? resolved.lf : (await findLfOfNode(b, args.nodeId)) ?? resolved.lf
        const node = resolveNode(targetLf, args.nodeId)
        if (!node) return { nodeId: args.nodeId, error: `节点不存在: ${args.nodeId}` }
        // 该画布是循环体时，工具节点要按 selfLoopId 写 reference=[selfLoopId,"item"]，故据 lf_id 推出 loopContext
        const loopContext = await deriveLoopContext(b, args.lf_id)
        // 按 node.type 取该节点自己的 useAgent（common.ts 由 glob 各节点 agent.ts 建的注册表）
        const useAgent = nodeUseAgentFor(node.type)
        if (!useAgent)
          return { nodeId: args.nodeId, error: `该节点类型暂无配置 agent: ${node.type}` }
        const child = createAgent(
          useAgent({
            lf: targetLf,
            modelId: b.modelId,
            call: b.call,
            // 画布类型随所在画布（循环体＝LOOP 变体），保证节点变体/字段选项正确
            workflowType: resolved.workflowType!,
            // 整体目标背景逐层透传：子节点配置 agent 借此"知情"链路（useAgentConfig 折进其 system）
            context: b.context,
            // 不传 messages：子 agent 不继承父对话历史，从空历史起（要配什么已由 input + context 说清）
            // position 追加 nodeId，即子 agent 的绑定节点
            position: [...(b.position ?? []), args.nodeId],
            // 透传跨层全景查询所需上下文：根画布（供 get_canvas_detail 全景 + getLf）与该节点所在循环层
            rootLf: b.rootLf ?? b.lf,
            loopContext
          })
        )
        childRef = child
        // 下发「开工指令」：父给了 input 就据 input 配（内容字段仅描述、由子 agent 生成），否则子 agent 自行判断
        const result = await child.run(configureTask(args.input))
        // 每配好一个节点就美化目标画布，保持生成过程画布整洁
        layoutCanvas(targetLf)
        // 子未正常结束（error/stopped）：把原因作为 error 回给父，模型据此重配（走失败自愈路径，不停父）
        if (result.status !== 'done') {
          const msg = typeof result.data === 'string' ? result.data : JSON.stringify(result.data ?? '')
          return { nodeId: args.nodeId, error: msg || `配置未完成（${result.status}）` }
        }
        // 响应 = 配置 agent 的交付（它自己说清配了什么；子 agent 隐式结束时即其结论文本）。nodeId 父自知、
        // field_list 需要时父可用 get_node_field_options 查，都不必回；只回交付结论。stop 不冒泡故不会误停父。
        return { nodeId: args.nodeId, result: result.data }
      })()
      return { promise, cancel: () => childRef?.stop() }
    }
  }
}

/** 校验器子 agent 的 prompt：对照「本层应完成的任务」，用 AI 审这层子画布是否合格（只判不改） */
const VALIDATOR_PROMPT = `你是「子画布校验器」。给你一个「本层应完成的任务」，判断当前这层子画布是否正确、完整地实现了它——只判断、不修改画布。

工作方式：
- 先调 get_canvas_detail 看整张工作流全景（nodes 节点 + edges 连线）与 youAreHere（你要审的就是 youAreHere 这一层）。
- 对照任务逐项核对这一层：
  ① 连线：任务要的节点都建了吗？该连的都连了吗？judge 的每个 if/elseif 分支都接了下游吗（对照 edges 里各 source_anchor_id——完整出口锚点，内含分支与 success/fail）？有没有接错分支、漏接异常输出、重复连线、悬空分支、除 loop-start 外的孤立节点？
  ② 引用：每个 location=tool_call 的工具节点，reference 是否指向本层(youAreHere.loopId)的 item，而不是别的循环？
  ③ 工具名：每个按工具名分流的 judge，其条件 value 是否都在对应 ai-chat 的 declaredTools 里（真实函数名，如 create_file 不是 create-file）？
  ④ 任务符合度：本层是否偏离任务、有无缺步；有没有把该交给内层循环的分发链重复铺在本层。
- 判完调 submit_verdict(qualified, problems) 提交结论即结束：qualified 是否合格；problems 为具体缺陷清单（每条定位到节点/连线，合格时可空）。`

/**
 * 校验器终态工具：提交校验结论（合格与否 + 缺陷清单）。返回 { status:'stop', data } —— 这是校验器的
 * "交卷"信号：createAgent 的循环一见 stop 即短路收尾，verdict 作为校验器 AgentResult.data 上抛给
 * validate 工具。不靠"模型自觉不再发工具"收尾（模型常无视软约束、反复重提同一 verdict 空转），提交即结束。
 */
function submitVerdictTool(): AgentTool {
  return {
    name: 'submit_verdict',
    description: '提交校验结论：qualified 本层是否合格，problems 具体缺陷清单（合格时可空）。这是校验的最后一步，提交即结束（调用后本次校验立即结束，无需再调）。',
    parameters: {
      type: 'object',
      properties: {
        qualified: { type: 'boolean', description: '本层子画布是否合格（正确完整实现了任务）' },
        problems: {
          type: 'array',
          items: { type: 'string' },
          description: '具体缺陷清单（每条定位到节点/连线）'
        }
      },
      required: ['qualified']
    },
    apply: (args) => ({
      status: 'stop',
      data: {
        qualified: !!args.qualified,
        problems: Array.isArray(args.problems) ? args.problems : []
      }
    })
  }
}

/**
 * validate 工具（委派型）：委派一个校验器子 agent，用 AI 对照「本层应完成的任务」审这层子画布是否合格。
 * 入参 task = 本层应完成什么；校验器读 get_canvas_detail（整张画布全景 + youAreHere 本层）判断，只判不改。
 * 返回 { qualified, problems }。建完/改完后调它；problems 非空就照着修，改完再调，直到 qualified=true。
 * 与「节点级 validate」（各节点自己的字段校验工具）分属两层：这层查图/连线/引用/工具名/任务符合度。
 */
export function validateTool(b: AgentBindings): AgentTool {
  return {
    name: 'validate',
    description:
      '校验你当前搭的这层子画布是否合格：委派一个校验 agent 用 AI 对照「本层应完成的任务」审查整张画布（连线/引用/工具名/结构/任务符合度），返回 { qualified, problems }。' +
      '入参 task 传本层应完成的任务。建完或改完后调它；problems 非空就逐条修（改引用/工具名用 configure_node，补连线用 add_edge），改完再调，直到 qualified=true 再收尾。',
    parameters: {
      type: 'object',
      properties: {
        task: {
          type: 'string',
          description: '本层子画布应完成的任务（自然语言，即你这层被要求搭建/实现什么）'
        }
      },
      required: ['task']
    },
    apply: (args) => {
      const validator: AgentBindings = {
        ...b,
        position: [...(b.position ?? []), 'validate'],
        // 校验器继承父至此的对话，据此对照"本层该搭成什么"审图（同样够不到别的层、只判不改）
        messages: inheritMessages(b)
      }
      // submit_verdict 是终态工具（返回 { status:'stop', data }）：createAgent 一见 stop 即短路收尾，
      // verdict 作为校验器 AgentResult.data 上抛，杜绝校验器反复重提同一 verdict 空转。
      const agent = createAgent(
        useAgentConfig(
          {
            prompt: VALIDATOR_PROMPT,
            tools: [
              getCanvasDetailTool(validator),
              getNodeAnchorsTool(validator),
              submitVerdictTool()
            ]
          },
          validator
        )
      )
      const promise = agent.run(`【本层应完成的任务】\n${args.task}`).then((result) => {
        // 正常路径：校验器以 submit_verdict(stop) 交付 → result.data = { qualified, problems }
        const v = result.data
        if (result.status !== 'done' || !v || typeof v !== 'object') {
          const msg = typeof result.data === 'string' ? result.data : ''
          return { error: msg || '校验失败', qualified: false, problems: [] }
        }
        return { qualified: !!v.qualified, problems: Array.isArray(v.problems) ? v.problems : [] }
      })
      return { promise, cancel: agent.stop }
    }
  }
}
