import { Node, mergeAttributes, Extension } from "@tiptap/core"
import { Plugin, PluginKey } from "@tiptap/pm/state"
import { VueNodeViewRenderer } from "@tiptap/vue-3"
import FileBlockView from "./index.vue"

export const FileBlockBackspaceGuard = Extension.create({
  name: 'fileBlockBackspaceGuard',
  addKeyboardShortcuts() {
    return {
      Backspace: () => {
        const { state, dispatch } = this.editor.view
        const { selection } = state
        const { $from, empty } = selection
        if (!empty || $from.parentOffset !== 0) return false
        const before = $from.before($from.depth)
        const nodeBefore = state.doc.resolve(before).nodeBefore
        if (nodeBefore?.type.name === 'fileBlock') {
          if ($from.parent.content.size === 0) {
            dispatch(state.tr.delete(before, before + $from.parent.nodeSize))
          } else {
            dispatch(state.tr.delete(before - nodeBefore.nodeSize, before))
          }
          return true
        }
        return false
      },
    }
  },
})

export interface FileBlockOptions {
  HTMLAttributes: Record<string, any>
  upload?: (file: File, onProgress?: (percent: number) => void) => Promise<string>
  /** 允许下载：是否显示下载按钮，默认 true */
  allowDownload: boolean
}

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    fileBlock: {
      setFileBlock: (options: {
        src: string
        name?: string
        size?: number
        mime?: string
      }) => ReturnType
    }
  }
}

