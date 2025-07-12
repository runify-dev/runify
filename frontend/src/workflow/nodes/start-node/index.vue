<template>
    <SimpleNodeContainer :model="model" :open="open" :close="close">

    </SimpleNodeContainer>

</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue';
import NodeFormDrawer from "../../common/NodeContentContainer.vue"
import type { BaseNodeModel } from '@logicflow/core';
import { inject, onMounted, ref } from "vue"

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const nodeFormDrawer = ref<InstanceType<typeof NodeFormDrawer>>()


const open = () => {
    nodeFormDrawer.value?.open(model)
    return Promise.resolve("ok")
}

const close = () => {
    return Promise.resolve("ok")
}

</script>
<style lang="scss" scoped></style>