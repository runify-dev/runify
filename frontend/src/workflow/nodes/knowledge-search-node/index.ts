import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import KnowledgeSearchNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class KnowledgeSearchNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, KnowledgeSearchNode)
  }
}
export default {
  type: 'knowledge-search-node',
  model: RootModel,
  view: KnowledgeSearchNodeView
}
