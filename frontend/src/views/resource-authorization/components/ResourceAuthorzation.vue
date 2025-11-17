<template>
  <AppSubLayout :left="0" :compute-aside-width="(x) => x - 142">
    <template #aside>
      <el-tree
        ref="treeRef"
        :highlight-current="true"
        @node-click="(node: any) => nodeClick(node)"
        :current-node-key="['root'].includes(currentId ? currentId : '') ? undefined : currentId"
        :default-expanded-keys="currentId ? [currentId] : []"
        :data="data"
        node-key="id"
        :props="propsConf"
      >
        <template v-slot="node">
          <NodeVue
            :data="node.data"
            :node="node.node"
            :resource="resource"
            :create="auth"
            :nodeClick="nodeClick"
          >
          </NodeVue>
        </template>
      </el-tree>
    </template>
  </AppSubLayout>
</template>
<script setup lang="ts">
import { computed, onMounted, ref, provide } from 'vue'
import NodeVue from '@/components/tree/node/index.vue'
import AppSubLayout from '@/layout/AppSubLayout.vue'
import 'md-editor-v3/lib/style.css'
import NodeApi from '@/api/node'
import { toTree } from '@/utils/common'
import { type Tree } from '@/api/type/node'
import { useRouter, useRoute } from 'vue-router'
import type { ResourceType, Type } from '@/api/type/common'
withDefaults(defineProps<{ resource?: ResourceType }>(), { resource: 'application' })
const propsConf = ref<any>({
  value: 'id',
  label: 'name',
  source: 'source',
  children: 'children',
  type: 'type',
  parent_id: 'parentId',
  meta: 'meta'
})
const currentId = ref<string>()

const data = ref<Array<Tree>>([])
const nodeClick = () => {}
const auth = (type: Type, id?: string) => {}
onMounted(() => {
  NodeApi.listTree('model', undefined).then((ok) => {
    data.value = toTree(ok.data)
  })
})
</script>
<style lang="scss" scoped></style>
