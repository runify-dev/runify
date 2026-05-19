import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import FileDownloadNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'

class FileDownloadNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, FileDownloadNode)
  }
}

export default {
  type: 'file-download-node',
  model: RootModel,
  view: FileDownloadNodeView
}
