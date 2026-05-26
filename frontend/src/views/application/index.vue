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
              :model="_items"
              class="w-full application-menu border-none!"
            >
              <template #item="{ item, props }">
                <a
                  v-ripple
                  @click="to(item.name)"
                  class="flex items-center"
                  :class="item.name === route.name ? 'p-menu-item-selected' : ''"
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
      <ApplicationFormDialog
        ref="applicationFormDialogRef"
        :api="treeCommonAPI"
        @create:success="createResourceSuccess"
        @edit:success="editResourceSuccess"
      ></ApplicationFormDialog>
      <AgentFormDialog
        ref="agentFormDialogRef"
        :api="treeCommonAPI"
        @create:success="createResourceSuccess"
      ></AgentFormDialog>
      <NoteSearchFormDialog
        ref="noteSearchFormDialogRef"
        :api="treeCommonAPI"
        @create:success="createResourceSuccess"
      ></NoteSearchFormDialog>
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

    <RouterView :key="route.name"></RouterView>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import ApplicationFormDialog from './component/ApplicationFormDialog.vue'
import AgentFormDialog from './component/AgentFormDialog.vue'
import NoteSearchFormDialog from './component/NoteSearchFormDialog.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import RenameDialog from '@/components/rename-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import {onMounted, ref, computed, onBeforeUnmount} from 'vue'
import Tree, {type TreeSelectionKeys} from 'primevue/tree'
import {toTree, toTreeNode} from '@/components/tree/index'
import {useRouter, useRoute} from 'vue-router'
import {TreeManager} from '@/components/tree/index'
import {TreeCommonAPI} from '@/api/tree'
import FlipCard from '@/components/flip-card/index.vue'
import type {TreeNode} from 'primevue/treenode'
import bus from '@/bus/index'
import TreeEmpty from '@/components/tree-empty/index.vue'
import {PermissionConstants} from '@/permission/data'
import {hasPermission} from '@/permission'
import {Role} from '@/permission/common'
import {ROOT_FOLDER_ID} from "@/constants/common.ts";
import useStore from "@/stores";

const {user} = useStore();

const route = useRoute()
const to = (routeName: string) => {
  router.push({name: routeName})
}
const isFlipped = ref<boolean>(
  ['applicationOverview', 'applicationSetting', 'applicationConversationLog'].includes(
    route.name as string
  )
)
const items = computed(() => {
  return [
    {
      name: 'applicationOverview',
      label: '概览',
      icon: 'pi pi-fw pi-objects-column p-1',
      permissions: [
        PermissionConstants.APPLICATION_OVERVIEW_READ.newResourcePermission(
          route.params.id as string
        ),
        Role.ADMIN
      ],
      shortcut: ''
    },
    {
      name: 'applicationSetting',
      label: '设置',
      icon: 'pi pi-fw pi-cog p-1',
      permissions: [
        PermissionConstants.APPLICATION_SETTING_READ.newResourcePermission(route.params.id as string),
        Role.ADMIN
      ]
    },
    {
      name: 'applicationConversationLog',
      label: '对话日志',
      icon: 'pi pi-fw pi-file p-1',
      permissions: [
        PermissionConstants.APPLICATION_CONVERSATION_LOG_READ.newResourcePermission(
          route.params.id as string
        ),
        Role.ADMIN
      ]
    }
  ]
})

