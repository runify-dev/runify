<template>
  <SimpleNodeContainer :model="model" :validate="validate" :submit="submit">
    <Content :WorkflowType="WorkflowType" :details="details" ref="contentRef"></Content>
  </SimpleNodeContainer>
</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import Content from './content/index.vue'
import { inject, onMounted, ref } from 'vue'
const WorkflowType = inject('WorkflowType')
const details = (inject('getDetails') as any)()
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const contentRef = ref<InstanceType<typeof Content>>()
const validate = () => {
  return contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
}
const submit = () => {
  return contentRef.value ? contentRef.value.submit() : Promise.resolve(true)
}
</script>
<style lang="scss" scoped></style>
