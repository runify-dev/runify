<template>
  <AppMenuContent>
    <template #menu>
      <div class="h-full">
        <Tree
          v-model:selectionKeys="selectedKeys"
          v-model:expandedKeys="expandedKeys"
          :filter="true"
          :value="nodes"
          class="w-full"
          selectionMode="single"
          @node-select="nodeSelect"
          :pt="{
            root: {
              style: { padding: '16px 0' }
            },
            nodeLabel: {
              style: { width: '100%' }
            }
          }"
        >
          <template #empty>
            <TreeEmpty></TreeEmpty>
          </template>
          <template #nodeicon="scope">
            <i class="pi pi-folder" v-if="scope.node.type == 'folder'"></i>
          </template>
          <template #default="{ node }">
            <div class="flex items-center justify-between w-full group">
              <span>{{ node.label }}</span>
              <div class="action-buttons">
                <DropdownMenu
                  :items="getMenuItems(node)"
                >
                  <template #item="scope">
                    <div class="p-tieredmenu-item-link">
                      <span>{{ scope.label }}</span>
                      <span v-if="scope.hasSubmenu" class="pi pi-angle-right ml-auto"/>
                    </div>
                  </template>
                  <template #default>
                    <Button
                      v-tooltip="t('model.operation')"
                      icon="pi pi-ellipsis-v"
                      variant="text"
                      aria-label="Filter"
                      severity="secondary"
                      size="small"
                    ></Button>
                  </template>
                </DropdownMenu>
              </div>
            </div>
          </template>
        </Tree>

        <ModelFormDialog
          ref="modelFormDialogRef"
          :api="treeCommonAPI"
          @create:success="createResourceSuccess"
        ></ModelFormDialog>
        <CreateFolderDialog
          @create:folder:success="createFolderSuccess"
          ref="createFolderDialogRef"
          :api="treeCommonAPI"
        ></CreateFolderDialog>
        <RenameDialog
          @rename:success="renameSuccess"
          ref="renameDialogRef"
          :api="treeCommonAPI"
        ></RenameDialog>
      </div>
    </template>
    <router-view :key="route.path"></router-view>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import ModelFormDialog from './components/ModelFormDialog.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import {computed, onMounted, ref, onBeforeUnmount} from 'vue'
import Tree, {type TreeSelectionKeys} from 'primevue/tree'
import {toTree, toTreeNode} from '@/components/tree/index'
import {useExpandSelectedAncestors} from '@/components/tree/useExpandSelectedAncestors'
import {useRouter, useRoute} from 'vue-router'
import {TreeManager} from '@/components/tree/index'
import {TreeCommonAPI} from '@/api/tree'
import bus from '@/bus/index'
import TreeEmpty from '@/components/tree-empty/index.vue'
import useStore from "@/stores";

const renameDialogRef = ref<InstanceType<typeof RenameDialog>>()
const {user} = useStore();
const route = useRoute()
import type {TreeNode} from 'primevue/treenode'
import {ROOT_FOLDER_ID} from "@/constants/common.ts";
import {hasPermission} from "@/permission";
import {PermissionConstants} from "@/permission/data.ts";
import {Role} from "@/permission/common.ts";
import RenameDialog from "@/components/rename-dialog/index.vue";
import { t } from '@/locales'

const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  return {[id]: true}
})
const treeCommonAPI = new TreeCommonAPI('model')
const router = useRouter()


const renameSuccess = (key: string, node: any) => {
  treeManage.value?.updateLabel(key, node.name)
}
const getMenuItems = (node: any) => {
  return [
    {
      label: t('model.create'),
      visible: node.data.type === 'folder' && hasPermission([
        PermissionConstants.MODEL_FOLDER_CREATE.newResourcePermission(node.key),
        PermissionConstants.MODEL_CREATE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      items: [
        {
          label: t('model.modelLabel'),
          visible: () => node.data.type === 'folder' && hasPermission([
            PermissionConstants.MODEL_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => {
            openCreateNoteDialog(node)
          }
        },
        {
          label: t('common.folder'),
          visible: node.data.type === 'folder' && hasPermission([
            PermissionConstants.MODEL_FOLDER_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => {
            openCreateFolderDialog(node)
          }
        }
      ]
    },
    {
      label: t('model.rename'),
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.MODEL_FOLDER_EDIT.newResourcePermission(node.key) : PermissionConstants.MODEL_EDIT.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      command: () => openRenameDialog(node)
    },
    {
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.MODEL_FOLDER_DELETE.newResourcePermission(node.key) : PermissionConstants.MODEL_DELETE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      label: t('model.delete'),
      command:
        () => {
          removeTreeNode(node)
        }
    }
  ]

}
const openRenameDialog = (node: TreeNode) => {
  renameDialogRef.value?.open(node.key, node.label || '', node.data.type === 'folder' ? 'folder' : 'resource')
}
const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({name: 'modelFolders', params: {id: ROOT_FOLDER_ID}})

    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({name: 'modelFolders', params: {id: treeNode.key}})
  } else {
    router.push({name: 'modelDetails', params: {id: treeNode.key}})
  }
}
const createResourceSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'model'})
    treeManage.value.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    router.push({name: 'modelDetails', params: {id: node.id}})
  })

}
const createFolderSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'folder'})
    treeManage.value.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    router.push({name: 'modelFolders', params: {id: node.id}})
  })

}
const modelFormDialogRef = ref<InstanceType<typeof ModelFormDialog>>()
const openCreateNoteDialog = (node?: TreeNode) => {
  modelFormDialogRef.value?.open(node)
}
const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const openCreateFolderDialog = (node?: TreeNode) => {
  createFolderDialogRef.value?.open(node)
}
const removeTreeNode = (node: TreeNode) => {
  ;(node.data.type === 'folder'
      ? treeCommonAPI.removeFolder(node.key)
      : treeCommonAPI.removeResource(node.key)
  ).then(() => {
    treeManage.value.remove(node.key)
  })
}

const nodes = ref<Array<any>>([])
const treeManage = ref()
const expandSelectedAncestors = useExpandSelectedAncestors(treeManage, expandedKeys)
onMounted(() => {
  bus.on('open:create:model:dialog', (id: string) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    openCreateNoteDialog(treeNode ? treeNode : undefined)
  })
  bus.on('tree:remove', (id: string) => {
    treeManage.value?.remove(id)
  })
  treeCommonAPI.listTree(ROOT_FOLDER_ID).then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
    expandSelectedAncestors()
  })
})
onBeforeUnmount(() => {
  bus.off('tree:remove')
  bus.off('open:create:model:dialog')
})
</script>
<style lang="scss">
.p-menu-item-selected {
  background: var(--p-tree-node-selected-background);
  color: var(--p-tree-node-selected-color);
}
</style>
