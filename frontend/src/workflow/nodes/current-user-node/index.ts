import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import currentUserNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class CurrentUserNodeView extends RootView {
    constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
        super(props, currentUserNode)

    }
}
export default {
    type: 'current-user-node',
    model: RootModel,
    view: CurrentUserNodeView
}
