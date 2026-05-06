import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import LoopStartNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class LoopStartNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, LoopStartNode)
  }
}
export default {
  type: 'loop-start-node',
  model: RootModel,
  view: LoopStartNodeView
}
