import { type Node, type Tree } from "@/api/type/node"
export const toTree = (nodeList: Array<Tree>) => {
    const nodeMap = Object.fromEntries(nodeList.map(item => [item.id, item]))
    for (let index = 0; index < nodeList.length; index++) {
        const element = nodeList[index];
        if (!element.children) {
            element.children = []
        }
        if (element.parentId) {
            const pNode = nodeMap[element.parentId]
            if (pNode) {
                if (!pNode.children) {
                    pNode.children = []
                }
                pNode.children.push(element)
            }

        }
    }
    return nodeList.filter(item => !item.parentId)
}