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
                                      label: '应用',
                                      command: () => {
                                        openCreateApplicationDialog()
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
                                label: '应用',
                                visible: node.data.type == 'folder',
                                command: () => {
                                  openCreateApplicationDialog(node)
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
                  :class="item.name === route.name ? 'p-menu-item-selected' : ''"
                  v-bind="props.action"
                >
                  <span :class="item.icon" />
                  <span>{{ item.label }}</span>
                  <Badge v-if="item.badge" class="ml-auto" :value="item.badge" />
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
      <CreateResourceDialog
        ref="createResourceDialogRef"
        :api="treeCommonAPI"
        name="应用"
        @create:resource:success="createResourceSuccess"
      ></CreateResourceDialog>
      <CreateFolderDialog
        @create:folder:success="createFolderSuccess"
        ref="createFolderDialogRef"
        :api="treeCommonAPI"
      ></CreateFolderDialog>
    </template>

    <RouterView></RouterView>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import CreateResourceDialog from '@/components/create-resource-dialog/index.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import { onMounted, ref, computed, onBeforeUnmount } from 'vue'
import Tree, { type TreeSelectionKeys } from 'primevue/tree'
import { toTree, toTreeNode } from '@/components/tree/index'
import { useRouter, useRoute } from 'vue-router'
import { TreeManager } from '@/components/tree/index'
import { TreeCommonAPI } from '@/api/tree'
import FlipCard from '@/components/flip-card/index.vue'
import type { TreeNode } from 'primevue/treenode'
import bus from '@/bus/index'
import TreeEmpty from '@/components/tree-empty/index.vue'
const route = useRoute()
const to = (routeName: string) => {
  router.push({ name: routeName })
}
const isFlipped = ref<boolean>(
  ['applicationOverview', 'applicationSetting', 'applicationConversationLog'].includes(
    route.name as string
  )
)
const items = ref([
  {
    name: 'applicationOverview',
    label: '概览',
    icon: 'pi pi-fw pi-objects-column p-1',
    shortcut: ''
  },
  {
    name: 'applicationSetting',
    label: '设置',
    icon: 'pi pi-fw pi-cog p-1'
  },
  {
    name: 'applicationConversationLog',
    label: '对话日志',
    icon: 'pi pi-fw pi-file p-1'
  }
])
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  if (id === 'root') {
    return undefined
  } else {
    return { [id]: true }
  }
})
const treeCommonAPI = new TreeCommonAPI('application')
const router = useRouter()
const flipCardRef = ref<InstanceType<typeof FlipCard>>()
const back = () => {
  const id = route.params.id as string
  const parent = treeManage.value?.findParentNode(id)
  if (parent) {
    router.push({ name: 'applicationFolders', params: { id: parent?.key } })
  } else {
    router.push({ name: 'applicationFolders', params: { id: 'root' } })
  }
  flipCardRef.value?.unflip()
}

const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({ name: 'applicationFolders', params: { id: 'root' } })
    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({ name: 'applicationFolders', params: { id: treeNode.key } })
  } else {
    flipCardRef.value?.flip()
    if (
      !['applicationOverview', 'applicationSetting', 'applicationConversationLog'].includes(
        route.name as string
      )
    ) {
      router.push({ name: 'applicationDetails', params: { id: treeNode.key } })
    } else {
      router.push({ name: route.name, params: { id: treeNode.key } })
    }
  }
}
const createResourceSuccess = (key: string, node: any) => {
  const treeNode = toTreeNode({ ...node, type: 'application' })
  treeManage.value?.addChild(key, treeNode)
  expandedKeys.value = { ...expandedKeys.value, [key]: true }
  nodeSelect(treeNode)
}
const createFolderSuccess = (key: string, node: any) => {
  const treeNode = toTreeNode({ ...node, type: 'folder' })
  treeManage.value?.addChild(key, treeNode)
  expandedKeys.value = { [key]: true }
  router.push({ name: 'applicationFolders', params: { id: node.id } })
}
const createResourceDialogRef = ref<InstanceType<typeof CreateResourceDialog>>()
const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const openCreateApplicationDialog = (node?: TreeNode) => {
  createResourceDialogRef.value?.open(node)
}
const openCreateFolderDialog = (node?: TreeNode) => {
  createFolderDialogRef.value?.open(node)
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
  bus.on('open:create:application:dialog', (id: string) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    openCreateApplicationDialog(treeNode ? treeNode : undefined)
  })

  bus.on('tree:remove', (id: string) => {
    treeManage.value?.remove(id)
  })

  bus.on('sidebar:flip', () => {
    flipCardRef.value?.flip()
  })

  treeCommonAPI.listTree('root').then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
  })
})
onBeforeUnmount(() => {
  bus.off('tree:remove')
  bus.off('sidebar:flip')
  bus.off('open:create:application:dialog')
})
</script>
<style lang="scss"></style>
