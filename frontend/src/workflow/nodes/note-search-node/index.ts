import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import NoteSearchNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class NoteSearchNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, NoteSearchNode)
  }
}
export default {
  type: 'note-search-node',
  model: RootModel,
  view: NoteSearchNodeView
}
