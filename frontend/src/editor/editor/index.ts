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
  CustomVideoBlock,
  CustomAudioBlock,
  CustomFileBlock

} from './nodes/index'
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-expect-error
import { BubbleMenu } from '@tiptap/vue-3/menus'
import FileAPI from '@/api/file'
import type { Ref } from 'vue'

const newInstance = (content?: string, onUpdate?: (editor: any) => void, editable?: boolean,) => {
  const editor: Editor = new Editor({
    editorProps: {
      attributes: {
        'aria-label': 'Main content area, start typing to enter text.',
        class: 'simple-editor',
      },
    },
    editable: editable !== undefined ? editable : true,
    extensions: [
      StarterKit.configure({ codeBlock: false }),
      HorizontalRule,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      TaskList,
      TaskItem.configure({ nested: true }),
      Highlight.configure({ multicolor: true }),
      Image.configure({
        upload: async (file, onProgress) => {
          const fd = new FormData()
          fd.append('file', file)
          return FileAPI.uploadFile(fd, onProgress).then((ok) => {
            return `./api/storage/file/${ok.data.id}`
          })
        }
      }),
      Typography,
      Superscript,
      Subscript,
      Selection,
      Markdown,
      CodeBlockLowlight,
      TableKit,
      Mathematics,
      BubbleMenu,
      CustomFileBlock.configure({
        upload: async (file, onProgress) => {
          const fd = new FormData()
          fd.append('file', file)
          return FileAPI.uploadFile(fd, onProgress).then((ok) => {
            return `./api/storage/file/${ok.data.id}`
          })
        }
      }),
      CustomAudioBlock.configure({
        upload: async (file, onProgress) => {
          const fd = new FormData()
          fd.append('file', file)
          return FileAPI.uploadFile(fd, onProgress).then((ok) => {
            return `./api/storage/file/${ok.data.id}`
          })
        }
      }),
      CustomVideoBlock.configure({
        upload: async (file, onProgress) => {
          const fd = new FormData()
          fd.append('file', file)
          return FileAPI.uploadFile(fd, onProgress).then((ok) => {
            return `./api/storage/file/${ok.data.id}`
          })
        }
      })
    ],
    onUpdate: onUpdate,
    content: content ? content : null,
    contentType: 'markdown', // parse initial content as Markdown

  })
  return editor
}
export default newInstance
