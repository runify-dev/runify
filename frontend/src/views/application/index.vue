<template>
    <AppSubLayout>
        <template #aside>
            <TreeAside v-bind:current-node="currentNode" @update:current-node="clickChange" :data="data">
            </TreeAside>
        </template>
        <template #main>
            <RouterView></RouterView>
        </template>
    </AppSubLayout>
</template>
<script setup lang="ts">
import AppSubLayout from "@/layout/AppSubLayout.vue";

import { onMounted, ref, watch } from "vue";
import 'md-editor-v3/lib/style.css';
import TreeAside from "@/views/application/tree/index.vue"
import NodeApi from "@/api/node"
import { toTree } from "@/utils/common"
import { type Tree } from '@/api/type/node';
import { type CurrentNode } from '@/api/type/node';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter()
const route = useRoute()
const {
    params: { id } // id为datasetID
} = route as any
const currentNode = ref<CurrentNode>({
    type: "all",
    node: "all"
});

const data = ref<Array<Tree>>([]);
if (['all', 'share', 'star'].includes(id)) {
    currentNode.value = {
        type: id,
        node: id
    }
} else if (['applicationList', 'applicationOverview'].includes(route.name as string)) {
    currentNode.value = { 'node': { 'id': id }, 'type': 'tree' }
}
const clickChange = (node: CurrentNode) => {
    currentNode.value = node;
    if (currentNode.value.type == 'tree' && (currentNode.value.node as Tree).type == 'file') {
        router.push({ name: 'applicationOverview', params: { id: currentNode.value.node.id, type: currentNode.value.node.subtype } })
    } else {
        let id: string = currentNode.value.type;
        if (currentNode.value.type == 'tree') {
            id = (currentNode.value.node as Tree).id
        }
        router.push({ name: 'applicationList', params: { id: id } })
    }
}

watch(route, () => {

    if (['applicationOverview'].includes(route.name as string)) {
        currentNode.value = { 'node': { 'id': id }, 'type': 'tree' }
    }
})

onMounted(() => {
    NodeApi.list({ source: 'application' }).then(ok => {
        data.value = toTree(ok.data)
    })
})
</script>
<style lang="scss"></style>
