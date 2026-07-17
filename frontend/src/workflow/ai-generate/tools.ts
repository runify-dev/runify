import { cloneDeep } from 'lodash'
import { reactive } from 'vue'
import { generateAnchor, randomId } from '@/utils/common'
import { TreeCommonAPI } from '@/api/tree'
import { ROOT_FOLDER_ID } from '@/constants/common'
import { baseWorkflow } from '@/workflow/common/data'
import {
  nodeCatalog,
  catalogTypes,
  buildNodeProperties,
  refreshNodeProperties,
  renderDoc,
  sanitizeNodeDataPatch
} from './node-catalog'
import { AGENT_TOOL_DEFINITIONS } from './agent-tools'
import { mergeJudgeBranches, lintWorkflowGraph } from './graph-ops'

/**
 * agent 工具执行所需的画布上下文，由挂载方（workflow/index.vue）提供
 */
export interface WorkflowAgentContext {
  getLf: () => any
  validateWorkflow: (options?: { silent?: boolean }) => Promise<{ valid: boolean; nodeId?: string; errors?: Record<string, string> }>
}

export interface ToolExecution {
  /** 是否改变了画布图结构/配置（用于批次后自动布局） */
  mutating: boolean
  /** 图变更后跳过 dagre 自动布局（如模板加载：坐标已人工排好） */
  skipLayout?: boolean
  /** 工具结果回喂模型的截断上限（默认见 useWorkflowAgent 的 TOOL_RESULT_LIMIT） */
  resultLimit?: number
  execute: (args: Record<string, any>, ctx: WorkflowAgentContext) => Promise<any> | any
}

const START_NODE_ID = 'start-node'
const LOOP_START_NODE_ID = 'loop-start-node'

