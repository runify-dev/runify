<template>
    <AppSubLayout>
        <template #aside>
            <TreeAside ref="treeAsideRef" :currentId="resourceId" :create="create" resource="model"
                :nodeClick="nodeClick" :data="data" :insertAfter="insertAfter">
            </TreeAside>
        </template>
        <template #main>
            <RouterView></RouterView>
        </template>
    </AppSubLayout>
</template>
<script setup lang="ts">
import AppSubLayout from "@/layout/AppSubLayout.vue";
import { computed, onMounted, ref } from "vue";
import 'md-editor-v3/lib/style.css';
import TreeAside from "@/components/tree/index.vue"
import NodeApi from "@/api/node"
import { toTree } from "@/utils/common"
import { type Tree } from '@/api/type/node';
import { useRouter, useRoute } from 'vue-router';
import type { Type } from '@/api/type/common';
const treeAsideRef = ref<typeof TreeAside>()
const create = (type: Type, id?: string) => {
    return NodeApi.create('model', (id ? id : 'root'), { type: type }).then(ok => {
        if (!id) {
            data.value.push({ ...ok.data, operate: 'rename' })
            nodeClick(ok.data, true)
        }
        return ok
    })
}

const nodeClick = (node: any, isCreate?: boolean) => {
    if (node.type == 'folder') {
        router.push({
            path: `/model/folder/${node.parentId ? node.parentId : 'root'}/resource/${node.id}`
        })
    } else {
        router.push({
            path: `/model/folder/${node.parentId ? node.parentId : 'root'}/resource/${node.id}/${isCreate ? 'edit' : 'details'}`
        })
    }

}

const router = useRouter()
const route = useRoute()
const insertAfter = (node: any) => {
    data.value.push(node)
}
const resourceId = computed(() => {
    const {
        params: { id }
    } = route as any
    return id
})

const data = ref<Array<Tree>>([]);

onMounted(() => {
    NodeApi.listTree('model', undefined).then(ok => {
        data.value = toTree(ok.data)
    })

})
</script>
<style lang="scss" scoped></style>
