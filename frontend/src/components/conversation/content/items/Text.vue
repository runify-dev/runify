<template>
  <editor-content :editor="editor" />
</template>
<script setup lang="ts">
import { computed, watch, reactive } from 'vue'
import newInstance from '@/editor/editor/index'
import { EditorContent, Editor } from '@tiptap/vue-3'
const editor: Editor = reactive(newInstance('')) as Editor
const props = defineProps<{ content: any }>()
const content = computed(() => {
  return props.content.content
})
watch(content, () => {
  editor.commands.setContent(content.value, { contentType: 'markdown' })
})
</script>
<style lang="scss"></style>
