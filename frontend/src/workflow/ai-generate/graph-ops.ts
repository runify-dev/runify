import { generateAnchor } from '@/utils/common'

/**
 * 图结构纯函数：judge 分支按 id 合并、工作流结构 lint
 * 不依赖 LogicFlow 实例，方便单测
 */

export interface GraphLike {
  nodes: any[]
  edges: any[]
}

export interface LintResult {
  /** 结构性错误（阻断 valid）：孤立节点、悬空边、非法分支边 */
  errors: string[]
  /** 结构性警告（不阻断，但需模型确认）：同名同类型重复节点等 */
  warnings: string[]
}

/**
 * 合并 judge 分支：incoming 里带 id 的沿用；缺 id 的按「类型 + 顺序」认领旧分支 id，
 * 认领不到才留给 normalize 生成新 id。返回最终被移除的旧分支 id（其出边需要清理）。
 *
 * 背景：judge 出边锚点 id 就是分支 id，若整体覆盖 branches 且模型没带原 id，
 * normalize 会全量重新生成 id，导致已有出边全部悬空。
 */
export function mergeJudgeBranches(
  existing: any[],
  incoming: any[]
): { branches: any[]; removedIds: string[] } {
  const existingList = Array.isArray(existing) ? existing : []
  const incomingList = Array.isArray(incoming) ? incoming : []
  const existingIds = new Set(existingList.map((b: any) => b?.id).filter(Boolean))
  const claimed = new Set<string>()
  for (const branch of incomingList) {
    if (branch?.id && existingIds.has(branch.id)) claimed.add(branch.id)
  }
  for (const branch of incomingList) {
    if (!branch || branch.id) continue
    const match = existingList.find(
      (b: any) => b?.id && !claimed.has(b.id) && b.type === branch.type
    )
    if (match) {
      branch.id = match.id
      claimed.add(match.id)
    }
  }
  const finalIds = new Set(incomingList.map((b: any) => b?.id).filter(Boolean))
  const removedIds = [...existingIds].filter((id) => !finalIds.has(id))
  return { branches: incomingList, removedIds }
}

/** 从 sourceAnchorId 解析分支段与状态段；格式 `${nodeId}_right_${branch}_${status}` */
function parseSourceAnchor(
  sourceNodeId: string,
  sourceAnchorId: string | undefined
): { branch: string; status: string } | null {
  if (!sourceAnchorId) return null
  const prefix = `${sourceNodeId}_right_`
  if (!sourceAnchorId.startsWith(prefix)) return null
  const rest = sourceAnchorId.slice(prefix.length)
  const cut = rest.lastIndexOf('_')
  if (cut <= 0) return null
  return { branch: rest.slice(0, cut), status: rest.slice(cut + 1) }
}

function nodeLabel(node: any): string {
  const name = node?.properties?.name
  return name ? `「${name}」(${node.id})` : `(${node.id})`
}

/**
 * 结构 lint（递归循环子画布）：
 * - errors：孤立节点（除入口外无入边）、悬空边（端点不存在）、非法出边锚点
 *   （judge 分支 id 不存在 / 非 judge 节点用了非 main 分支 / 失败分支）
 * - warnings：同名同类型重复节点
 */
export function lintWorkflowGraph(
  graph: GraphLike,
  scopeLabel = '主画布',
  entryId = 'start-node'
): LintResult {
  const errors: string[] = []
  const warnings: string[] = []
  const nodes = graph?.nodes ?? []
  const edges = graph?.edges ?? []
  const nodeMap = new Map<string, any>(nodes.map((n: any) => [n.id, n]))

  // 同名同类型重复节点
  const byTypeName = new Map<string, any[]>()
  for (const node of nodes) {
    const name = node?.properties?.name
    if (!name) continue
    const key = `${node.type}|${name}`
    const list = byTypeName.get(key) ?? []
    list.push(node)
    byTypeName.set(key, list)
  }
  for (const list of byTypeName.values()) {
    if (list.length > 1) {
      warnings.push(
        `${scopeLabel}存在 ${list.length} 个同名同类型节点「${list[0].properties.name}」` +
          `(${list[0].type})：${list.map((n: any) => n.id).join(', ')}。` +
          `若属误重复添加请 delete_node 删除多余节点；确属业务需要请改名区分`
      )
    }
  }

  // 孤立节点：除入口外都应有入边
  const hasIncoming = new Set(edges.map((e: any) => e.targetNodeId))
  for (const node of nodes) {
    if (node.id === entryId) continue
    if (!hasIncoming.has(node.id)) {
      errors.push(`${scopeLabel}节点${nodeLabel(node)}没有入边（孤立节点），请连线或删除`)
    }
  }

  // 连线合法性
  for (const edge of edges) {
    const source = nodeMap.get(edge.sourceNodeId)
    const target = nodeMap.get(edge.targetNodeId)
    if (!source || !target) {
      errors.push(
        `${scopeLabel}存在悬空连线 ${edge.sourceNodeId} → ${edge.targetNodeId}` +
          `（${!source ? '源' : '目标'}节点不存在），请 delete_edge 清理`
      )
      continue
    }
    const anchor = parseSourceAnchor(edge.sourceNodeId, edge.sourceAnchorId)
    if (!anchor) continue
    if (anchor.status === 'fail') {
      errors.push(`${scopeLabel}连线 ${nodeLabel(source)} → ${nodeLabel(target)} 使用了失败分支（暂不支持）`)
      continue
    }
    if (source.type === 'judge-node') {
      const branchIds = (source.properties?.nodeData?.branches ?? [])
        .map((b: any) => b?.id)
        .filter(Boolean)
      if (!branchIds.includes(anchor.branch)) {
        errors.push(
          `${scopeLabel}连线 ${nodeLabel(source)} → ${nodeLabel(target)} 引用的分支 ` +
            `${anchor.branch} 已不存在（现有分支：${branchIds.join(', ')}），` +
            `请 delete_edge 后按现有分支重新连线`
        )
      }
    } else if (anchor.branch !== 'main') {
      errors.push(
        `${scopeLabel}连线 ${nodeLabel(source)} → ${nodeLabel(target)} 的出口锚点非法` +
          `（非 judge 节点只有 main 出口）`
      )
    }
  }

  // 递归循环子画布
  for (const node of nodes) {
    if (node.type !== 'loop-node') continue
    const children = node?.properties?.nodeData?.children
    if (!children) continue
    const child = lintWorkflowGraph(
      { nodes: children.nodes ?? [], edges: children.edges ?? [] },
      `循环${nodeLabel(node)}内`,
      'loop-start-node'
    )
    errors.push(...child.errors)
    warnings.push(...child.warnings)
  }

  return { errors, warnings }
}
