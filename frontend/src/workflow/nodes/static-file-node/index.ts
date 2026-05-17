import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import StaticFileNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class StaticFileNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, StaticFileNode)
  }
}
export default {
  type: 'static-file-node',
  model: RootModel,
  view: StaticFileNodeView
}
