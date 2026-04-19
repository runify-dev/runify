<template>
  <div>
    <Drawer v-model:visible="visible" header="资源授权" position="right" class="!w-3/5">
      <div class="flex flex-col h-full gap-4 p-4">
        <!-- Resource type tabs -->
        <div class="flex items-center gap-6 border-b border-gray-200 pb-2">
          <button
            v-for="option in menuOptions"
            :key="option.value"
            class="px-3 py-2 text-lg font-medium transition-all"
            :class="
              active === option.value
                ? 'text-[#10b981] border-b-2 border-[#10b981]'
                : 'text-gray-600 hover:text-[#10b981]'
            "
            @click="active = option.value"
          >
            {{ option.label }}
          </button>
        </div>

        <!-- Search box -->
        <div class="w-full">
          <div class="relative">
            <input
              type="text"
              placeholder="搜索名称"
              v-model="searchKeyword"
              class="w-full px-4 py-2 pl-10 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#10b981] focus:border-transparent"
            />
            <span class="absolute left-3 top-2.5 text-gray-400">
              <i class="pi pi-search"></i>
            </span>
          </div>
        </div>

        <!-- TreeTable -->
        <div
          class="flex-1 overflow-auto rounded-xl border border-surface-200 dark:border-surface-700 shadow-sm"
        >
          <TreeTable
            ref="treeTableRef"
            :value="filteredTreeNodes"
            scrollable
            scroll-height="flex"
            class="resource-tree-table"
            size="normal"
            :rowStyle="{ height: '60px' }"
            :headerStyle="{ backgroundColor: '#f5f5f5' }"
            :expandedKeys="expandedKeys"
          >
            <Column
              field="name"
              header="资源名称"
              expander
              style="min-width: 200px; padding-left: 20px"
            >
              <template #body="{ node }">
                <div class="flex items-center gap-2 pl-4">
                  <span
                    v-if="
                      node.data.type === 'folder' || (node.children && node.children.length > 0)
                    "
                    class="text-[#10b981]"
                  >
                    <i class="pi pi-folder"></i>
                  </span>
                  <span v-else-if="active === 'application'" class="text-[#10b981]">
                    <i class="pi pi-reddit"></i>
                  </span>
                  <span v-else-if="active === 'model'" class="text-[#10b981]">
                    <i class="pi pi-box"></i>
                  </span>
                  <span v-else-if="active === 'project'" class="text-[#10b981]">
                    <i class="pi pi-android"></i>
                  </span>
                  <span v-else class="text-[#10b981]">
                    <i class="pi pi-file"></i>
                  </span>
                  <span class="font-medium text-surface-800 dark:text-surface-100 tracking-wide">
                    {{ node.data.name }}
                  </span>
                </div>
              </template>
            </Column>
            <Column field="permission" header="权限" style="min-width: 250px">
              <template #body="{ node }">
                <div class="flex items-center gap-4">
                  <label
                    class="flex items-center gap-1.5 cursor-pointer"
                    v-for="item in getPermOptions(node.data)"
                  >
                    <RadioButton
                      :model-value="node.data.permission"
                      :value="item.value"
                      @update:model-value="node.data.change(item.value)"
                      :input-id="`${node.key}-not-auth`"
                    />
                    <span class="text-sm text-surface-600 dark:text-surface-300">{{
                      item.label
                    }}</span>
                  </label>
                </div>
              </template>
            </Column>
          </TreeTable>
        </div>
      </div>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import Drawer from 'primevue/drawer'
import TreeTable from 'primevue/treetable'
import Column from 'primevue/column'
import RadioButton from 'primevue/radiobutton'
import { TreeCommonAPI } from '@/api/tree'
import { toTree } from '@/utils/common'

const visible = ref(false)
const userId = ref<string>()
const active = ref<'application' | 'note' | 'model' | 'project'>('application')
const treeNodes = ref<any[]>([])
const filteredTreeNodes = ref<any[]>([])
const searchKeyword = ref('')
const treeTableRef = ref<any>(null)
const expandedKeys = ref<string[]>([])

const menuOptions = [
  { label: '应用', value: 'application' as const, icon: 'app-icon app-application' },
  { label: '笔记', value: 'note' as const, icon: 'app-icon app-document' },
  { label: '模型', value: 'model' as const, icon: 'app-icon app-model' },
  { label: '项目', value: 'project' as const, icon: 'app-icon app-document' }
]

// 将普通树形数据转换为 TreeTable 需要的 TreeNode 格式
const toTreeNodes = (items: any[]): any[] => {
  return items.map((item) => ({
    key: item.id,
    data: item,
    children: item.children ? toTreeNodes(item.children) : undefined
  }))
}

const getPermOptions = (row: any) => {
  const base = [
    { label: '不授权', value: 'NOT_AUTH' },
    { label: '查看', value: 'VIEW' },
    { label: '管理', value: 'MANAGE' },
    { label: '按角色', value: 'ROLE' }
  ]
  return row.parentId ? [{ label: '继承', value: 'INHERIT' }, ...base] : base
}

const close = () => {
  visible.value = false
}

watch(active, () => {
  listResourcePermission()
})

watch(searchKeyword, () => {
  filterTreeNodes()
})

const filterTreeNodes = () => {
  if (!searchKeyword.value) {
    filteredTreeNodes.value = treeNodes.value
    expandedKeys.value = []
    return
  }

  const keyword = searchKeyword.value.toLowerCase()
  const keysToExpand = new Set<string>()

  // 遍历所有节点，收集需要展开的节点key
  const collectExpandedKeys = (nodes: any[], parentKeys: string[] = []) => {
    nodes.forEach((node) => {
      const currentKeys = [...parentKeys, node.key]
      const matches = node.data.name.toLowerCase().includes(keyword)

      if (matches) {
        // 将当前节点及其所有父节点的key添加到展开列表
        currentKeys.forEach((key) => keysToExpand.add(key))
      }

      if (node.children && node.children.length > 0) {
        collectExpandedKeys(node.children, currentKeys)
      }
    })
  }

  // 收集需要展开的节点
  collectExpandedKeys(treeNodes.value)

  // 更新expandedKeys
  expandedKeys.value = Array.from(keysToExpand)

  // 过滤出匹配的节点及其父节点
  const filterNodes = (nodes: any[]) => {
    return nodes.filter((node) => {
      const matches = node.data.name.toLowerCase().includes(keyword)

      if (node.children && node.children.length > 0) {
        node.children = filterNodes(node.children)
        const hasMatchingChildren = node.children.length > 0

        return matches || hasMatchingChildren
      }

      return matches
    })
  }

  // 过滤节点
  filteredTreeNodes.value = filterNodes([...treeNodes.value])
}

const listResourcePermission = () => {
  const api = new TreeCommonAPI(active.value)
  api.listResourcePermission(userId.value as string).then((ok) => {
    ok.data.forEach((item: any) => {
      item.change = function (permission: string) {
        api.authResourcePermission(userId.value as string, this.id, permission).then(() => {
          this.permission = permission
        })
      }
    })
    treeNodes.value = toTreeNodes(toTree(ok.data))
    filteredTreeNodes.value = treeNodes.value
  })
}

const open = (uId: string) => {
  visible.value = true
  userId.value = uId
  listResourcePermission()
}

defineExpose({ close, open })
</script>

<style scoped></style>
