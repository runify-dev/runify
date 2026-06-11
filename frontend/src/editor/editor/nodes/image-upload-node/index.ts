import Image from '@tiptap/extension-image'
import { Plugin, PluginKey } from '@tiptap/pm/state'

export interface ImageUploadOptions {
  upload?: (file: File, onProgress?: (percent: number) => void) => Promise<string>
}

export const ImageUpload = Image.extend<ImageUploadOptions>({
  addOptions() {
    return {
      ...this.parent?.(),
      upload: undefined,
    }
  },

  addProseMirrorPlugins() {
    const parentPlugins = this.parent?.() ?? []
    const self = this

    const imagePastePlugin = new Plugin({
      key: new PluginKey('imagePasteUpload'),
      props: {
        handlePaste(_view, event) {
          const items = event.clipboardData?.items
          if (!items) return false

          const imageFiles = Array.from(items)
            .filter(item => item.kind === 'file' && item.type.startsWith('image/'))
            .map(item => item.getAsFile())
            .filter((f): f is File => f !== null)

          if (imageFiles.length === 0) return false

          event.preventDefault()

          const upload = self.options.upload
          imageFiles.forEach(file => {
            if (upload) {
              const fd = new FormData()
              fd.append('file', file)
              upload(file).then(url => {
                self.editor.chain().focus().setImage({ src: url }).run()
              }).catch(err => {
                console.error('图片上传失败:', err)
              })
            } else {
              const url = URL.createObjectURL(file)
              self.editor.chain().focus().setImage({ src: url }).run()
            }
          })

          return true
        },
        handleDrop(_view, event) {
          const files = event.dataTransfer?.files
          if (!files?.length) return false

          const imageFiles = Array.from(files).filter(f => f.type.startsWith('image/'))
          if (imageFiles.length === 0) return false

          event.preventDefault()

          const upload = self.options.upload
          imageFiles.forEach(file => {
            if (upload) {
              const fd = new FormData()
              fd.append('file', file)
              upload(file).then(url => {
                self.editor.chain().focus().setImage({ src: url }).run()
              }).catch(err => {
                console.error('图片上传失败:', err)
              })
            } else {
              const url = URL.createObjectURL(file)
              self.editor.chain().focus().setImage({ src: url }).run()
            }
          })

          return true
        },
      },
    })

    return [...parentPlugins, imagePastePlugin]
  },
})
