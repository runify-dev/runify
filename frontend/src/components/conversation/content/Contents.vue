<template>
  <template v-for="(content, index) in contents" :key="index">
    <Content :type="content.type" :content="content"></Content>
  </template>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import Content from './index.vue'
const props = defineProps<{ content: Array<any> }>()
const contents = computed(() => {
  return props.content
    .map((item, index) => ({ item, index }))
    .sort((a, b) => {
      const idA = a.item.realNodeId || ''
      const idB = b.item.realNodeId || ''

      // 先按 realNodeId 排序
      if (idA !== idB) {
        return idA.localeCompare(idB)
      }

      // realNodeId 相同时，保持原始顺序
      return a.index - b.index
    })
    .map(({ item }) => item)
})
</script>
<style lang="scss"></style>
