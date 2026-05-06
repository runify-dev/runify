import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import VariableAssignNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class VariableAssignNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, VariableAssignNode)
  }
}

export default {
  type: 'variable-assign-node',
  model: RootModel,
  view: VariableAssignNodeView
}
