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
                  <i class="pi pi-folder" v-if="scope.node.type === 'folder'"/>
                  <i class="pi pi-bolt" v-else-if="scope.node.type === 'skill'"/>
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
                          <Button v-tooltip="t('skill.operation')" icon="pi pi-ellipsis-v" variant="text" severity="secondary" size="small"/>
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
              <Button icon="pi pi-arrow-left" severity="contrast" variant="text" rounded :aria-label="t('common.back')" @click="back"/>
            </div>
            <div class="flex items-center justify-between px-3 py-2 border-b" style="border-color: var(--p-content-border-color);">
              <span class="text-sm font-semibold">{{ t('skill.details.files') }}</span>
              <div class="flex gap-1">
                <Button icon="pi pi-folder" size="small" variant="text" severity="secondary" v-tooltip="t('skill.details.newFolder')" @click="skillCreateFolderRef?.open({skillId: currentSkillId, parentId: fileParentId})"/>
                <Button icon="pi pi-file-edit" size="small" variant="text" severity="secondary" v-tooltip="t('skill.details.newText')" @click="skillCreateTextRef?.open({skillId: currentSkillId, parentId: fileParentId})"/>
                <Button icon="pi pi-upload" size="small" variant="text" severity="secondary" v-tooltip="t('skill.details.uploadFile')" @click="fileInputRef?.click()"/>
              </div>
            </div>
            <div class="flex-1 overflow-auto p-2">
              <Tree
                :value="skillFileNodes"
                :expandedKeys="fileExpandedKeys"
                selectionMode="single"
                :selectionKeys="fileSelectedKeys"
                @node-select="onFileNodeSelect"
                @node-expand="(n: any) => fileExpandedKeys[n.key] = true"
                @node-collapse="(n: any) => delete fileExpandedKeys[n.key]"
                :pt="{ root: { style: { padding: '0', border: '0' } }, nodeLabel: { style: { width: '100%' } } }"
              >
                <template #empty>
                  <p class="text-xs text-surface-400 text-center py-4">{{ t('skill.details.noFiles') }}</p>
                </template>
                <template #nodeicon="scope">
                  <i class="pi pi-folder text-sm" v-if="scope.node.data?.type === 'folder'"/>
                  <i class="pi pi-file-edit text-sm" v-else-if="scope.node.data?.type === 'text'"/>
                  <i class="pi pi-file text-sm" v-else/>
                </template>
                <template #default="{ node }">
                  <div class="flex items-center justify-between w-full group">
                    <span class="text-sm truncate">{{ node.label }}</span>
                    <Button icon="pi pi-ellipsis-v" variant="text" severity="secondary" size="small"
                      class="!w-6 !h-6 !p-0 opacity-0 group-hover:opacity-100 transition-opacity"
                      @click.stop="showFileMenu($event, node)"/>
                  </div>
                </template>
              </Tree>
            </div>
            <input type="file" ref="fileInputRef" class="hidden" @change="handleSkillFileUpload" multiple/>
            <div class="border-t p-2 shrink-0" style="border-color: var(--p-content-border-color);">
              <Button icon="pi pi-cog" :label="t('skill.settings')" size="small" class="w-full justify-start" @click="openSetting"
                :style="route.name === 'skillSetting'
                  ? 'color: var(--p-primary-color); background: var(--p-primary-50); border-color: var(--p-primary-color);'
                  : ''"
                :variant="route.name === 'skillSetting' ? 'outlined' : 'text'"
                severity="secondary"/>
            </div>
          </template>
        </FlipCard>

        <!-- 文件夹树 Dialog -->
        <SkillFormDialog ref="skillFormDialogRef" :api="treeCommonAPI" @create:success="createResourceSuccess"/>
        <CreateFolderDialog ref="createFolderDialogRef" :api="treeCommonAPI" @create:folder:success="createFolderSuccess"/>
        <RenameDialog ref="renameDialogRef" :api="treeCommonAPI" @rename:success="renameSuccess"/>

        <!-- 文件树 Dialog -->
        <SkillCreateFolderDialog ref="skillCreateFolderRef" @create:success="onSkillFolderCreated"/>
        <SkillCreateTextDialog ref="skillCreateTextRef" @create:success="onSkillTextCreated"/>
        <SkillFileRenameDialog ref="skillRenameRef" @rename:success="onSkillFileRenamed"/>

        <!-- 文件右键菜单 -->
        <Menu ref="fileMenuRef" :model="fileMenuItems" popup/>

        <ConfirmDialog/>
      </div>
    </template>
    <router-view :key="route.path"></router-view>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import SkillFormDialog from './component/SkillFormDialog.vue'
