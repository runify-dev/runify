<template>
  <SimpleNodeContainer ref="containerRef" :model="model" :validate="validate" :submit="submit">
    <Content :WorkflowType="workflowType" :details="details" ref="contentRef"></Content>
  </SimpleNodeContainer>
</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import Content from './content/index.vue'
import { init } from './content'
import { inject, ref, onMounted } from 'vue'
import { WorkflowType } from '@/workflow/common/data'
import { useNodeValidator } from '@/workflow/common/useNodeValidator'
const workflowType = inject('WorkflowType') || WorkflowType.APPLICATION
const details = (inject('getDetails') as any)()

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const containerRef = ref<InstanceType<typeof SimpleNodeContainer>>()
const contentRef = ref<InstanceType<typeof Content>>()
const validate = () => {
  return contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
}
const submit = () => {
  return contentRef.value ? contentRef.value.submit() : Promise.resolve(true)
}
useNodeValidator(model, containerRef)

onMounted(() => {
  init({ model, workflowType: workflowType as string, details })
})
</script>
<style lang="scss" scoped></style>
