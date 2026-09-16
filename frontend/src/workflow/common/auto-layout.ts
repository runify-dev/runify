import dagre from 'dagre'

/**
 * 对给定 lf 的画布做 Dagre LR 自动布局：整理节点坐标、按出口锚点 Y 排序同源多出边、刷新连线，并适配视野。
 * 纯函数、作用于传入的 lf（主画布或循环体子画布皆可），供 AI 生成每配好一个节点后美化画布，也供工具栏「自动布局」复用。
 */
export function layoutCanvas(lf: any): void {
  const graphModel = lf?.graphModel
  if (!graphModel || !graphModel.nodes?.length) return

  const nodeMap = new Map<string, any>(graphModel.nodes.map((n: any) => [n.id, n]))

  // 按源节点分组，对每个节点的出边按其出口锚点 Y 坐标排序（多分支节点出边顺序稳定）
  const edgesBySource = new Map<string, any[]>()
  graphModel.edges.forEach((edge: any) => {
    if (!edgesBySource.has(edge.sourceNodeId)) edgesBySource.set(edge.sourceNodeId, [])
    edgesBySource.get(edge.sourceNodeId)!.push(edge)
  })
  const sortedEdges: any[] = []
  edgesBySource.forEach((edges, nodeId) => {
    const sourceNode = nodeMap.get(nodeId)
    if (sourceNode) {
      edges.sort((a, b) => {
        const ay = sourceNode.anchors?.find((an: any) => an.id === a.sourceAnchorId)?.y ?? 0
        const by = sourceNode.anchors?.find((an: any) => an.id === b.sourceAnchorId)?.y ?? 0
        return ay - by
      })
    }
    sortedEdges.push(...edges)
  })

  const g = new dagre.graphlib.Graph()
  g.setGraph({ rankdir: 'LR', align: '', nodesep: 60, ranksep: 100 })
  g.setDefaultEdgeLabel(() => ({}))
  graphModel.nodes.forEach((node: any) =>
    g.setNode(node.id, { width: node.width || 150, height: node.height || 50 })
  )
  sortedEdges.forEach((edge: any) => g.setEdge(edge.sourceNodeId, edge.targetNodeId))
  dagre.layout(g)

  graphModel.nodes.forEach((node: any) => {
    const pos = g.node(node.id)
    if (pos) {
      node.x = pos.x
      node.y = pos.y
    }
  })
  graphModel.edges.forEach((edge: any) => edge.updatePathByAnchor?.())
  lf.fitView?.(40, 40)
}
