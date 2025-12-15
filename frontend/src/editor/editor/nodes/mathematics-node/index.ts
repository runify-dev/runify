import { Mathematics as M } from '@tiptap/extension-mathematics'
import bus from '@/bus'
export const Mathematics = M.configure({
  inlineOptions: {
    // optional options for the inline math node
  },
  blockOptions: {
    onClick: (node: any, pos: number) => {
      bus.emit("edit-mathematics-block", node.attrs.latex)
    }
  },
  katexOptions: {
    // optional options for the KaTeX renderer
  }
})