import CreateFolderDialog from '@/components/create-folder-dialog/index.vue'
import RenameDialog from '@/components/rename-dialog/index.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import FlipCard from '@/components/flip-card/index.vue'
import SkillCreateFolderDialog from './component/CreateFolderDialog.vue'
import SkillCreateTextDialog from './component/CreateTextDialog.vue'
import SkillFileRenameDialog from './component/FileRenameDialog.vue'
import {computed, onMounted, ref, onBeforeUnmount, provide, watch} from 'vue'
import Tree, {type TreeSelectionKeys} from 'primevue/tree'
import {toTree, toTreeNode} from '@/components/tree/index'
import {useExpandSelectedAncestors} from '@/components/tree/useExpandSelectedAncestors'
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
import skillApi from '@/api/skill'
import type {SkillFile} from '@/api/skill'
import {useToast} from 'primevue/usetoast'
import {useConfirm} from 'primevue/useconfirm'
import {t} from '@/locales'

const {user} = useStore()
const route = useRoute()
const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

// ===================== 文件夹树（正面） =====================
const expandedKeys = ref<TreeSelectionKeys>()
const selectedKeys = computed(() => ({[(route.params.id as string)]: true}))
const treeCommonAPI = new TreeCommonAPI('skill')
const nodes = ref<Array<any>>([])
const treeManage = ref<TreeManager>()
const expandSelectedAncestors = useExpandSelectedAncestors(treeManage, expandedKeys)

const nodeSelect = (treeNode?: TreeNode) => {
  if (!treeNode) {
    router.push({name: 'skillFolders', params: {id: ROOT_FOLDER_ID}})
    return
  }
  if (treeNode.data.type === 'folder') {
    router.push({name: 'skillFolders', params: {id: treeNode.key}})
  } else {
    flipCardRef.value?.flip()
    loadSkillFileTree(treeNode.key)
    router.push({name: 'skillSetting', params: {id: treeNode.key}})
  }
}

const getMenuItems = (node: any) => [
  {
    label: t('common.create'),
    visible: node.data.type === 'folder' && hasPermission([
      PermissionConstants.SKILL_CREATE.newResourcePermission(node.key),
      PermissionConstants.SKILL_FOLDER_CREATE.newResourcePermission(node.key),
      Role.ADMIN], "OR"),
    items: [
      {
        label: t('skill.skillLabel'),
        visible: () => node.data.type === 'folder' && hasPermission([
          PermissionConstants.SKILL_CREATE.newResourcePermission(node.key), Role.ADMIN], "OR"),
        command: () => skillFormDialogRef.value?.open(node)
      },
      {
        label: t('skill.details.newFolder'),
        visible: node.data.type === 'folder' && hasPermission([
          PermissionConstants.SKILL_FOLDER_CREATE.newResourcePermission(node.key), Role.ADMIN], "OR"),
        command: () => createFolderDialogRef.value?.open(node)
      }
    ]
  },
  {
    label: t('common.rename'),
    visible: hasPermission([
      node.data.type === 'folder'
        ? PermissionConstants.SKILL_FOLDER_EDIT.newResourcePermission(node.key)
        : PermissionConstants.SKILL_EDIT.newResourcePermission(node.key),
      Role.ADMIN], "OR"),
    command: () => renameDialogRef.value?.open(node.key, node.label || '', node.data.type === 'folder' ? 'folder' : 'resource')
  },
  {
    label: t('common.delete'),
    visible: hasPermission([
      node.data.type === 'folder'
        ? PermissionConstants.SKILL_FOLDER_DELETE.newResourcePermission(node.key)
        : PermissionConstants.SKILL_DELETE.newResourcePermission(node.key),
      Role.ADMIN], "OR"),
    command: () => {
      confirm.require({
        message: t('skill.deleteMessage', {name: node.label}),
        header: t('skill.deleteConfirm'),
        icon: 'pi pi-exclamation-triangle',
        rejectProps: {label: t('common.cancel'), severity: 'secondary', variant: 'outlined'},
        acceptProps: {label: t('common.delete'), severity: 'danger'},
        accept: () => {
          const api = node.data.type === 'folder' ? treeCommonAPI.removeFolder : treeCommonAPI.removeResource
          api(node.key).then(() => treeManage.value?.remove(node.key))
        }
      })
    }
  }
]

