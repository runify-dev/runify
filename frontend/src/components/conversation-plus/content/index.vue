<template>
  <component v-if="commands[type]" :is="commands[type]" :content="content"> </component>
</template>
<script setup lang="ts">
const nodes: any = import.meta.glob('./items/*.vue', { eager: true })
const commands: any = {
  ...Object.keys(nodes)
    .map((key) => {
      return {
        [key
          .substring(key.lastIndexOf('/') + 1, key.length)
          .replace('.vue', '')
          .toUpperCase()]: nodes[key].default
      }
    })
    .reduce((pre, next) => ({ ...pre, ...next }))
}
defineProps<{ type: string; content: any }>()
</script>
<style lang="scss"></style>
