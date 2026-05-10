import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import CacheQueryNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class CacheQueryNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, CacheQueryNode)
  }
}
export default {
  type: 'cache-query-node',
  model: RootModel,
  view: CacheQueryNodeView
}
