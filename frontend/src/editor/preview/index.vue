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
let animationFrameId: number | null = null
let latestContent = ''
let destroyed = false
let pendingContent: string | null = null

function safeMarkdown(md: string): string {
  return md
}

editor.value = newInstance('', undefined, false)

function safeSetContent(content: string) {
  if (destroyed || !editor.value || editor.value.isDestroyed) return

  try {
    // 确保编辑器完全准备好
    if (!editor.value.view || !(editor.value.view as any).docView) {
      return
    }

    // 使用 markdown 扩展的 parse 方法先转换内容为 JSON
    if (editor.value.markdown && typeof editor.value.markdown.parse === 'function') {
      const jsonDoc = editor.value.markdown.parse(content)
      if (jsonDoc !== null && jsonDoc !== undefined) {
        // setContent 可以直接接受 JSON 文档格式
        editor.value.commands.setContent(jsonDoc)
      }
    } else {
      // 降级：直接设置内容
      editor.value.commands.setContent(content)
    }
  } catch (e) {
    console.warn('setContent failed', e)
  }
}

function flush() {
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }

  if (destroyed || !editor.value || editor.value.isDestroyed) {
    pendingContent = null
    return
  }

  const contentToSet = pendingContent
  pendingContent = null

  if (contentToSet === null) return

  // 使用 requestAnimationFrame 确保在下一帧渲染前执行
  animationFrameId = requestAnimationFrame(() => {
    if (destroyed || !editor.value || editor.value.isDestroyed) return
    safeSetContent(safeMarkdown(contentToSet))
  })
}

watch(
  () => props.modelValue,
  (val) => {
    if (destroyed || !editor.value || editor.value.isDestroyed) return
    latestContent = val
    pendingContent = val
    if (flushTimer) return
    flushTimer = setTimeout(flush, 100)
  },
  { flush: 'post', immediate: true }
)

onBeforeUnmount(() => {
  destroyed = true
  pendingContent = null
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
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
