<template>
  <component :is="commands[data.type]" :data="data" :node="node" :config="config"> </component>
</template>
<script setup lang="ts">
import { type TreeNodeData, type TreeNode } from 'element-plus'
import { Config } from '@/components/tree/index'
const nodes: any = import.meta.glob('./components/*.vue', { eager: true })
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

defineProps<{
  data: TreeNodeData
  node: TreeNode
  config: Config
}>()
</script>
<style lang="scss"></style>