const skillFormDialogRef = ref<InstanceType<typeof SkillFormDialog>>()
const createFolderDialogRef = ref<InstanceType<typeof CreateFolderDialog>>()
const renameDialogRef = ref<InstanceType<typeof RenameDialog>>()

const createResourceSuccess = (key: string, node: any) => {
  user.resetProfile().then(() => {
    const treeNode = toTreeNode({...node, type: 'skill'})
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
    router.push({name: 'skillFolders', params: {id: node.id}})
  })
}

const renameSuccess = (key: string, node: any) => {
  treeManage.value?.updateLabel(key, node.name)
}

// ===================== FlipCard =====================
const isFlipped = ref(route.name === 'skillDetails' || route.name === 'skillSetting')
const flipCardRef = ref<InstanceType<typeof FlipCard>>()

const back = () => {
  const parent = treeManage.value?.findParentNode(route.params.id as string)
  router.push({name: 'skillFolders', params: {id: parent?.key || ROOT_FOLDER_ID}})
  flipCardRef.value?.unflip()
  selectedSkillFile.value = null
  fileSelectedKeys.value = {}
}

const openSetting = () => {
  router.push({name: 'skillSetting', params: {id: currentSkillId.value}})
}

// ===================== 文件树（背面） =====================
const currentSkillId = ref('')
const skillFileNodes = ref<any[]>([])
const skillFiles = ref<SkillFile[]>([])
const fileExpandedKeys = ref<Record<string, boolean>>({})
const fileSelectedKeys = ref<Record<string, boolean>>({})
const selectedSkillFile = ref<SkillFile | null>(null)
const fileInputRef = ref<HTMLInputElement>()

const fileParentId = computed(() =>
  selectedSkillFile.value?.type === 'folder' ? selectedSkillFile.value.id : ROOT_FOLDER_ID
)

provide('currentSkillId', currentSkillId)
provide('selectedSkillFile', selectedSkillFile)

// 路由 fileId 变化时同步选中文件
watch(() => route.params.fileId, (fileId) => {
  if (!fileId || route.name !== 'skillDetails') return
  const file = skillFiles.value.find(f => f.id === fileId)
  if (file) {
    selectedSkillFile.value = file
    fileSelectedKeys.value = {[file.id]: true}
    expandParentChain(fileId as string, skillFiles.value)
  }
})

const expandParentChain = (fileId: string, files: SkillFile[]) => {
  const expanded: Record<string, boolean> = {}
  let current = files.find(f => f.id === fileId)
  while (current && current.parentId && current.parentId !== ROOT_FOLDER_ID) {
    expanded[current.parentId] = true
    current = files.find(f => f.id === current!.parentId)
  }
  fileExpandedKeys.value = {...fileExpandedKeys.value, ...expanded}
}

const loadSkillFileTree = (skillId: string) => {
  currentSkillId.value = skillId
  skillApi.tree(skillId).then(res => {
    skillFiles.value = res.data
    skillFileNodes.value = buildFileTree(res.data, ROOT_FOLDER_ID)
    // 根据路由 fileId 同步选中
    const fileId = route.params.fileId as string
    if (fileId) {
      const file = res.data.find(f => f.id === fileId)
      if (file) {
        selectedSkillFile.value = file
        fileSelectedKeys.value = {[file.id]: true}
        expandParentChain(fileId, res.data)
      }
    }
  })
}

const buildFileTree = (items: SkillFile[], parentId: string): any[] =>
  items.filter(f => f.parentId === parentId).map(f => ({
    key: f.id, label: f.name, data: f, children: buildFileTree(items, f.id)
  }))

const onFileNodeSelect = (node: any) => {
  const file = node.data as SkillFile
  selectedSkillFile.value = file
  fileSelectedKeys.value = {[file.id]: true}
  router.push({name: 'skillDetails', params: {id: currentSkillId.value, fileId: file.id}})
}

const refreshFileTree = () => {
  if (currentSkillId.value) loadSkillFileTree(currentSkillId.value)
}

// ---- 文件树 Dialog 回调 ----
const skillCreateFolderRef = ref<InstanceType<typeof SkillCreateFolderDialog>>()
const skillCreateTextRef = ref<InstanceType<typeof SkillCreateTextDialog>>()
const skillRenameRef = ref<InstanceType<typeof SkillFileRenameDialog>>()

