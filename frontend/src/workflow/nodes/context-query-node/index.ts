import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ContextQueryNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class ContextQueryNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ContextQueryNode)
  }
}
export default {
  type: 'context-query-node',
  model: RootModel,
  view: ContextQueryNodeView
}
