import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import CacheWriteNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class CacheWriteNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, CacheWriteNode)
  }
}
export default {
  type: 'cache-write-node',
  model: RootModel,
  view: CacheWriteNodeView
}
