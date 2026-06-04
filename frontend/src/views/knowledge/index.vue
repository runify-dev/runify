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
                :pt="{ root: { style: { padding: '16px 0' } }, nodeLabel: { style: { width: '100%' } } }"
              >
                <template #empty><TreeEmpty/></template>
                <template #nodeicon="scope">
                  <img v-if="scope.node.data?.icon" :src="scope.node.data.icon" class="w-4 h-4 object-cover rounded"/>
                  <i v-else-if="scope.node.type === 'folder'" class="pi pi-folder"/>
                  <i v-else-if="scope.node.type === 'knowledge'" class="pi pi-book"/>
                </template>
                <template #default="{ node }">
                  <div class="flex items-center justify-between w-full group">
                    <span>{{ node.label }}</span>
                    <div class="action-buttons">
                      <DropdownMenu :items="getMenuItems(node)">
                        <template #item="scope">
                          <div class="p-tieredmenu-item-link">
                            <span>{{ scope.label }}</span>
                            <span v-if="scope.hasSubmenu" class="pi pi-angle-right ml-auto"/>
                          </div>
                        </template>
                        <template #default>
                          <Button v-tooltip="'操作'" icon="pi pi-ellipsis-v" variant="text" severity="secondary" size="small"/>
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
              <Button icon="pi pi-arrow-left" severity="contrast" variant="text" rounded aria-label="返回" @click="back"/>
            </div>
            <div class="px-3 py-2 border-b" style="border-color: var(--p-content-border-color);">
              <IconField>
                <InputIcon class="pi pi-search"/>
                <InputText v-model="docSearchText" placeholder="搜索文档..." size="small" fluid class="!text-sm"/>
              </IconField>
            </div>
            <div class="flex-1 overflow-auto p-2">
              <Tree
                :value="filteredDocNodes"
                :expandedKeys="docExpandedKeys"
                selectionMode="single"
                :selectionKeys="docSelectedKeys"
                @node-select="onDocumentNodeSelect"
                @node-expand="(n: any) => docExpandedKeys[n.key] = true"
                @node-collapse="(n: any) => delete docExpandedKeys[n.key]"
                :pt="{ root: { style: { padding: '0', border: '0' } }, nodeLabel: { style: { width: '100%' } } }"
              >
                <template #empty>
                  <p class="text-xs text-surface-400 text-center py-4">暂无文档</p>
                </template>
                <template #nodeicon="scope">
                  <i class="pi pi-folder text-sm" v-if="scope.node.data?.type === 'folder'"/>
                  <i class="pi pi-file-edit text-sm" v-else/>
                </template>
                <template #default="{ node }">
                  <div class="flex items-center justify-between w-full group">
                    <span class="text-sm truncate">{{ node.label }}</span>
                    <DropdownMenu :items="getDocumentMenuItems(node)">
                      <template #item="scope">
                        <div class="p-tieredmenu-item-link">
                          <span>{{ scope.label }}</span>
                          <span v-if="scope.hasSubmenu" class="pi pi-angle-right ml-auto"/>
                        </div>
                      </template>
                      <template #default>
                        <Button icon="pi pi-ellipsis-v" variant="text" severity="secondary" size="small"
                          class="!w-6 !h-6 !p-0"/>
                      </template>
                    </DropdownMenu>
                  </div>
                </template>
              </Tree>
            </div>
            <div class="border-t p-2 shrink-0" style="border-color: var(--p-content-border-color);">
              <Button icon="pi pi-cog" label="设置" size="small" class="w-full justify-start" @click="openSetting"
                :style="route.name === 'knowledgeSetting'
                  ? 'color: var(--p-primary-color); background: var(--p-primary-50); border-color: var(--p-primary-color);'
                  : ''"
                :variant="route.name === 'knowledgeSetting' ? 'outlined' : 'text'"
                severity="secondary"/>
            </div>
          </template>
        </FlipCard>

        <!-- 文件夹树 Dialog -->
        <KnowledgeFormDialog ref="knowledgeFormDialogRef" :api="treeCommonAPI" @create:success="createResourceSuccess"/>
        <CreateFolderDialog ref="createFolderDialogRef" :api="treeCommonAPI" @create:folder:success="createFolderSuccess"/>
        <RenameDialog ref="renameDialogRef" :api="treeCommonAPI" @rename:success="renameSuccess"/>

        <!-- 文档树 Dialog -->
        <KnowledgeCreateFolderDialog ref="docCreateFolderRef" @create:success="onDocumentFolderCreated"/>
        <KnowledgeCreateTextDialog ref="docCreateTextRef" @create:success="onDocumentTextCreated"/>
        <KnowledgeDocumentRenameDialog ref="docRenameRef" @rename:success="onDocumentRenamed"/>


        <ConfirmDialog/>
      </div>
    </template>
    <router-view :key="route.path"></router-view>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import KnowledgeFormDialog from './component/KnowledgeFormDialog.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import RenameDialog from '@/components/rename-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import FlipCard from '@/components/flip-card/index.vue'
