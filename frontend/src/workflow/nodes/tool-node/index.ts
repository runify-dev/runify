import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ToolNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class ToolNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ToolNode)
  }
}
export default {
  type: 'tool-node',
  model: RootModel,
  view: ToolNodeView
}
