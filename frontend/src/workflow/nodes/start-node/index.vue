<template>
    <SimpleNodeContainer :model="model" :validate="validate">

    </SimpleNodeContainer>

</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue';
import type { BaseNodeModel } from '@logicflow/core';
import { inject } from "vue"

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const validate = () => {
    return Promise.resolve(true)
}


</script>
<style lang="scss" scoped></style>