<template>
  <div class="md-editor" :class="{ focused: isFocused }">
    <EditorContent
      :editor="editor"
      @focus="isFocused = true; emit('focus')"
      @blur="isFocused = false; emit('blur')"
    />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, shallowRef, watch } from 'vue'
import { EditorContent, Editor } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Markdown } from '@tiptap/markdown'

const props = defineProps<{
  modelValue?: string
  disabled?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'paste-images': [files: File[]]
  'paste-videos': [files: File[]]
  'paste-files': [files: File[]]
  'paste-text': [text: string]
  submit: []
  focus: []
  blur: []
}>()

const LONG_TEXT_THRESHOLD = 300

const isFocused = shallowRef(false)

const editor = new Editor({
  editorProps: {
    attributes: {
      class: 'md-editor-input',
      'data-placeholder': props.placeholder || '发送消息…'
    },
    handleKeyDown: (_, event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        emit('submit')
        return true
      }
      return false
    },
    handlePaste: (_, event) => {
      const items = event.clipboardData?.items
      if (!items) return false

      const fileItems = Array.from(items)
        .filter((item) => item.kind === 'file')
        .map((item) => item.getAsFile())
        .filter(Boolean) as File[]

      if (fileItems.length) {
        event.preventDefault()

        const images = fileItems.filter((f) => f.type.startsWith('image/'))
        const videos = fileItems.filter((f) => f.type.startsWith('video/'))
        const others = fileItems.filter(
          (f) => !f.type.startsWith('image/') && !f.type.startsWith('video/')
        )

        if (images.length) emit('paste-images', images)
        if (videos.length) emit('paste-videos', videos)
        if (others.length) emit('paste-files', others)
        return true
      }

      // 大文本
      const text = event.clipboardData?.getData('text/plain') || ''
      if (text.length > LONG_TEXT_THRESHOLD) {
        event.preventDefault()
        emit('paste-text', text)
        return true
      }

      return false
    }
  },
  extensions: [
    StarterKit.configure({
      heading: false,
      horizontalRule: false
    }),
    Markdown
  ],
  content: props.modelValue || '',
  onUpdate: ({ editor: ed }) => {
    const md = ed.getMarkdown()
    emit('update:modelValue', md)
  }
})

watch(
  () => props.disabled,
  (val) => {
    editor.setEditable(!val)
  },
  { immediate: true }
)

watch(
  () => props.modelValue,
  (val) => {
    const md = editor.getMarkdown()
    if (val === md) return
    editor.commands.setContent(val || '', { contentType: 'markdown' })
  }
)

const clear = () => {
  editor.commands.clearContent()
}

const focus = () => {
  editor.commands.focus('end')
}

defineExpose({ clear, focus, editor })

onBeforeUnmount(() => {
  editor.destroy()
})
</script>

<style scoped>
.md-editor {
  flex: 1;
  min-width: 0;
}

.md-editor :deep(.tiptap) {
  outline: none;
  font-size: 14px;
  font-family: inherit;
  color: var(--t1);
  line-height: 1.6;
  min-height: 44px;
  max-height: 160px;
  overflow-y: auto;
  padding: 4px 0;
  margin: 0;
  word-break: break-word;
}

.md-editor :deep(.tiptap) p {
  margin: 0;
}

.md-editor :deep(.tiptap) p + p {
  margin-top: 4px;
}

/* placeholder */
.md-editor :deep(.tiptap)p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  color: var(--t3);
  pointer-events: none;
  float: left;
  height: 0;
}

/* inline code */
.md-editor :deep(.tiptap) code {
  background: var(--hv);
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 0.9em;
  color: var(--t1);
  font-family: monospace;
}

/* code block */
.md-editor :deep(.tiptap) pre {
  background: var(--hv);
  padding: 8px 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 4px 0;
}

.md-editor :deep(.tiptap) pre code {
  background: transparent;
  padding: 0;
  font-size: 13px;
}

/* bold */
.md-editor :deep(.tiptap) strong {
  font-weight: 600;
  color: var(--t1);
}

/* italic */
.md-editor :deep(.tiptap) em {
  font-style: italic;
}

/* strikethrough */
.md-editor :deep(.tiptap) s {
  text-decoration: line-through;
  color: var(--t3);
}

/* blockquote */
.md-editor :deep(.tiptap) blockquote {
  border-left: 3px solid var(--bd);
  padding-left: 10px;
  color: var(--t2);
  margin: 4px 0;
}

/* lists */
.md-editor :deep(.tiptap) ul,
.md-editor :deep(.tiptap) ol {
  padding-left: 20px;
  margin: 4px 0;
}

.md-editor :deep(.tiptap) li {
  color: var(--t2);
}

/* links */
.md-editor :deep(.tiptap) a {
  color: var(--t1);
  text-decoration: underline;
  text-underline-offset: 2px;
}
</style>
