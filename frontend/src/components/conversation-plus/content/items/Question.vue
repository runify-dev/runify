<template>
  <div class="flex w-full justify-end mb-2">
    {{ content }}
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ content: any }>()
const content = computed(() => {
  return props.content.content
})
</script>
<style lang="scss" scoped></style>
