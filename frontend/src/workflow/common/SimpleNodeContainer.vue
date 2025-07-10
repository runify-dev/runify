<template>
    <div class="w-full h-full grid grid-flow-col  grid-rows-1">

        <div class="col-span-1 flex items-center mr-1">
            <img class="size-12 shrink-0 object-fill w-8 h-8 rounded-md" src="/user.jpeg" alt="节点Icon" />
        </div>
        <div class="col-span-2 truncate font-normal align-middle text-sm/8">{{
            model.properties.name }}
        </div>
        <div class="col-span-3 flex justify-center items-center">
            <div class="w-5 h-5 hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer flex rounded-xs justify-center items-center"
                @click="open">
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

        <slot></slot>
        <NodeMenu :append-node="appendNode" :workflowType="'APPLICATION'" ref="nodeMenuRef"></NodeMenu>
    </div>
</template>
<script setup lang="ts">
import { BaseNodeModel, type BaseEdgeModel } from '@logicflow/core'
import { inject, ref, onMounted } from "vue"
import AppIcon from '@/components/icons/AppIcon.vue'
import NodeMenu from "@/workflow/common/NodeMenu.vue"
const nodeMenuRef = ref<InstanceType<typeof NodeMenu>>();
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
defineProps<{
    open: () => Promise<string>,
    close: () => Promise<string>
}>();
const appendNode = (node: any, anchorData: any) => {
    const nodeModel = model.graphModel.addNode({
        type: node.type,
        properties: node.properties,
        x: anchorData.x + node.properties.width / 2 + 200,
        y: anchorData.y
    })

    model.graphModel.addEdge({
        sourceNodeId: model.id,
        sourceAnchorId: anchorData.id,
        targetNodeId: nodeModel.id,
        targetAnchorId: nodeModel.id + '_left'
    })
    nodeMenuRef.value?.close()
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