import KnowledgeCreateFolderDialog from './component/CreateFolderDialog.vue'
import KnowledgeCreateTextDialog from './component/CreateTextDialog.vue'
import KnowledgeDocumentRenameDialog from './component/DocumentRenameDialog.vue'
import {computed, onMounted, ref, onBeforeUnmount, provide, watch} from 'vue'
import Tree, {type TreeSelectionKeys} from 'primevue/tree'
import {toTree, toTreeNode} from '@/components/tree/index'
import {useRouter, useRoute} from 'vue-router'
import {TreeManager} from '@/components/tree/index'
import {TreeCommonAPI} from '@/api/tree'
import bus from '@/bus/index'
import type {TreeNode} from 'primevue/treenode'
import TreeEmpty from '@/components/tree-empty/index.vue'
import {ROOT_FOLDER_ID} from "@/constants/common.ts"
import useStore from "@/stores"
import {hasPermission} from "@/permission"
import {PermissionConstants} from "@/permission/data.ts"
import {Role} from "@/permission/common.ts"
import knowledgeApi from '@/api/knowledge'
import type {Document} from '@/api/knowledge'
import {useToast} from 'primevue/usetoast'
import {useConfirm} from 'primevue/useconfirm'

const {user} = useStore()
const route = useRoute()
const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

// ===================== 文件夹树（正面） =====================
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => ({[(route.params.id as string)]: true}))
const treeCommonAPI = new TreeCommonAPI('knowledge')
const nodes = ref<Array<any>>([])
const treeManage = ref<TreeManager>()

const nodeSelect = (treeNode?: TreeNode) => {
  if (!treeNode) {
    router.push({name: 'knowledgeFolders', params: {id: ROOT_FOLDER_ID}})
    return
  }
  if (treeNode.data.type === 'folder') {
    router.push({name: 'knowledgeFolders', params: {id: treeNode.key}})
  } else {
    flipCardRef.value?.flip()
    loadDocumentTree(treeNode.key)
    router.push({name: 'knowledgeDocFolder', params: {id: treeNode.key, folderId: ROOT_FOLDER_ID}})
  }
}

