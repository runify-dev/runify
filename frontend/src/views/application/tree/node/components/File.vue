<template>
    <div v-menus:right="menus" style="width: 100%;display: flex;align-items: center;">
        <AppIcon :name="icon"></AppIcon>
        <span v-if="operate === 'view'" style="margin-left: 5px;">{{ data.name }}</span>
        <el-input v-else @blur="enter" @keydown.enter="enter" v-focus v-model="nodeName"
            style="width: calc(100% - 20px);margin-left: 5px;" size="small" />
    </div>
</template>
<script setup lang="ts">
import { ref, computed } from "vue"
import { type TreeNodeData } from "element-plus"
import { set } from "lodash"
import AppIcon from '@/components/icons/AppIcon.vue';
import NodeApi from "@/api/node"
import type Node from 'element-plus/es/components/tree/src/model/node'
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
        set(props.data, 'operate', 'view')
        set(props.data, 'id', ok.data.id)
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
    } else if (operate.value === 're_name') {
        if (!nodeName.value) {
            set(props.data, 'operate', 'view')
            nodeName.value = props.data.cloneName
            return
        }
        reName()
    }

}
const operate = computed(() => {
    if (props.data.operate) {
        return props.data.operate
    } else {
        return "view"
    }
})
const nodeName = computed({
    get: () => {
        return props.data.name
    },
    set: (name: string) => {
        set(props.data, 'name', name)
    }
});
const fileTypeMap: any = {
    "markdown": "app-md",
    "default": "Document"
}
const icon = computed(() => {
    return fileTypeMap[props.data.subtype] || 'Document'
})
const menus = ref({
    menus: [
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

</script>
<style lang="scss">
.el-input__inner {
    box-shadow: none
}
</style>