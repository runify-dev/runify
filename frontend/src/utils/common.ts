import { type Node, type Tree } from "@/api/type/node"
import { cloneDeep } from "lodash"
export const toTree = (nodeList: Array<Tree>) => {
  nodeList = cloneDeep(nodeList)
  const nodeMap = Object.fromEntries(nodeList.map(item => [item.id, item]))
  const childrenList = new Set<string>();
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
        childrenList.add(element.id)
        pNode.children.push(element)
      }
    }
  }
  return nodeList.filter(item => !childrenList.has(item.id))
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

export const formatDateTime = (date = new Date()) => {
  const pad = (n: any) => n.toString().padStart(2, '0')

  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}
