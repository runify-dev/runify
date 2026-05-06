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
const useProvide=(model:any)=>{
const getNodeFieldOptions = (withSelf?:boolean) => {
  const getUpNode = (id: string, result: Array<any>) => {
    const upNodes = model.graphModel.getNodeIncomingNode(id)
    upNodes.forEach((node: any) => {
      result.push(node)
      getUpNode(node.id, result)
    })
    return result
  }
  const upNodes = getUpNode(model.id, withSelf?[model]:[])

  const result = upNodes.filter(node=>node.properties.field_list&&node.properties.field_list.length>0).map((node) => {
    return {
      label: node.properties.name,
      value: node.id,
      disabled: true,
      children: node.properties.field_list.map((item: any) => ({
        label: item.label,
        value: item.value,
        children: item.children
      }))
    }
  })

  // 合并父级变量（子画布场景）
  const parentGetOptions = model.graphModel.getParentNodeFieldOptions
  if (parentGetOptions) {
    parentGetOptions().forEach((item: any) => {
      result.push(item)
    })
  }

  getGlobalFieldOptions().forEach((item) => {
    result.push(item)
  })
  return result
}

const getGlobalFieldOptions = () => {
  const result = []
   // 合并父级变量（子画布场景）
  const parentGetOptions = model.graphModel.parentGetGlobalFieldOptions
  if (parentGetOptions) {
    parentGetOptions().forEach((item: any) => {
      result.push(item)
    })
  }
  const startNode = model.graphModel.getNodeModelById('start-node')
  if (startNode) {
    if(startNode?.properties?.globalFieldList&&startNode?.properties?.globalFieldList.length>0){
  result.push({
      label: 'global',
      disabled: true,
      value: 'global',
      children: startNode?.properties?.globalFieldList || []
    })
    }

  }
  const getParentModel= model.graphModel.getParentModel;
  if(getParentModel){
    const pm=getParentModel()
   result.push({
      label: pm.properties.name,
      value: pm.id,
      disabled: true,
      children: pm.properties.field_list.map((item: any) => ({
        label: item.label,
        value: item.value,
        children: item.children
      }))
    })
  }
  return result
}

const flattenVariables = (nodes: any[], parentLabel = '', parentValue = ''): any[] => {
  const result: any[] = []

  for (const node of nodes) {
    if (node.disabled) {
      // 分组节点，递归子级，传递当前 label 作为前缀
      result.push(...flattenVariables(node.children ?? [], node.label, node.label))
    } else {
      const value = parentValue ? `${parentValue}.${node.value}` : node.value
      const label = parentLabel ? `${parentLabel} / ${node.label}` : node.label
      result.push({ value, label, children: node.children })
    }
  }

  return result
}
const  getTemplateVariables=() => {
  return flattenVariables(getNodeFieldOptions())
}

return {getGlobalFieldOptions,getNodeFieldOptions,getTemplateVariables}
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
      node.setAttribute('data-node-id', this.props.model.id)
      node.setAttribute('data-node-type', this.props.model.type)
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
        connect(this.targetId(), this.component, root, model, graphModel,useProvide(model))
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
              const model= this.props.model;
              const appendNode = (node: any, anchorData: any) => {
                const nodeModel = model.graphModel.addNode({
                  type: node.type,
                  properties: node.properties,
                  x: anchorData.x + node.properties.width / 2 + 200,
                  y: anchorData.y
                })

                model.graphModel.addEdge({
                  type: 'run-edge',
                  sourceNodeId: model.id,
                  sourceAnchorId: anchorData.id,
                  targetNodeId: nodeModel.id,
                  targetAnchorId: generateAnchor(nodeModel.id, 'left', 'main', 'success')
                })
                return Promise.resolve({ message: '添加成功' })
              }
              model.graphModel.eventCenter.emit('runify:node:open-add-node-dialog', {
                call: appendNode,
                anchorData: anchorData
              })

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
      overflow: 'visible',
      fill: 'none'
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
        stroke: 'var(--p-red-500)',
        fill: '#fff',
        r: 4
      }
    }
    return {
      stroke: 'var(--p-primary-color)',
      fill: '#fff',
      r: 4
    }
  }
  getDefaultAnchor() {
    const { id, x, y, width, type } = this
    const anchors: any = []
    if (type.toString() !== 'Base') {
      if (type.toString() !== 'start-node' && type.toString() !== 'loop-start-node') {
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
