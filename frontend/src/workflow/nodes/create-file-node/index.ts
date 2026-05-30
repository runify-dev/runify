import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import CreateFileNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class CreateFileNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, CreateFileNode)
  }
}

export default {
  type: 'create-file-node',
  model: RootModel,
  view: CreateFileNodeView
}
