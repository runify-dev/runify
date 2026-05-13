import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ListDirNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class ListDirNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ListDirNode)
  }
}

export default {
  type: 'list-dir-node',
  model: RootModel,
  view: ListDirNodeView
}
