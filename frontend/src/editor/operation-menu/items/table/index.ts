import { Editor } from '@tiptap/vue-3'
import { type OperationMenu, getListVirtualElement } from '../index'
import Table from './index.vue'
const data: Array<OperationMenu> = [
  {
    compoent: Table,
    shouldShow: (editor: Editor) => editor.isActive('table'),
    getReferencedVirtualElement: (editor: Editor) => getListVirtualElement(editor, 'table'),
    options: () => ({ placement: 'top-end', offset: 8 }),
  },
]
export default data
