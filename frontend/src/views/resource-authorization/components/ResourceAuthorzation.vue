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
            :config="config"
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
import { onMounted, ref } from 'vue'
import NodeVue from '@/components/tree/node/index.vue'
import AppSubLayout from '@/layout/AppSubLayout.vue'
import 'md-editor-v3/lib/style.css'
import NodeApi from '@/api/node'
import { toTree } from '@/utils/common'
import { type Tree } from '@/api/type/node'
import { Config } from '@/components/tree/index'
import type { Resource, Type } from '@/api/type/common'

const props = withDefaults(defineProps<{ resource?: Resource }>(), { resource: 'application' })
const config = new Config(
  props.resource,
  [],
  () => Promise.resolve(true),
  () => {}
)
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
const nodeClick = (node: any) => {}
const auth = (type: Type, id?: string) => {}
onMounted(() => {
  NodeApi.listTree('model', undefined).then((ok) => {
    data.value = toTree(ok.data)
  })
})
</script>
<style lang="scss" scoped></style>
