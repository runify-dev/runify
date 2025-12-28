<template>
  <component :is="kw[details.protocol]" :processor="details" ref="processorRef"></component>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import HttpProtocolProcessor from './http/index.vue'
defineProps<{
  details: any
}>()
const kw: any = {
  HTTP: HttpProtocolProcessor
}
const processorRef = ref()
const validate = () => {
  return processorRef.value?.validate()
}
const submit = () => {
  return processorRef.value?.submit()
}
defineExpose({
  validate: validate,
  submit: submit
})
</script>
<style lang="scss" scoped></style>
