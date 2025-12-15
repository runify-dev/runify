<template>
  <div>
    <Toolbar :editor="editor"></Toolbar>
    <div class="simple-editor-content">
      <OperationMenu :editor="editor"></OperationMenu>
      <EditorContent :editor="editor" />
    </div>
  </div>
</template>
<script setup lang="ts">
import { EditorContent, Editor } from '@tiptap/vue-3'
import { nextTick, onMounted, reactive, ref } from 'vue'
import './editor/nodes/index.scss'
import newInstance from './editor/index'
import Toolbar from './toolbar/index.vue'
import OperationMenu from './operation-menu/index.vue'
const emit = defineEmits(['change'])
const change = (v: any) => {
  emit('change', v)
}
const editor: Editor = reactive(newInstance('', change))
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
