import { Node, mergeAttributes, Extension } from "@tiptap/core"
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
      setVideoBlock: (options: { src: string; title?: string; poster?: string }) => ReturnType
    }
  }
}

export const CustomVideoBlock = Node.create<VideoBlockOptions>({
  name: "videoBlock",

  addOptions() {
    return { HTMLAttributes: {}, upload: undefined, inline: false }
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

  markdownTokenizer: {
    name: 'videoBlock',
    level: 'block',
    // ★ 优化：indexOf 比 RegExp.search 快约 3-5x，作为廉价预检
    start: (src: string) => src.toLowerCase().indexOf('<video'),
    tokenize: (src: string) => {
      const match = /^[ \t]*<video([\s\S]*?)(?:\/>|><\/video>)/i.exec(src)
      if (!match) return undefined
      const attrs = match[1] ?? ''
      const srcAttr = attrs.match(/\bsrc=["']([^"']+)["']/i)?.[1] ?? ''
      if (!srcAttr) return undefined
      return {
        type: 'videoBlock',
        raw: match[0],
        videoSrc: srcAttr,
        videoTitle: attrs.match(/\btitle=["']([^"']*)["']/i)?.[1] ?? '',
        videoPoster: attrs.match(/\bposter=["']([^"']*)["']/i)?.[1] ?? '',
      }
    },
  },

  // @ts-ignore
  parseMarkdown(token: any) {
    if (!token.videoSrc) return null
    return {
      type: 'videoBlock',
      attrs: { src: token.videoSrc, title: token.videoTitle ?? '', poster: token.videoPoster ?? '' },
    }
  },

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
            commands.insertContent({ type: this.name, attrs: options }),
    }
  },

  addProseMirrorPlugins() {
    const self = this

    const cleanupPlugin = new Plugin({
      key: new PluginKey("videoBlockCleanup"),

      appendTransaction(transactions, _oldState, newState) {
        if (transactions.some(tr => tr.getMeta("videoBlockCleanup"))) return null
        if (!transactions.some(tr => tr.docChanged)) return null

        // ★ 优化：把所有 step 的变更范围合并成一组不重叠区间
        // 避免多个 step 范围重叠导致同一节点被重复检查
        const ranges: Array<[number, number]> = []
        for (const transaction of transactions) {
          for (const step of transaction.steps as any[]) {
            step.getMap().forEach(
              (_os: number, _oe: number, ns: number, ne: number) => {
                ranges.push([ns, ne])
              }
            )
          }
        }

        if (ranges.length === 0) return null

        // 合并重叠区间（先排序再合并）
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

        const docSize = newState.doc.content.size
        const tr = newState.tr
        let modified = false
        for (const [start, end] of merged) {
          const clampedStart = Math.max(0, Math.min(start, docSize))
          const clampedEnd = Math.max(0, Math.min(end, docSize))
          if (clampedStart >= clampedEnd) continue
          newState.doc.nodesBetween(clampedStart, clampedEnd, (node, pos) => {
            if (node.type.name !== "audioBlock") return true
            const nextPos = pos + node.nodeSize
            if (nextPos > docSize) return true   // ← 额外守卫
            const nextNode = newState.doc.nodeAt(nextPos)
            if (nextNode?.type.name === "paragraph" && nextNode.content.size === 0) {
              tr.delete(nextPos, nextPos + nextNode.nodeSize)
              modified = true
            }
          })
        }

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

      // ★ 优化：缓存节点位置 + 用 mapping 追踪，避免每次进度回调都 descendants 全树扫描
      // 初始插入后记录位置，后续通过 tr mapping 更新，只在 mapping 失效时才回退到全树搜索
      let cachedPos: number = insertPos

      const findByUploadId = (): number => {
        // 先尝试缓存位置（O(1)）
        const node = self.editor.state.doc.nodeAt(cachedPos)
        if (node?.type.name === 'videoBlock' && node.attrs.uploadId === uploadId) {
          return cachedPos
        }
        // 缓存失效，回退全树搜索（O(n)），并更新缓存
        let pos = -1
        self.editor.state.doc.descendants((n, p) => {
          if (n.type.name === 'videoBlock' && n.attrs.uploadId === uploadId) {
            pos = p
            return false
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
        // ★ 优化：dispatch 后同步更新缓存位置（setNodeMarkup 不改变位置，但保险起见更新）
        cachedPos = pos
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
          // ★ 优化：Array.from 只调用一次，避免重复转换
          const videoFiles = Array.from(items)
            .filter(item => item.kind === "file" && item.type.startsWith("video/"))
            .map(item => item.getAsFile())
            .filter((f): f is File => f !== null)
          if (videoFiles.length === 0) return false
          event.preventDefault()
          // ★ 优化：合并为一个 setTimeout，减少任务队列碎片
          setTimeout(() => videoFiles.forEach(insertVideoNode), 0)
          return true
        },

        handleDrop(_view, event) {
          const files = event.dataTransfer?.files
          if (!files?.length) return false
          const videoFiles = Array.from(files).filter(f => f.type.startsWith("video/"))
          if (videoFiles.length === 0) return false
          event.preventDefault()
          setTimeout(() => videoFiles.forEach(insertVideoNode), 0)
          return true
        },
      },
    })

    return [cleanupPlugin, filePlugin]
  },
})
