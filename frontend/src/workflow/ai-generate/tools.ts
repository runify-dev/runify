import { cloneDeep } from 'lodash'
import { randomId, generateAnchor } from '@/utils/common'
import { TreeCommonAPI } from '@/api/tree'
import { ROOT_FOLDER_ID } from '@/constants/common'
import { baseWorkflow } from '@/workflow/common/data'
import { buildNodeProperties, refreshNodeProperties, renderDoc, sanitizeNodeDataPatch } from './node-catalog'
import { mergeJudgeBranches, lintWorkflowGraph } from './graph-ops'
import type { WorkflowAgentProfile } from './profiles/types'
import type { WorkflowAgentContext, ToolExecution } from './types'
import {
  START_NODE_ID,
  LOOP_START_NODE_ID,
  getGraphModel,
  hasCanvasContent,
  queueLoopRefresh,
  flushLoopRefresh,
  requireLoopChildren,
  requireScopedNode,
  collectAllCanvasNodes,
  resolveSourceBranch,
  removeBranchEdges,
  nextPosition,
  detailNodeData,
  simplifyCanvas,
  buildWorkflowSnapshot
} from './canvas-ops'

// 引擎类型 WorkflowAgentContext / ToolExecution 见 ./types；画布操作辅助见 ./canvas-ops。
// 本文件只负责通用工具的 executors 与 schemas 定义、下发与解析。

export const toolExecutors: Record<string, ToolExecution> = {
  get_node_schema: {
    mutating: false,
    execute: ({ types }, ctx) => {
      const list = Array.isArray(types) ? types : []
      if (!list.length) throw new Error('types 不能为空')
      return list.map((type: string) => {
        const entry = ctx.profile.catalog[type]
        if (!entry) {
          return { type, error: `当前画布不支持该节点类型，可用类型: ${Object.keys(ctx.profile.catalog).join(', ')}` }
        }
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
      const entry = ctx.profile.catalog[type]
      if (!entry) {
        throw new Error(
          `当前画布不支持该节点类型: ${type}，可用类型: ${Object.keys(ctx.profile.catalog).join(', ')}`
        )
      }
      if (entry.addable === false) {
        throw new Error(`${type} 是画布固有节点，不可添加，只能 update_node 修改`)
      }
      const { properties, warnings } = buildNodeProperties(ctx.profile.catalog, type, name, nodeData)
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
    execute: async ({ nodeId, name, nodeData, parentLoopId }, ctx) => {
      if (nodeId === START_NODE_ID) {
        // 开始节点是否可修改由画布档案决定（如处理器：HTTP 入参可配，且需联动持久化到处理器配置）
        if (!ctx.profile.updateStartNode) throw new Error('开始节点不可修改')
        return await ctx.profile.updateStartNode(ctx, nodeData ?? {})
      }
      if (nodeId === LOOP_START_NODE_ID) throw new Error('循环开始节点不可修改')
      const node = requireScopedNode(ctx, nodeId, parentLoopId)
      if (name) node.properties.name = name
      let warnings: string[] = []
      let removedBranchIds: string[] = []
      if (nodeData && typeof nodeData === 'object') {
        const sanitized = sanitizeNodeDataPatch(ctx.profile.catalog, node.type, cloneDeep(nodeData))
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
      refreshNodeProperties(ctx.profile.catalog, node.type, node.properties)
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
      let result = await ctx.validateWorkflow({ silent: true })
      // 开始节点错误的归类取决于画布档案：可修改（有 updateStartNode 钩子）时按普通配置错误
      // 让 AI 自行修复；不可修改时单独归类为 startNodeErrors，不阻塞 finish，但必须在总结中
      // 提醒用户；后者需跳过开始节点再校验一遍，避免短路校验掩盖其余节点的错误
      let startNodeErrors: Record<string, string> | undefined
      if (!result.valid && result.nodeId === START_NODE_ID && !ctx.profile.updateStartNode) {
        startNodeErrors = result.errors
        result = await ctx.validateWorkflow({ silent: true, skipNodeIds: [START_NODE_ID] })
      }
      // 结构 lint：孤立节点/悬空边/非法分支边为错误，同名同类型重复节点为警告
      const lint = lintWorkflowGraph(getGraphModel(ctx).getGraphData())
      return {
        valid: result.valid && lint.errors.length === 0,
        ...(result.nodeId ? { nodeId: result.nodeId } : {}),
        ...(result.errors ? { errors: result.errors } : {}),
        ...(startNodeErrors
          ? {
              startNodeErrors,
              startNodeNote:
                '开始节点的配置由用户维护，你无权修改：不影响本次交付（valid 不含它），但必须在 finish 总结中提醒用户按提示到开始节点完成配置'
            }
          : {}),
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

/** 工具执行器解析：通用执行器优先，其次画布档案的专属执行器 */
export function resolveToolExecutor(name: string, profile: WorkflowAgentProfile): ToolExecution | undefined {
  return toolExecutors[name] ?? profile.tools?.executors[name]
}

/**
 * 每轮请求前由前端按画布状态与画布档案决定传哪些工具（AI 只在被给到工具时才可能调用）：
 * - clear_workflow 仅在画布非空时提供（是否清空由 AI 决策；空画布没有可清的内容）
 * - 节点类型 enum（get_node_schema/add_node）按画布档案的目录收窄
 * - 画布专属工具（profile.tools.schemas）追加在通用工具之后
 */
export function buildActiveToolSchemas(lf: any, profile: WorkflowAgentProfile): any[] {
  const hasContent = hasCanvasContent(lf)
  const types = Object.keys(profile.catalog)
  const base = toolSchemas
    .filter((tool: any) => tool.function.name !== 'clear_workflow' || hasContent)
    .map((tool: any) => {
      const name = tool.function.name
      if (name !== 'get_node_schema' && name !== 'add_node') return tool
      const clone = cloneDeep(tool)
      if (name === 'get_node_schema') clone.function.parameters.properties.types.items.enum = types
      if (name === 'add_node') {
        // 画布固有节点（addable=false，如处理器 start-node）可查文档但不可添加
        clone.function.parameters.properties.type.enum = types.filter(
          (type) => profile.catalog[type].addable !== false
        )
      }
      return clone
    })
  return [...base, ...(profile.tools?.schemas ?? [])]
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
            // enum 由 buildActiveToolSchemas 按画布档案的目录注入
            items: { type: 'string' },
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
          // enum 由 buildActiveToolSchemas 按画布档案的目录注入
          type: { type: 'string', description: '节点类型' },
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
