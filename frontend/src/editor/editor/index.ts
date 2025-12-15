import { Editor, useEditor } from '@tiptap/vue-3'
import {
  StarterKit,
  CodeBlockLowlight,
  HorizontalRule,
  TextAlign,
  TaskList,
  TaskItem,
  Highlight,
  Typography,
  Image,
  Superscript,
  Subscript,
  Selection,
  Markdown,
  Mathematics,
  TableKit,
} from './nodes/index'
import { BubbleMenu } from '@tiptap/vue-3/menus'
import type { Ref } from 'vue'
const newInstance = (content?: string, onUpdate?: (editor: any) => void) => {
  const editor = new Editor({
    immediatelyRender: false,
    editorProps: {
      attributes: {
        'aria-label': 'Main content area, start typing to enter text.',
        class: 'simple-editor',
      },
    },
    extensions: [
      StarterKit.configure({ codeBlock: false }),
      HorizontalRule,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      TaskList,
      TaskItem.configure({ nested: true }),
      Highlight.configure({ multicolor: true }),
      Image,
      Typography,
      Superscript,
      Subscript,
      Selection,
      Markdown,
      CodeBlockLowlight,
      TableKit,
      Mathematics,
      BubbleMenu
    ],
    onUpdate: onUpdate,
    content: content ? content : null,
    contentType: 'markdown', // parse initial content as Markdown

  })
  return editor
}
export default newInstance
