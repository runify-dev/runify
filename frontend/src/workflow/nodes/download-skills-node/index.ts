import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import DownloadSkillsNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class DownloadSkillsNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, DownloadSkillsNode)
  }
}

export default {
  type: 'download-skills-node',
  model: RootModel,
  view: DownloadSkillsNodeView
}
