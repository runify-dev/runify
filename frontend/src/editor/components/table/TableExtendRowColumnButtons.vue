<template>
  <Teleport to="body">
    <button
      v-if="addRowBtn.visible && editable"
      class="tt-extend-btn tt-extend-btn--row"
      :style="{ top: `${addRowBtn.y}px`, left: `${addRowBtn.x}px`, width: `${addRowBtn.width}px` }"
      @mousedown.prevent="editor.chain().focus().addRowAfter().run()"
      title="添加行"
    >
      <PlusIcon /><span>添加行</span>
    </button>
    <button
      v-if="addColBtn.visible && editable"
      class="tt-extend-btn tt-extend-btn--col"
      :style="{
        top: `${addColBtn.y}px`,
        left: `${addColBtn.x}px`,
        height: `${addColBtn.height}px`
      }"
      @mousedown.prevent="editor.chain().focus().addColumnAfter().run()"
      title="添加列"
    >
      <PlusIcon />
    </button>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import PlusIcon from '@/editor/components/ui/PlusIcon.vue'

interface ExtendBtn {
  visible: boolean
  x: number
  y: number
  width?: number
  height?: number
}

const props = defineProps<{
  editor: Editor
  editable: boolean
}>()

const addRowBtn = ref<ExtendBtn>({ visible: false, x: 0, y: 0, width: 0 })
const addColBtn = ref<ExtendBtn>({ visible: false, x: 0, y: 0, height: 0 })

function findTableElement(): Element | null {
  const { state, view } = props.editor
  const { $from } = state.selection
  for (let d = $from.depth; d >= 0; d--) {
    if ($from.node(d).type.name === 'table') {
      return view.nodeDOM($from.before(d)) as Element | null
    }
  }
  return null
}

function updateButtons(): void {
  if (!props.editable) {
    addRowBtn.value.visible = false
    addColBtn.value.visible = false
    return
  }

  const { $from } = props.editor.state.selection
  let inTable = false
  for (let d = $from.depth; d >= 0; d--) {
    if ($from.node(d).type.name === 'table') {
      inTable = true
      break
    }
  }
  if (!inTable) {
    addRowBtn.value.visible = false
    addColBtn.value.visible = false
    return
  }

  const tableEl = findTableElement()
  if (!tableEl) return

  const rect = tableEl.getBoundingClientRect()
  addRowBtn.value = { visible: true, x: rect.left, y: rect.bottom + 4, width: rect.width }
  addColBtn.value = { visible: true, x: rect.right + 4, y: rect.top, height: rect.height }
}

onMounted(() => {
  props.editor.on('transaction', updateButtons)
})
onUnmounted(() => {
  props.editor.off('transaction', updateButtons)
})
</script>

<style lang="scss">
.tt-extend-btn {
  position: fixed;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px dashed var(--tt-color-border-strong);
  border-radius: var(--tt-radius-sm);
  background: var(--tt-color-bg);
  color: var(--tt-color-text-secondary);
  font-size: var(--tt-font-size-sm);
  cursor: pointer;
  z-index: 40;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
  svg {
    width: 12px;
    height: 12px;
    flex-shrink: 0;
  }
  &:hover {
    background: var(--tt-color-accent-light);
    border-color: var(--tt-color-accent);
    color: var(--tt-color-accent);
  }
  &--row {
    height: 24px;
    padding: 0 8px;
  }
  &--col {
    width: 24px;
    padding: 4px 0;
    flex-direction: column;
    span {
      display: none;
    }
  }
}
</style>
