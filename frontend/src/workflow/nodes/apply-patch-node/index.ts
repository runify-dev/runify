import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ApplyPatchNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class ApplyPatchNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ApplyPatchNode)
  }
}

export default {
  type: 'apply-patch-node',
  model: RootModel,
  view: ApplyPatchNodeView
}
