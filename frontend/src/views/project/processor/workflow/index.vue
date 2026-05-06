<template>
  <div class="relative h-full">
    <!-- 顶部操作栏 -->
    <div
      class="absolute z-10 right-6 top-4 flex items-center gap-1 bg-white/80 backdrop-blur-sm border border-surface-200 rounded-lg px-1.5 py-1 shadow-sm"
    >
      <Button
        icon="pi pi-save"
        label="保存"
        variant="text"
        size="small"
        @click="save"
      />
      <div class="w-px h-5 bg-surface-200" />
      <Button
        v-if="processor && processor.isDeploy"
        icon="pi pi-cloud-download"
        label="取消部署"
        variant="text"
        size="small"
        severity="warning"
        @click="unDeploy"
      />
      <Button
        v-if="processor && !processor.isDeploy"
        icon="pi pi-cloud-upload"
        label="部署"
        variant="text"
        size="small"
        severity="success"
        @click="deploy"
      />
    </div>

    <Workflow ref="workflowRef" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, provide } from 'vue'
import Workflow from '@/workflow/index.vue'
import processorAPI from '@/api/processor'
import { useRoute } from 'vue-router'
import bus from '@/bus'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'

const route = useRoute()
const workflowRef = ref<InstanceType<typeof Workflow>>()
const processor = ref<any>()

provide('getDetails', () => processor.value)
provide('WorkflowType', WorkflowType.PROCESSOR)

const deploy = () => {
  processorAPI.deploy(route.params.id as string, route.params.processorId as string).then((ok) => {
    processor.value = ok.data
    bus.emit('message:success', '部署成功')
  })
}

const unDeploy = () => {
  processorAPI
    .undeploy(route.params.id as string, route.params.processorId as string)
    .then((ok) => {
      processor.value = ok.data
      bus.emit('message:success', '取消部署成功')
    })
}

const save = () => {
  processorAPI
    .editProcessor(route.params.id as string, route.params.processorId as string, {
      workflow: workflowRef.value?.getGraphData()
    })
    .then(() => {
      bus.emit('message:success', '保存成功')
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
