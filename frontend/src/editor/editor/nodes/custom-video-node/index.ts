import { createAtomBlockMarkdownSpec, Node, mergeAttributes, Extension } from "@tiptap/core"
import { Plugin, PluginKey } from "@tiptap/pm/state"
import { VueNodeViewRenderer } from "@tiptap/vue-3"
import VideoBlockView from "./index.vue"

export const VideoBlockBackspaceGuard = Extension.create({
  name: 'videoBlockBackspaceGuard',

  addKeyboardShortcuts() {
    return {
      Backspace: () => {
        const { state, dispatch } = this.editor.view
        const { selection } = state
        const { $from, empty } = selection

        if (!empty || $from.parentOffset !== 0) return false

        const before = $from.before($from.depth)
        const nodeBefore = state.doc.resolve(before).nodeBefore

        if (nodeBefore?.type.name === 'videoBlock') {
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

export interface VideoBlockOptions {
  HTMLAttributes: Record<string, any>
  upload?: (file: File, onProgress?: (percent: number) => void) => Promise<string>
  inline: boolean
}

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    videoBlock: {
      setVideoBlock: (options: {
        src: string
        title?: string
        poster?: string
      }) => ReturnType
    }
  }
}

export const CustomVideoBlock = Node.create<VideoBlockOptions>({
  name: "videoBlock",

  addOptions() {
    return {
      HTMLAttributes: {},
      upload: undefined,
      inline: false,
    }
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
      title: { default: "" },
      poster: { default: "" },
      currentTime: { default: 0 },
      uploadProgress: { default: null },
      uploadId: { default: null, rendered: false },
    }
  },

  // ★ 核心修复：使用自定义 block 级 tokenizer
  // 让 MarkedJS 在词法分析阶段就把 <video> 识别为独立的 block token
  // 完全绕开 html_block / html_inline 歧义问题
  markdownTokenizer: {
    name: 'videoBlock',
    level: 'block',
    // 快速预检，只有包含 <video 才进入 tokenize，提升性能
    start: (src: string) => src.search(/<video[\s>]/i),
    tokenize: (src: string) => {
      // 匹配 <video ...></video> 或 <video .../> 形式
      // 支持多行属性，使用非贪婪匹配
      const match = /^[ \t]*<video([\s\S]*?)(?:\/>|><\/video>)/i.exec(src)
      if (!match) return undefined

      const attrs = match[1] ?? ''
      const srcAttr = attrs.match(/\bsrc=["']([^"']+)["']/i)?.[1] ?? ''

      // src 是必须的，没有就不处理
      if (!srcAttr) return undefined

      const titleAttr = attrs.match(/\btitle=["']([^"']*)["']/i)?.[1] ?? ''
      const posterAttr = attrs.match(/\bposter=["']([^"']*)["']/i)?.[1] ?? ''

      return {
        type: 'videoBlock',
        raw: match[0],
        // 把解析好的属性挂在 token 上，parseMarkdown 直接取用
        videoSrc: srcAttr,
        videoTitle: titleAttr,
        videoPoster: posterAttr,
      }
    },
  },

  // ★ 对应 markdownTokenizer 生成的 token，转换为 ProseMirror 节点
  parseMarkdown(token: any) {
    if (!token.videoSrc) return null
    return {
      type: 'videoBlock',
      attrs: {
        src: token.videoSrc,
        title: token.videoTitle ?? '',
        poster: token.videoPoster ?? '',
      },
    }
  },

  // ★ 确保序列化输出干净，前后双换行，不产生 &nbsp;
  renderMarkdown(node: any) {
    const { src = "", title = "", poster = "" } = node.attrs ?? {}
    if (!src) return ""
    const t = title ? ` title="${title}"` : ""
    const p = poster ? ` poster="${poster}"` : ""
    return `<video src="${src}"${t}${p}></video>\n\n`
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="video-block"]',
        isBlock: true,
        ignoreDom: false,
        getAttrs: (el) => {
          const div = el as HTMLElement
          return {
            src: div.getAttribute("data-src") ?? "",
            title: div.getAttribute("data-title") ?? "",
            poster: div.getAttribute("data-poster") ?? "",
          }
        },
      },
      {
        tag: "video",
        isBlock: true,
        ignoreDom: false,
        getAttrs: (el) => {
          const v = el as HTMLVideoElement
          return {
            src: v.getAttribute("src") || v.querySelector("source")?.getAttribute("src") || "",
            title: v.getAttribute("title") ?? "",
            poster: v.getAttribute("poster") ?? "",
          }
        },
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      "div",
      mergeAttributes(this.options.HTMLAttributes, {
        "data-type": "video-block",
        "data-src": HTMLAttributes.src ?? "",
        "data-title": HTMLAttributes.title ?? "",
        "data-poster": HTMLAttributes.poster ?? "",
      }),
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(VideoBlockView)
  },

  addCommands() {
    return {
      setVideoBlock:
        (options) =>
          ({ commands }) =>
            commands.insertContent({
              type: this.name,
              attrs: options,
            }),
    }
  },

  addProseMirrorPlugins() {
    const self = this

    const cleanupPlugin = new Plugin({
      key: new PluginKey("videoBlockCleanup"),

      appendTransaction(transactions, oldState, newState) {
        if (transactions.some(tr => tr.getMeta("videoBlockCleanup"))) {
          return null
        }

        if (!transactions.some(tr => tr.docChanged)) {
          return null
        }

        const tr = newState.tr
        let modified = false

        transactions.forEach(transaction => {
          transaction.steps.forEach((step: any) => {
            const map = step.getMap()
            map.forEach((_oldStart: number, _oldEnd: number, newStart: number, newEnd: number) => {
              newState.doc.nodesBetween(newStart, newEnd, (node, pos) => {
                if (node.type.name !== "videoBlock") return

                const nextPos = pos + node.nodeSize
                const nextNode = newState.doc.nodeAt(nextPos)

                if (
                  nextNode &&
                  nextNode.type.name === "paragraph" &&
                  nextNode.content.size === 0
                ) {
                  tr.delete(nextPos, nextPos + nextNode.nodeSize)
                  modified = true
                }
              })
            })
          })
        })

        if (!modified) return null

        tr.setMeta("videoBlockCleanup", true)
        tr.setMeta("addToHistory", false)

        return tr
      },
    })

    const insertVideoNode = (file: File) => {
      const upload = self.options.upload
      const uploadId = `upload_${Date.now()}_${Math.random().toString(36).slice(2)}`

      const { state, dispatch } = self.editor.view
      const nodeType = state.schema.nodes.videoBlock
      if (!nodeType) return

      const videoNode = nodeType.create({
        src: null,
        title: file.name,
        uploadProgress: 0,
        uploadId,
      })

      const insertPos = state.selection.to
      const insertTr = state.tr.insert(insertPos, videoNode)
      insertTr.setMeta('addToHistory', false)
      dispatch(insertTr)

      const findByUploadId = (): number => {
        let pos = -1
        self.editor.state.doc.descendants((node, p) => {
          if (node.type.name === 'videoBlock' && node.attrs.uploadId === uploadId) {
            pos = p
            return false
          }
        })
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
        self.editor.view.dispatch(patchTr)
      }

      const doUpload = upload
        ? upload(file, (pct) => patchAttrs({ uploadProgress: Math.min(Math.round(pct), 99) }))
        : Promise.resolve(URL.createObjectURL(file))

      doUpload
        .then((url) => patchAttrs({ src: url, uploadProgress: null, uploadId: null }))
        .catch((err) => {
          console.error("视频上传失败:", err)
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
      key: new PluginKey("videoBlockFile"),
      props: {
        handlePaste(view, event) {
          const items = event.clipboardData?.items
          if (!items) return false

          const videoFiles: File[] = []
          for (const item of Array.from(items)) {
            if (item.kind === "file" && item.type.startsWith("video/")) {
              const file = item.getAsFile()
              if (file) videoFiles.push(file)
            }
          }

          if (videoFiles.length === 0) return false

          event.preventDefault()

          setTimeout(() => {
            for (const file of videoFiles) {
              insertVideoNode(file)
            }
          }, 0)

          return true
        },

        handleDrop(view, event) {
          const files = event.dataTransfer?.files
          if (!files?.length) return false

          const videoFiles: File[] = []
          for (const file of Array.from(files)) {
            if (file.type.startsWith("video/")) videoFiles.push(file)
          }

          if (videoFiles.length === 0) return false

          event.preventDefault()

          setTimeout(() => {
            for (const file of videoFiles) {
              insertVideoNode(file)
            }
          }, 0)

          return true
        },
      },
    })

    return [cleanupPlugin, filePlugin]
  },
})
