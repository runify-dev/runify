import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ExtractNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class ExtractNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ExtractNode)
  }
}

export default {
  type: 'extract-node',
  model: RootModel,
  view: ExtractNodeView
}
