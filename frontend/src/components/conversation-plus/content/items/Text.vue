<template>
  <PreView :modelValue="content"></PreView>
</template>
<script setup lang="ts">
import { computed } from 'vue'

import PreView from '@/editor/preview/index.vue'

const props = defineProps<{ content: any }>()
const content = computed(() => {
  return props.content.content
})
</script>
<style lang="scss" scoped></style>
