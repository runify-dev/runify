import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import RunSkillNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class RunSkillNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, RunSkillNode)
  }
}

export default {
  type: 'run-skill-node',
  model: RootModel,
  view: RunSkillNodeView
}
