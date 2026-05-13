import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import GrepNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class GrepNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, GrepNode)
  }
}

export default {
  type: 'grep-node',
  model: RootModel,
  view: GrepNodeView
}