export const CustomFileBlock = Node.create<FileBlockOptions>({
  name: "fileBlock",

  addOptions() {
    return { HTMLAttributes: {}, upload: undefined, allowDownload: true }
  },

  group: "block",
  inline: false,
  atom: true,
  selectable: true,
  draggable: true,
  isolating: true,
  defining: true,
  content: "",

  addAttributes() {
    return {
      src: { default: null },
      name: { default: "" },
      size: { default: 0 },
      mime: { default: "" },
      uploadProgress: { default: null },
      uploadId: { default: null, rendered: false },
    }
  },

  markdownTokenizer: {
    name: 'fileBlock',
    level: 'block',
    start: (src: string) => src.indexOf('!file['),
    tokenize: (src: string) => {
      // !file[name](url){size=123 mime=application/pdf}
      const match = /^!file\[([^\]]*)\]\(([^)]+)\)(?:\{([^}]*)\})?/.exec(src)
      if (!match) return undefined
      const meta = match[3] ?? ''
      const get = (key: string) => meta.match(new RegExp(`${key}=([^\\s}]+)`))?.[1] ?? ''
      return {
        type: 'fileBlock',
        raw: match[0],
        fileSrc: match[2],
        fileName: match[1] ?? '',
        fileSize: parseInt(get('size') || '0', 10) || 0,
        fileMime: get('mime'),
      }
    },
  },

  // @ts-ignore
  parseMarkdown(token: any) {
    if (!token.fileSrc) return null
    return {
      type: 'fileBlock',
      attrs: {
        src: token.fileSrc,
        name: token.fileName ?? '',
        size: token.fileSize ?? 0,
        mime: token.fileMime ?? '',
      },
    }
  },

  renderMarkdown(node: any) {
    const { src = "", name = "", size = 0, mime = "" } = node.attrs ?? {}
    if (!src) return ""
    const meta = [
      size ? `size=${size}` : '',
      mime ? `mime=${mime}` : '',
    ].filter(Boolean).join(' ')
    return `!file[${name}](${src})${meta ? `{${meta}}` : ''}\n\n`
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="file-block"]',
        getAttrs: (el) => {
          const div = el as HTMLElement
          return {
            src: div.getAttribute("data-src") ?? "",
            name: div.getAttribute("data-name") ?? "",
            size: parseInt(div.getAttribute("data-size") ?? "0", 10),
            mime: div.getAttribute("data-mime") ?? "",
          }
        },
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      "div",
      mergeAttributes(this.options.HTMLAttributes, {
        "data-type": "file-block",
        "data-src": HTMLAttributes.src ?? "",
        "data-name": HTMLAttributes.name ?? "",
        "data-size": HTMLAttributes.size ?? 0,
        "data-mime": HTMLAttributes.mime ?? "",
      }),
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(FileBlockView)
  },

  addCommands() {
    return {
      setFileBlock:
        (options) =>
          ({ commands }) =>
            commands.insertContent({ type: this.name, attrs: options }),
    }
  },

  addProseMirrorPlugins() {
    const self = this

    const cleanupPlugin = new Plugin({
      key: new PluginKey("fileBlockCleanup"),
      appendTransaction(transactions, _oldState, newState) {
        if (transactions.some(tr => tr.getMeta("fileBlockCleanup"))) return null
        if (!transactions.some(tr => tr.docChanged)) return null

        const ranges: Array<[number, number]> = []
        for (const transaction of transactions) {
          for (const step of transaction.steps as any[]) {
            step.getMap().forEach((_os: number, _oe: number, ns: number, ne: number) => {
              ranges.push([ns, ne])
            })
          }
        }
        if (ranges.length === 0) return null

        ranges.sort((a, b) => a[0] - b[0])
        const merged: Array<[number, number]> = [ranges[0]]
        for (let i = 1; i < ranges.length; i++) {
          const last = merged[merged.length - 1]
          if (ranges[i][0] <= last[1]) {
            last[1] = Math.max(last[1], ranges[i][1])
          } else {
            merged.push(ranges[i])
          }
        }

        const tr = newState.tr
        let modified = false
        for (const [start, end] of merged) {
          newState.doc.nodesBetween(start, end, (node, pos) => {
            if (node.type.name !== "fileBlock") return true
            const nextPos = pos + node.nodeSize
            const nextNode = newState.doc.nodeAt(nextPos)
            if (nextNode?.type.name === "paragraph" && nextNode.content.size === 0) {
              tr.delete(nextPos, nextPos + nextNode.nodeSize)
              modified = true
            }
          })
        }
        if (!modified) return null
        tr.setMeta("fileBlockCleanup", true)
        tr.setMeta("addToHistory", false)
        return tr
      },
    })

    const insertFileNode = (file: File) => {
      const upload = self.options.upload
      const uploadId = `upload_${Date.now()}_${Math.random().toString(36).slice(2)}`

      const { state, dispatch } = self.editor.view
      const nodeType = state.schema.nodes.fileBlock
      if (!nodeType) return

      const fileNode = nodeType.create({
        src: null,
        name: file.name,
        size: file.size,
        mime: file.type,
        uploadProgress: 0,
        uploadId,
      })

      const insertPos = state.selection.to
      const insertTr = state.tr.insert(insertPos, fileNode)
      insertTr.setMeta('addToHistory', false)
      dispatch(insertTr)

      let cachedPos: number = insertPos

      const findByUploadId = (): number => {
        const node = self.editor.state.doc.nodeAt(cachedPos)
        if (node?.type.name === 'fileBlock' && node.attrs.uploadId === uploadId) return cachedPos
        let pos = -1
        self.editor.state.doc.descendants((n, p) => {
          if (n.type.name === 'fileBlock' && n.attrs.uploadId === uploadId) {
            pos = p; return false
          }
        })
        if (pos !== -1) cachedPos = pos
        return pos
      }

      const patchAttrs = (patch: Record<string, any>) => {
        const s = self.editor.state
        const pos = findByUploadId()
        if (pos === -1) return
        const n = s.doc.nodeAt(pos)
        if (!n) return
        const patchTr = s.tr.setNodeMarkup(pos, undefined, { ...n.attrs, ...patch })
        patchTr.setMeta('addToHistory', false)
        cachedPos = pos
        self.editor.view.dispatch(patchTr)
      }

      const doUpload = upload
        ? upload(file, (pct) => patchAttrs({ uploadProgress: Math.min(Math.round(pct), 99) }))
        : Promise.resolve(URL.createObjectURL(file))

      doUpload
        .then((url) => patchAttrs({ src: url, uploadProgress: null, uploadId: null }))
        .catch((err) => {
          console.error("文件上传失败:", err)
          const s = self.editor.state
          const pos = findByUploadId()
          if (pos === -1) return
          const n = s.doc.nodeAt(pos)
          if (n) {
            const delTr = s.tr.delete(pos, pos + n.nodeSize)
            delTr.setMeta('addToHistory', false)
            self.editor.view.dispatch(delTr)
          }
        })
    }

    const filePlugin = new Plugin({
      key: new PluginKey("fileBlockFile"),
      props: {
        handlePaste(_view, event) {
          const items = event.clipboardData?.items
          if (!items) return false
          // 排除图片/视频/音频（已有专门扩展）
          const files = Array.from(items)
            .filter(item => item.kind === "file" && !item.type.startsWith("image/") && !item.type.startsWith("video/") && !item.type.startsWith("audio/"))
            .map(item => item.getAsFile())
            .filter((f): f is File => f !== null)
          if (files.length === 0) return false
          event.preventDefault()
          setTimeout(() => files.forEach(insertFileNode), 0)
          return true
        },
        handleDrop(_view, event) {
          const files = event.dataTransfer?.files
          if (!files?.length) return false
          const validFiles = Array.from(files).filter(f =>
            !f.type.startsWith("image/") && !f.type.startsWith("video/") && !f.type.startsWith("audio/")
          )
          if (validFiles.length === 0) return false
          event.preventDefault()
          setTimeout(() => validFiles.forEach(insertFileNode), 0)
          return true
        },
      },
    })

    return [cleanupPlugin, filePlugin]
  },
})
