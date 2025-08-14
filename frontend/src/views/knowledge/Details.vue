<template>
  <header class="sticky z-60 top-0 left-0 right-0 bg-white">
    <div
      class="w-full h-10 flex items-center gap-x-4 p-4 mb-5 shadow-lg outline outline-black/5 dark:bg-slate-800 dark:shadow-none dark:-outline-offset-1 dark:outline-white/10"
      style="background: linear-gradient(135deg, rgb(29 43 100 / 79%), rgb(248, 205, 218));"
    >
      <span class="text-white">{{ knowledge.name }}</span>
      <div class="flex-auto"></div>
      <el-button type="primary" text bg @click="goEdit">编辑 </el-button>
    </div>
  </header>
  <MdPreview
    class="pr-10 pl-10"
    style="box-sizing: border-box; overflow: auto; box-sizing: border-box"
    :modelValue="knowledge.content"
    previewTheme="default"
  >
  </MdPreview>
</template>
<script setup lang="ts">
import type { ResourceType } from '@/api/type/common'
import { MdPreview } from 'md-editor-v3'
defineProps<{ resource: ResourceType }>()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
import { computed, onMounted, ref, watch } from 'vue'
import NodeApi from '@/api/node'
const knowledge = ref<any>({})
const folderId = computed(() => {
  const {
    params: { folderId }
  } = route as any
  return folderId
})
const goEdit = () => {
  router.push({ name: 'knowledgeEdit', params: { folderId: folderId.value, id: resourceId.value } })
}
const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const get = () => {
  NodeApi.resourceInfo('knowledge', folderId.value, resourceId.value).then((ok) => {
    knowledge.value = ok.data
    console.log(ok.data)
  })
}
watch(resourceId, () => {
  get()
})
onMounted(() => {
  get()
})
</script>
<style lang="scss" scoped></style>
