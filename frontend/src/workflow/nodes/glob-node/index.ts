import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import GlobNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class GlobNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, GlobNode)
  }
}

export default {
  type: 'glob-node',
  model: RootModel,
  view: GlobNodeView
}
