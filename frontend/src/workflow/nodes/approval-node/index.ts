import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ApprovalNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class ApprovalNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ApprovalNode)
  }
}

export default {
  type: 'approval-node',
  model: RootModel,
  view: ApprovalNodeView
}
