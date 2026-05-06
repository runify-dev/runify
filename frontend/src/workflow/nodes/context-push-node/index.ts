import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ContextPushNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class ContextPushNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ContextPushNode)
  }
}

export default {
  type: 'context-push-node',
  model: RootModel,
  view: ContextPushNodeView
}
