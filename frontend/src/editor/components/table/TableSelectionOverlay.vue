<template>
  <div
    v-if="overlay.visible && editable"
    class="tt-selection-overlay"
    :style="{
      top: `${overlay.top}px`,
      left: `${overlay.left}px`,
      width: `${overlay.width}px`,
      height: `${overlay.height}px`
    }"
    :data-multi="overlay.multiCell"
  >
    <div class="tt-selection-actions" @mousedown.stop>
      <slot :editor="editor" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { CellSelection } from 'prosemirror-tables'

interface OverlayState {
  visible: boolean
  top: number
  left: number
  width: number
  height: number
  multiCell: boolean
}

const props = defineProps<{
  editor: Editor
  editable: boolean
}>()

const overlay = ref<OverlayState>({
  visible: false,
  top: 0,
  left: 0,
  width: 0,
  height: 0,
  multiCell: false
})

function updateOverlay(): void {
  const { state, view } = props.editor
  const sel = state.selection

  if (!(sel instanceof CellSelection)) {
    overlay.value.visible = false
    return
  }

  const cells: Element[] = []
  sel.forEachCell((_, pos) => {
    const dom = view.nodeDOM(pos)
    if (dom) cells.push(dom as Element)
  })

  if (cells.length === 0) {
    overlay.value.visible = false
    return
  }

  const editorRect = (view.dom.parentElement as HTMLElement).getBoundingClientRect()
  let top = Infinity,
    left = Infinity,
    bottom = -Infinity,
    right = -Infinity
  cells.forEach((el) => {
    const r = el.getBoundingClientRect()
    top = Math.min(top, r.top - editorRect.top)
    left = Math.min(left, r.left - editorRect.left)
    bottom = Math.max(bottom, r.bottom - editorRect.top)
    right = Math.max(right, r.right - editorRect.left)
  })

  overlay.value = {
    visible: true,
    top,
    left,
    width: right - left,
    height: bottom - top,
    multiCell: cells.length > 1
  }
}

onMounted(() => {
  props.editor.on('transaction', updateOverlay)
})
onUnmounted(() => {
  props.editor.off('transaction', updateOverlay)
})
</script>

<style lang="scss">
.tt-selection-overlay {
  position: absolute;
  pointer-events: none;
  z-index: 10;
  border: 2px solid var(--tt-color-accent);
  background: var(--tt-color-selected);
  border-radius: 2px;
  &[data-multi='true'] {
    box-shadow: 0 0 0 1px var(--tt-color-accent);
  }
}
.tt-selection-actions {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  pointer-events: all;
  z-index: 50;
}
</style>
