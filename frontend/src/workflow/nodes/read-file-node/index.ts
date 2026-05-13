import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ReadFileNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class ReadFileNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ReadFileNode)
  }
}

export default {
  type: 'read-file-node',
  model: RootModel,
  view: ReadFileNodeView
}
