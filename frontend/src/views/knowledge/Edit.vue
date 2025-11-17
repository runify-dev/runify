<template>
  <div class="sticky h-full z-60 top-0 left-0 right-0">
    <div
      class="w-full h-10 flex items-center gap-x-4 p-4 mb-5 shadow-lg outline outline-black/5 dark:bg-slate-800 dark:shadow-none dark:-outline-offset-1 dark:outline-white/10"
      style="background: linear-gradient(135deg, rgb(29 43 100 / 79%), rgb(248, 205, 218))"
    >
      <el-icon class="hover:cursor-pointer" @click="goDetails">
        <ArrowLeft class="text-white" />
      </el-icon>
      <span class="text-white">{{ knowledge.name }}</span>
      <div class="flex-auto"></div>
      <el-button type="primary" text bg @click="edit">保存 </el-button>
    </div>
    <MdEditor
      class="rounded-xl"
      style="height: calc(100% - 100px)"
      :editorId="state.id"
      @onUploadImg="onUploadImg"
      :onGetCatalog="onGetCatalog"
      ref="mdEditorRef"
      v-model="knowledge.content"
      :toolbars="toolbars"
      previewTheme="default"
    >
    </MdEditor>
  </div>
</template>
<script setup lang="ts">
import { MdEditor, type ToolbarNames, type HeadList } from 'md-editor-v3'
import FileAPI from '@/api/file'
import { computed, onMounted, ref, reactive } from 'vue'
import NodeApi from '@/api/node'
import KnowledgeApi from '@/api/knowledge'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const goDetails = () => {
  router.push({
    name: 'knowledgeDetails',
    params: { folderId: folderId.value, id: resourceId.value }
  })
}
const knowledge = ref<any>({})
const folderId = computed(() => {
  const {
    params: { folderId }
  } = route as any
  return folderId
})

const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const get = () => {
  NodeApi.resourceInfo('knowledge', folderId.value, resourceId.value).then((ok) => {
    knowledge.value = ok.data
  })
}
onMounted(() => {
  get()
})
const mdEditorRef = ref<InstanceType<typeof MdEditor>>()
const onGetCatalog = (list: HeadList[]) => {
  console.log(list)
}
const state = reactive({
  theme: 'dark',
  text: '标题',
  id: 'my-editor'
})

const onUploadImg = (files: any, callback: any) => {
  const uploadFileList = files.map((file: any) => {
    const fd = new FormData()
    fd.append('file', file)
    return FileAPI.uploadFile(fd).then((ok) => ok.data.id)
  })
  Promise.all(uploadFileList).then((id_list) => {
    callback(id_list)
  })
}

// 工具栏配置
const toolbars = ref<ToolbarNames[]>([
  'bold',
  'underline',
  'italic',
  'strikeThrough',
  '-',
  'title',
  'sub',
  'sup',
  'quote',
  'unorderedList',
  'orderedList',
  'task',
  'codeRow',
  'code',
  '-',
  'link',
  'image',
  'table',
  'mermaid',
  'katex',
  '-',
  'prettier',
  'pageFullscreen',
  'catalog',
  'preview',
  'previewOnly',
  '=',
  0,
  1,
  2
])

const edit = () => {
  KnowledgeApi.edit(resourceId.value, {
    content: knowledge.value.content
  }).then((ok) => {
    goDetails()
  })
}
</script>
<style lang="scss"></style>
