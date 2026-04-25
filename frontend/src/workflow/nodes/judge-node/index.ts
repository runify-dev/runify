import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import JudgeNode from './index.vue'
import { generateAnchor } from '@/utils/common'
import { RootModel, RootView } from '@/workflow/common/node'
import { defaulBranches } from '@/workflow/common/data'
const CONDITION_HEIGHT = 28
const CONDITION_LOGIC_HEIGHT = 10
const ELSE_HEIGHT = 28
const BRANCH_GAP = 6

const NODE_HEADER_HEIGHT = 50
const CONTENT_PADDING_TOP = 8

function getNodeBranches(properties: any) {
  const branches = properties?.nodeData?.branches

  if (Array.isArray(branches) && branches.length > 0) {
    return branches
  }

  return defaulBranches
}

function buildBranchAnchors(options: {
  nodeId: string
  x: number
  nodeX: number
  nodeY: number
  nodeHeight: number
  branches: Array<{
    id: string
    type: 'if' | 'elseif' | 'else'
    conditions?: any[]
  }>
}) {
  const { nodeId, x, nodeY, nodeHeight, branches } = options

  const nodeTop = nodeY - nodeHeight / 2
  let currentTop = nodeTop + NODE_HEADER_HEIGHT + CONTENT_PADDING_TOP

  return branches.map((branch, index) => {
    const branchHeight = getBranchHeight(branch)
    const branchY = currentTop + branchHeight / 2

    currentTop += branchHeight + BRANCH_GAP

    const branchId = branch.id || `${branch.type}_${index}`

    return {
      x,
      y: branchY,
      id: generateAnchor(nodeId, 'right', branchId, 'success'),
      direction: 'right',
      branch: branchId,
      branchType: branch.type,
      branchIndex: index,
      status: 'success'
    }
  })
}

function getBranchHeight(branch: { type: 'if' | 'elseif' | 'else'; conditions?: any[] }) {
  if (branch.type === 'else') {
    return ELSE_HEIGHT
  }

  const conditionCount =
    Array.isArray(branch.conditions) && branch.conditions.length > 0 ? branch.conditions.length : 1

  return (
    conditionCount * CONDITION_HEIGHT + Math.max(conditionCount - 1, 0) * CONDITION_LOGIC_HEIGHT
  )
}

function getFailAnchorY(options: {
  nodeY: number
  nodeHeight: number
  branches: Array<{
    type: 'if' | 'elseif' | 'else'
    conditions?: any[]
  }>
}) {
  const { nodeY, nodeHeight, branches } = options

  const nodeTop = nodeY - nodeHeight / 2
  const contentTop = nodeTop + NODE_HEADER_HEIGHT + CONTENT_PADDING_TOP

  const totalBranchHeight =
    branches.reduce((sum, branch) => sum + getBranchHeight(branch), 0) +
    Math.max(branches.length - 1, 0) * BRANCH_GAP

  return contentTop + totalBranchHeight
}
class JudgeNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, JudgeNode)
  }
}
class JudgeNodeModel extends RootModel {
  setHeight(height: number) {
    const sourceHeight = this.height
    const targetHeight = height
    this.height = targetHeight
    this.properties['height'] = targetHeight
    this.move(0, (targetHeight - sourceHeight) / 2)
    this.outgoing.edges.forEach((edge: any) => {
      // 调用自定义的更新方案
      edge.updatePathByAnchor()
    })
    this.incoming.edges.forEach((edge: any) => {
      // 调用自定义的更新方案
      edge.updatePathByAnchor()
    })
  }
  getDefaultAnchor() {
    const { id, x, y, width, height, type } = this

    const anchors: any[] = []

    if (type.toString() === 'Base') {
      return anchors
    }

    if (type.toString() !== 'start-node') {
      anchors.push({
        x: x - width / 2,
        y,
        id: generateAnchor(id, 'left', 'main', 'success'),
        edgeAddable: false,
        branch: 'main',
        status: 'success',
        direction: 'left'
      })
    }

    const branches = getNodeBranches(this.properties)

    anchors.push(
      ...buildBranchAnchors({
        nodeId: id,
        x: x + width / 2,
        nodeX: x,
        nodeY: y,
        nodeHeight: height,
        branches
      })
    )

    if (this.properties.errorCaptureEnabled) {
      anchors.push({
        x: x + width / 2,
        y: getFailAnchorY({
          nodeY: y,
          nodeHeight: height,
          branches
        }),
        id: generateAnchor(id, 'right', 'main', 'fail'),
        direction: 'right',
        branch: 'main',
        status: 'fail'
      })
    }

    return anchors
  }
}

export default {
  type: 'judge-node',
  model: JudgeNodeModel,
  view: JudgeNodeView
}
