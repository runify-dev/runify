<template>
    <div class="w-full h-full grid grid-flow-col  grid-rows-1">

        <div class="col-span-1 flex items-center mr-1">
            <img class="size-12 shrink-0 object-fill w-8 h-8 rounded-md" src="/user.jpeg" alt="节点Icon" />
        </div>
        <div class="col-span-2 truncate font-normal align-middle text-sm/8">{{
            model.properties.name }}
        </div>
        <div class="col-span-3 flex justify-center items-center">
            <div @click="openContent"
                class="w-5 h-5 hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer flex rounded-xs justify-center items-center">
                <app-icon name="Edit"></app-icon>
            </div>

            <div
                class="w-5 h-5 hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer flex rounded-xs justify-center items-center">
                <app-icon name="Share"></app-icon>
            </div>
            <div
                class="w-5 h-5 hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer flex rounded-xs justify-center items-center">
                <el-dropdown trigger="click">
                    <app-icon name="More"></app-icon>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item>重命名</el-dropdown-item>
                            <el-dropdown-item>删除</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>

        </div>
        <NodeContentContainer :validate="validate" ref="nodeContentContainerRef">
            <template #content v-if="$slots.default">
                <slot></slot>
            </template>
        </NodeContentContainer>
        <NodeMenu :append-node="appendNode" :workflowType="'APPLICATION'" ref="nodeMenuRef"></NodeMenu>
    </div>
</template>
<script setup lang="ts">
import { BaseNodeModel } from '@logicflow/core'
import { inject, ref, onMounted } from "vue"
import AppIcon from '@/components/icons/AppIcon.vue'
import NodeMenu from "@/workflow/common/NodeMenu.vue"
import { generateAnchor } from '@/utils/common'
import NodeContentContainer from "@/workflow/common/NodeContentContainer.vue"
const nodeMenuRef = ref<InstanceType<typeof NodeMenu>>();
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
defineProps<{
    validate: () => Promise<boolean>,
}>();
const appendNode = (node: any, anchorData: any) => {
    const nodeModel = model.graphModel.addNode({
        type: node.type,
        properties: node.properties,
        x: anchorData.x + node.properties.width / 2 + 200,
        y: anchorData.y,

    })
    model.graphModel.addEdge({
        type: 'run-edge',
        sourceNodeId: model.id,
        sourceAnchorId: anchorData.id,
        targetNodeId: nodeModel.id,
        targetAnchorId: generateAnchor(nodeModel.id, 'left', 'main', 'success')
    })
    nodeMenuRef.value?.close()
}
const nodeContentContainerRef = ref<InstanceType<typeof NodeContentContainer>>()
const openContent = () => {
    nodeContentContainerRef.value?.open(model)
}
onMounted(() => {
    model.openNodeMenu = (anchorData: any) => {
        nodeMenuRef.value?.open(anchorData)
    }
})
</script>
<style lang="scss">
.workflow-simple-node-card-header {
    height: 10px;
    padding: 1px;
}

.lf-node-selected {
    .workflow-simple-node-card {
        border: 3px solid var(--el-color-primary);
    }
}
</style>