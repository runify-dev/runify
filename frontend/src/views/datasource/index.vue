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
            <i class="pi pi-database" v-else></i>
          </template>
          <template #default="{ node }">
            <div class="flex items-center justify-between w-full group">
              <span>{{ node.label }}</span>
              <div class="action-buttons">
                <DropdownMenu
                  :items="getMenuItems(node)"
                >
                  <template #default>
                    <Button
                      v-tooltip="'操作'"
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

        <CreateFolderDialog
          @create:folder:success="createFolderSuccess"
          ref="createFolderDialogRef"
          :api="treeCommonAPI"
        ></CreateFolderDialog>
        <DatasourceFormDialog
          ref="datasourceFormDialogRef"
          :api="treeCommonAPI"
          @create:success="createResourceSuccess"
        ></DatasourceFormDialog>
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
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import DatasourceFormDialog from './components/DatasourceFormDialog.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import Tree, {type TreeSelectionKeys} from 'primevue/tree'
import {toTree, toTreeNode} from '@/components/tree/index'
import {useRouter, useRoute} from 'vue-router'
import {TreeManager} from '@/components/tree/index'
import {TreeCommonAPI} from '@/api/tree'
import TreeEmpty from '@/components/tree-empty/index.vue'
import type {TreeNode} from 'primevue/treenode'
import bus from '@/bus/index'
import {ROOT_FOLDER_ID} from "@/constants/common.ts";
import useStore from "@/stores";
import {hasPermission} from "@/permission";
import {PermissionConstants} from "@/permission/data.ts";
import {Role} from "@/permission/common.ts";
import RenameDialog from '@/components/rename-dialog/index.vue'

const {user} = useStore();
const route = useRoute()
const router = useRouter()
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  return {[id]: true}
})
const treeCommonAPI = new TreeCommonAPI('datasource')

const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({name: 'datasourceFolders', params: {id: ROOT_FOLDER_ID}})
    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({name: 'datasourceFolders', params: {id: treeNode.key}})
  } else {
    router.push({name: 'datasourceDetails', params: {id: treeNode.key}})
  }
}

const createResourceSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'datasource'})
    treeManage.value.addChild(key || ROOT_FOLDER_ID, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key || ROOT_FOLDER_ID]: true}
    nodeSelect(treeNode)
  })
}

const createFolderSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'folder'})
    treeManage.value.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
  })

}

const getMenuItems = (node: any) => {
  return [
    {
      label: '新建',
      visible: node.data.type === 'folder' && hasPermission([
        PermissionConstants.DATASOURCE_CREATE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      items: [
        {
          label: '数据源',
          visible: () => node.data.type === 'folder' && hasPermission([
            PermissionConstants.DATASOURCE_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => openCreateResourceDialog(node)
        },
        {
          label: '文件夹',
          visible: node.data.type === 'folder' && hasPermission([
            PermissionConstants.DATASOURCE_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => openCreateFolderDialog(node)
        }
      ]
    },
    {
      label: '重命名',
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.DATASOURCE_FOLDER_EDIT.newResourcePermission(node.key) : PermissionConstants.DATASOURCE_EDIT.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      command: () => openRenameDialog(node)
    },
    {
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.DATASOURCE_FOLDER_DELETE.newResourcePermission(node.key) : PermissionConstants.DATASOURCE_DELETE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      label: '删除',
      command: () => removeTreeNode(node)
    }
  ]
}

const datasourceFormDialogRef = ref<InstanceType<typeof DatasourceFormDialog>>()
const openCreateResourceDialog = (node?: TreeNode) => {
  datasourceFormDialogRef.value?.open(node)
}

const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const renameDialogRef = ref<InstanceType<typeof RenameDialog>>()
const openCreateFolderDialog = (node?: TreeNode) => {
  createFolderDialogRef.value?.open(node)
}
const openRenameDialog = (node: TreeNode) => {
  renameDialogRef.value?.open(node.key, node.label || '', node.data.type === 'folder' ? 'folder' : 'resource')
}
const renameSuccess = (key: string, node: any) => {
  treeManage.value?.updateLabel(key, node.name)
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
const reloadTree = () => {
  treeCommonAPI.listTree(ROOT_FOLDER_ID).then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
  })
}
onMounted(() => {
  bus.on('open:create:datasource:dialog', ({folderId}: { folderId: string }) => {
    const treeNode = treeManage.value?.findNodeByKey(folderId)
    openCreateResourceDialog(treeNode ? treeNode : undefined)
  })

  reloadTree()
})

onBeforeUnmount(() => {
  bus.off('open:create:datasource:dialog')
})
</script>
