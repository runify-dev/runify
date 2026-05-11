import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import FileUploadNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class FileUploadNodeView extends RootView {
  constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
    super(props, FileUploadNode)
  }
}
export default {
  type: 'file-upload-node',
  model: RootModel,
  view: FileUploadNodeView
}
