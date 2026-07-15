import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ContextManageNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class ContextManageNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ContextManageNode)
  }
}
export default {
  type: 'context-manage-node',
  model: RootModel,
  view: ContextManageNodeView
}
