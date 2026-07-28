<template>
  <div class="flex h-[360px] border rounded overflow-hidden" style="border-color: var(--p-content-border-color);">
    <!-- 左：文件夹树 -->
    <div class="w-1/3 border-r overflow-auto" style="border-color: var(--p-content-border-color);">
      <Tree
        v-model:selectionKeys="selectedKeys"
        :value="folderTree"
        selectionMode="single"
        class="w-full !p-2 !border-0"
        @node-select="onFolderSelect"
      >
        <template #nodeicon><i class="pi pi-folder text-sm"></i></template>
        <template #empty><div class="text-xs text-surface-400 p-2">暂无目录</div></template>
      </Tree>
    </div>
    <!-- 右：工具列表 -->
    <div class="flex-1 overflow-auto p-2">
      <div
        v-for="tool in tools"
        :key="tool.id"
        class="flex items-start gap-2 px-3 py-2 rounded cursor-pointer hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors"
        @click="pick(tool)"
      >
        <div class="w-8 h-8 rounded-lg bg-primary-50 flex items-center justify-center text-primary-500 shrink-0">
          <i class="pi pi-wrench text-sm"></i>
        </div>
        <div class="min-w-0">
          <div class="text-sm font-medium truncate">{{ tool.label || tool.name }}</div>
          <div class="text-xs text-surface-400 line-clamp-2">{{ tool.desc || '暂无描述' }}</div>
        </div>
        <span class="ml-auto text-[10px] px-1.5 py-0.5 rounded-full bg-surface-100 text-surface-500 shrink-0">{{ tool.runtime }}</span>
      </div>
      <div v-if="tools.length === 0" class="flex flex-col items-center justify-center h-full text-surface-400">
        <i class="pi pi-inbox text-3xl mb-2 opacity-40"></i>
        <span class="text-xs">该目录暂无工具</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Tree from 'primevue/tree'
import type { TreeNode } from 'primevue/treenode'
import type { TreeSelectionKeys } from 'primevue/tree'
import { cloneDeep } from 'lodash'
import { toTree } from '@/components/tree/index'
import { ROOT_FOLDER_ID } from '@/constants/common'
import toolApi from '@/api/tool'
import { runToolNode } from '@/workflow/common/data'

const emit = defineEmits(['selected'])

const folderTree = ref<any[]>([])
const selectedKeys = ref<TreeSelectionKeys>({ [ROOT_FOLDER_ID]: true })
const tools = ref<any[]>([])

// 只保留文件夹作为左侧树
const foldersOnly = (nodes: any[]): any[] =>
  nodes
    .filter((n) => n.data?.type === 'folder')
    .map((n) => ({ ...n, children: foldersOnly(n.children || []) }))

const loadTools = (folderId: string) => {
  toolApi.listResource(folderId).then((ok: any) => (tools.value = ok.data || []))
}

const onFolderSelect = (node: TreeNode) => {
  loadTools(node.key as string)
}

const pick = (tool: any) => {
  const node = cloneDeep(runToolNode)
  node.properties.name = tool.label || tool.name
  node.properties.nodeData.toolId = tool.id
  emit('selected', node)
}

onMounted(() => {
  toolApi.listTree(ROOT_FOLDER_ID).then((ok: any) => {
    folderTree.value = foldersOnly(toTree(ok.data))
  })
  loadTools(ROOT_FOLDER_ID)
})
</script>

<style lang="scss" scoped></style>