const onSkillFolderCreated = (data: {skillId: string; parentId: string; name: string}) => {
  skillApi.createFolder(data.skillId, data.parentId, data.name).then(() => {
    refreshFileTree()
    toast.add({severity: 'success', summary: t('skill.createSuccess'), life: 2000})
  })
}

const onSkillTextCreated = (data: {skillId: string; parentId: string; name: string}) => {
  skillApi.createText(data.skillId, data.parentId, data.name).then(() => {
    refreshFileTree()
    toast.add({severity: 'success', summary: t('skill.createSuccess'), life: 2000})
  })
}

const onSkillFileRenamed = (data: {skillId: string; fileId: string; name: string}) => {
  skillApi.rename(data.skillId, data.fileId, data.name).then(() => {
    refreshFileTree()
    toast.add({severity: 'success', summary: t('skill.renameSuccess'), life: 2000})
  })
}

// ---- 文件上传 ----
const handleSkillFileUpload = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  for (const file of input.files) {
    const formData = new FormData()
    formData.append('file', file)
    skillApi.uploadFile(currentSkillId.value, fileParentId.value, formData).then(() => {
      toast.add({severity: 'success', summary: t('skill.uploadSuccess', {name: file.name}), life: 2000})
      refreshFileTree()
    })
  }
  input.value = ''
}

// ---- 文件右键菜单 ----
const fileMenuRef = ref()
const fileMenuTarget = ref<SkillFile | null>(null)
const fileMenuItems = computed(() => [
  {
    label: t('common.rename'),
    icon: 'pi pi-pencil',
    command: () => {
      if (fileMenuTarget.value) {
        skillRenameRef.value?.open({
          skillId: currentSkillId.value,
          fileId: fileMenuTarget.value.id,
          currentName: fileMenuTarget.value.name
        })
      }
    }
  },
  {separator: true},
  {
    label: t('common.delete'),
    icon: 'pi pi-trash',
    class: '!text-red-500 [&_.p-menuitem-icon]:!text-red-500',
    command: () => {
      const file = fileMenuTarget.value
      if (!file) return
      confirm.require({
        message: t('skill.deleteMessage', {name: file.name}),
        header: t('skill.deleteConfirm'),
        icon: 'pi pi-exclamation-triangle',
        rejectProps: {label: t('common.cancel'), severity: 'secondary', variant: 'outlined'},
        acceptProps: {label: t('common.delete'), severity: 'danger'},
        accept: () => {
          skillApi.remove(currentSkillId.value, file.id).then(() => {
            if (selectedSkillFile.value?.id === file.id) {
              selectedSkillFile.value = null
              fileSelectedKeys.value = {}
            }
            refreshFileTree()
            toast.add({severity: 'success', summary: t('skill.deleteSuccess'), life: 2000})
          })
        }
      })
    }
  }
])

const showFileMenu = (event: Event, node: any) => {
  fileMenuTarget.value = node.data as SkillFile
  fileMenuRef.value?.toggle(event)
}

// ===================== 生命周期 =====================
onMounted(() => {
  bus.on('open:create:skill:dialog', (id: string) => {
    const treeNode = treeManage.value?.findNodeByKey(id)
    skillFormDialogRef.value?.open(treeNode || undefined)
  })
  bus.on('tree:remove', (id: string) => treeManage.value?.remove(id))
  bus.on('sidebar:flip', (skillId?: string) => {
    flipCardRef.value?.flip()
    const id = skillId || route.params.id as string
    if (id) {
      loadSkillFileTree(id)
      router.push({name: 'skillSetting', params: {id}})
    }
  })
  treeCommonAPI.listTree(ROOT_FOLDER_ID).then(ok => {
    nodes.value = toTree(ok.data)
    treeManage.value = new TreeManager(nodes.value)
    expandSelectedAncestors()
    // 刷新时如果在详情页或设置页，加载文件树
    if (route.name === 'skillDetails' || route.name === 'skillSetting') {
      const skillId = route.params.id as string
      if (skillId) loadSkillFileTree(skillId)
    }
  })
})

onBeforeUnmount(() => {
  bus.off('tree:remove')
  bus.off('sidebar:flip')
  bus.off('open:create:skill:dialog')
})
</script>
<style lang="scss">
.p-menu-item-selected {
  background: var(--p-tree-node-selected-background);
  color: var(--p-tree-node-selected-color);
}
</style>
