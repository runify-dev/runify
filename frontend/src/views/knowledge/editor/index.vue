<template>
    <component :is="commands[type]" :id="id"></component>
</template>
<script setup lang="ts">
const nodes: any = import.meta.glob("./components/*/index.vue", { eager: true });
defineProps<{ id: string, type: string }>()
const commands: any = {
    ...Object.keys(nodes).map((key) => {
        return {
            [key.split('/')[2]]: nodes[key].default
        }
    }).reduce((pre, next) => ({ ...pre, ...next }))
}

</script>
<style lang="scss"></style>