const getMenuItems = (node: any) => {
  return [
    {
      label: '新建',
      visible: () => {
        return node.data.type === 'folder' && hasPermission([
          PermissionConstants.APPLICATION_CREATE.newResourcePermission(node.key),
          PermissionConstants.APPLICATION_FOLDER_CREATE.newResourcePermission(node.key),
          Role.ADMIN], "OR")
      },
      items: [
        {
          label: '应用',
          visible: () => node.data.type === 'folder' && hasPermission([
            PermissionConstants.APPLICATION_CREATE.newResourcePermission(node.key),
            Role.ADMIN], "OR"),
          command: () => openCreateApplicationDialog(node)
        },
        {
          label: '文件夹',
          visible: node.data.type === 'folder' && hasPermission([
            PermissionConstants.APPLICATION_FOLDER_CREATE.newResourcePermission(node.key as string),
            Role.ADMIN], "OR"),
          command: () => openCreateFolderDialog(node)
        }
      ]
    },
    {
      label: '重命名',
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.APPLICATION_FOLDER_EDIT.newResourcePermission(node.key) : PermissionConstants.APPLICATION_EDIT.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      command: () => openRenameDialog(node)
    },
    {
      visible: hasPermission([
        node.data.type === 'folder' ? PermissionConstants.APPLICATION_FOLDER_DELETE.newResourcePermission(node.key) : PermissionConstants.APPLICATION_DELETE.newResourcePermission(node.key),
        Role.ADMIN], "OR"),
      label: '删除',
      command: () => removeTreeNode(node)
    }
  ]
}
const _items = computed(() => {
  return items.value.filter((item: any) => {
    return hasPermission(item.permissions, 'OR')
  })
})
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  return {[id]: true}
})
const treeCommonAPI = new TreeCommonAPI('application')
const router = useRouter()
const flipCardRef = ref<InstanceType<typeof FlipCard>>()
const back = () => {
  const id = route.params.id as string
  const parent = treeManage.value?.findParentNode(id)
  if (parent) {
    router.push({name: 'applicationFolders', params: {id: parent?.key}})
  } else {
    router.push({name: 'applicationFolders', params: {id: ROOT_FOLDER_ID}})
  }
  flipCardRef.value?.unflip()
}

const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({name: 'applicationFolders', params: {id: ROOT_FOLDER_ID}})
    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({name: 'applicationFolders', params: {id: treeNode.key}})
  } else {
    flipCardRef.value?.flip()
    if (
      !['applicationOverview', 'applicationSetting', 'applicationConversationLog'].includes(
        route.name as string
      )
    ) {
      router.push({name: 'applicationDetails', params: {id: treeNode.key}})
    } else {
      router.push({name: route.name, params: {id: treeNode.key}})
    }
  }
}
const createResourceSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'application'})
    treeManage.value?.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    nodeSelect(treeNode)
  })
}
const editResourceSuccess = (id: string, data: any) => {
  treeManage.value?.updateLabel(id, data.name)
  bus.emit('application:edit:success', {id, ...data})
}
const createFolderSuccess = (key: string, node: any) => {
  user.resetProfile().then(ok => {
    const treeNode = toTreeNode({...node, type: 'folder'})
    treeManage.value?.addChild(key, treeNode)
    expandedKeys.value = {[key]: true}
    router.push({name: 'applicationFolders', params: {id: node.id}})
  })
}
const applicationFormDialogRef = ref<InstanceType<typeof ApplicationFormDialog>>()
const agentFormDialogRef = ref<InstanceType<typeof AgentFormDialog>>()
const noteSearchFormDialogRef = ref<InstanceType<typeof NoteSearchFormDialog>>()
const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const renameDialogRef = ref<InstanceType<typeof RenameDialog>>()
const openCreateApplicationDialog = (node?: TreeNode) => {
  applicationFormDialogRef.value?.openCreate(node)
}
const openAgentFormDialog = (node?: TreeNode) => {
  agentFormDialogRef.value?.open(node)
}
const openNoteSearchFormDialog = (node?: TreeNode) => {
  noteSearchFormDialogRef.value?.open(node)
}
const openEditApplicationDialog = (data: any) => {
  applicationFormDialogRef.value?.openEdit(data)
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
  bus.on('open:create:application:dialog', ({id, type}: { id: string; type?: string }) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    if (type === 'agent') {
      openAgentFormDialog(treeNode ? treeNode : undefined)
    } else if (type === 'search') {
      openNoteSearchFormDialog(treeNode ? treeNode : undefined)
    } else {
      openCreateApplicationDialog(treeNode ? treeNode : undefined)
    }
  })

  bus.on('open:edit:application:dialog', (data: any) => {
    openEditApplicationDialog(data)
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
  bus.off('open:create:application:dialog')
  bus.off('open:edit:application:dialog')
})
</script>
<style lang="scss"></style>
