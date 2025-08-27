<template>
    <div style="height: calc(100vh - 16px)">
        <div>
            <div class="knowledge-menu" @click="router.push({
                path: `/${resource}/star`
            })" :class="star ? 'is_current' : ''">
                <AppIcon name="app-collect"></AppIcon>
                收藏
            </div>
            <div class="knowledge-menu" @click="router.push({
                path: `/${resource}/share`
            })" :class="share ? 'is_current' : ''">
                <AppIcon name="app-share"></AppIcon>
                分享
            </div>
        </div>
        <el-divider />
        <div class="group knowledge-menu flex items-center" :class="currentId == 'root' ? 'is_current' : ''" @click="router.push({
            path: `/${resource}/folder/root/resource/root`
        })">
            <div>全部{{ resourceNameRecord[resource] }}</div>
            <div class="flex-auto"></div>
            <div class="group-hover:block hidden">
                <div class="grid place-items-center">
                    <el-dropdown trigger="click">
                        <el-icon>
                            <More />
                        </el-icon>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="a" @click="create('md')">创建</el-dropdown-item>
                                <el-dropdown-item command="a" @click="create('folder')">创建目录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </div>
        </div>
        <div style="overflow-y: auto; height:calc(100vh - 140px)">
            <el-tree ref="treeRef" :highlight-current="true" @node-click="(node: any) => nodeClick(node)"
                :current-node-key="['root'].includes(currentId ? currentId : '') ? undefined : currentId"
                :default-expanded-keys="currentId ? [currentId] : []" :data="data" node-key="id" :props="propsConf">
                <template v-slot="node">
                    <NodeVue :data="node.data" :node="node.node" :resource="resource" :create="create"
                        :nodeClick="nodeClick">
                    </NodeVue>
                </template>
            </el-tree>
        </div>
    </div>
</template>
<script setup lang="ts">
import { type Tree } from '@/api/type/node';
import { useRouter } from 'vue-router';
import NodeVue from '@/components/tree/node/index.vue';
import { ref } from "vue"
import { ElTree } from 'element-plus'
import AppIcon from '@/components/icons/AppIcon.vue';
import type { ResourceType, Type } from '@/api/type/common';
import { resourceNameRecord } from './data';
const router = useRouter()
const treeRef = ref<InstanceType<typeof ElTree>>()
withDefaults(defineProps<{
    create: (type: Type, id?: string) => Promise<any>,
    nodeClick: (node: any, isCreate?: boolean) => void
    currentId?: string,
    star?: boolean,
    share?: boolean,
    resource: ResourceType,
    data: Array<Tree>,
    propsConf?: any
}>(), {
    resource: 'knowledge',
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