const getMenuItems = (node: any) => [
  {
    label: '新建',
    visible: node.data.type === 'folder' && hasPermission([
      PermissionConstants.KNOWLEDGE_CREATE.newResourcePermission(node.key),
      PermissionConstants.KNOWLEDGE_FOLDER_CREATE.newResourcePermission(node.key),
      Role.ADMIN], "OR"),
    items: [
      {
        label: '知识库',
        visible: () => node.data.type === 'folder' && hasPermission([
          PermissionConstants.KNOWLEDGE_CREATE.newResourcePermission(node.key),
          Role.ADMIN], "OR"),
        command: () => knowledgeFormDialogRef.value?.open(node)
      },
      {
        label: '文件夹',
        visible: node.data.type === 'folder' && hasPermission([
          PermissionConstants.KNOWLEDGE_FOLDER_CREATE.newResourcePermission(node.key),
          Role.ADMIN], "OR"),
        command: () => createFolderDialogRef.value?.open(node)
      }
    ]
  },
  {
    label: '重命名',
    visible: hasPermission([
      node.data.type === 'folder'
        ? PermissionConstants.KNOWLEDGE_FOLDER_EDIT.newResourcePermission(node.key)
        : PermissionConstants.KNOWLEDGE_EDIT.newResourcePermission(node.key),
      Role.ADMIN], "OR"),
    command: () => renameDialogRef.value?.open(node.key, node.label || '', node.data.type === 'folder' ? 'folder' : 'resource')
  },
  {
    label: '删除',
    visible: hasPermission([
      node.data.type === 'folder'
        ? PermissionConstants.KNOWLEDGE_FOLDER_DELETE.newResourcePermission(node.key)
        : PermissionConstants.KNOWLEDGE_DELETE.newResourcePermission(node.key),
      Role.ADMIN], "OR"),
    command: () => {
      confirm.require({
        message: `确定要删除「${node.label}」吗？`,
        header: '删除确认',
        icon: 'pi pi-exclamation-triangle',
        rejectProps: {label: '取消', severity: 'secondary', variant: 'outlined'},
        acceptProps: {label: '删除', severity: 'danger'},
        accept: () => {
          const api = node.data.type === 'folder' ? treeCommonAPI.removeFolder : treeCommonAPI.removeResource
          api(node.key).then(() => treeManage.value?.remove(node.key))
        }
      })
    }
  }
]

const knowledgeFormDialogRef = ref<InstanceType<typeof KnowledgeFormDialog>>()
const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const renameDialogRef = ref<InstanceType<typeof RenameDialog>>()

const createResourceSuccess = (key: string, node: any) => {
  user.resetProfile().then(() => {
    const treeNode = toTreeNode({...node, type: 'knowledge'})
    treeManage.value?.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    nodeSelect(treeNode)
  })
}

const createFolderSuccess = (key: string, node: any) => {
  user.resetProfile().then(() => {
    const treeNode = toTreeNode({...node, type: 'folder'})
    treeManage.value?.addChild(key, treeNode)
    expandedKeys.value = {...expandedKeys.value, [key]: true}
    router.push({name: 'knowledgeFolders', params: {id: node.id}})
  })
}

const renameSuccess = (key: string, node: any) => {
  treeManage.value?.updateLabel(key, node.name)
}

// ===================== FlipCard =====================
const isFlipped = ref(route.name === 'knowledgeDocFolder' || route.name === 'knowledgeDocument' || route.name === 'knowledgeSetting')
const flipCardRef = ref<InstanceType<typeof FlipCard>>()

const back = () => {
  const parent = treeManage.value?.findParentNode(route.params.id as string)
  router.push({name: 'knowledgeFolders', params: {id: parent?.key || ROOT_FOLDER_ID}})
  flipCardRef.value?.unflip()
}

const openSetting = () => {
  router.push({name: 'knowledgeSetting', params: {id: currentKnowledgeId.value}})
}

// ===================== 文档树（背面） =====================
const currentKnowledgeId = ref('')
const documentNodes = ref<any[]>([])
const documents = ref<Document[]>([])
const docExpandedKeys = ref<Record<string, boolean>>({})
const docSearchText = ref('')

const docSelectedKeys = computed(() => {
  if (route.name === 'knowledgeDocument') {
    const docId = route.params.documentId as string
    return docId ? {[docId]: true} : {}
  }
  if (route.name === 'knowledgeDocFolder') {
    const folderId = route.params.folderId as string
    if (!folderId || folderId === ROOT_FOLDER_ID) {
      return {[`root-${currentKnowledgeId.value}`]: true}
    }
    return {[folderId]: true}
  }
  return {}
})

