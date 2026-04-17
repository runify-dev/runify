<template>
  <div>
    <Drawer v-model:visible="visible" header="资源授权" position="right" class="!w-3/5">
      <div class="flex flex-col h-full gap-4 p-4">
        <!-- Top bar with SelectButton -->
        <div class="flex items-center gap-3">
          <SelectButton
            v-model="active"
            :options="menuOptions"
            option-label="label"
            option-value="value"
            :allow-empty="false"
          >
            <template #option="{ option }">
              <span class="flex items-center gap-1.5 text-sm font-medium px-1">
                <i :class="option.icon" />
                {{ option.label }}
              </span>
            </template>
          </SelectButton>
        </div>

        <!-- TreeTable -->
        <div
          class="flex-1 overflow-auto rounded-xl border border-surface-200 dark:border-surface-700 shadow-sm"
        >
          <TreeTable
            :value="treeNodes"
            scrollable
            scroll-height="flex"
            class="resource-tree-table"
            size="small"
          >
            <Column field="name" header="名称" expander>
              <template #body="{ node }">
                <span class="font-medium text-surface-800 dark:text-surface-100 tracking-wide">
                  {{ node.data.name }}
                </span>
              </template>
            </Column>
            <Column field="permission" header="权限">
              <template #body="{ node }">
                <div class="flex items-center gap-3 flex-wrap">
                  <label
                    v-for="opt in getPermOptions(node.data)"
                    :key="opt.value"
                    class="flex items-center gap-1.5 cursor-pointer group"
                  >
                    <RadioButton
                      :model-value="node.data.permission"
                      :value="opt.value"
                      @update:model-value="node.data.change($event)"
                      :input-id="`${node.key}-${opt.value}`"
                    />
                    <span
                      class="text-sm text-surface-600 dark:text-surface-300 group-hover:text-primary-600 transition-colors select-none"
                    >
                      {{ opt.label }}
                    </span>
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
import SelectButton from 'primevue/selectbutton'
import TreeTable from 'primevue/treetable'
import Column from 'primevue/column'
import RadioButton from 'primevue/radiobutton'
import { TreeCommonAPI } from '@/api/tree'
import { toTree } from '@/utils/common'

const visible = ref(false)
const userId = ref<string>()
const active = ref<'application' | 'knowledge' | 'model'>('application')
const treeNodes = ref<any[]>([])

const menuOptions = [
  { label: '应用', value: 'application', icon: 'app-icon app-application' },
  { label: '笔记', value: 'note', icon: 'app-icon app-document' },
  { label: '模型', value: 'model', icon: 'app-icon app-model' },
  { label: '项目', value: 'project', icon: 'app-icon app-document' }
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
