import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import DatabaseSearchNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class DatabaseSearchNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, DatabaseSearchNode)

  }
}
export default {
  type: 'database-search-node',
  model: RootModel,
  view: DatabaseSearchNodeView
}
