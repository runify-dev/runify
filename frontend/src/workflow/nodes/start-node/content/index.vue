<template>
  <component :is="kw[WorkflowType]" :details="details" ref="contentRef"></component>
</template>
<script setup lang="ts">
import { ref, inject } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import Processor from './processor/index.vue'
import Chat from './chat/index.vue'
import { validate as validateNodeData } from './validator'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const props = defineProps<{
  details: any
  WorkflowType: any
}>()
const kw: any = {
  PROCESSOR: Processor,
  APPLICATION: Chat
}
const contentRef = ref()
const validate = () => {
  if (contentRef.value) {
    return contentRef.value.validate()
  }
  const result = validateNodeData(model.properties.nodeData, undefined, props.WorkflowType)
  if (result.valid) {
    return Promise.resolve({ values: model.properties.nodeData, errors: {} })
  }
  return Promise.resolve({ values: {}, errors: result.errors })
}
const submit = () => {
  return contentRef.value?.submit()
}
defineExpose({
  validate: validate,
  submit: submit
})
</script>
<style lang="scss" scoped></style>
