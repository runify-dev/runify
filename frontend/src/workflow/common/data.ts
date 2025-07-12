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

export const iconMap = {
    'connected': (color: string) => `<svg width="100%" height="100%" viewBox="0 0 42 42" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <g filter="url(#filter0_d_5119_232585)">
                  <path d="M20.9998 29.8333C28.0875 29.8333 33.8332 24.0876 33.8332 17C33.8332 9.91231 28.0875 4.16663 20.9998 4.16663C13.9122 4.16663 8.1665 9.91231 8.1665 17C8.1665 24.0876 13.9122 29.8333 20.9998 29.8333Z" fill="white"/>
                  <path fill-rule="evenodd" clip-rule="evenodd" d="M20.9998 27.5C26.7988 27.5 31.4998 22.799 31.4998 17C31.4998 11.201 26.7988 6.49996 20.9998 6.49996C15.2008 6.49996 10.4998 11.201 10.4998 17C10.4998 22.799 15.2008 27.5 20.9998 27.5ZM33.8332 17C33.8332 24.0876 28.0875 29.8333 20.9998 29.8333C13.9122 29.8333 8.1665 24.0876 8.1665 17C8.1665 9.91231 13.9122 4.16663 20.9998 4.16663C28.0875 4.16663 33.8332 9.91231 33.8332 17Z" fill="${color}"/>
                  </g>
                  <defs>
                  <filter id="filter0_d_5119_232585" x="-1" y="-1" width="44" height="44" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
                  <feFlood flood-opacity="0" result="BackgroundImageFix"/>
                  <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
                  <feOffset dy="4"/>
                  <feGaussianBlur stdDeviation="4"/>
                  <feComposite in2="hardAlpha" operator="out"/>
                  <feColorMatrix type="matrix" values="0 0 0 0 0.2 0 0 0 0 0.439216 0 0 0 0 1 0 0 0 0.1 0"/>
                  <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_5119_232585"/>
                  <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_5119_232585" result="shape"/>
                  </filter>
                  </defs>
                  </svg>`,
    'not_connected': (color: string) => `<svg width="100%" height="100%" viewBox="0 0 42 42" fill="none" xmlns="http://www.w3.org/2000/svg">
            <g filter="url(#filter0_d_5199_166905)">
            <path d="M20.9998 29.8333C28.0875 29.8333 33.8332 24.0876 33.8332 17C33.8332 9.91231 28.0875 4.16663 20.9998 4.16663C13.9122 4.16663 8.1665 9.91231 8.1665 17C8.1665 24.0876 13.9122 29.8333 20.9998 29.8333Z" fill="${color}"/>
            <path d="M19.8332 11.75C19.8332 11.4278 20.0943 11.1666 20.4165 11.1666H21.5832C21.9053 11.1666 22.1665 11.4278 22.1665 11.75V15.8333H26.2498C26.572 15.8333 26.8332 16.0945 26.8332 16.4166V17.5833C26.8332 17.9055 26.572 18.1666 26.2498 18.1666H22.1665V22.25C22.1665 22.5721 21.9053 22.8333 21.5832 22.8333H20.4165C20.0943 22.8333 19.8332 22.5721 19.8332 22.25V18.1666H15.7498C15.4277 18.1666 15.1665 17.9055 15.1665 17.5833V16.4166C15.1665 16.0945 15.4277 15.8333 15.7498 15.8333H19.8332V11.75Z" fill="white"/>
            </g>
            <defs>
            <filter id="filter0_d_5199_166905" x="-1" y="-1" width="44" height="44" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
            <feFlood flood-opacity="0" result="BackgroundImageFix"/>
            <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
            <feOffset dy="4"/>
            <feGaussianBlur stdDeviation="4"/>
            <feComposite in2="hardAlpha" operator="out"/>
            <feColorMatrix type="matrix" values="0 0 0 0 0.2 0 0 0 0 0.439216 0 0 0 0 1 0 0 0 0.1 0"/>
            <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_5199_166905"/>
            <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_5199_166905" result="shape"/>
            </filter>
            </defs>
            </svg>`
}

export const anchorIconMap: any = {
    'left': {
        'success': {
            connected: iconMap.connected('var(--el-color-primary)'),
            not_connected: iconMap.not_connected('var(--el-color-primary)')
        },
        'fail': {
            connected: iconMap.connected('var(--el-color-error)'),
            not_connected: iconMap.not_connected('var(--el-color-error)')
        }
    },
    'right': {
        'success': {
            connected: iconMap.connected('var(--el-color-primary)'),
            not_connected: iconMap.not_connected('var(--el-color-primary)')
        },
        'fail': {
            connected: iconMap.connected('var(--el-color-error)'),
            not_connected: iconMap.not_connected('var(--el-color-error)')
        }
    }
}