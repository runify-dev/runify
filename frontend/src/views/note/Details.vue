<template>
  <div class="note-detail">
    <div class="pt-0" style="padding-top: 0">
      <Editor ref="editorRef" @change="change"></Editor>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router'
import Editor from '@/editor/index.vue'
import useStore from '@/stores'
const route = useRoute()
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { TreeCommonAPI } from '@/api/tree'
const treeCommonAPI = new TreeCommonAPI('note')
import NoteAPI from '@/api/note'
let timer: any
let currentId: string
const change = (editor: any) => {
  if (timer) clearTimeout(timer)
  const id = resourceId.value
  timer = setTimeout(() => NoteAPI.edit(id, editor.editor.getMarkdown()), 3000)
}
const editorRef = ref<InstanceType<typeof Editor>>()
const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const get = () => {
  currentId = resourceId.value
  treeCommonAPI.getResource(currentId).then((ok) => {
    editorRef.value?.setContent(ok.data.content)
  })
}

function saveSync() {
  if (!editorRef.value || !currentId) return
  const content = editorRef.value.getEditor().getMarkdown()
  if (!content) return
  const baseURL = window.RUNIFY_APP.admin.baseURL + '/api'
  const { user } = useStore()
  const token = user.getToken()
  fetch(`${baseURL}/note/resources/${currentId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', AUTHORIZATION: `Bearer ${token}` },
    body: JSON.stringify({ content }),
    keepalive: true,
  })
}

function onBeforeUnload() {
  if (timer) clearTimeout(timer)
  saveSync()
}
window.addEventListener('beforeunload', onBeforeUnload)

onMounted(() => {
  get()
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
  if (timer) clearTimeout(timer)
  if (currentId) NoteAPI.edit(currentId, editorRef.value?.getEditor().getMarkdown() ?? '')
})
</script>
<style lang="scss" scoped></style>
