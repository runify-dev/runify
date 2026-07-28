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
          :pt="{ root: { style: { padding: '16px 0' } }, nodeLabel: { style: { width: '100%' } } }"
        >
          <template #empty><TreeEmpty></TreeEmpty></template>
          <template #nodeicon="scope">
            <i class="pi pi-folder" v-if="scope.node.type == 'folder'"></i>
            <i class="pi pi-wrench" v-else></i>
          </template>
          <template #default="{ node }">
            <div class="flex items-center justify-between w-full group">
              <span>{{ node.label }}</span>
              <div class="action-buttons">
                <DropdownMenu :items="getMenuItems(node)">
                  <template #default>
                    <Button v-tooltip="t('common.operation')" icon="pi pi-ellipsis-v" variant="text"
                            severity="secondary" size="small"></Button>
                  </template>
                </DropdownMenu>
              </div>
            </div>
          </template>
        </Tree>

        <CreateFolderDialog @create:folder:success="createFolderSuccess" ref="createFolderDialogRef" :api="treeCommonAPI" />
        <RenameDialog @rename:success="renameSuccess" ref="renameDialogRef" :api="treeCommonAPI" />

        <Dialog v-model:visible="createVisible" modal header="新建工具" :style="{ width: '30rem' }">
          <div class="flex flex-col gap-3">
            <div class="flex flex-col gap-1">
              <label class="text-sm font-semibold">名称</label>
              <InputText v-model="createForm.name" placeholder="合法标识符" fluid />
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-sm font-semibold">运行时</label>
              <SelectButton v-model="createForm.runtime" :options="runtimeOptions" option-label="label" option-value="value" />
            </div>
          </div>
          <template #footer>
            <Button :label="t('common.cancel')" text @click="createVisible = false" />
            <Button :label="t('common.confirm')" @click="doCreate" :loading="creating" />
          </template>
        </Dialog>
      </div>
    </template>
    <router-view :key="route.path"></router-view>
  </AppMenuContent>
</template>

<script setup lang="ts">
import { t } from '@/locales'
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import RenameDialog from '@/components/rename-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import { computed, onMounted, onBeforeUnmount, ref, reactive } from 'vue'
import bus from '@/bus'
import Tree, { type TreeSelectionKeys } from 'primevue/tree'
import { toTree, toTreeNode, TreeManager } from '@/components/tree/index'
import { useExpandSelectedAncestors } from '@/components/tree/useExpandSelectedAncestors'
import { useRouter, useRoute } from 'vue-router'
import { TreeCommonAPI } from '@/api/tree'
import TreeEmpty from '@/components/tree-empty/index.vue'
import type { TreeNode } from 'primevue/treenode'
import { ROOT_FOLDER_ID } from '@/constants/common'
import useStore from '@/stores'
import { hasPermission } from '@/permission'
import { PermissionConstants } from '@/permission/data'
import { Role } from '@/permission/common'

const { user } = useStore()
const route = useRoute()
const router = useRouter()
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => {
  const id = route.params.id as string
  return { [id]: true }
})
const treeCommonAPI = new TreeCommonAPI('tool')

const runtimeOptions = [
  { label: '工作流', value: 'WORKFLOW' },
  { label: 'JS 脚本', value: 'JS' }
]

const nodeSelect = (treeNode?: TreeNode) => {
  if (treeNode === undefined) {
    router.push({ name: 'toolFolders', params: { id: ROOT_FOLDER_ID } })
    return
  }
  if (treeNode.data.type == 'folder') {
    router.push({ name: 'toolFolders', params: { id: treeNode.key } })
  } else {
    router.push({ name: 'toolDetails', params: { id: treeNode.key } })
  }
}

const createFolderSuccess = (key: string, node: any) => {
  user.resetProfile().then(() => {
    const treeNode = toTreeNode({ ...node, type: 'folder' })
    treeManage.value.addChild(key, treeNode)
    expandedKeys.value = { ...expandedKeys.value, [key]: true }
  })
}

// ===== 创建工具 =====
const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '', runtime: 'JS', folderId: ROOT_FOLDER_ID })
const openCreateResourceDialog = (node?: TreeNode) => {
  createForm.name = ''
  createForm.runtime = 'JS'
  createForm.folderId = node?.key || ROOT_FOLDER_ID
  createVisible.value = true
}
const doCreate = () => {
  creating.value = true
  treeCommonAPI
    .createResource(createForm.folderId, { name: createForm.name, runtime: createForm.runtime })
    .then((res: any) => {
      createVisible.value = false
      user.resetProfile().then(() => {
        const treeNode = toTreeNode({ ...res.data, type: 'tool' })
        treeManage.value.addChild(createForm.folderId, treeNode)
        expandedKeys.value = { ...expandedKeys.value, [createForm.folderId]: true }
        nodeSelect(treeNode)
      })
    })
    .finally(() => (creating.value = false))
}

const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const renameDialogRef = ref<InstanceType<typeof RenameDialog>>()
const openCreateFolderDialog = (node?: TreeNode) => createFolderDialogRef.value?.open(node)
const openRenameDialog = (node: TreeNode) =>
  renameDialogRef.value?.open(node.key, node.label || '', node.data.type === 'folder' ? 'folder' : 'resource')
const renameSuccess = (key: string, node: any) => treeManage.value?.updateLabel(key, node.name)

const getMenuItems = (node: any) => [
  {
    label: t('common.create'),
    visible: node.data.type === 'folder' && hasPermission([PermissionConstants.TOOL_CREATE.newResourcePermission(node.key), Role.ADMIN], 'OR'),
    items: [
      { label: '工具', command: () => openCreateResourceDialog(node) },
      { label: t('common.folder'), command: () => openCreateFolderDialog(node) }
    ]
  },
  {
    label: t('common.rename'),
    command: () => openRenameDialog(node)
  },
  {
    label: t('common.delete'),
    command: () => removeTreeNode(node)
  }
]

const removeTreeNode = (node: TreeNode) => {
  ;(node.data.type === 'folder' ? treeCommonAPI.removeFolder(node.key) : treeCommonAPI.removeResource(node.key)).then(() => {
    treeManage.value.remove(node.key)
  })
}

const nodes = ref<Array<any>>([])
const treeManage = ref()
const expandSelectedAncestors = useExpandSelectedAncestors(treeManage, expandedKeys)
const reloadTree = () => {
  treeCommonAPI.listTree(ROOT_FOLDER_ID).then((ok) => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
    expandSelectedAncestors()
  })
}

// 列表页「新建工具」按钮通过总线触发
const openCreateByFolderId = ({ folderId }: { folderId: string }) => {
  const treeNode = treeManage.value?.findNodeByKey(folderId)
  openCreateResourceDialog(treeNode ? treeNode : undefined)
  createForm.folderId = folderId || ROOT_FOLDER_ID
}

onMounted(() => {
  reloadTree()
  bus.on('open:create:tool:dialog', openCreateByFolderId)
})
onBeforeUnmount(() => bus.off('open:create:tool:dialog', openCreateByFolderId))
defineExpose({ openCreateResourceDialog })
</script>

<style lang="scss" scoped></style>