const filteredDocNodes = computed(() => {
  if (!docSearchText.value) return documentNodes.value
  const keyword = docSearchText.value.toLowerCase()
  const filterTree = (nodes: any[]): any[] => {
    return nodes.reduce((acc: any[], node: any) => {
      const children = node.children ? filterTree(node.children) : []
      const matched = node.label?.toLowerCase().includes(keyword)
      if (matched || children.length > 0) {
        acc.push({...node, children})
      }
      return acc
    }, [])
  }
  return filterTree(documentNodes.value)
})

provide('currentKnowledgeId', currentKnowledgeId)

// 路由变化时展开侧边树
watch(() => route.params.documentId, (documentId) => {
  if (!documentId || route.name !== 'knowledgeDocument') return
  if (documentId !== ROOT_FOLDER_ID) {
    expandParentChain(documentId as string, documents.value)
  }
})
watch(() => route.params.folderId, (folderId) => {
  if (!folderId || route.name !== 'knowledgeDocFolder') return
  if (folderId !== ROOT_FOLDER_ID) {
    expandParentChain(folderId as string, documents.value)
  }
})

const expandParentChain = (documentId: string, docs: Document[]) => {
  const expanded: Record<string, boolean> = {}
  let current = docs.find(f => f.id === documentId)
  while (current && current.parentId && current.parentId !== ROOT_FOLDER_ID) {
    expanded[current.parentId] = true
    current = docs.find(f => f.id === current!.parentId)
  }
  docExpandedKeys.value = {...docExpandedKeys.value, ...expanded}
}

const loadDocumentTree = (knowledgeId: string) => {
  currentKnowledgeId.value = knowledgeId
  knowledgeApi.tree(knowledgeId).then(res => {
    documents.value = res.data
    documentNodes.value = buildDocumentTreeWithRoot(res.data, knowledgeId)
    // 始终展开根节点
    docExpandedKeys.value = {[`root-${knowledgeId}`]: true, ...docExpandedKeys.value}
    // 展开当前文档的父链
    const docId = route.params.documentId as string
    if (docId && docId !== ROOT_FOLDER_ID) {
      expandParentChain(docId, res.data)
    }
  })
}

const buildDocumentTree = (items: Document[], parentId: string): any[] =>
  items.filter(f => f.parentId === parentId).map(f => ({
    key: f.id, label: f.name, data: f, children: buildDocumentTree(items, f.id)
  }))

const buildDocumentTreeWithRoot = (items: Document[], knowledgeId: string) => {
  const children = buildDocumentTree(items, ROOT_FOLDER_ID)
  return [{
    key: `root-${knowledgeId}`,
    label: '根目录',
    data: { id: ROOT_FOLDER_ID, type: 'folder', parentId: null, knowledgeId } as any,
    children
  }]
}

const onDocumentNodeSelect = (node: any) => {
  const doc = node.data as Document
  if (doc.type === 'folder') {
    router.push({name: 'knowledgeDocFolder', params: {id: currentKnowledgeId.value, folderId: doc.id}})
  } else {
    router.push({name: 'knowledgeDocument', params: {id: currentKnowledgeId.value, documentId: doc.id}})
  }
}

const refreshDocumentTree = () => {
  if (currentKnowledgeId.value) loadDocumentTree(currentKnowledgeId.value)
}

// ---- 文档树 Dialog 回调 ----
const docCreateFolderRef = ref<InstanceType<typeof KnowledgeCreateFolderDialog>>()
const docCreateTextRef = ref<InstanceType<typeof KnowledgeCreateTextDialog>>()
const docRenameRef = ref<InstanceType<typeof KnowledgeDocumentRenameDialog>>()

const onDocumentFolderCreated = (data: {knowledgeId: string; parentId: string; name: string}) => {
  knowledgeApi.createFolder(data.knowledgeId, data.parentId, data.name).then(() => {
    refreshDocumentTree()
    toast.add({severity: 'success', summary: '创建成功', life: 2000})
  })
}

const onDocumentTextCreated = (data: {knowledgeId: string; parentId: string; name: string}) => {
  knowledgeApi.createText(data.knowledgeId, data.parentId, data.name).then(() => {
    refreshDocumentTree()
    toast.add({severity: 'success', summary: '创建成功', life: 2000})
  })
}

