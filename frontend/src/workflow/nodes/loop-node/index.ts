import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import LoopNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class LoopNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, LoopNode)
  }
}

class LoopNodeModel extends RootModel {
}

export default {
  type: 'loop-node',
  model: LoopNodeModel,
  view: LoopNodeView
}
