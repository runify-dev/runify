const startNode = {
    type: 'start-node',
    text: "",
    x: 0,
    y: 0,
    label: "开始节点",
    properties: {
        width: 250,
        height: 50,
        name: "开始节点",
        isHovered: false,
        field_list: [
            {
                label: '用户问题',
                value: 'question'
            }
        ]
    }
}
const aiChatNode = {
    type: 'ai-chat-node',
    text: "",
    x: 0,
    y: 0,
    label: "ai对话",
    properties: {
        width: 250,
        height: 50,
        name: "ai对话",
        isHovered: false,
        field_list: [
            {
                label: '回答',
                value: 'content'
            }
        ]
    }
}

export type WorkflowType = 'APPLICATION'

class NodeMeta {
    node: any
    group: string
    supportWorkflowTypeList: Array<WorkflowType>
    imgSrc: string
    constructor(node: any, group: string, supportWorkflowTypeList: Array<WorkflowType>,
        imgSrc: string) {
        this.node = node
        this.group = group
        this.supportWorkflowTypeList = supportWorkflowTypeList
        this.imgSrc = imgSrc
    }
}
const nodeMetaList = [
    // new NodeMeta(startNode, '基本节点', ['APPLICATION'], "/ui/login.jpg"),
    new NodeMeta(aiChatNode, '基本节点', ['APPLICATION'], "/ui/login.jpg"),
]

export const getNodeMenuList = (workflowType: WorkflowType) => {
    return nodeMetaList.filter(item => item.supportWorkflowTypeList.includes(workflowType))
        .reduce((result: any, item) => {
            if (!result[item.group]) {
                result[item.group] = [];
            }
            result[item.group].push(item);
            return result
        }, {})
}