import { HtmlResize } from '@logicflow/extension'
import { BaseNode, BaseNodeModel, GraphModel, Point, type Model, h as lh } from '@logicflow/core'
import { isActive, connect, disconnect } from './teleport'
import { generateAnchor } from '@/utils/common'
import { anchorIconMap } from '@/workflow/common/data'
const getNodeName = (model: BaseNodeModel) => {
  const eqTypeNodes = model.graphModel.nodes.filter(
    (node: any) => node.id != model.id && node.type == model.type
  )
  const originalName = model.properties.name
  let i = 0
  while (true) {
    const currentName = i > 0 ? `${originalName}${i}` : originalName
    const find = eqTypeNodes.find((n: any) => n.properties.name === currentName)
    if (find) {
      i = i + 1
    } else {
      return currentName
    }
  }
}
class RootView extends HtmlResize.view {
  component: any
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }, vueNode: any) {
    super(props)
    this.component = vueNode
    if (!props.model.properties.nodeData) {
      props.model.properties.name = getNodeName(props.model)
    }
  }
  setHtml(rootEl: SVGForeignObjectElement): void {
    if (!rootEl.innerHTML) {
      const node = document.createElement('div')
      rootEl.appendChild(node)
      this.renderVueComponent(node)
    }
  }
  protected targetId() {
    return `${this.props.graphModel.flowId}:${this.props.model.id}`
  }
  protected renderVueComponent(root: any) {
    const { model, graphModel } = this.props
    if (root) {
      if (isActive()) {
        connect(this.targetId(), this.component, root, model, graphModel)
      }
    }
  }
  unmount() {
    if (isActive()) {
      disconnect(this.targetId())
    }
  }
  getAnchorShape(anchorData: any) {
    const { x, y, direction } = anchorData

    let isConnect = false

    if (direction == 'left') {
      isConnect = this.props.graphModel.edges.some((edge) => edge.targetAnchorId == anchorData.id)
    } else {
      isConnect = this.props.graphModel.edges.some((edge) => edge.sourceAnchorId == anchorData.id)
    }
    return lh(
      'foreignObject',
      {
        ...anchorData,
        x: x - 10,
        y: y - 9,
        width: 20,
        height: 20
      },
      [
        lh('div', {
          style: { zindex: 0 },
          onClick: () => {
            if (direction == 'right') {
              this.props.model.openNodeMenu(anchorData)
            }
          },
          dangerouslySetInnerHTML: {
            __html: isConnect
              ? anchorIconMap[direction][anchorData.status].connected
              : anchorIconMap[direction][anchorData.status].not_connected
          }
        })
      ]
    )
  }
}
class RootModel extends HtmlResize.model {
  refreshDegrees() {
    this.incoming.edges.forEach((edge: any) => {
      edge.updatePathByAnchor()
    })
    this.outgoing.edges.forEach((edge: any) => {
      edge.updatePathByAnchor()
    })
  }
  getResizeOutlineStyle() {
    const style = super.getResizeOutlineStyle()
    style.stroke = 'none'
    return style
  }
  getControlPointStyle() {
    const style = super.getControlPointStyle()
    style.stroke = 'none'
    style.fill = 'none'
    return style
  }
  getNodeStyle() {
    return {
      overflow: 'visible'
    }
  }
  getOutlineStyle() {
    const style = super.getOutlineStyle()
    style.stroke = 'none'
    if (style.hover) {
      style.hover.stroke = 'none'
    }
    return style
  }

  getAnchorStyle(_anchorInfo: any) {
    if (_anchorInfo.type == 'right' && _anchorInfo.status == 'fail') {
      return {
        stroke: 'var(--el-color-danger)',
        fill: '#fff',
        r: 4
      }
    }
    return {
      stroke: 'var(--el-color-success)',
      fill: '#fff',
      r: 4
    }
  }
  getDefaultAnchor() {
    const { id, x, y, width, type } = this
    const anchors: any = []
    if (type.toString() !== 'Base') {
      if (type.toString() !== 'start-node') {
        anchors.push({
          x: x - width / 2,
          y: y,
          id: generateAnchor(id, 'left', 'main', 'success'),
          edgeAddable: false,
          branch: 'main',
          status: 'success',
          direction: 'left'
        })
      }
      anchors.push({
        x: x + width / 2,
        y: this.properties.errorCaptureEnabled ? y - 10 : y,
        id: generateAnchor(id, 'right', 'main', 'success'),
        direction: 'right',
        branch: 'main',
        status: 'success'
      })
      if (this.properties.errorCaptureEnabled) {
        anchors.push({
          x: x + width / 2,
          y: y + 15,
          id: generateAnchor(id, 'right', 'main', 'fail'),
          direction: 'right',
          branch: 'main',
          status: 'fail'
        })
      }
    }

    return anchors
  }
}

export { RootView, RootModel }
