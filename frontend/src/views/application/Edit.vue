<template>
    <Workflow ref="workflowRef"></Workflow>
</template>
<script setup lang="ts">

import { onMounted, ref } from 'vue';
import Workflow from '@/workflow/index.vue';

const workflowRef = ref<InstanceType<typeof Workflow>>()

onMounted(() => {
    workflowRef.value?.render({
        nodes:
            Array.from({ length: 1 }).map(item => ({
                type: 'start-node',
                text: "",
                x: 0,
                y: 0,
                label: "开始节点",
                properties: {
                    width: 250,
                    height: 50,
                    name: "开始节点",
                    isHovered: false,
                    field_list: [
                        {
                            label: '用户问题',
                            value: 'question'
                        }
                    ]
                }
            }))

        ,
        edges: []
    })

})
</script>
<style lang="scss" scoped></style>