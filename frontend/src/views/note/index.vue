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
          <template #header>
            <div @click="nodeSelect(undefined)" class="p-tree-node">
              <div
                class="p-tree-node-content p-tree-node-selectable"
                :class="selectedKeys ? '' : 'p-tree-node-selected'"
              >
                <div class="p-tree-node-label w-full">
                  <div class="flex items-center justify-between w-full group">
                    <span>全部</span>
                    <div class="action-buttons">
                      <DropdownMenu
                        :items="[
                          {
                            label: '新建',

                            items: [
                              {
                                label: '笔记',
                                command: () => {
                                  openCreateNoteDialog()
                                }
                              },
                              {
                                label: '文件夹',
                                command: () => {
                                  openCreateFolderDialog()
                                }
                              }
                            ]
                          }
                        ]"
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
                </div>
              </div>
            </div>
          </template>
          <template #default="{ node }">
            <div class="flex items-center justify-between w-full group">
              <span>{{ node.label }}</span>
              <div class="action-buttons">
                <DropdownMenu
                  :items="[
                    {
                      label: '新建',
                      visible: node.data.type == 'folder',
                      items: [
                        {
                          label: '笔记',
                          visible: node.data.type == 'folder',
                          command: () => {
                            openCreateNoteDialog(node)
                          }
                        },
                        {
                          label: '文件夹',
                          visible: node.data.type == 'folder',
                          command: () => {
                            openCreateFolderDialog(node)
                          }
                        }
                      ]
                    },
                    {
                      label: '删除',
                      command: () => {
                        removeTreeNode(node)
                      }
                    }
                  ]"
                >
                  <template #item="scope">
                    <div class="p-tieredmenu-item-link">
                      <span>{{ scope.label }}</span>
                      <span v-if="scope.hasSubmenu" class="pi pi-angle-right ml-auto" />
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

        <CreateResourceDialog
          ref="createResourceDialogRef"
          :api="treeCommonAPI"
          name="笔记"
          @create:resource:success="createResourceSuccess"
        ></CreateResourceDialog>
        <CreateFolderDialog
          @create:folder:success="createFolderSuccess"
          ref="createFolderDialogRef"
          :api="treeCommonAPI"
        ></CreateFolderDialog>
      </div>
    </template>
    <router-view :key="route.path"></router-view>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import CreateResourceDialog from '@/components/create-resource-dialog/index.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import { computed, onMounted, ref, onBeforeUnmount } from 'vue'
import Tree, { type TreeSelectionKeys } from 'primevue/tree'
import { toTree, toTreeNode } from '@/components/tree/index'
import { useRouter, useRoute } from 'vue-router'
import { TreeManager } from '@/components/tree/index'
import { TreeCommonAPI } from '@/api/tree'
import bus from '@/bus/index'
const route = useRoute()
import type { TreeNode } from 'primevue/treenode'
import TreeEmpty from '@/components/tree-empty/index.vue'

const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  if (id === 'root') {
    return undefined
  } else {
    return { [id]: true }
  }
})
const treeCommonAPI = new TreeCommonAPI('note')
const router = useRouter()

const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({ name: 'noteFolders', params: { id: 'root' } })

    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({ name: 'noteFolders', params: { id: treeNode.key } })
  } else {
    router.push({ name: 'noteDetails', params: { id: treeNode.key } })
  }
}
const createResourceSuccess = (key: string, node: any) => {
  const treeNode = toTreeNode({ ...node, type: 'note' })
  treeManage.value.addChild(key, treeNode)
  expandedKeys.value = { ...expandedKeys.value, [key]: true }
  router.push({ name: 'noteDetails', params: { id: node.id } })
}
const createFolderSuccess = (key: string, node: any) => {
  const treeNode = toTreeNode({ ...node, type: 'folder' })
  treeManage.value.addChild(key, treeNode)
  expandedKeys.value = { ...expandedKeys.value, [key]: true }
  router.push({ name: 'noteFolders', params: { id: node.id } })
}
const createResourceDialogRef = ref<InstanceType<typeof CreateResourceDialog>>()
const openCreateNoteDialog = (node?: TreeNode) => {
  createResourceDialogRef.value?.open(node)
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
  bus.on('open:create:note:dialog', (id: string) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    openCreateNoteDialog(treeNode ? treeNode : undefined)
  })
  bus.on('tree:remove', (id: string) => {
    treeManage.value?.remove(id)
  })
  treeCommonAPI.listTree('root').then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
  })
})
onBeforeUnmount(() => {
  bus.off('tree:remove')
  bus.off('open:create:note:dialog')
})
</script>
<style lang="scss">
.p-menu-item-selected {
  background: var(--p-tree-node-selected-background);
  color: var(--p-tree-node-selected-color);
}
</style>
