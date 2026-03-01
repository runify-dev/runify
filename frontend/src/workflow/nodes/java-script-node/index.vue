<template>
  <SimpleNodeContainer :model="model" :validate="validate" :submit="submit">
    <Content :WorkflowType="WorkflowType" :details="details" ref="contentRef"></Content>
  </SimpleNodeContainer>
</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { inject, ref, onMounted } from 'vue'
import Content from './content/index.vue'
const WorkflowType = inject('WorkflowType')
const details = (inject('getDetails') as any)()
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const form = ref({
  code: '',
  functionName: '',
  parameters: []
})
const contentRef = ref()
const validate = () => {
  return contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
}
const submit = () => {
  return contentRef.value ? contentRef.value.submit() : Promise.resolve(true)
}
onMounted(() => {
  if (model.properties.nodeData) {
    form.value = JSON.parse(JSON.stringify(model.properties.nodeData))
  } else {
    model.properties.nodeData = form.value
  }
})
</script>
<style lang="scss" scoped></style>
