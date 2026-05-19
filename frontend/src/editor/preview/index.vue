<template>
  <div>
    <div class="simple-editor-content relative">
      <EditorContent v-if="editor" :editor="editor" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { EditorContent } from '@tiptap/vue-3'
import { shallowRef, watch, onBeforeUnmount } from 'vue'
import newInstance from '@/editor/editor/index'

import '../editor/nodes/index.scss'
const props = defineProps<{
  modelValue: string
}>()

const editor = shallowRef<ReturnType<typeof newInstance> | null>(null)

let flushTimer: ReturnType<typeof setTimeout> | null = null
let latestContent = ''
let destroyed = false

function safeMarkdown(md: string): string {
  return md
}

editor.value = newInstance('', undefined, false)

function flush() {
  if (destroyed || !editor.value || editor.value.isDestroyed) return
  try {
    if (editor.value.commands?.setContent) {
      editor.value.commands.setContent(safeMarkdown(latestContent), { contentType: 'markdown' })
    }
  } catch (e) {
    console.warn('setContent failed', e)
  }
  flushTimer = null
}

watch(
  () => props.modelValue,
  (val) => {
    if (destroyed || !editor.value || editor.value.isDestroyed) return
    latestContent = val
    if (flushTimer) return
    flushTimer = setTimeout(flush, 100)
  },
  { flush: 'post', immediate: true }
)

onBeforeUnmount(() => {
  destroyed = true
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }
  editor.value?.destroy()
})
</script>
<style lang="scss" scoped>
:deep(.simple-editor-content) .tiptap.ProseMirror.simple-editor {
  padding: 0;
}
.bubble-menu {
  background-color: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: var(--p-content-border-radius);
  box-shadow: var(--p-overlay-popover-shadow);
  display: flex;
  padding: 0.2rem;

  button {
    background-color: unset;

    &:hover {
      background-color: var(--p-content-hover-background);
    }

    &.is-active {
      background-color: var(--p-primary-color);

      &:hover {
        background-color: var(--p-primary-600);
      }
    }
  }
}
</style>
