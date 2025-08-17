<template>
  <MdPreview v-bind:model-value="content" ref="editorRef" editorId="preview-only" />
</template>
<script setup lang="ts">
import { MdPreview } from 'md-editor-v3'
import { computed } from 'vue'
const props = defineProps<{ chunk: any }>()
const content = computed(() => {
  return props.chunk.markdown
})
</script>
<style lang="scss"></style>
