import { createAtomBlockMarkdownSpec, mergeAttributes, Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import VariableView from './index.vue'

export interface VariableOptions {
  HTMLAttributes: Record<string, any>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    variable: {
      setVariable: (options: { value: string; label: string }) => ReturnType
    }
  }
}

export const Variable = Node.create<VariableOptions>({
  name: 'variable',

  addOptions() {
    return { HTMLAttributes: {} }
  },

  group: 'block',
  atom: true,
  selectable: true,
  draggable: false,

  addAttributes() {
    return {
      value: { default: null },
      label: { default: '' },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'span[data-variable]',
        getAttrs: (el) => {
          const span = el as HTMLElement
          return {
            value: span.getAttribute('data-value') ?? '',
            label: span.getAttribute('data-label') ?? '',
          }
        },
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'span',
      mergeAttributes(this.options.HTMLAttributes, {
        'data-variable': '',
        'data-value': HTMLAttributes.value ?? '',
        'data-label': HTMLAttributes.label ?? '',
      }),
      `[${HTMLAttributes.label ?? ''}]`,
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(VariableView)
  },

  addCommands() {
    return {
      setVariable:
        (options) =>
        ({ commands }) =>
          commands.insertContent({ type: this.name, attrs: options }),
    }
  },

  ...createAtomBlockMarkdownSpec({
    nodeName: 'variable',
    allowedAttributes: ['value', 'label'],
  }),

  renderMarkdown: (node: any) => {
    const attrs = node.attrs || {}
    const parts: string[] = []
    if (attrs.value != null) parts.push(`value="${attrs.value}"`)
    if (attrs.label) parts.push(`label="${attrs.label}"`)
    return `:::variable {${parts.join(' ')}} :::\n`
  },
})
