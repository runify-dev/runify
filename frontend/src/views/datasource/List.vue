<template>
  <div class="p-4">
    <div class="flex items-center justify-between gap-4 mb-6">
      <InputGroup class="max-w-sm">
        <InputGroupAddon class="!bg-surface-0 !border-surface-200">
          <i class="pi pi-search text-surface-400 text-sm" />
        </InputGroupAddon>
        <InputText
          v-model="searchText"
          placeholder="搜索数据源..."
          class="!border-l-0 !border-surface-200 !bg-surface-0 text-sm focus:!border-primary-400 focus:!ring-2 focus:!ring-primary-100 placeholder:text-surface-400 transition-all duration-200"
        />
      </InputGroup>
      <Button icon="pi pi-plus" label="新建数据源" @click="openCreate" class="shrink-0" />
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <template v-for="item in filteredList" :key="item.id">
        <div
          class="group relative flex flex-col gap-3 p-4 rounded-xl border border-surface-200 bg-surface-0 cursor-pointer overflow-hidden hover:border-primary-300 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
          @click="handleOpen(item)"
        >
          <div
            class="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary-400 to-primary-600 opacity-0 group-hover:opacity-100 transition-opacity duration-200"
          />
          <div class="flex items-start justify-between">
            <div
              class="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500 text-lg shrink-0"
            >
              <i class="pi pi-database" />
            </div>
            <div @click.stop>
              <Button
                icon="pi pi-ellipsis-v"
                severity="secondary"
                variant="text"
                size="small"
                class="!w-7 !h-7 !p-0 opacity-0 group-hover:opacity-100 transition-opacity duration-150"
                @click.stop="toggleMenu($event, item)"
              />
            </div>
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="text-sm font-semibold text-surface-900 truncate mb-1">
              {{ item.name }}
            </h3>
            <p class="text-xs text-surface-500 leading-relaxed line-clamp-2 min-h-[2.5rem]">
              {{ item.desc || '暂无描述' }}
            </p>
          </div>
          <div class="flex items-center justify-between pt-2.5 border-t border-surface-100">
            <span
              class="text-[11px] font-medium px-2 py-0.5 rounded-full bg-primary-50 text-primary-600"
            >
              {{ item.dataSourceType || '数据源' }}
            </span>
            <span class="flex items-center gap-1 text-[11px] text-surface-400">
              <i class="pi pi-clock text-[10px]" />
              {{ item.updateTime }}
            </span>
          </div>
        </div>
      </template>

      <div
        v-if="filteredList.length === 0"
        class="col-span-full flex flex-col items-center justify-center py-16 text-surface-400"
      >
        <i class="pi pi-inbox text-5xl mb-4 opacity-40" />
        <p class="text-sm">暂无数据源，点击「新建数据源」开始创建</p>
      </div>
    </div>

    <Menu ref="menuRef" :model="menuItems" popup />
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed, onMounted, ref, watch } from 'vue'
import { type Node } from '@/api/type/node'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import bus from '@/bus/index'
import { TreeCommonAPI } from '@/api/tree'

const treeCommonAPI = new TreeCommonAPI('datasource')
const router = useRouter()
const route = useRoute()
const confirm = useConfirm()
const toast = useToast()

const nodeList = ref<Array<Node>>([])
const searchText = ref<string>('')
const menuRef = ref()
const activeItem = ref<Node | null>(null)

const filteredList = computed(() =>
  searchText.value
    ? nodeList.value.filter((n) => n.name?.includes(searchText.value))
    : nodeList.value
)

const folderId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const menuItems = computed(() => [
  {
    label: '打开',
    icon: 'pi pi-arrow-up-right',
    command: () => activeItem.value && handleOpen(activeItem.value)
  },
  { separator: true },
  {
    label: '删除',
    icon: 'pi pi-trash',
    class: '!text-red-500 [&_.p-menuitem-icon]:!text-red-500',
    command: () => activeItem.value && handleDelete(activeItem.value)
  }
])

const toggleMenu = (event: Event, item: Node) => {
  activeItem.value = item
  menuRef.value?.toggle(event)
}

const openCreate = () => {
  router.push({ name: 'datasourceCreate', params: { folderId: folderId.value } })
}

const handleOpen = (item: Node) => {
  router.push({ name: 'datasourceDetails', params: { id: item.id } })
}

const handleDelete = (item: Node) => {
  confirm.require({
    message: `确定要删除「${item.name}」吗？此操作不可撤销。`,
    header: '删除确认',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: '取消', severity: 'secondary', variant: 'outlined' },
    acceptProps: { label: '删除', severity: 'danger' },
    accept: () => {
      treeCommonAPI.removeResource(item.id).then(() => {
        nodeList.value = nodeList.value.filter((n) => n.id !== item.id)
        bus.emit('tree:remove', item.id)
        toast.add({ severity: 'success', summary: '删除成功', life: 2000 })
      })
    }
  })
}

const listResource = () => {
  treeCommonAPI.listResource(folderId.value).then((ok) => {
    nodeList.value = ok.data
  })
}

watch(folderId, () => {
  listResource()
})

onMounted(() => {
  listResource()
})
</script>
