<template>
    <SimpleNodeContainer :model="model" :close="close">

    </SimpleNodeContainer>

</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue';

import type { BaseNodeModel } from '@logicflow/core';
import { inject } from "vue"
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const close = () => {
    return Promise.resolve("ok")
}
</script>
<style lang="scss" scoped></style>