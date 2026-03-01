<template>
  <component ref="componentRef" :is="kw[contentType]" :requestBody="requestBody"></component>
</template>
<script setup lang="ts">
import ApplicationJson from './application-json/index.vue'
import FormData from './form-data/index.vue'
import { ref } from 'vue'
const kw: any = {
  'application/json': ApplicationJson,
  'multipart/form-data': FormData
}
const componentRef = ref<any>()
defineProps<{ contentType: string; requestBody: Array<any> }>()
const getBody = () => {
  return componentRef.value.getBody()
}

defineExpose({ getBody })
</script>
<style lang="scss" scoped></style>
