<template>
  <Teleport to="body">
    <!-- Row Handle -->
    <div
      v-if="rowHandle.visible || openMenu === 'row'"
      class="tt-table-handle tt-table-handle--row"
      :style="{ top: `${rowHandle.y}px`, left: `${rowHandle.x}px` }"
      @mouseenter="onHandleEnter"
      @mouseleave="onHandleLeave"
    >
      <div class="tt-handle-bridge tt-handle-bridge--row" />
      <button
        class="tt-handle-btn"
        :class="{ 'tt-handle-btn--active': openMenu === 'row' }"
        @click.stop="toggleMenu('row')"
        title="行操作"
      >
        <GripIcon />
      </button>
      <Transition name="tt-menu">
        <div v-if="openMenu === 'row'" class="tt-handle-menu" @click.stop>
          <button class="tt-menu-item" @mousedown.prevent="exec(addRowBefore)">
            <PlusIcon /> 上方插入行
          </button>
          <button class="tt-menu-item" @mousedown.prevent="exec(addRowAfter)">
            <PlusIcon /> 下方插入行
          </button>
          <div class="tt-menu-sep" />
          <button class="tt-menu-item tt-menu-item--danger" @mousedown.prevent="exec(deleteRow)">
            <TrashIcon /> 删除此行
          </button>
        </div>
      </Transition>
    </div>

    <!-- Col Handle -->
    <div
      v-if="colHandle.visible || openMenu === 'col'"
      class="tt-table-handle tt-table-handle--col"
      :style="{ top: `${colHandle.y}px`, left: `${colHandle.x}px` }"
      @mouseenter="onHandleEnter"
      @mouseleave="onHandleLeave"
    >
      <div class="tt-handle-bridge tt-handle-bridge--col" />
      <button
        class="tt-handle-btn"
        :class="{ 'tt-handle-btn--active': openMenu === 'col' }"
        @click.stop="toggleMenu('col')"
        title="列操作"
      >
        <GripHIcon />
      </button>
      <Transition name="tt-menu">
        <div v-if="openMenu === 'col'" class="tt-handle-menu tt-handle-menu--col" @click.stop>
          <button class="tt-menu-item" @mousedown.prevent="exec(addColumnBefore)">
            <PlusIcon /> 左侧插入列
          </button>
          <button class="tt-menu-item" @mousedown.prevent="exec(addColumnAfter)">
            <PlusIcon /> 右侧插入列
          </button>
          <div class="tt-menu-sep" />
          <button class="tt-menu-item tt-menu-item--danger" @mousedown.prevent="exec(deleteColumn)">
            <TrashIcon /> 删除此列
          </button>
        </div>
      </Transition>
    </div>

    <div
      v-if="openMenu"
      class="tt-menu-backdrop"
      @click="openMenu = null"
      @mousedown="openMenu = null"
    />
  </Teleport>
</template>

<script setup lang="ts">
import type { Editor } from '@tiptap/vue-3'
import { useTableHandle } from '@/editor/components/composables/useTableHandle'
import GripIcon from '@/editor/components/ui/GripIcon.vue'
import GripHIcon from '@/editor/components/ui/GripHIcon.vue'
import PlusIcon from '@/editor/components/ui/PlusIcon.vue'
import TrashIcon from '@/editor/components/ui/TrashIcon.vue'

const props = defineProps<{
  editor: Editor
  editable: boolean
}>()

const editorRef = {
  get value() {
    return props.editor
  }
}
import { toRef } from 'vue'
const editableRef = toRef(props, 'editable')

const {
  rowHandle,
  colHandle,
  openMenuRef,
  setHandleHovered,
  addRowBefore,
  addRowAfter,
  deleteRow,
  addColumnBefore,
  addColumnAfter,
  deleteColumn
} = useTableHandle(editorRef, editableRef)

const openMenu = openMenuRef

function onHandleEnter(): void {
  setHandleHovered(true)
}

function onHandleLeave(): void {
  setHandleHovered(false)
  if (!openMenu.value) {
    setTimeout(() => {
      if (!openMenu.value) {
        rowHandle.value = { ...rowHandle.value, visible: false }
        colHandle.value = { ...colHandle.value, visible: false }
      }
    }, 200)
  }
}

function toggleMenu(which: 'row' | 'col'): void {
  openMenu.value = openMenu.value === which ? null : which
}

function exec(fn: () => void): void {
  fn()
  openMenu.value = null
}
</script>

<style lang="scss">
.tt-table-handle {
  position: fixed;
  z-index: 60;
  overflow: visible;
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--tt-table-handle-size);
  height: var(--tt-table-handle-size);
}
.tt-handle-bridge {
  position: absolute;
  pointer-events: all;
  &--row {
    top: 0;
    left: 100%;
    width: 10px;
    height: 100%;
  }
  &--col {
    top: 100%;
    left: 0;
    width: 100%;
    height: 10px;
  }
}
.tt-handle-btn {
  position: relative;
  z-index: 1;
  width: var(--tt-table-handle-size);
  height: var(--tt-table-handle-size);
  border-radius: var(--tt-radius-sm);
  border: 1px solid var(--tt-color-border);
  background: var(--tt-color-bg);
  box-shadow: var(--tt-shadow-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--tt-color-text-secondary);
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s,
    box-shadow 0.15s;
  svg {
    width: 11px;
    height: 11px;
  }
  &:hover,
  &--active {
    background: var(--tt-color-accent);
    border-color: var(--tt-color-accent);
    color: #fff;
    box-shadow: 0 2px 8px rgba(124, 58, 237, 0.35);
  }
}
.tt-handle-menu {
  position: absolute;
  left: calc(100% + 6px);
  top: 0;
  min-width: 148px;
  background: var(--tt-color-bg);
  border: 1px solid var(--tt-color-border);
  border-radius: var(--tt-radius-md);
  box-shadow: var(--tt-shadow-lg);
  padding: 4px;
  z-index: 100;
  &--col {
    left: 0;
    top: calc(100% + 6px);
  }
}
.tt-menu-item {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  padding: 7px 10px;
  background: transparent;
  border: none;
  border-radius: var(--tt-radius-sm);
  font-size: var(--tt-font-size-md);
  color: var(--tt-color-text);
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;
  white-space: nowrap;
  svg {
    width: 13px;
    height: 13px;
    flex-shrink: 0;
    color: var(--tt-color-text-secondary);
  }
  &:hover {
    background: var(--tt-color-bg-hover);
  }
  &--danger {
    color: var(--tt-color-danger);
    svg {
      color: var(--tt-color-danger);
    }
    &:hover {
      background: var(--tt-color-danger-bg);
    }
  }
}
.tt-menu-sep {
  height: 1px;
  background: var(--tt-color-border);
  margin: 3px 0;
}
.tt-menu-backdrop {
  position: fixed;
  inset: 0;
  z-index: 59;
}
.tt-menu-enter-active,
.tt-menu-leave-active {
  transition:
    opacity 0.12s,
    transform 0.12s;
}
.tt-menu-enter-from,
.tt-menu-leave-to {
  opacity: 0;
  transform: scale(0.96) translateY(-3px);
}
</style>