function getGraphModel(ctx: WorkflowAgentContext) {
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
function findLoopChildren(container: { nodes: any[] }, loopId: string): { nodes: any[]; edges: any[] } | null {
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

function queueLoopRefresh(loopId: string) {
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

function requireLoopChildren(ctx: WorkflowAgentContext, loopId: string): { nodes: any[]; edges: any[] } {
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
function requireScopedNode(ctx: WorkflowAgentContext, nodeId: string, parentLoopId?: string) {
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
function collectAllCanvasNodes(lf: any): any[] {
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
function resolveSourceBranch(sourceNode: any, sourceBranchId?: string): string {
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
function removeBranchEdges(
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
function nextPosition(nodes: any[]): { x: number; y: number } {
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
function detailNodeData(type: string, nodeData: Record<string, any> | null): Record<string, any> | null {
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

function simplifyCanvas(lf: any) {
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

export const toolExecutors: Record<string, ToolExecution> = {
  get_node_schema: {
    mutating: false,
    execute: ({ types }) => {
      const list = Array.isArray(types) ? types : []
      if (!list.length) throw new Error('types 不能为空')
      return list.map((type: string) => {
        const entry = nodeCatalog[type]
        if (!entry) return { type, error: `未知节点类型，可用类型: ${catalogTypes.join(', ')}` }
        return { type, label: entry.label, doc: renderDoc(entry) }
      })
    }
  },

  clear_workflow: {
    mutating: true,
    skipLayout: true,
    execute: (_args, ctx) => {
      const lf = getGraphModel(ctx)
      lf.render(cloneDeep(baseWorkflow))
      lf.fitView(40, 40)
      return { ok: true }
    }
  },

  add_node: {
    mutating: true,
    execute: ({ type, name, nodeData, parentLoopId }, ctx) => {
      const lf = getGraphModel(ctx)
      const { properties, warnings } = buildNodeProperties(type, name, nodeData)
      // 重名告警：AI 路径没有画布手动添加的自动改名逻辑，重复添加往往意味着模型忘了自己已经加过
      const duplicate = collectAllCanvasNodes(lf).find(
        (n: any) => n.type === type && n.properties?.name === properties.name
      )
      if (duplicate) {
        warnings.push(
          `画布已存在同名同类型节点「${properties.name}」(id=${duplicate.id})。` +
            `若本次属于误重复添加，请 delete_node 删除多余的那个；确需两个请改名区分`
        )
      }
      const withWarnings = (result: Record<string, any>) =>
        warnings.length ? { ...result, warnings } : result
      if (parentLoopId) {
        const children = requireLoopChildren(ctx, parentLoopId)
        const { x, y } = nextPosition(children.nodes)
        const id = randomId()
        children.nodes.push({ id, type, x, y, properties, text: '' })
        queueLoopRefresh(parentLoopId)
        return withWarnings({ nodeId: id, field_list: properties.field_list ?? [] })
      }
      const { x, y } = nextPosition(lf.graphModel.nodes)
      const nodeModel = lf.graphModel.addNode({ type, properties, x, y })
      if (type === 'loop-node') {
        // 新建循环批次结束后展开子画布，让后续构建过程可见
        queueLoopRefresh(nodeModel.id)
      }
      return withWarnings({
        nodeId: nodeModel.id,
        field_list: nodeModel.properties.field_list ?? []
      })
    }
  },

  update_node: {
    mutating: true,
    execute: ({ nodeId, name, nodeData, parentLoopId }, ctx) => {
      if (nodeId === START_NODE_ID) throw new Error('开始节点不可修改')
      if (nodeId === LOOP_START_NODE_ID) throw new Error('循环开始节点不可修改')
      const node = requireScopedNode(ctx, nodeId, parentLoopId)
      if (name) node.properties.name = name
      let warnings: string[] = []
      let removedBranchIds: string[] = []
      if (nodeData && typeof nodeData === 'object') {
        const sanitized = sanitizeNodeDataPatch(node.type, cloneDeep(nodeData))
        warnings = sanitized.warnings
        const patch = sanitized.patch
        // 子画布由 add_node(parentLoopId) 管理，禁止经 update_node 整体覆盖
        if (node.type === 'loop-node') delete patch.children
        // judge 分支按 id 合并：出边锚点 = 分支 id，整体覆盖会把已有连线全部打断
        if (node.type === 'judge-node' && Array.isArray(patch.branches)) {
          const merged = mergeJudgeBranches(node.properties.nodeData?.branches ?? [], patch.branches)
          patch.branches = merged.branches
          removedBranchIds = merged.removedIds
        }
        node.properties.nodeData = {
          ...(node.properties.nodeData ?? {}),
          ...patch
        }
      }
      refreshNodeProperties(node.type, node.properties)
      if (removedBranchIds.length) {
        const removedEdges = removeBranchEdges(ctx, nodeId, removedBranchIds, parentLoopId)
        warnings.push(
          `分支已移除：${removedBranchIds.join(', ')}` +
            (removedEdges ? `，其 ${removedEdges} 条出边已一并删除，请确认后续流程是否需要重新连线` : '')
        )
      }
      if (parentLoopId) queueLoopRefresh(parentLoopId)
      return {
        nodeId,
        field_list: node.properties.field_list ?? [],
        ...(node.type === 'judge-node'
          ? {
              branches: (node.properties.nodeData?.branches ?? []).map((b: any) => ({
                id: b.id,
                type: b.type
              }))
            }
          : {}),
        ...(warnings.length ? { warnings } : {})
      }
    }
  },

  add_edge: {
    mutating: true,
    execute: ({ sourceNodeId, targetNodeId, sourceBranchId, parentLoopId }, ctx) => {
      if (targetNodeId === START_NODE_ID || targetNodeId === LOOP_START_NODE_ID) {
        throw new Error('开始节点不能作为连线目标')
      }
      const sourceNode = requireScopedNode(ctx, sourceNodeId, parentLoopId)
      requireScopedNode(ctx, targetNodeId, parentLoopId)
      // judge 分支必须真实存在，否则产出的边锚点悬空、运行时路由不到
      const branch = resolveSourceBranch(sourceNode, sourceBranchId)
      const sourceAnchorId = generateAnchor(sourceNodeId, 'right', branch, 'success')
      const targetAnchorId = generateAnchor(targetNodeId, 'left', 'main', 'success')
      if (parentLoopId) {
        const children = requireLoopChildren(ctx, parentLoopId)
        const exists = children.edges.some(
          (edge: any) => edge.sourceAnchorId === sourceAnchorId && edge.targetNodeId === targetNodeId
        )
        if (exists) throw new Error('该连线已存在')
        children.edges.push({
          id: randomId(),
          type: 'run-edge',
          sourceNodeId,
          sourceAnchorId,
          targetNodeId,
          targetAnchorId
        })
        queueLoopRefresh(parentLoopId)
        return { ok: true }
      }
      const lf = getGraphModel(ctx)
      const exists = lf.graphModel.edges.some(
        (edge: any) => edge.sourceAnchorId === sourceAnchorId && edge.targetNodeId === targetNodeId
      )
      if (exists) throw new Error('该连线已存在')
      lf.graphModel.addEdge({
        type: 'run-edge',
        sourceNodeId,
        sourceAnchorId,
        targetNodeId,
        targetAnchorId
      })
      return { ok: true }
    }
  },

  delete_edge: {
    mutating: true,
    execute: ({ sourceNodeId, targetNodeId, sourceBranchId, parentLoopId }, ctx) => {
      const matches = (edge: any) =>
        edge.sourceNodeId === sourceNodeId &&
        edge.targetNodeId === targetNodeId &&
        (!sourceBranchId ||
          edge.sourceAnchorId === generateAnchor(sourceNodeId, 'right', sourceBranchId, 'success'))
      if (parentLoopId) {
        const children = requireLoopChildren(ctx, parentLoopId)
        const before = children.edges.length
        children.edges = children.edges.filter((edge: any) => !matches(edge))
        const deleted = before - children.edges.length
        if (!deleted) {
          throw new Error(`循环 ${parentLoopId} 内不存在连线: ${sourceNodeId} → ${targetNodeId}`)
        }
        queueLoopRefresh(parentLoopId)
        return { ok: true, deleted }
      }
      const lf = getGraphModel(ctx)
      const hits = lf.graphModel.edges.filter(matches)
      if (!hits.length) throw new Error(`连线不存在: ${sourceNodeId} → ${targetNodeId}`)
      for (const edgeId of hits.map((edge: any) => edge.id)) {
        lf.graphModel.deleteEdgeById(edgeId)
      }
      return { ok: true, deleted: hits.length }
    }
  },

  delete_node: {
    mutating: true,
    execute: ({ nodeId, parentLoopId }, ctx) => {
      if (nodeId === START_NODE_ID) throw new Error('开始节点不可删除')
      if (nodeId === LOOP_START_NODE_ID) throw new Error('循环开始节点不可删除')
      if (parentLoopId) {
        const children = requireLoopChildren(ctx, parentLoopId)
        const index = children.nodes.findIndex((n: any) => n.id === nodeId)
        if (index < 0) throw new Error(`循环 ${parentLoopId} 内不存在节点: ${nodeId}`)
        children.nodes.splice(index, 1)
        children.edges = children.edges.filter(
          (edge: any) => edge.sourceNodeId !== nodeId && edge.targetNodeId !== nodeId
        )
        queueLoopRefresh(parentLoopId)
        return { ok: true }
      }
      const lf = getGraphModel(ctx)
      requireScopedNode(ctx, nodeId)
      lf.deleteNode(nodeId)
      return { ok: true }
    }
  },

  get_workflow: {
    mutating: false,
    resultLimit: 20000,
    execute: (_args, ctx) => simplifyCanvas(getGraphModel(ctx))
  },

  get_node_detail: {
    mutating: false,
    resultLimit: 20000,
    execute: ({ nodeId, parentLoopId }, ctx) => {
      const node = requireScopedNode(ctx, nodeId, parentLoopId)
      const properties = node.properties ?? {}
      return {
        id: nodeId,
        type: node.type,
        name: properties.name,
        nodeData: detailNodeData(node.type, properties.nodeData ?? null),
        field_list: (properties.field_list ?? []).map((f: any) => ({ label: f.label, value: f.value }))
      }
    }
  },

  get_models: {
    mutating: false,
    execute: async () => {
      const res = await new TreeCommonAPI('model').listResource(ROOT_FOLDER_ID)
      return (res.data ?? []).map((item: any) => ({
        id: item.id,
        name: item.name,
        modelName: item.modelName,
        modelType: item.modelType
      }))
    }
  },

  get_knowledge_bases: {
    mutating: false,
    execute: async () => {
      const res = await new TreeCommonAPI('knowledge').listResource(ROOT_FOLDER_ID)
      return (res.data ?? []).map((item: any) => ({ id: item.id, name: item.name }))
    }
  },

  validate_workflow: {
    mutating: false,
    execute: async (_args, ctx) => {
      const result = await ctx.validateWorkflow({ silent: true })
      // 结构 lint：孤立节点/悬空边/非法分支边为错误，同名同类型重复节点为警告
      const lint = lintWorkflowGraph(getGraphModel(ctx).getGraphData())
      return {
        valid: result.valid && lint.errors.length === 0,
        ...(result.nodeId ? { nodeId: result.nodeId } : {}),
        ...(result.errors ? { errors: result.errors } : {}),
        ...(lint.errors.length ? { structuralErrors: lint.errors } : {}),
        ...(lint.warnings.length ? { structuralWarnings: lint.warnings } : {})
      }
    }
  },

  locate_node: {
    mutating: false,
    execute: ({ nodeId }, ctx) => {
      const lf = getGraphModel(ctx)
      requireScopedNode(ctx, nodeId)
      lf.selectElementById(nodeId)
      lf.focusOn({ id: nodeId })
      return { ok: true }
    }
  },

  finish: {
    mutating: false,
    execute: ({ summary }) => ({ ok: true, summary: summary ?? '' })
  }
}

const parentLoopIdSchema = {
  type: 'string',
  description: '可选：循环节点 id。传入后在该循环的子画布内操作（支持嵌套循环）；省略则在主画布操作'
}

/**
 * 每轮请求前由前端按画布状态决定传哪些工具（AI 只在被给到工具时才可能调用）：
 * - clear_workflow 仅在画布非空时提供（是否清空由 AI 决策；空画布没有可清的内容）
 */
export function buildActiveToolSchemas(lf: any): any[] {
  const hasContent = hasCanvasContent(lf)
  return toolSchemas.filter((tool: any) => {
    if (tool.function.name === 'clear_workflow') return hasContent
    return true
  })
}

export const toolSchemas = [
  {
    type: 'function',
    function: {
      name: 'get_node_schema',
      description: '获取指定节点类型的详细配置文档（nodeData 结构、必填项、输出字段）。新增或修改节点前必须先查询。',
      parameters: {
        type: 'object',
        properties: {
          types: {
            type: 'array',
            items: { type: 'string', enum: catalogTypes },
            description: '要查询的节点类型列表'
          }
        },
        required: ['types']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'plan',
      description:
        '提交或更新任务规划（todolist），会展示给用户。开始改动画布前必须先提交完整计划；之后每完成一步、或计划有变化时，携带完整列表再次调用以更新状态。',
      parameters: {
        type: 'object',
        properties: {
          items: {
            type: 'array',
            description: '完整的任务列表（每次都传全量）',
            items: {
              type: 'object',
              properties: {
                text: { type: 'string', description: '任务描述（中文，一句话）' },
                status: { type: 'string', enum: ['pending', 'doing', 'done'], description: '任务状态' }
              },
              required: ['text', 'status']
            }
          }
        },
        required: ['items']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'clear_workflow',
      description:
        '清空画布，恢复为只有开始节点。仅当用户明确要求推倒重做、或现有工作流与需求根本冲突时使用；能增量修改就不要清空。',
      parameters: { type: 'object', properties: {} }
    }
  },
  {
    type: 'function',
    function: {
      name: 'add_node',
      description:
        '在画布新增一个节点，返回 nodeId 与输出字段 field_list。坐标自动布局，无需指定。添加 loop-node 时子画布自动初始化 loop-start-node 入口。返回值含 warnings 时说明 nodeData 存在未知字段/非法取值（已被忽略）或画布已有同名同类型节点（可能是重复添加），必须按提示处理。',
      parameters: {
        type: 'object',
        properties: {
          type: { type: 'string', enum: catalogTypes, description: '节点类型' },
          name: { type: 'string', description: '节点显示名（中文，简洁描述用途）' },
          nodeData: { type: 'object', description: '节点配置，结构见 get_node_schema；可只传需要覆盖的字段' },
          parentLoopId: parentLoopIdSchema
        },
        required: ['type']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'update_node',
      description:
        '更新已有节点的配置（nodeData 顶层字段合并覆盖）或显示名，返回更新后的输出字段。修改前必须先 get_node_detail 获取完整配置，在其基础上增量修改。judge-node 的 branches 按分支 id 合并：保留的分支必须带原 id（仅新增分支可省略）；被移除分支的出边会被自动删除并在 warnings 中告知。返回值含 warnings 时必须按提示处理。',
      parameters: {
        type: 'object',
        properties: {
          nodeId: { type: 'string', description: '节点 id（add_node 返回或 get_workflow 查询）' },
          name: { type: 'string', description: '新的显示名' },
          nodeData: { type: 'object', description: '要覆盖的 nodeData 字段' },
          parentLoopId: parentLoopIdSchema
        },
        required: ['nodeId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'add_edge',
      description:
        '连接两个节点（源节点出口 → 目标节点入口）。judge-node 必须用 sourceBranchId 指定分支。循环子画布内连线传 parentLoopId，且第一条边从 "loop-start-node" 引出。',
      parameters: {
        type: 'object',
        properties: {
          sourceNodeId: { type: 'string', description: '源节点 id' },
          targetNodeId: { type: 'string', description: '目标节点 id' },
          sourceBranchId: { type: 'string', description: '仅 judge-node 需要：分支 id（branches 中的 id）' },
          parentLoopId: parentLoopIdSchema
        },
        required: ['sourceNodeId', 'targetNodeId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'delete_edge',
      description:
        '删除两个节点之间的连线。改接线、或在 A→B 之间插入新节点时使用（先 delete_edge(A,B)，再 add_node + add_edge 两条新边）。judge-node 引出的边如有多条，用 sourceBranchId 精确指定；省略则删除两点间的全部连线。',
      parameters: {
        type: 'object',
        properties: {
          sourceNodeId: { type: 'string', description: '源节点 id' },
          targetNodeId: { type: 'string', description: '目标节点 id' },
          sourceBranchId: { type: 'string', description: '仅 judge-node 需要：分支 id' },
          parentLoopId: parentLoopIdSchema
        },
        required: ['sourceNodeId', 'targetNodeId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'delete_node',
      description: '删除一个节点（连带其连线），用于纠错。开始节点/循环开始节点不可删除。',
      parameters: {
        type: 'object',
        properties: {
          nodeId: { type: 'string', description: '节点 id' },
          parentLoopId: parentLoopIdSchema
        },
        required: ['nodeId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'get_workflow',
      description:
        '获取当前画布的结构概览：节点 id/类型/名称/输出字段、judge 分支 id、循环子画布 children、全部连线。不含节点详细配置——查看/修改某个节点的配置前先调用 get_node_detail。',
      parameters: { type: 'object', properties: {} }
    }
  },
  {
    type: 'function',
    function: {
      name: 'get_node_detail',
      description:
        '获取单个节点的完整配置（nodeData 全文）。修改已有节点前必须先调用它拿到当前配置，在此基础上做增量 update_node，禁止凭概览或记忆重写整个配置。ai-chat 的标准工具以 {"type":"function","function":{"name":"标准名"}} 形式表示，回填时保持该形式即可。',
      parameters: {
        type: 'object',
        properties: {
          nodeId: { type: 'string', description: '节点 id' },
          parentLoopId: parentLoopIdSchema
        },
        required: ['nodeId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'get_models',
      description: '获取可用的模型列表（需要填 modelId 时才调用，如 ai-chat-node 配置或 load_template 前）。',
      parameters: { type: 'object', properties: {} }
    }
  },
  {
    type: 'function',
    function: {
      name: 'get_knowledge_bases',
      description: '获取可用的知识库列表（仅当方案用到知识检索节点或 search 模板时调用）。',
      parameters: { type: 'object', properties: {} }
    }
  },
  {
    type: 'function',
    function: {
      name: 'validate_workflow',
      description:
        '校验当前工作流（含循环子画布）：节点配置完整性 + 结构检查（孤立节点、悬空边、失效分支边为错误；同名同类型重复节点为警告）。完成搭建后必须调用，structuralErrors 必须修复、structuralWarnings 必须逐条确认。',
      parameters: { type: 'object', properties: {} }
    }
  },
  {
    type: 'function',
    function: {
      name: 'locate_node',
      description: '把画布视角定位到指定主画布节点（向用户演示时使用，可选）。',
      parameters: {
        type: 'object',
        properties: {
          nodeId: { type: 'string', description: '节点 id' }
        },
        required: ['nodeId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'finish',
      description: '声明搭建完成并结束。必须在 validate_workflow 通过后调用。',
      parameters: {
        type: 'object',
        properties: {
          summary: { type: 'string', description: '搭建结果总结（中文，说明工作流的流程与用法）' }
        },
        required: ['summary']
      }
    }
  }
]
