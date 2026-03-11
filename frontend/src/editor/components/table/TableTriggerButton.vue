<template>
  <div class="tt-trigger-wrap" ref="wrapRef">
    <button
      class="tt-toolbar-btn"
      :class="{ 'tt-toolbar-btn--active': grid.isOpen.value }"
      @click="grid.toggle()"
      title="插入表格"
    >
      <TableIcon />
      <span>表格</span>
      <ChevronIcon />
    </button>
    <Transition name="tt-grid-pop">
      <div v-if="grid.isOpen.value" class="tt-grid-popup" @mouseleave="grid.hover(0, 0)">
        <div class="tt-grid-label">
          {{
            grid.hoveredRow.value && grid.hoveredCol.value
              ? `${grid.hoveredRow.value} × ${grid.hoveredCol.value}`
              : '插入表格'
          }}
        </div>
        <div class="tt-grid-cells">
          <template v-for="row in grid.grid()" :key="row[0].r">
            <button
              v-for="cell in row"
              :key="cell.c"
              class="tt-grid-cell"
              :class="{
                'tt-grid-cell--hover':
                  cell.r <= grid.hoveredRow.value && cell.c <= grid.hoveredCol.value
              }"
              @mouseenter="grid.hover(cell.r, cell.c)"
              @click="grid.pick(cell.r, cell.c)"
            />
          </template>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { useTableGrid } from '@/editor/composables/useTableGrid'
import TableIcon from '@/editor/components/ui/TableIcon.vue'
import ChevronIcon from '@/editor/components/ui/ChevronIcon.vue'

const props = defineProps<{ editor: Editor }>()

const editorRef = {
  get value() {
    return props.editor
  }
}
const grid = useTableGrid(editorRef, { maxRows: 8, maxCols: 8 })
const wrapRef = ref<HTMLElement | null>(null)

function onClickOutside(e: MouseEvent): void {
  if (wrapRef.value && !wrapRef.value.contains(e.target as Node)) grid.close()
}
onMounted(() => document.addEventListener('mousedown', onClickOutside))
onUnmounted(() => document.removeEventListener('mousedown', onClickOutside))
</script>

<style lang="scss">
.tt-trigger-wrap {
  position: relative;
  display: inline-flex;
}
.tt-toolbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--tt-radius-sm);
  font-size: var(--tt-font-size-md);
  color: var(--tt-color-text);
  cursor: pointer;
  transition:
    background 0.12s,
    border-color 0.12s;
  svg {
    width: 14px;
    height: 14px;
    color: var(--tt-color-text-secondary);
  }
  &:hover {
    background: var(--tt-color-bg-hover);
    border-color: var(--tt-color-border);
  }
  &--active {
    background: var(--tt-color-bg-active);
    border-color: var(--tt-color-accent);
    color: var(--tt-color-accent);
  }
}
.tt-grid-popup {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  padding: 10px;
  background: var(--tt-color-bg);
  border: 1px solid var(--tt-color-border);
  border-radius: var(--tt-radius-md);
  box-shadow: var(--tt-shadow-lg);
  z-index: 200;
}
.tt-grid-label {
  font-size: var(--tt-font-size-sm);
  color: var(--tt-color-text-secondary);
  margin-bottom: 8px;
  text-align: center;
  min-height: 16px;
}
.tt-grid-cells {
  display: grid;
  grid-template-columns: repeat(8, 18px);
  gap: 2px;
}
.tt-grid-cell {
  width: 18px;
  height: 18px;
  border: 1px solid var(--tt-color-border);
  border-radius: 2px;
  background: var(--tt-color-bg);
  cursor: pointer;
  transition:
    background 0.08s,
    border-color 0.08s;
  &:hover,
  &--hover {
    background: var(--tt-color-accent-light);
    border-color: var(--tt-color-accent);
  }
}
.tt-grid-pop-enter-active,
.tt-grid-pop-leave-active {
  transition:
    opacity 0.15s,
    transform 0.15s;
}
.tt-grid-pop-enter-from,
.tt-grid-pop-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}
</style>
