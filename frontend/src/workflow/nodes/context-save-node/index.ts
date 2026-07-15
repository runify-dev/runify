import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ContextSaveNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class ContextSaveNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ContextSaveNode)
  }
}
export default {
  type: 'context-save-node',
  model: RootModel,
  view: ContextSaveNodeView
}
