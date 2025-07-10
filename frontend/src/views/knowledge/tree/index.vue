<template>
    <div style="height: calc(100vh - 16px)">
        <div>
            <div class="knowledge-menu" @click="select('star')"
                :class="currentNodeComputed.node === 'star' ? 'is_current' : ''">
                <AppIcon name="app-document"></AppIcon>
                加星
            </div>
            <div class="knowledge-menu" @click="select('share')"
                :class="currentNodeComputed.node === 'share' ? 'is_current' : ''">
                <AppIcon name="app-document"></AppIcon>
                分享
            </div>
        </div>
        <el-divider />
        <div class="knowledge-menu" @click="select('all')"
            :class="currentNodeComputed.node === 'all' ? 'is_current' : ''" v-menus:right="menus" style="width: 100%">
            全部知识库</div>
        <div style="overflow-y: auto; height:calc(100vh - 140px)">
            <el-tree ref="treeRef" :highlight-current="true" @current-change="(node) => { currentNodeComputed = node }"
                :current-node-key="currentNodeComputed.type == 'tree' ? (currentNodeComputed.node as Tree).id : undefined"
                :data="data" node-key="id" :props="propsConf">
                <template v-slot="node">
                    <NodeVue :data="node.data" :node="node.node"></NodeVue>
                </template>
            </el-tree>
        </div>

    </div>
</template>
<script setup lang="ts">
import { type Tree } from '@/api/type/node';
import { computed } from "vue"
import NodeVue from '@/views/knowledge/tree/node/index.vue';
import { ref } from "vue"
import { ElTree, type TreeKey } from 'element-plus'
import AppIcon from '@/components/icons/AppIcon.vue';
import { type CurrentNode } from '@/api/type/node';
const treeRef = ref<InstanceType<typeof ElTree>>()
const props = withDefaults(defineProps<{
    currentNode: CurrentNode,
    data: Array<Tree>,
    propsConf?: any
}>(), {
    propsConf: {
        value: "id",
        label: "name",
        source: "source",
        children: "children",
        type: 'type',
        parent_id: 'parentId',
        meta: 'meta'
    }
});


const select = (type: "star" | "share" | "all") => {
    currentNodeComputed.value = type
}
const emit = defineEmits(["update:currentNode"])

const currentNodeComputed = computed({
    get: () => {
        return props.currentNode
    },
    set: (value: "star" | "share" | "all" | "tree" | Tree) => {
        if (typeof value === 'string' && ['new', "star", "share", "all"].includes(value)) {
            emit('update:currentNode', { type: value, node: value })
        } else {
            emit('update:currentNode', { type: 'tree', node: value })
        }
    }
})

const menus = ref({
    menus: [
        {
            label: "创建",
            children: [
                {
                    label: "Markdown",
                    click: () => {
                        treeRef.value?.append({
                            "type": "file",
                            "source": "knowledge",
                            "operate": "create",
                            "name": "",
                            "parentId": null,
                            "subtype": "markdown",
                            "meta": {}
                        }, undefined as unknown as TreeKey)
                    }
                },
                {
                    label: "创建文件夹",
                    tip: '不关闭菜单',
                    click: () => {
                        treeRef.value?.append({
                            "type": "folder",
                            "source": "knowledge",
                            "operate": "create",
                            "name": "",
                            "parentId": null,
                            "subtype": "folder",
                            "meta": {}
                        }, undefined as unknown as TreeKey)
                    }
                }
            ],
        },

    ]
})
</script>
<style lang="scss" scoped>
ui {
    list-style: none;
    display: inline-block;
}

.is_current {
    background-color: var(--el-color-primary-light-9);
}

.knowledge-menu {
    height: var(--el-tree-node-content-height, 26px);

    &:hover {
        cursor: pointer;
        background-color: var(--el-fill-color-light);
    }
}

:deep.el-divider--horizontal {
    margin: 8px 0;
}
</style>