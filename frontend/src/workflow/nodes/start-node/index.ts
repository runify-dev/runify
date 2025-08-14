import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import StartNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class StartNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, StartNode)

  }
}
export default {
  type: 'start-node',
  model: RootModel,
  view: StartNodeView
}
