import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import LoopContinueNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class LoopContinueNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, LoopContinueNode)
  }
}

class LoopContinueNodeModel extends RootModel {
}

export default {
  type: 'loop-continue-node',
  model: LoopContinueNodeModel,
  view: LoopContinueNodeView
}
