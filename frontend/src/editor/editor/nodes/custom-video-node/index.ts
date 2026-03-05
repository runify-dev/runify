import { Node, mergeAttributes } from "@tiptap/core"
import { Plugin, PluginKey } from "@tiptap/pm/state"
import { VueNodeViewRenderer } from "@tiptap/vue-3"
import { splitBlock } from "@tiptap/pm/commands"
import VideoBlockView from "./index.vue"

export interface VideoBlockOptions {
  HTMLAttributes: Record<string, any>
  upload?: (file: File) => Promise<string>
}

export const CustomVideoBlock = Node.create<VideoBlockOptions>({
  name: "videoBlock",

  group: "block",

  atom: true,
  selectable: true,
  draggable: true,
  isolating: true,

  addOptions() {
    return {
      HTMLAttributes: {},
      upload: undefined,
    }
  },

  addAttributes() {
    return {
      src: { default: null },
      title: { default: "" },
      poster: { default: "" },
      currentTime: { default: 0 },
    }
  },

  markdownTokenName: "html_block",
  // @ts-ignore
  parseMarkdown(token: any) {
    const html: string = token.content ?? ""
    if (!/^<video[\s>]/i.test(html.trim())) return null

    let src = ""
    let title = ""
    let poster = ""

    try {
      const doc = new DOMParser().parseFromString(html, "text/html")
      const v = doc.querySelector("video")
      if (!v) return null
      src = v.getAttribute("src") ?? v.querySelector("source")?.getAttribute("src") ?? ""
      title = v.getAttribute("title") ?? ""
      poster = v.getAttribute("poster") ?? ""
    } catch {
      src = html.match(/\bsrc=["']([^"']+)["']/i)?.[1] ?? ""
      title = html.match(/\btitle=["']([^"']*)["']/i)?.[1] ?? ""
      poster = html.match(/\bposter=["']([^"']*)["']/i)?.[1] ?? ""
    }

    if (!src) return null
    return { type: "videoBlock", attrs: { src, title, poster } }
  },

  renderMarkdown(node: any) {
    const { src = "", title = "", poster = "" } = node.attrs ?? {}
    const posterAttr = poster ? ` poster="${poster}"` : ""
    const titleAttr = title ? ` title="${title}"` : ""
    return `<video src="${src}"${posterAttr}${titleAttr}></video>\n\n`
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="video-block"]',
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
        getAttrs: (el) => {
          const video = el as HTMLVideoElement
          const src =
            video.getAttribute("src") ||
            video.querySelector("source")?.getAttribute("src") ||
            ""
          return {
            src,
            title: video.getAttribute("title") ?? "",
            poster: video.getAttribute("poster") ?? "",
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
  // @ts-ignore
  addCommands(self: any) {
    return {
      setVideoBlock:
        (options: any) =>
          ({ commands }: any) => {
            return commands.insertContent({ type: this.name, attrs: options })
          },
    }
  },

  addProseMirrorPlugins() {
    const upload = this.options.upload

    const handleFile = (file: File, view: any) => {
      if (!file.type.startsWith("video/")) return false

      const insertVideoNode = (url: string) => {
        const { state, dispatch } = view
        const { $from } = state.selection

        const node = state.schema.nodes.videoBlock.create({
          src: url,
          title: file.name,
        })

        // 使用 splitBlock 的返回值 tr 而不是立即 dispatch
        splitBlock(state, (tr) => {
          tr.replaceSelectionWith(node).scrollIntoView()
          dispatch(tr)
          return true
        })
      }

      if (upload) {
        upload(file)
          .then(insertVideoNode)
          .catch((err) => {
            console.error("视频上传失败:", err)
            alert("视频上传失败")
          })
      } else {
        insertVideoNode(URL.createObjectURL(file))
      }

      return true
    }

    return [
      new Plugin({
        key: new PluginKey("videoBlockFile"),

        props: {
          handlePaste(view, event) {
            const items = event.clipboardData?.items
            if (!items) return false
            for (const item of items) {
              if (item.kind === "file") {
                const file = item.getAsFile()
                if (file && handleFile(file, view)) return true
              }
            }
            return false
          },

          handleDrop(view, event) {
            const files = event.dataTransfer?.files
            if (!files?.length) return false
            for (const file of files) {
              if (handleFile(file, view)) return true
            }
            return false
          },

          handleKeyDown(view, event) {
            if (event.key !== "Backspace") return false
            const { state } = view
            const { $from } = state.selection
            const nodeBefore = $from.nodeBefore
            if (nodeBefore?.type.name === "videoBlock") {
              view.dispatch(
                state.tr.delete($from.pos - nodeBefore.nodeSize, $from.pos)
              )
              return true
            }
            return false
          },
        },
      }),
    ]
  },
})
