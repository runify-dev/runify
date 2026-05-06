import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import LoopBreakNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class LoopBreakNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, LoopBreakNode)
  }
}

class LoopBreakNodeModel extends RootModel {
}

export default {
  type: 'loop-break-node',
  model: LoopBreakNodeModel,
  view: LoopBreakNodeView
}
