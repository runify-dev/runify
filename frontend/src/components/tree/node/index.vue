<template>
    <component :is="commands[data.type]" :data="data" :node="node" :resource="resource" :create="create"></component>
</template>
<script setup lang="ts">
import { type TreeNodeData, type TreeNode } from "element-plus"
import type { ResourceType, Type } from '@/api/type/common';
const nodes: any = import.meta.glob("./components/*.vue", { eager: true });
const commands: any = {
    ...Object.keys(nodes).map((key) => {
        return {
            [key
                .substring(key.lastIndexOf('/') + 1, key.length)
                .replace('.vue', '').toLowerCase()]: nodes[key].default
        }
    }).reduce((pre, next) => ({ ...pre, ...next }))
}

defineProps<{
    data: TreeNodeData,
    node: TreeNode,
    resource: ResourceType,
    create: (type: Type, id?: string) => Promise<any>,
}>()
</script>
<style lang="scss"></style>