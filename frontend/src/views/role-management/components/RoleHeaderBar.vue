<template>
  <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
    <div class="min-w-0">
      <div class="flex flex-wrap items-center gap-2">
        <div class="truncate text-xl font-semibold leading-6 text-slate-900">
          {{ roleName || '角色管理' }}
        </div>

        <span class="text-sm font-medium text-slate-400">
          ({{ inheritedRoleLabel(roleType) }})
        </span>

        <span class="flex items-center gap-1 text-xs text-slate-400">
          <i class="pi pi-users text-xs" />
          <span>{{ memberTotal }}</span>
        </span>

        <Tag
          v-if="showReadonlyTag"
          severity="secondary"
          value="内置角色不可编辑"
          class="ml-1"
        />
      </div>
    </div>

    <Tabs :value="activeTab" class="w-auto" @update:value="emit('update:activeTab', $event as 'permission' | 'member')">
      <TabList class="bg-slate-100 rounded-lg p-1">
        <Tab value="permission" :class="activeTab === 'permission' ? 'bg-[#10b981] text-white' : 'text-slate-700 hover:bg-slate-200'">权限配置</Tab>
        <Tab value="member" :class="activeTab === 'member' ? 'bg-[#10b981] text-white' : 'text-slate-700 hover:bg-slate-200'">成员</Tab>
      </TabList>
    </Tabs>
  </div>
</template>

<script setup lang="ts">
import Tab from 'primevue/tab'
import TabList from 'primevue/tablist'
import Tabs from 'primevue/tabs'
import Tag from 'primevue/tag'

defineProps<{
  roleName?: string
  roleType?: string | null
  memberTotal: number
  showReadonlyTag: boolean
  activeTab: 'permission' | 'member'
  inheritedRoleLabel: (type?: string | null) => string
}>()

const emit = defineEmits<{
  (e: 'update:activeTab', value: 'permission' | 'member'): void
}>()
</script>
