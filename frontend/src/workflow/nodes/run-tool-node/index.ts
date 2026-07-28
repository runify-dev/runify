import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import RunToolNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class RunToolNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, RunToolNode)
  }
}

export default {
  type: 'run-tool-node',
  model: RootModel,
  view: RunToolNodeView
}
