<template>
  <div class="tt-cell-menu">
    <button class="tt-cell-btn" @mousedown.prevent="run('mergeCells')" title="合并单元格">
      <MergeCellsIcon />
    </button>
    <button class="tt-cell-btn" @mousedown.prevent="run('splitCell')" title="拆分单元格">
      <SplitCellIcon />
    </button>
    <div class="tt-cell-sep" />
    <button class="tt-cell-btn" @mousedown.prevent="run('toggleHeaderCell')" title="切换表头">
      <HeaderIcon />
    </button>
    <button
      class="tt-cell-btn tt-cell-btn--danger"
      @mousedown.prevent="run('deleteTable')"
      title="删除表格"
    >
      <TrashIcon />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { Editor } from '@tiptap/vue-3'
import MergeCellsIcon from '@/editor/components/ui/MergeCellsIcon.vue'
import SplitCellIcon from '@/editor/components/ui/SplitCellIcon.vue'
import HeaderIcon from '@/editor/components/ui/HeaderIcon.vue'
import TrashIcon from '@/editor/components/ui/TrashIcon.vue'

const props = defineProps<{ editor: Editor }>()

function run(cmd: string): void {
  ;(props.editor.chain().focus() as any)[cmd]().run()
}
</script>

<style lang="scss">
.tt-cell-menu {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px;
  background: var(--tt-color-bg);
  border: 1px solid var(--tt-color-border);
  border-radius: var(--tt-radius-md);
  box-shadow: var(--tt-shadow-md);
  white-space: nowrap;
}
.tt-cell-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--tt-radius-sm);
  color: var(--tt-color-text-secondary);
  cursor: pointer;
  transition:
    background 0.12s,
    color 0.12s;
  svg {
    width: 14px;
    height: 14px;
  }
  &:hover {
    background: var(--tt-color-bg-hover);
    color: var(--tt-color-text);
  }
  &--danger:hover {
    background: var(--tt-color-danger-bg);
    color: var(--tt-color-danger);
  }
}
.tt-cell-sep {
  width: 1px;
  height: 18px;
  background: var(--tt-color-border);
  margin: 0 2px;
}
</style>
