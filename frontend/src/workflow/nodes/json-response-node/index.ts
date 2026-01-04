import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import JSONResponseNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class JSONResponseNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, JSONResponseNode)

  }
}
export default {
  type: 'json-response-node',
  model: RootModel,
  view: JSONResponseNodeView
}