const onDocumentRenamed = (data: {knowledgeId: string; documentId: string; name: string}) => {
  knowledgeApi.rename(data.knowledgeId, data.documentId, data.name).then(() => {
    refreshDocumentTree()
    toast.add({severity: 'success', summary: '重命名成功', life: 2000})
  })
}

// ---- 文档菜单 ----
const getDocumentMenuItems = (node: any) => {
  const doc = node.data as Document
  const isRoot = doc.id === ROOT_FOLDER_ID
  return [
    {
      label: '新建',
      icon: 'pi pi-plus',
      visible: doc.type === 'folder',
      items: [
        {
          label: '文件夹',
          icon: 'pi pi-folder',
          command: () => docCreateFolderRef.value?.open({knowledgeId: currentKnowledgeId.value, parentId: doc.id})
        },
        {
          label: '文档',
          icon: 'pi pi-file-edit',
          command: () => docCreateTextRef.value?.open({knowledgeId: currentKnowledgeId.value, parentId: doc.id})
        }
      ]
    },
    {
      label: '重命名',
      icon: 'pi pi-pencil',
      visible: !isRoot,
      command: () => docRenameRef.value?.open({knowledgeId: currentKnowledgeId.value, documentId: doc.id, currentName: doc.name})
    },
    {separator: true, visible: !isRoot},
    {
      label: '删除',
      icon: 'pi pi-trash',
      visible: !isRoot,
      class: '!text-red-500 [&_.p-menuitem-icon]:!text-red-500',
      command: () => {
        confirm.require({
          message: `确定要删除「${doc.name}」吗？`,
          header: '删除确认',
          icon: 'pi pi-exclamation-triangle',
          rejectProps: {label: '取消', severity: 'secondary', variant: 'outlined'},
          acceptProps: {label: '删除', severity: 'danger'},
          accept: () => {
            knowledgeApi.remove(currentKnowledgeId.value, doc.id).then(() => {
              if (route.params.documentId === doc.id) {
                router.push({name: 'knowledgeDocFolder', params: {id: currentKnowledgeId.value, folderId: ROOT_FOLDER_ID}})
              }
              refreshDocumentTree()
              toast.add({severity: 'success', summary: '删除成功', life: 2000})
            })
          }
        })
      }
    }
  ]
}

// ===================== 生命周期 =====================
onMounted(() => {
  bus.on('open:create:knowledge:dialog', (id: string) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    knowledgeFormDialogRef.value?.open(treeNode || undefined)
  })
  bus.on('tree:remove', (id: string) => treeManage.value?.remove(id))
  bus.on('knowledge:document:refresh', () => {
    if (currentKnowledgeId.value) loadDocumentTree(currentKnowledgeId.value)
  })
  bus.on('sidebar:flip', (knowledgeId?: string) => {
    flipCardRef.value?.flip()
    const id = knowledgeId || route.params.id as string
    if (id) {
      loadDocumentTree(id)
      router.push({name: 'knowledgeDocFolder', params: {id, folderId: ROOT_FOLDER_ID}})
    }
  })
  treeCommonAPI.listTree(ROOT_FOLDER_ID).then(ok => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
    // 刷新时如果在详情页或设置页，加载文档树
    if (route.name === 'knowledgeDocFolder' || route.name === 'knowledgeDocument' || route.name === 'knowledgeSetting') {
      const knowledgeId = route.params.id as string
      if (knowledgeId) loadDocumentTree(knowledgeId)
    }
  })
})

onBeforeUnmount(() => {
  bus.off('tree:remove')
  bus.off('sidebar:flip')
  bus.off('open:create:knowledge:dialog')
  bus.off('knowledge:document:refresh')
})
</script>
<style lang="scss">
.p-menu-item-selected {
  background: var(--p-tree-node-selected-background);
  color: var(--p-tree-node-selected-color);
}
</style>
