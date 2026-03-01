<template>
  <component :is="kw[contentType]"></component>
</template>
<script setup lang="ts">
import applicationJson from './application-json/index.vue'
import formData from './form-data/index.vue'
defineProps<{
  contentType: string
}>()

const kw: any = {
  'application/json': applicationJson,
  'multipart/form-data': formData
}
</script>
<style lang="scss" scoped></style>
