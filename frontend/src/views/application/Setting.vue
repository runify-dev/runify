<template>
  <div
    class="absolute z-10 right-6 top-14 items-center space-x-0 bg-white border border-gray-300 rounded-full p-px inline-flex"
  >
    <button @click="save" class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full">
      保存
    </button>
    <button
      @click="() => (debug = true)"
      class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full"
    >
      调试
    </button>
  </div>
  <el-collapse-transition>
    <DebugConversation
      v-if="debug"
      @close="debug = false"
      :forder-id="folderId"
      :application="application"
    ></DebugConversation>
  </el-collapse-transition>

  <Workflow ref="workflowRef"></Workflow>
</template>
<script setup lang="ts">
import { onMounted, ref, computed, inject, provide } from 'vue'
import Workflow from '@/workflow/index.vue'
import ApplicationAPI from '@/api/application'
import { useRoute } from 'vue-router'
import bus from '@/bus'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'
import DebugConversation from './DebugConversation.vue'
const debug = ref<boolean>(false)
const route = useRoute()
// 注入父组件提供的方法
const getApplication = inject('getApplication') as any
provide('WorkflowType', WorkflowType.APPLICATION)
provide('getDetails', () => application.value)
const workflowRef = ref<InstanceType<typeof Workflow>>()
const application = ref<any>()
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
  ApplicationAPI.edit(resourceId.value, workflowRef.value?.getGraphData()).then(() => {
    bus.emit('message:success', '保存成功')
  })
}

onMounted(() => {
  getApplication().then((app: any) => {
    application.value = app
    workflowRef.value?.render(app.workflow.nodes ? app.workflow : baseWorkflow)
  })
})
</script>
<style lang="scss"></style>
