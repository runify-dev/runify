import { Editor } from '@tiptap/vue-3'
import { findParentNode, posToDOMRect } from '@tiptap/core'

export interface OperationMenu {
  compoent: any
  shouldShow: (editor: Editor) => boolean
  getReferencedVirtualElement: (editor: Editor) => any | null
  options: () => any
}

export const getListVirtualElement = (
  editor: Editor,
  nodeName: string,
  findParentNodeCall?: (node: any) => boolean,
) => {
  if (!findParentNodeCall) {
    findParentNodeCall = (node: any) => node.type.name === nodeName
  }
  const parentNode = findParentNode(findParentNodeCall)(editor.state.selection)

  if (parentNode) {
    const domRect = posToDOMRect(
      editor.view,
      parentNode.start,
      parentNode.start + parentNode.node.nodeSize,
    )
    return {
      getBoundingClientRect: () => domRect,
      getClientRects: () => [domRect],
    }
  }
  return null
}
