import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import TerminalNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class TerminalNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, TerminalNode)
  }
}

export default {
  type: 'terminal-node',
  model: RootModel,
  view: TerminalNodeView
}
