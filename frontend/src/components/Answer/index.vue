<template>
  <component :is="commands[type]" :chunk="chunk"> </component>
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
          .toLowerCase()]: nodes[key].default
      }
    })
    .reduce((pre, next) => ({ ...pre, ...next }))
}
defineProps<{ type: string; chunk: any }>()
</script>
<style lang="scss"></style>
