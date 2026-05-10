import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import DatabaseInsertNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class DatabaseInsertNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, DatabaseInsertNode)
  }
}
export default {
  type: 'database-insert-node',
  model: RootModel,
  view: DatabaseInsertNodeView
}
