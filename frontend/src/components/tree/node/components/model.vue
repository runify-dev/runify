<template>
  <div class="w-full flex items-center group">
    <AppIcon :name="icon"></AppIcon>
    <span v-if="operate === 'view'" style="margin-left: 5px">{{ data.name }}</span>
    <el-input
      v-else
      @blur="enter"
      @keydown.enter="enter"
      v-focus
      v-model="nodeName"
      style="width: calc(100% - 20px); margin-left: 5px"
      size="small"
    />
    <div class="flex-auto"></div>
    <div class="group-hover:block hidden">
      <div class="grid place-items-center" @click.stop>
        <el-dropdown trigger="click">
          <el-icon>
            <More />
          </el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="a" @click="reName">重命名</el-dropdown-item>
              <el-dropdown-item command="a" @click="remove">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { type TreeNodeData } from 'element-plus'
import { set } from 'lodash'
import AppIcon from '@/components/icons/AppIcon.vue'
import NodeApi from '@/api/node'
import type Node from 'element-plus/es/components/tree/src/model/node'
import type { ResourceType } from '@/api/type/common'
const props = defineProps<{
  data: TreeNodeData
  node: Node
  resource: ResourceType
}>()

const remove = () => {
  NodeApi.remove(props.resource, props.data.parentId, props.data.id).then(() => {
    props.node.remove()
  })
}
const reName = () => {
  set(props.data, 'operate', 'rename')
}
const enter = () => {
  if (nodeName.value) {
    NodeApi.rename(props.resource, props.data.parentId, props.data.id, nodeName.value).then(() => {
      set(props.data, 'operate', 'view')
      set(props.data, 'name', nodeName.value)
    })
  }
}
const operate = computed(() => {
  if (props.data.operate) {
    return props.data.operate
  } else {
    return 'view'
  }
})

const nodeName = computed({
  get: () => {
    return props.data.name
  },
  set: (name: string) => {
    set(props.data, 'name', name)
  }
})
const fileTypeMap: any = {
  model: 'app-model',
  default: 'Document'
}
const icon = computed(() => {
  return fileTypeMap[props.data.type]
})
</script>
<style lang="scss">
.el-input__inner {
  box-shadow: none;
}
</style>
