<template>
  <div class="p-4">
    <div class="flex items-center justify-between gap-4 mb-6">
      <InputGroup class="max-w-sm">
        <InputGroupAddon><i class="pi pi-search" /></InputGroupAddon>
        <InputText v-model="searchText" placeholder="搜索工具" />
      </InputGroup>
      <Button v-if="permissionCreate" icon="pi pi-plus" label="新建工具" class="shrink-0" @click="openCreate" />
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <Card
        v-for="item in filteredList"
        :key="item.id"
        class="group cursor-pointer hover:-translate-y-0.5 transition-all duration-200"
        :pt="{
          root: { style: 'border: 1px solid var(--p-content-border-color); box-shadow: var(--p-shadow-1); background: var(--p-content-background);' },
          body: { class: 'flex-1 flex flex-col gap-3' }
        }"
        @click="open(item)"
      >
        <template #header>
          <div class="flex items-start justify-between p-4 pb-0">
            <div class="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500 text-lg"><i class="pi pi-wrench" /></div>
          </div>
        </template>
        <template #content>
          <h3 class="text-sm font-semibold text-surface-900 truncate mb-1">{{ item.label || item.name }}</h3>
          <p class="text-xs text-surface-500 leading-relaxed line-clamp-2 min-h-[2.5rem]">{{ item.desc || '暂无描述' }}</p>
        </template>
        <template #footer>
          <div class="flex items-center justify-between pt-2.5 border-t" style="border-color: var(--p-content-border-color);">
            <span class="text-[11px] font-medium px-2 py-0.5 rounded-full bg-primary-50 text-primary-600">{{ item.runtime }}</span>
            <span class="flex items-center gap-1 text-[11px] text-surface-400"><i class="pi pi-clock text-[10px]" /> {{ item.updateTime }}</span>
          </div>
        </template>
      </Card>

      <div v-if="filteredList.length === 0" class="col-span-full flex flex-col items-center justify-center py-16 text-surface-400">
        <i class="pi pi-inbox text-5xl mb-4 opacity-40" />
        <p class="text-sm">暂无工具</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { TreeCommonAPI } from '@/api/tree'
import { ROOT_FOLDER_ID } from '@/constants/common'
import bus from '@/bus'
import { hasPermission } from '@/permission'
import { PermissionConstants } from '@/permission/data'
import { Role } from '@/permission/common'

const route = useRoute()
const router = useRouter()
const api = new TreeCommonAPI('tool')
const list = ref<any[]>([])
const searchText = ref('')

const folderId = computed(() => (route.params.id as string) || ROOT_FOLDER_ID)
const permissionCreate = computed(() =>
  hasPermission([PermissionConstants.TOOL_CREATE.newResourcePermission(folderId.value), Role.ADMIN], 'OR')
)
const openCreate = () => bus.emit('open:create:tool:dialog', { folderId: folderId.value })

const filteredList = computed(() =>
  list.value.filter((i) => !searchText.value || (i.label || i.name || '').includes(searchText.value))
)

const reload = () => {
  const folderId = (route.params.id as string) || ROOT_FOLDER_ID
  api.listResource(folderId).then((ok) => (list.value = ok.data || []))
}

const open = (item: any) => router.push({ name: 'toolDetails', params: { id: item.id } })

watch(() => route.params.id, reload, { immediate: true })
</script>

<style lang="scss" scoped></style>
