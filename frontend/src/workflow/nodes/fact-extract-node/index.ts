import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import FactExtractNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class FactExtractNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, FactExtractNode)
  }
}
export default {
  type: 'fact-extract-node',
  model: RootModel,
  view: FactExtractNodeView
}
