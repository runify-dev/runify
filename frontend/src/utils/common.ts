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

export const generateAnchor = (id: string, direction: 'left' | 'right' | 'top' | 'bottom', branch: 'main' | string, status: 'success' | 'fail',) => {
  return `${id}_${direction}_${branch}_${status}`
}

export const groupBy = (groupArray: Array<any>, key: string | ((item: any) => string)) => {
  return groupArray.reduce((acc: any, item: any) => {
    const _key = typeof key == 'string' ? item[key] : key(item)
    if (!acc[_key]) {
      acc[_key] = []
    }
    // 将当前项添加到对应的分类数组中
    acc[_key].push(item)
    return acc
  }, {})

}
