<template>
  <component :is="kw[WorkflowType]" :details="details" ref="contentRef"></component>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import Processor from './processor/index.vue'
import Chat from './chat/index.vue'
defineProps<{
  details: any
  WorkflowType: any
}>()
const kw: any = {
  PROCESSOR: Processor,
  APPLICATION: Chat
}
const contentRef = ref()
const validate = () => {
  return contentRef.value?.validate()
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
