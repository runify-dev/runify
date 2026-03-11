<template>
  <div>
    <Toolbar :editor="editor"></Toolbar>
    <div class="simple-editor-content relative">
      <OperationMenu :editor="editor"></OperationMenu>
      <EditorContent :editor="editor" />
      <TableSelectionOverlay v-if="editor" :editor="editor" :editable="editable">
        <template #default="{ editor: ed }">
          <TableCellHandleMenu :editor="ed" />
        </template>
      </TableSelectionOverlay>
    </div>
    <!-- Floating table components -->
    <TableHandle v-if="editor" :editor="editor" :editable="editable" />
    <TableExtendRowColumnButtons v-if="editor" :editor="editor" :editable="editable" />
  </div>
</template>
<script setup lang="ts">
import { EditorContent, Editor } from '@tiptap/vue-3'
import { reactive, computed } from 'vue'
import './editor/nodes/index.scss'
import newInstance from './editor/index'
import Toolbar from './toolbar/index.vue'
import OperationMenu from './operation-menu/index.vue'
import TableHandle from '@/editor/components/table/TableHandle.vue'
import TableSelectionOverlay from '@/editor/components/table/TableSelectionOverlay.vue'
import TableCellHandleMenu from '@/editor/components/table/TableCellHandleMenu.vue'
import TableExtendRowColumnButtons from '@/editor/components/table/TableExtendRowColumnButtons.vue'

const emit = defineEmits(['change'])
const change = (v: any) => {
  emit('change', v)
}
const editable = computed(() => {
  return editor.isEditable
})
const editor: Editor = reactive(newInstance('', change)) as Editor
const setContent = (content: string) => {
  editor.commands.setContent(content, { contentType: 'markdown' })
}
const getEditor = () => {
  return editor
}
defineExpose({ setContent, getEditor })
</script>
<style lang="scss">
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
