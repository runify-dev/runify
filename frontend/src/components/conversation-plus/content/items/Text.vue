<template>
  <div class="text-content">
    <PreView :modelValue="content"></PreView>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import PreView from '@/editor/preview/index.vue'

const props = defineProps<{ content: any }>()
const content = computed(() => {
  return props.content.content
})
</script>
<style scoped>
.text-content {
  color: var(--t2);
  font-size: 14px;
  line-height: 1.6;
}

:deep(p) {
  color: var(--t2);
}

:deep(strong) {
  color: var(--t1);
  font-weight: 600;
}

:deep(a) {
  color: var(--t1);
  text-decoration: underline;
  text-underline-offset: 2px;
}

:deep(code) {
  background: var(--hv);
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 0.9em;
  color: var(--t1);
}

:deep(pre) {
  background: var(--hv);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
}

:deep(pre code) {
  background: transparent;
  padding: 0;
}

:deep(blockquote) {
  border-left: 3px solid var(--bd);
  padding-left: 12px;
  color: var(--t3);
}

:deep(ul), :deep(ol) {
  padding-left: 20px;
}

:deep(li) {
  color: var(--t2);
}

:deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
  color: var(--t1);
  font-weight: 600;
}

</style>
