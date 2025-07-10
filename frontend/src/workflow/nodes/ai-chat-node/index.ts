import type { BaseNodeModel, GraphModel } from '@logicflow/core'
import aiChatNode from './index.vue'
import { RootModel, RootView } from '@/workflow/common/node'
class AiChatNodeView extends RootView {
    constructor(props: { model: BaseNodeModel; graphModel: GraphModel }) {
        super(props, aiChatNode)

    }
}
export default {
    type: 'ai-chat-node',
    model: RootModel,
    view: AiChatNodeView
}
