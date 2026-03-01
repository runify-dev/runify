<template>
  <component :is="kw[protocol]" :meta="meta"></component>
</template>
<script setup lang="ts">
import HttpExpansion from './http/index.vue'
const kw: any = {
  HTTP: HttpExpansion
}
defineProps<{
  protocol: string
  meta: any
}>()
</script>
<style lang="scss" scoped></style>
