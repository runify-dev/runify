<template>
    <Workflow ref="workflowRef"></Workflow>
</template>
<script setup lang="ts">
import { inject, onUnmounted } from 'vue'
import { onMounted, ref } from 'vue';
import Workflow from '@/workflow/index.vue';
// 注入父组件提供的方法
const { addButton, removeButton } = inject('buttonActions') as any
const workflowRef = ref<InstanceType<typeof Workflow>>()

const save = () => {
    console.log('save')
}
// 组件挂载时添加按钮
addButton({
    text: '保存',
    type: 'primary',
    handler: save
})

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
// 组件卸载时移除按钮
onUnmounted(() => {
    removeButton('保存')
})
</script>
<style lang="scss"></style>