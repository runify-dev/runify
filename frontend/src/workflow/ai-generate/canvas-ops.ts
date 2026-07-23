import { cloneDeep } from 'lodash'
import { reactive } from 'vue'
import { generateAnchor } from '@/utils/common'
import { AGENT_TOOL_DEFINITIONS } from './agent-tools'
import type { WorkflowAgentContext } from './types'

/**
 * 画布操作层：直接操作 LogicFlow 实例的副作用函数（循环子画布查找、子画布刷新合并、
 * 节点定位、judge 分支边、落点计算、快照 simplify）。
 * 与 graph-ops.ts（纯数据函数、有单测）区分：这里的函数依赖运行时画布实例。
 * 工具执行器（tools.ts）与 profile 钩子复用这些操作。
 */

/** 画布固有节点 id */
export const START_NODE_ID = 'start-node'
export const LOOP_START_NODE_ID = 'loop-start-node'

export function getGraphModel(ctx: WorkflowAgentContext) {
  const lf = ctx.getLf()
  if (!lf) throw new Error('画布未初始化')
  return lf
}

/** 画布是否有开始节点以外的内容 */
export function hasCanvasContent(lf: any): boolean {
  return !!lf && (lf.graphModel.nodes.length > 1 || lf.graphModel.edges.length > 0)
}

/**
 * 递归查找循环节点的子画布（支持嵌套循环）
 * 返回 children（{nodes, edges} 的 JSON 引用，可直接改写）
 */
export function findLoopChildren(
  container: { nodes: any[] },
  loopId: string
): { nodes: any[]; edges: any[] } | null {
  for (const node of container.nodes ?? []) {
    const type = node.type
    if (type !== 'loop-node') continue
    const nodeData = (node.properties.nodeData = node.properties.nodeData ?? {})
    nodeData.children = nodeData.children ?? { nodes: [], edges: [] }
    nodeData.children.nodes = nodeData.children.nodes ?? []
    nodeData.children.edges = nodeData.children.edges ?? []
    if (node.id === loopId) {
      return nodeData.children
    }
    const found = findLoopChildren(nodeData.children, loopId)
    if (found) return found
  }
  return null
}

function containsLoopId(children: { nodes?: any[] }, loopId: string): boolean {
  for (const node of children.nodes ?? []) {
    if (node.id === loopId) return true
    if (
      node.type === 'loop-node' &&
      containsLoopId(node.properties?.nodeData?.children ?? { nodes: [] }, loopId)
    ) {
      return true
    }
  }
  return false
}

/** 找到包含指定循环的顶层 loop-node id（loopId 本身在顶层时即它自己） */
function findTopLoopId(lf: any, loopId: string): string | null {
  for (const model of lf.graphModel.nodes) {
    if (model.type !== 'loop-node') continue
    if (
      model.id === loopId ||
      containsLoopId(model.properties?.nodeData?.children ?? { nodes: [] }, loopId)
    ) {
      return model.id
    }
  }
  return null
}

/**
 * 子画布刷新按批合并：工具执行只登记受影响的循环，
 * 批次结束由 flushLoopRefresh 统一触发展开+重渲染（避免加 N 个子节点重排 N 次）
 */
const pendingLoopRefresh = new Set<string>()

export function queueLoopRefresh(loopId: string) {
  pendingLoopRefresh.add(loopId)
}

/** 展开顶层循环并强制以 nodeData.children 重渲染子画布（每个受影响的顶层循环仅一次） */
export function flushLoopRefresh(lf: any) {
  if (!pendingLoopRefresh.size) return
  const topIds = new Set<string>()
  if (lf) {
    for (const loopId of pendingLoopRefresh) {
      const topId = findTopLoopId(lf, loopId)
      if (topId) topIds.add(topId)
    }
  }
  pendingLoopRefresh.clear()
  for (const topId of topIds) {
    lf.graphModel.eventCenter.emit('runify:node:refresh-body', topId)
  }
}

export function requireLoopChildren(ctx: WorkflowAgentContext, loopId: string): { nodes: any[]; edges: any[] } {
  const lf = getGraphModel(ctx)
  const top = lf.graphModel.getNodeModelById(loopId)
  if (top && top.type !== 'loop-node') {
    throw new Error(`parentLoopId ${loopId} 不是循环节点`)
  }
  const children = findLoopChildren({ nodes: lf.graphModel.nodes }, loopId)
  if (!children) throw new Error(`循环节点不存在: ${loopId}`)
  return children
}

/**
 * 在主画布或指定循环子画布中查找节点（返回统一的 {id,type,properties} 视图）
 * 主画布节点包成 Vue reactive 代理再返回：teleport 里节点组件读的是 reactive(model)，
 * 直接改 raw model 不会触发视图更新（改名/改配置后卡片显示旧值）
 */
export function requireScopedNode(ctx: WorkflowAgentContext, nodeId: string, parentLoopId?: string) {
  if (parentLoopId) {
    const children = requireLoopChildren(ctx, parentLoopId)
    const node = children.nodes.find((n: any) => n.id === nodeId)
    if (!node) throw new Error(`循环 ${parentLoopId} 内不存在节点: ${nodeId}`)
    return node
  }
  const lf = getGraphModel(ctx)
  const nodeModel = lf.graphModel.getNodeModelById(nodeId)
  if (!nodeModel) throw new Error(`节点不存在: ${nodeId}`)
  return reactive(nodeModel)
}

