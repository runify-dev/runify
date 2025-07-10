<template>
  <div v-menus:right="menus" @contextmenu.stop style="width: 100%;display: flex;align-items: center;">
    <AppIcon name="Folder"></AppIcon>
    <span v-if="operate === 'view'" style="margin-left: 5px;">{{ data.name }}</span>
    <el-input v-else @blur="enter" v-focus @keydown.enter="enter" v-model="nodeName"
      style="width: calc(100% - 20px);margin-left: 5px;" size="small" />
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from "vue"
import { type TreeNodeData } from "element-plus"
import type Node from 'element-plus/es/components/tree/src/model/node'
import { set } from "lodash"
import AppIcon from '@/components/icons/AppIcon.vue';
import NodeApi from "@/api/node"
const props = defineProps<{
  data: TreeNodeData,
  node: Node
}>()

const create = () => {
  NodeApi.create({
    "name": props.data.name,
    "meta": props.data.meta,
    "parentId": props.data.parentId,
    "source": props.data.source,
    "type": props.data.type,
    "subtype": props.data.subtype
  }).then((ok) => {
    set(props.data, 'id', ok.data.id)
    set(props.data, 'operate', 'view')
    props.node.initialize()
  });
}
const remove = () => {
  NodeApi.remove(props.data.id).then(() => {
    props.node.remove()
  })
}
const reName = () => {
  NodeApi.edit(props.data.id, { name: nodeName.value }).then((ok) => {
    set(props.data, 'name', ok.data.name)
    set(props.data, 'operate', 'view')
  })
}
const enter = () => {
  if (operate.value === 'create') {
    if (!nodeName.value) {
      props.node.remove()
      return
    }
    create()
  } else {
    if (!nodeName.value) {
      set(props.data, 'operate', 'view')
      nodeName.value = props.data.cloneName
      return
    }
    reName()
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
const menus = ref({
  menus: [
    {
      label: "创建",
      children: [
        {
          label: "应用",
          click: () => {
            props.node?.insertAfter({
              data: {
                "type": "file",
                "source": "application",
                "operate": "create",
                "name": "",
                "parentId": props.data.id,
                "subtype": "application",
                "meta": {
                }
              }
            }, props.node)
            set(props.node, 'expanded', true)
          }
        },
        {
          label: "创建文件夹",
          tip: '不关闭菜单',
          click: () => {
            props.node?.insertAfter({
              data: {
                "type": "folder",
                "source": "application",
                "operate": "create",
                "name": "",
                "parentId": props.data.id,
                "subtype": "folder",
                children: [],
                "meta": {
                }
              }
            }, props.node)
            set(props.node, 'expanded', true)
          }
        }
      ],
    },
    {
      label: "重命名",
      click: () => {
        set(props.data, 'operate', 're_name')
        set(props.data, 'cloneName', props.data.name)
      }
    },
    {
      label: "删除",
      click: remove
    }

  ]
})
const operate = computed(() => {
  if (props.data.operate) {
    return props.data.operate
  } else {
    return "view"
  }
})

</script>
<style lang="scss"></style>