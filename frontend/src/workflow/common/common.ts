
import LogicFlow from '@logicflow/core'
export class WorkflowAPI {
  lg: LogicFlow
  constructor(lg: LogicFlow) {
    this.lg = lg
  }
  /**
   * 重命名
   * @param name 重命名的名称
   * @returns 是否成功
   */
  rename = (node_id: string, name: string) => {
    const model: any = this.lg.graphModel.getNodeModelById(node_id)
    const nodes = this.lg.graphModel.nodes
    if (nodes.filter((node: any) => node.id !== node_id).some((node: any) => node.properties.stepName === name.trim())) {
      return Promise.reject({ "message": "节点名称已存在" })
    }
    model.properties.name = name
    console.log(model)
    return Promise.resolve({ "data": "修改成功" })
  }

  addNode = (node: any) => {

  }

}

