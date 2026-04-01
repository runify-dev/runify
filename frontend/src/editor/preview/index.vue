<template>
  <div>
    <div class="simple-editor-content relative">
      <EditorContent :editor="editor" />
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
    editor.value.commands.setContent(safeMarkdown(latestContent), { contentType: 'markdown' })
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
  background-color: var(--white);
  border: 1px solid var(--gray-1);
  border-radius: 0.7rem;
  box-shadow: var(--shadow);
  display: flex;
  padding: 0.2rem;

  button {
    background-color: unset;

    &:hover {
      background-color: var(--gray-3);
    }

    &.is-active {
      background-color: var(--purple);

      &:hover {
        background-color: var(--purple-contrast);
      }
    }
  }
}
</style>
