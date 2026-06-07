import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import ListSkillsNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class ListSkillsNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, ListSkillsNode)
  }
}

export default {
  type: 'list-skills-node',
  model: RootModel,
  view: ListSkillsNodeView
}
