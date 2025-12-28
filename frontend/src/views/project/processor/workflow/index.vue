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
    <button
      v-if="processor && processor.activate"
      @click="() => (debug = true)"
      class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full"
    >
      取消部署
    </button>
    <button
      v-if="processor && !processor.activate"
      @click="deploy"
      class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full"
    >
      部署
    </button>
  </div>

  <Workflow ref="workflowRef"></Workflow>
</template>
<script setup lang="ts">
import { onMounted, ref, provide } from 'vue'
import Workflow from '@/workflow/index.vue'
import processorAPI from '@/api/processor'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'
const debug = ref<boolean>(false)
const route = useRoute()
// 注入父组件提供的方法
const workflowRef = ref<InstanceType<typeof Workflow>>()
const processor = ref<any>()
provide('getDetails', () => processor.value)
provide('WorkflowType', WorkflowType.PROCESSOR)
const deploy = () => {
  processorAPI.deploy(route.params.id as string, route.params.processorId as string).then(() => {
    console.log('ok')
  })
}
const save = () => {
  processorAPI
    .editProcessor(route.params.id as string, route.params.processorId as string, {
      workflow: workflowRef.value?.getGraphData()
    })
    .then(() => {
      ElMessage.success('成功')
    })
}

onMounted(() => {
  processorAPI
    .getProcessor(route.params.id as string, route.params.processorId as string)
    .then((ok) => {
      processor.value = ok.data
      if (ok.data.workflow?.nodes) {
        workflowRef.value?.render(ok.data.workflow)
      } else {
        workflowRef.value?.render(baseWorkflow)
      }
    })
})
</script>
<style lang="scss"></style>
