<template>
  <EditorContent :editor="editor" />
</template>
<script setup lang="ts">
import { EditorContent, Editor } from '@tiptap/vue-3'
import { reactive, watch } from 'vue'
import newInstance from '@/editor/editor/index'
const editor: Editor = reactive(newInstance('', undefined, false)) as Editor
const props = defineProps<{
  modelValue: string
}>()
watch(
  () => props.modelValue,
  () => {
    editor.commands.setContent(props.modelValue, { contentType: 'markdown' })
  }
)
</script>
<style lang="scss" scoped></style>
