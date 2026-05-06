<template>
  <component :is="kw[details.protocol]" :processor="details" ref="processorRef"></component>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import HttpProtocolProcessor from './http/index.vue'
import { validate as validateNodeData } from './validator'
defineProps<{
  details: any
}>()
const kw: any = {
  HTTP: HttpProtocolProcessor
}
const processorRef = ref()
const validate = () => {
  if (processorRef.value) {
    return processorRef.value.validate()
  }
  return Promise.resolve({ values: {}, errors: {} })
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
