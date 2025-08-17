<template>
  <div
    class="absolute z-10 right-6 top-14 items-center space-x-0 bg-white border border-gray-300 rounded-full p-px inline-flex"
  >
    <button @click="save" class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full">
      保存
    </button>
    <button class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full">调试</button>
  </div>

  <Workflow ref="workflowRef"></Workflow>
</template>
<script setup lang="ts">
import { onMounted, ref, computed, inject } from 'vue'
import Workflow from '@/workflow/index.vue'
import ApplicationAPI from '@/api/application'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { baseWorkflow } from '@/workflow/common/data'

const route = useRoute()
// 注入父组件提供的方法
const getApplication = inject('getApplication') as any
const workflowRef = ref<InstanceType<typeof Workflow>>()
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
const save = () => {
  ApplicationAPI.edit(folderId.value, resourceId.value, workflowRef.value?.getGraphData()).then(
    (ok) => {
      ElMessage.success('保存成功')
    }
  )
}

onMounted(() => {
  getApplication().then((application: any) => {
    workflowRef.value?.render(application.workflow.nodes ? application.workflow : baseWorkflow)
  })
})
</script>
<style lang="scss"></style>
