<template>
  <div class="relative h-full">
    <div
      class="absolute z-10 right-6 top-4 items-center space-x-0 bg-white border border-gray-300 rounded-full p-px inline-flex"
    >
      <button @click="save" class="px-2 py-0.5 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full">
        {{ t('application.save') }}
      </button>
      <button
        @click="debugFn"
        class="px-2 py-0.5 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full"
      >
        {{ t('application.debug') }}
      </button>
    </div>

    <DebugConversation
      v-if="debug"
      @close="debug = false"
      :forder-id="folderId"
      :application="application"
    ></DebugConversation>

    <Workflow ref="workflowRef"></Workflow>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref, computed, inject, provide } from 'vue'
import Workflow from '@/workflow/index.vue'
import ApplicationAPI from '@/api/application'
import { useRoute } from 'vue-router'
import bus from '@/bus'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'
import DebugConversation from './DebugConversation.vue'
import { t } from '@/locales'
const debug = ref<boolean>(false)
const route = useRoute()
// 注入父组件提供的方法
const getApplication = inject('getApplication') as any
provide('WorkflowType', WorkflowType.APPLICATION)
provide('getDetails', () => application.value)
const workflowRef = ref<InstanceType<typeof Workflow>>()
const application = ref<any>()
const debugFn=()=>{
  workflowRef.value?.validateWorkflow().then(ok=>{
    console.log(ok)
    if(ok.valid){

      debug.value=true
    }
  })
}
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
  ApplicationAPI.edit(resourceId.value,{workflow: workflowRef.value?.getGraphData()}).then(() => {
    bus.emit('message:success', t('application.saveSuccess'))
  })
}

onMounted(() => {
  getApplication().then((app: any) => {
    application.value = app
    workflowRef.value?.render(app.workflow.nodes ? app.workflow : baseWorkflow)
  })
})
</script>
<style lang="scss">
.layout-main {
  padding: 0 !important;
  border: 1px solid var(--surface-border);
  border-radius: 12px;
  overflow: hidden;
}

.layout-content-height {
  height: calc(100dvh - var(--app-content-height-offset) + 3rem - 2px);
}
</style>