/** 递归收集整个画布（含循环子画布）的节点，用于重名检测 */
export function collectAllCanvasNodes(lf: any): any[] {
  const result: any[] = []
  const walk = (nodes: any[]) => {
    for (const node of nodes ?? []) {
      result.push(node)
      const children = node?.properties?.nodeData?.children
      if (children?.nodes?.length) walk(children.nodes)
    }
  }
  walk(lf.graphModel.nodes)
  return result
}

/** judge 出边解析分支；其余节点固定 main 出口 */
export function resolveSourceBranch(sourceNode: any, sourceBranchId?: string): string {
  if (sourceNode.type === 'judge-node') {
    const branchIds = (sourceNode.properties?.nodeData?.branches ?? [])
      .map((b: any) => b?.id)
      .filter(Boolean)
    if (!sourceBranchId) {
      throw new Error(`judge-node 出边必须指定 sourceBranchId，可用分支: ${branchIds.join(', ')}`)
    }
    if (!branchIds.includes(sourceBranchId)) {
      throw new Error(`分支 ${sourceBranchId} 不存在，可用分支: ${branchIds.join(', ')}`)
    }
    return sourceBranchId
  }
  if (sourceBranchId) {
    throw new Error(`sourceBranchId 仅用于 judge-node 出边（${sourceNode.id} 是 ${sourceNode.type}）`)
  }
  return 'main'
}

/** 删除指定分支的出边（judge 分支被移除后清理悬空边），返回删除数量 */
export function removeBranchEdges(
  ctx: WorkflowAgentContext,
  nodeId: string,
  branchIds: string[],
  parentLoopId?: string
): number {
  const anchorIds = new Set(branchIds.map((b) => generateAnchor(nodeId, 'right', b, 'success')))
  if (parentLoopId) {
    const children = requireLoopChildren(ctx, parentLoopId)
    const before = children.edges.length
    children.edges = children.edges.filter((edge: any) => !anchorIds.has(edge.sourceAnchorId))
    return before - children.edges.length
  }
  const lf = getGraphModel(ctx)
  const hits = lf.graphModel.edges.filter((edge: any) => anchorIds.has(edge.sourceAnchorId))
  for (const edgeId of hits.map((edge: any) => edge.id)) {
    lf.graphModel.deleteEdgeById(edgeId)
  }
  return hits.length
}

/** 新节点的临时落点：现有节点最右侧再往右（主画布会被 dagre 重排；子画布按此简单排链） */
export function nextPosition(nodes: any[]): { x: number; y: number } {
  if (!nodes.length) return { x: 300, y: 300 }
  const rightmost = nodes.reduce((a: any, b: any) => (a.x > b.x ? a : b))
  return { x: rightmost.x + 320, y: rightmost.y }
}

/**
 * get_workflow 只返回拓扑概览（不含 nodeData 配置全文）：
 * 完整工作流的配置 JSON 动辄几十 KB，会被工具结果截断拦腰砍掉——
 * 模型看不到末尾节点和全部连线，既发现不了重复节点也修不了接线。
 * 节点详细配置改由 get_node_detail 按需获取。
 */
function simplifyNode(node: any): Record<string, any> {
  const properties = node.properties ?? {}
  const nodeData = properties.nodeData ?? null
  const result: Record<string, any> = {
    id: node.id,
    type: node.type,
    name: properties.name,
    field_list: (properties.field_list ?? []).map((f: any) => ({ label: f.label, value: f.value }))
  }
  if (node.type === 'judge-node') {
    result.branches = (nodeData?.branches ?? []).map((b: any) => ({ id: b.id, type: b.type }))
  }
  if (node.type === 'loop-node') {
    result.children = simplifyGraphData(nodeData?.children ?? { nodes: [], edges: [] })
  }
  return result
}

/**
 * get_node_detail 返回的完整 nodeData：
 * - ai-chat 的标准工具定义压缩为 {"function":{"name":标准名}}（回填也按此格式，系统自动替换全文）
 * - loop 的 children 不在此返回（拓扑看 get_workflow，子节点配置逐个查）
 */
export function detailNodeData(type: string, nodeData: Record<string, any> | null): Record<string, any> | null {
  if (!nodeData) return null
  const data = cloneDeep(nodeData)
  if (type === 'ai-chat-node' && Array.isArray(data.tools?.tools)) {
    data.tools.tools = data.tools.tools.map((tool: any) => {
      const name = tool?.function?.name
      return name && AGENT_TOOL_DEFINITIONS[name] ? { type: 'function', function: { name } } : tool
    })
  }
  if (type === 'loop-node') delete data.children
  return data
}

function simplifyGraphData(graph: { nodes: any[]; edges: any[] }) {
  return {
    nodes: (graph.nodes ?? []).map(simplifyNode),
    edges: (graph.edges ?? []).map((edge: any) => ({
      sourceNodeId: edge.sourceNodeId,
      targetNodeId: edge.targetNodeId,
      sourceAnchorId: edge.sourceAnchorId
    }))
  }
}

export function simplifyCanvas(lf: any) {
  return simplifyGraphData({ nodes: lf.graphModel.nodes, edges: lf.graphModel.edges })
}

const SNAPSHOT_LIMIT = 6000

/** 供用户消息自动附带的画布结构快照（拓扑概览，超长截断并提示改用 get_workflow） */
export function buildWorkflowSnapshot(lf: any): string {
  try {
    const json = JSON.stringify(simplifyCanvas(lf))
    return json.length > SNAPSHOT_LIMIT
      ? json.slice(0, SNAPSHOT_LIMIT) + '…(快照超长已截断，完整结构请调用 get_workflow)'
      : json
  } catch {
    return ''
  }
}
