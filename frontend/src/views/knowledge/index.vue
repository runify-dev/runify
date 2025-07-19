<template>
  <AppSubLayout>
    <template #aside>
      <TreeAside :currentId="resourceId" :create="create" resource="knowledge" @update:current-node="clickChange"
        @node-click="nodeClick" :data="data" :insertAfter="insertAfter">
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
import { type Tree, type CurrentNode } from '@/api/type/node';

import { useRouter, useRoute } from 'vue-router';
import type { Type } from '@/api/type/common';
const create = (type: Type, id?: string) => {
  return NodeApi.create('knowledge', (id ? id : 'root'), { type: type })
}
const nodeClick = (node: any) => {
  if (node.type == 'folder') {
    router.push({
      path: `/knowledge/folder/${node.parentId ? node.parentId : 'root'}/resource/${node.id}`
    })
  } else {
    router.push({
      path: `/knowledge/folder/${node.parentId ? node.parentId : 'root'}/resource/${node.id}/details`
    })
  }

}

const router = useRouter()
const route = useRoute()
const insertAfter = (node: any) => {
  data.value.push(node)
}
const folderId = computed(() => {
  const {
    params: { folderId }
  } = route as any
  return folderId
})
const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const currentNode = ref<CurrentNode>({
  type: "all",
  node: "all"
});



const data = ref<Array<Tree>>([]);

const clickChange = (node: CurrentNode) => {
  currentNode.value = node;
  if (currentNode.value.type == 'tree' && (currentNode.value.node as Tree).type == 'file') {
    router.push({ name: 'knowledgeDetails', params: { id: currentNode.value.node.id, type: currentNode.value.node.subtype } })
  } else {
    let id: string = currentNode.value.type;
    if (currentNode.value.type == 'tree') {
      id = (currentNode.value.node as Tree).id
    }
    router.push({ name: 'knowledgeList', params: { id: id } })
  }
}



onMounted(() => {
  NodeApi.listTree('knowledge', undefined).then(ok => {
    data.value = toTree(ok.data)
  })

})
</script>
<style lang="scss" scoped></style>
