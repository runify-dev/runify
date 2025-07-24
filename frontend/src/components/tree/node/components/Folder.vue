<template>
  <div @contextmenu.stop class="w-full flex items-center group">
    <AppIcon name="Folder">
    </AppIcon>
    <span v-if="operate === 'view'" style="margin-left: 5px;">{{ data.name }}</span>
    <el-input v-else @blur="enter" v-focus @keydown.enter="enter" v-model="nodeName"
      style="width: calc(100% - 20px);margin-left: 5px;" size="small" />
    <div class="flex-auto"></div>
    <div class="group-hover:block hidden">
      <div class="grid place-items-center" @click.stop>
        <el-dropdown trigger="click">
          <el-icon>
            <More />
          </el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="createResource('folder')">创建目录</el-dropdown-item>
              <el-dropdown-item @click="createResource('md')">创建</el-dropdown-item>
              <el-dropdown-item @click="reName">重命名</el-dropdown-item>
              <el-dropdown-item @click="remove">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

    </div>

  </div>
</template>
<script setup lang="ts">
import { computed } from "vue"
import { type TreeNodeData } from "element-plus"
import type Node from 'element-plus/es/components/tree/src/model/node'
import { set } from "lodash"
import AppIcon from '@/components/icons/AppIcon.vue';
import NodeApi from "@/api/node"
import type { ResourceType, Type } from '@/api/type/common';
const props = defineProps<{
  data: TreeNodeData,
  node: Node,
  resource: ResourceType,
  create: (type: Type, id?: string) => Promise<any>,
  nodeClick: (node: any, isCreate?: boolean) => void
}>()

const remove = () => {
  NodeApi.remove(props.resource, props.data.parentId, props.data.id)
    .then(() => {
      props.node.remove()
    })
}

const reName = () => {
  set(props.data, 'operate', 'rename')
}

const createResource = (type: 'folder' | 'md') => {
  props.create(type, props.data.id).then(ok => {
    props.node.insertAfter({ data: { ...ok.data, operate: 'rename' } }, props.node)
    props.nodeClick(ok.data, true)
  })
}

const enter = () => {
  if (nodeName.value) {
    NodeApi.rename(props.resource, props.data.parentId ? props.data.parentId : 'root', props.data.id, nodeName.value).then(() => {
      set(props.data, 'operate', 'view')
      set(props.data, 'name', nodeName.value)
    })
    return
  }
}
const nodeName = computed({
  get: () => {
    return props.data.name
  },
  set: (name: string) => {
    set(props.data, 'name', name)
  }
});

const operate = computed(() => {
  if (props.data.operate) {
    return props.data.operate
  } else {
    return "view"
  }
})

</script>
<style lang="scss"></style>