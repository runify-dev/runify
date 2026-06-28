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
                      v-tooltip="t('integration.operation')"
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

        <IntegrationFormDialog
          ref="integrationFormDialogRef"
          :api="treeCommonAPI"
          @create:success="createResourceSuccess"
        ></IntegrationFormDialog>
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
import IntegrationFormDialog from './components/IntegrationFormDialog.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import {computed, onMounted, ref, onBeforeUnmount} from 'vue'
import Tree, {type TreeSelectionKeys} from 'primevue/tree'
import {toTree, toTreeNode} from '@/components/tree/index'
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
const treeCommonAPI = new TreeCommonAPI('integration')
const router = useRouter()


const renameSuccess = (key: string, node: any) => {
  treeManage.value?.updateLabel(key, node.name)
}
const getMenuItems = (node: any) => {
  return [
    {
      label: t('integration.create'),
      visible: node.data.type === 'folder' && hasPermission([
        PermissionConstants.INTEGRATION_FOLDER_CREATE.newResourcePermission(node.key),
        PermissionConstants.INTEGRATION_CREATE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      items: [
        {
          label: t('integration.integrationLabel'),
          visible: () => node.data.type === 'folder' && hasPermission([
            PermissionConstants.INTEGRATION_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => {
            openCreateDialog(node)
          }
        },
        {
          label: t('common.folder'),
          visible: node.data.type === 'folder' && hasPermission([
            PermissionConstants.INTEGRATION_FOLDER_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => {
            openCreateFolderDialog(node)
          }
        }
      ]
    },
    {
      label: t('integration.rename'),
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.INTEGRATION_FOLDER_EDIT.newResourcePermission(node.key) : PermissionConstants.INTEGRATION_EDIT.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      command: () => openRenameDialog(node)
    },
    {
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.INTEGRATION_FOLDER_DELETE.newResourcePermission(node.key) : PermissionConstants.INTEGRATION_DELETE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      label: t('integration.delete'),
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
    router.push({name: 'integrationFolders', params: {id: ROOT_FOLDER_ID}})

    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({name: 'integrationFolders', params: {id: treeNode.key}})
  } else {
    router.push({name: 'integrationDetails', params: {id: treeNode.key}})
  }
}
const createResourceSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'integration'})
    treeManage.value.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    router.push({name: 'integrationDetails', params: {id: node.id}})
  })

}
const createFolderSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'folder'})
    treeManage.value.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    router.push({name: 'integrationFolders', params: {id: node.id}})
  })

}
const integrationFormDialogRef = ref<InstanceType<typeof IntegrationFormDialog>>()
const openCreateDialog = (node?: TreeNode) => {
  integrationFormDialogRef.value?.open(node)
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
onMounted(() => {
  bus.on('open:create:integration:dialog', (id: string) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    openCreateDialog(treeNode ? treeNode : undefined)
  })
  bus.on('tree:remove', (id: string) => {
    treeManage.value?.remove(id)
  })
  treeCommonAPI.listTree(ROOT_FOLDER_ID).then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
  })
})
onBeforeUnmount(() => {
  bus.off('tree:remove')
  bus.off('open:create:integration:dialog')
})
</script>
<style lang="scss">
.p-menu-item-selected {
  background: var(--p-tree-node-selected-background);
  color: var(--p-tree-node-selected-color);
}
</style>
