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
                                label: '数据源',
                                command: () => {
                                  openCreateResourceDialog()
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
                          label: '数据源',
                          visible: node.data.type == 'folder',
                          command: () => {
                            openCreateResourceDialog(node)
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
      </div>
    </template>
    <router-view :key="route.path"></router-view>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import Tree, { type TreeSelectionKeys } from 'primevue/tree'
import { toTree, toTreeNode } from '@/components/tree/index'
import { useRouter, useRoute } from 'vue-router'
import { TreeManager } from '@/components/tree/index'
import { TreeCommonAPI } from '@/api/tree'
import TreeEmpty from '@/components/tree-empty/index.vue'
import type { TreeNode } from 'primevue/treenode'
import bus from '@/bus/index'

const route = useRoute()
const router = useRouter()
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  if (id === 'root') {
    return undefined
  } else {
    return { [id]: true }
  }
})
const treeCommonAPI = new TreeCommonAPI('datasource')

const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({ name: 'datasourceFolders', params: { id: 'root' } })
    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({ name: 'datasourceFolders', params: { id: treeNode.key } })
  } else {
    router.push({ name: 'datasourceDetails', params: { id: treeNode.key } })
  }
}

const createFolderSuccess = (key: string, node: any) => {
  const treeNode = toTreeNode({ ...node, type: 'folder' })
  treeManage.value.addChild(key, treeNode)
  expandedKeys.value = { ...expandedKeys.value, [key]: true }
}

const openCreateResourceDialog = (node?: TreeNode) => {
  const folderId = node ? node.key : 'root'
  router.push({ name: 'datasourceCreate', params: { folderId } })
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
const reloadTree = () => {
  treeCommonAPI.listTree('root').then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
  })
}
onMounted(() => {
  bus.on('datasource:created', ({ folderId }: { folderId: string }) => {
    reloadTree()
    expandedKeys.value = { ...expandedKeys.value, [folderId]: true }
  })

  reloadTree()
})

onBeforeUnmount(() => {
  bus.off('datasource:created')
})
</script>
