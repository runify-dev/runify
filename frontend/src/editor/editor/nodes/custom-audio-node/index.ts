import { Node, mergeAttributes, Extension } from "@tiptap/core"
import { Plugin, PluginKey } from "@tiptap/pm/state"
import { VueNodeViewRenderer } from "@tiptap/vue-3"
import AudioBlockView from "./index.vue"

export const AudioBlockBackspaceGuard = Extension.create({
  name: 'audioBlockBackspaceGuard',
  addKeyboardShortcuts() {
    return {
      Backspace: () => {
        const { state, dispatch } = this.editor.view
        const { selection } = state
        const { $from, empty } = selection
        if (!empty || $from.parentOffset !== 0) return false
        const before = $from.before($from.depth)
        const nodeBefore = state.doc.resolve(before).nodeBefore
        if (nodeBefore?.type.name === 'audioBlock') {
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

export interface AudioBlockOptions {
  HTMLAttributes: Record<string, any>
  upload?: (file: File, onProgress?: (percent: number) => void) => Promise<string>
}

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    audioBlock: {
      setAudioBlock: (options: { src: string; title?: string; artist?: string; cover?: string }) => ReturnType
    }
  }
}

export const CustomAudioBlock = Node.create<AudioBlockOptions>({
  name: "audioBlock",

  addOptions() {
    return { HTMLAttributes: {}, upload: undefined }
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
      artist: { default: "" },
      cover: { default: "" },
      uploadProgress: { default: null },
      uploadId: { default: null, rendered: false },
    }
  },

  markdownTokenizer: {
    name: 'audioBlock',
    level: 'block',
    start: (src: string) => src.toLowerCase().indexOf('<audio'),
    tokenize: (src: string) => {
      const match = /^[ \t]*<audio([\s\S]*?)(?:\/>|><\/audio>)/i.exec(src)
      if (!match) return undefined
      const attrs = match[1] ?? ''
      const srcAttr = attrs.match(/\bsrc=["']([^"']+)["']/i)?.[1] ?? ''
      if (!srcAttr) return undefined
      return {
        type: 'audioBlock',
        raw: match[0],
        audioSrc: srcAttr,
        audioTitle: attrs.match(/\btitle=["']([^"']*)["']/i)?.[1] ?? '',
        audioArtist: attrs.match(/\bartist=["']([^"']*)["']/i)?.[1] ?? '',
        audioCover: attrs.match(/\bcover=["']([^"']*)["']/i)?.[1] ?? '',
      }
    },
  },

  // @ts-ignore
  parseMarkdown(token: any) {
    if (!token.audioSrc) return null
    return {
      type: 'audioBlock',
      attrs: {
        src: token.audioSrc,
        title: token.audioTitle ?? '',
        artist: token.audioArtist ?? '',
        cover: token.audioCover ?? '',
      },
    }
  },

  renderMarkdown(node: any) {
    const { src = "", title = "", artist = "", cover = "" } = node.attrs ?? {}
    if (!src) return ""
    const t = title ? ` title="${title}"` : ""
    const a = artist ? ` artist="${artist}"` : ""
    const c = cover ? ` cover="${cover}"` : ""
    return `<audio src="${src}"${t}${a}${c}></audio>\n\n`
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="audio-block"]',
        getAttrs: (el) => {
          const div = el as HTMLElement
          return {
            src: div.getAttribute("data-src") ?? "",
            title: div.getAttribute("data-title") ?? "",
            artist: div.getAttribute("data-artist") ?? "",
            cover: div.getAttribute("data-cover") ?? "",
          }
        },
      },
      {
        tag: "audio",
        getAttrs: (el) => {
          const a = el as HTMLAudioElement
          return {
            src: a.getAttribute("src") || a.querySelector("source")?.getAttribute("src") || "",
            title: a.getAttribute("title") ?? "",
            artist: a.getAttribute("artist") ?? "",
            cover: a.getAttribute("cover") ?? "",
          }
        },
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      "div",
      mergeAttributes(this.options.HTMLAttributes, {
        "data-type": "audio-block",
        "data-src": HTMLAttributes.src ?? "",
        "data-title": HTMLAttributes.title ?? "",
        "data-artist": HTMLAttributes.artist ?? "",
        "data-cover": HTMLAttributes.cover ?? "",
      }),
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(AudioBlockView)
  },

  addCommands() {
    return {
      setAudioBlock:
        (options) =>
          ({ commands }) =>
            commands.insertContent({ type: this.name, attrs: options }),
    }
  },

  addProseMirrorPlugins() {
    const self = this

    const cleanupPlugin = new Plugin({
      key: new PluginKey("audioBlockCleanup"),
      appendTransaction(transactions, _oldState, newState) {
        if (transactions.some(tr => tr.getMeta("audioBlockCleanup"))) return null
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
        tr.setMeta("audioBlockCleanup", true)
        tr.setMeta("addToHistory", false)
        return tr
      },
    })

    const insertAudioNode = (file: File) => {
      const upload = self.options.upload
      const uploadId = `upload_${Date.now()}_${Math.random().toString(36).slice(2)}`

      const { state, dispatch } = self.editor.view
      const nodeType = state.schema.nodes.audioBlock
      if (!nodeType) return

      const audioNode = nodeType.create({
        src: null,
        title: file.name.replace(/\.[^.]+$/, ''),
        artist: '',
        uploadProgress: 0,
        uploadId,
      })

      const insertPos = state.selection.to
      const insertTr = state.tr.insert(insertPos, audioNode)
      insertTr.setMeta('addToHistory', false)
      dispatch(insertTr)

      let cachedPos: number = insertPos

      const findByUploadId = (): number => {
        const node = self.editor.state.doc.nodeAt(cachedPos)
        if (node?.type.name === 'audioBlock' && node.attrs.uploadId === uploadId) {
          return cachedPos
        }
        let pos = -1
        self.editor.state.doc.descendants((n, p) => {
          if (n.type.name === 'audioBlock' && n.attrs.uploadId === uploadId) {
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
        cachedPos = pos
        self.editor.view.dispatch(patchTr)
      }

      const doUpload = upload
        ? upload(file, (pct) => patchAttrs({ uploadProgress: Math.min(Math.round(pct), 99) }))
        : Promise.resolve(URL.createObjectURL(file))

      doUpload
        .then((url) => patchAttrs({ src: url, uploadProgress: null, uploadId: null }))
        .catch((err) => {
          console.error("音频上传失败:", err)
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
      key: new PluginKey("audioBlockFile"),
      props: {
        handlePaste(view, event) {
          const items = event.clipboardData?.items
          if (!items) return false
          const audioFiles = Array.from(items)
            .filter(item => item.kind === "file" && item.type.startsWith("audio/"))
            .map(item => item.getAsFile())
            .filter((f): f is File => f !== null)
          if (audioFiles.length === 0) return false
          event.preventDefault()
          setTimeout(() => audioFiles.forEach(insertAudioNode), 0)
          return true
        },
        handleDrop(_view, event) {
          const files = event.dataTransfer?.files
          if (!files?.length) return false
          const audioFiles = Array.from(files).filter(f => f.type.startsWith("audio/"))
          if (audioFiles.length === 0) return false
          event.preventDefault()
          setTimeout(() => audioFiles.forEach(insertAudioNode), 0)
          return true
        },
      },
    })

    return [cleanupPlugin, filePlugin]
  },
})
