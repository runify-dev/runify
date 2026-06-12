<template>
  <AppMenuContent>
    <template #menu>
      <div class="h-full">
        <FlipCard v-model="isFlipped" ref="flipCardRef">
          <template #front>
            <div class="custom-front">
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
                            v-tooltip="t('common.operation')"
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
            </div>
          </template>
          <template #back>
            <div class="custom-back">
              <Button
                icon="pi pi-arrow-left"
                severity="contrast"
                variant="text"
                rounded
                aria-label="Star"
                @click="back"
              />
            </div>
            <Menu
              :model="items"
              class="w-full application-menu"
              :pt="{ root: { style: { border: 0 } } }"
            >
              <template #item="{ item, props }">
                <a
                  v-ripple
                  @click="to(item.name)"
                  class="flex items-center"
                  :class="item.activeNames.includes(route.name) ? 'p-menu-item-selected' : ''"
                  v-bind="props.action"
                >
                  <span :class="item.icon"/>
                  <span>{{ item.label }}</span>
                  <Badge v-if="item.badge" class="ml-auto" :value="item.badge"/>
                  <span
                    v-if="item.shortcut"
                    class="ml-auto border border-surface rounded bg-emphasis text-muted-color text-xs p-1"
                  >{{ item.shortcut }}</span
                  >
                </a>
              </template>
            </Menu>
          </template>
        </FlipCard>
      </div>
      <CreateProjectDialog
        ref="createProjectDialogRef"
        :api="treeCommonAPI"
        :name="t('project.projectLabel')"
        @create:resource:success="createResourceSuccess"
      ></CreateProjectDialog>
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
    </template>
    <RouterView></RouterView>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import CreateProjectDialog from '@/views/project/components/CreateProjectDialog .vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import RenameDialog from '@/components/rename-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import {onMounted, ref, computed, onBeforeUnmount, provide} from 'vue'
import Tree, {type TreeSelectionKeys} from 'primevue/tree'
import {toTree, toTreeNode} from '@/components/tree/index'
import {useRouter, useRoute} from 'vue-router'
import {TreeManager} from '@/components/tree/index'
import {TreeCommonAPI} from '@/api/tree'
import FlipCard from '@/components/flip-card/index.vue'
import TreeEmpty from '@/components/tree-empty/index.vue'
import type {TreeNode} from 'primevue/treenode'
import bus from '@/bus/index'
import {ROOT_FOLDER_ID} from "@/constants/common.ts";
import useStore from "@/stores";
import {hasPermission} from "@/permission";
import {PermissionConstants} from "@/permission/data.ts";
import {Role} from "@/permission/common.ts";
import { t } from '@/locales'

const {user} = useStore();
const route = useRoute()

const to = (routeName: string) => {
  router.push({name: routeName})
}
const isFlipped = ref<boolean>(
  ['databaseCollectionPool', 'projectProcessor', 'processorTable', 'processorWorkflow'].includes(
    route.name as string
  )
)
const items = computed(() => [

  {
    name: 'projectProcessor',
    label: t('project.processor'),
    icon: 'pi pi-fw pi-cog p-1',
    activeNames: ['projectProcessor', 'processorWorkflow', 'processorTable']
  }
])
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  return {[id]: true}
})
const treeCommonAPI = new TreeCommonAPI('project')
provide('treeCommonAPI', treeCommonAPI)
const router = useRouter()
const flipCardRef = ref<InstanceType<typeof FlipCard>>()
const back = () => {
  const id = route.params.id as string
  const parent = treeManage.value?.findParentNode(id)
  if (parent) {
    router.push({name: 'projectFolders', params: {id: parent?.key}})
  } else {
    router.push({name: 'projectFolders', params: {id: ROOT_FOLDER_ID}})
  }
  flipCardRef.value?.unflip()
}

const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({name: 'projectFolders', params: {id: ROOT_FOLDER_ID}})
    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({name: 'projectFolders', params: {id: treeNode.key}})
  } else {
    flipCardRef.value?.flip()
    if (!['projectPublic', 'projectProcessor'].includes(route.name as string)) {
      router.push({name: 'projectProcessor', params: {id: treeNode.key}})
    } else {
      router.push({name: route.name, params: {id: treeNode.key}})
    }
  }
}
const createResourceSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'project'})
    treeManage.value?.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    flipCardRef.value?.flip()
    router.push({name: 'projectDetails', params: {id: node.id}})
  })

}
const createFolderSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'folder'})
    treeManage.value?.addChild(key, treeNode)
    expandedKeys.value = {[key]: true}
    router.push({name: 'projectFolders', params: {id: node.id}})
  })
}
const createProjectDialogRef = ref<InstanceType<typeof CreateProjectDialog>>()
const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const renameDialogRef = ref<InstanceType<typeof RenameDialog>>()
const openCreateProjectDialog = (node?: TreeNode) => {
  createProjectDialogRef.value?.open(node)
}
const openCreateFolderDialog = (node?: TreeNode) => {
  createFolderDialogRef.value?.open(node)
}
const openRenameDialog = (node: TreeNode) => {
  renameDialogRef.value?.open(node.key, node.label || '', node.data.type === 'folder' ? 'folder' : 'resource')
}
const renameSuccess = (key: string, node: any) => {
  treeManage.value?.updateLabel(key, node.name)
}
const getMenuItems = (node: any) => {
  return [
    {
      label: t('common.create'),
      visible: node.data.type === 'folder' && hasPermission([
        PermissionConstants.PROJECT_CREATE.newResourcePermission(node.key),
        PermissionConstants.PROJECT_FOLDER_CREATE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      items: [
        {
          label: t('project.projectLabel'),
          visible: () => node.data.type === 'folder' && hasPermission([
            PermissionConstants.PROJECT_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => openCreateProjectDialog(node)
        },
        {
          label: t('project.folder'),
          visible: node.data.type === 'folder' && hasPermission([
            PermissionConstants.PROJECT_FOLDER_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => openCreateFolderDialog(node)
        }
      ]
    },
    {
      label: t('project.rename'),
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.PROJECT_FOLDER_EDIT.newResourcePermission(node.key) : PermissionConstants.PROJECT_EDIT.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      command: () => openRenameDialog(node)
    },
    {
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.PROJECT_FOLDER_DELETE.newResourcePermission(node.key) : PermissionConstants.PROJECT_DELETE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      label: t('project.delete'),
      command: () => removeTreeNode(node)
    }
  ]
}

const removeTreeNode = (node: TreeNode) => {
  ;(node.data.type === 'folder'
      ? treeCommonAPI.removeFolder(node.key)
      : treeCommonAPI.removeResource(node.key)
  ).then(() => {
    treeManage.value?.remove(node.key)
  })
}

const nodes = ref<Array<any>>([])
const treeManage = ref<TreeManager>()
onMounted(() => {
  bus.on('open:create:project:dialog', (id: string) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    openCreateProjectDialog(treeNode ? treeNode : undefined)
  })
  bus.on('tree:remove', (id: string) => {
    treeManage.value?.remove(id)
  })

  bus.on('sidebar:flip', () => {
    flipCardRef.value?.flip()
  })
  treeCommonAPI.listTree(ROOT_FOLDER_ID).then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
  })
})
onBeforeUnmount(() => {
  bus.off('tree:remove')
  bus.off('sidebar:flip')
  bus.off('open:create:project:dialog')
})
</script>
<style lang="scss">
.p-menu-item-selected {
  background: var(--p-tree-node-selected-background);
  color: var(--p-tree-node-selected-color);
}
</style>
