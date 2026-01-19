<template>
  <div class="flex flex-col">
    <div class="card overflow-auto" style="height: calc(100vh - 8rem); padding-top: 0">
      <Editor ref="editorRef" @change="change"></Editor>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router'
import Editor from '@/editor/index.vue'
const route = useRoute()
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { TreeCommonAPI } from '@/api/tree'
const treeCommonAPI = new TreeCommonAPI('note')
import NoteAPI from '@/api/note'
let timer: any
const change = (editor: any) => {
  if (timer) {
    clearTimeout(timer)
  }
  timer = setTimeout(() => NoteAPI.edit(resourceId.value, editor.editor.getMarkdown()), 3000)
}
const editorRef = ref<InstanceType<typeof Editor>>()
const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const get = () => {
  treeCommonAPI.getResource(resourceId.value).then((ok) => {
    editorRef.value?.setContent(ok.data.content)
  })
}
const getUnmountSetContent = () => {
  const noteId = resourceId.value
  return () => {
    if (timer) {
      clearTimeout(timer)
    }
    if (editorRef.value) {
      NoteAPI.edit(noteId, editorRef.value.getEditor().getMarkdown())
    }
  }
}
const unmountSetContent = getUnmountSetContent()
onMounted(() => {
  get()
})
onBeforeUnmount(() => {
  unmountSetContent()
})
</script>
<style lang="scss" scoped></style>
