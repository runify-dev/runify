import { Editor } from '@tiptap/vue-3'
import { findParentNode, posToDOMRect } from '@tiptap/core'

export interface OperationMenu {
  compoent: any
  shouldShow: (editor: Editor) => boolean
  getReferencedVirtualElement: (editor: Editor) => any | null
  options: () => any
}

export const getListVirtualElement = (editor: Editor, nodeName: string) => {
  const finder = (node: any) => node.type.name === nodeName
  // 返回稳定的对象引用，不每次创建新对象
  return {
    getBoundingClientRect: () => {
      const parentNode = findParentNode(finder)(editor.state.selection)
      if (parentNode) {
        return posToDOMRect(editor.view, parentNode.start, parentNode.start + parentNode.node.nodeSize)
      }
      return new DOMRect()
    },
    getClientRects: () => [],
  }
}
