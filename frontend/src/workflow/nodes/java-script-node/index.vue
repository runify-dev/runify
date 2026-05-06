<template>
  <SimpleNodeContainer ref="containerRef" :model="model" :validate="validate" :submit="submit">
    <Content :WorkflowType="WorkflowType" :details="details" ref="contentRef"></Content>
  </SimpleNodeContainer>
</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { inject, ref, onMounted } from 'vue'
import Content from './content/index.vue'
import { init } from './content'
import { useNodeValidator } from '@/workflow/common/useNodeValidator'
const WorkflowType = inject('WorkflowType')
const details = (inject('getDetails') as any)()
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const form = ref({
  code: '',
  functionName: '',
  parameters: []
})
const containerRef = ref<InstanceType<typeof SimpleNodeContainer>>()
const contentRef = ref()
const validate = () => {
  return contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
}
const submit = () => {
  return contentRef.value ? contentRef.value.submit() : Promise.resolve(true)
}
useNodeValidator(model, containerRef)
onMounted(() => {
  init({ model, workflowType: WorkflowType as string, details })
  if (model.properties.nodeData) {
    form.value = JSON.parse(JSON.stringify(model.properties.nodeData))
  } else {
    model.properties.nodeData = form.value
  }
})
</script>
<style lang="scss" scoped></style>
