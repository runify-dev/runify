<template>
  <div class="p-4 ">
    <!-- 顶部工具栏 -->
    <div class="flex items-center justify-between gap-4 mb-6">
      <!-- 搜索框 -->
      <InputGroup class="max-w-sm">
        <InputGroupAddon>
          <i class="pi pi-search"/>
        </InputGroupAddon>
        <InputText
          v-model="searchText"
          :placeholder="t('model.search')"
        />
      </InputGroup>

      <!-- 新建按钮 -->
      <Button icon="pi pi-plus" v-if="permissionCreate" :label="t('model.create')" @click="openCreate"
              class="shrink-0"/>
    </div>

    <!-- 应用网格 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <template v-for="item in filteredList" :key="item.id">
        <Card
          class="group cursor-pointer hover:-translate-y-0.5 transition-all duration-200"
          :pt="{
            root: { class: 'h-full flex flex-col', style: 'border: 1px solid var(--p-content-border-color); box-shadow: var(--p-shadow-1); background: var(--p-content-background);' },
            body: { class: 'flex-1 flex flex-col gap-3' },
            content: { class: 'flex-1' }
          }"
          @click="handleOpen(item)"
        >
          <template #header>
            <div class="flex items-start justify-between p-4 pb-0">
              <div class="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500 text-lg shrink-0"><i class="pi pi-th-large"/></div>
              <div @click.stop><Button icon="pi pi-ellipsis-v" severity="secondary" variant="text" size="small" class="!w-7 !h-7 !p-0 opacity-0 group-hover:opacity-100 transition-opacity duration-150" @click.stop="toggleMenu($event, item)"/></div>
            </div>
          </template>
          <template #content>
            <h3 class="text-sm font-semibold text-surface-900 truncate mb-1">{{ item.name }}</h3>
            <p class="text-xs text-surface-500 leading-relaxed line-clamp-2 min-h-[2.5rem]">{{ item.desc || t('model.noDesc') }}</p>
          </template>
          <template #footer>
            <div class="flex items-center justify-between pt-2.5 border-t" style="border-color: var(--p-content-border-color);">
              <span class="text-[11px] font-medium px-2 py-0.5 rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-300">{{ t('model.modelLabel') }}</span>
              <span class="flex items-center gap-1 text-[11px] text-surface-400"><i class="pi pi-clock text-[10px]"/> {{ item.updateTime }}</span>
            </div>
          </template>
        </Card>
      </template>

      <!-- 空状态 -->
      <div
        v-if="filteredList.length === 0"
        class="col-span-full flex flex-col items-center justify-center py-16 text-surface-400"
      >
        <i class="pi pi-inbox text-5xl mb-4 opacity-40"/>
        <p class="text-sm">{{ t('model.empty') }}</p>
      </div>
    </div>

    <!-- 操作菜单 -->
    <Menu ref="menuRef" :model="menuItems" popup/>

    <!-- 删除确认 -->
    <ConfirmDialog/>
  </div>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from 'vue-router'
import {computed, onMounted, ref, watch} from 'vue'
import {type Node} from '@/api/type/node'
import {useConfirm} from 'primevue/useconfirm'
import {useToast} from 'primevue/usetoast'
import bus from '@/bus/index'
import {TreeCommonAPI} from '@/api/tree'
import {hasPermission} from "@/permission";
import {PermissionConstants} from "@/permission/data.ts";
import {Role} from "@/permission/common.ts";
import { t } from '@/locales'

const treeCommonAPI = new TreeCommonAPI('model')
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
const permissionCreate = computed(() => {
  return hasPermission([PermissionConstants.MODEL_CREATE.newResourcePermission(folderId.value), Role.ADMIN], 'OR')
})
const folderId = computed(() => {
  const {
    params: {id}
  } = route as any
  return id
})

const menuItems = computed(() => [
  {
    label: t('model.open'),
    icon: 'pi pi-arrow-up-right',
    visible: hasPermission([
      PermissionConstants.MODEL_READ.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    command: () => activeItem.value && handleOpen(activeItem.value)
  },
  {separator: true},
  {
    label: t('model.delete'),
    visible: hasPermission([
      PermissionConstants.MODEL_DELETE.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
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
  bus.emit('open:create:model:dialog', folderId.value)
}

const handleOpen = (item: Node) => {
  router.push({name: 'modelDetails', params: {id: item.id}})
  bus.emit('sidebar:flip')
}

const handleEdit = (item: Node) => {
  bus.emit('open:edit:model:dialog', item)
}

const handleDelete = (item: Node) => {
  confirm.require({
    message: t('model.deleteMessage', { name: item.name }),
    header: t('model.deleteConfirm'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {label: t('common.cancel'), severity: 'secondary', variant: 'outlined'},
    acceptProps: {label: t('common.delete'), severity: 'danger'},
    accept: () => {
      treeCommonAPI.removeResource(item.id).then(() => {
        nodeList.value = nodeList.value.filter((n) => n.id !== item.id)
        bus.emit('tree:remove', item.id)
        toast.add({severity: 'success', summary: t('model.deleteSuccess'), life: 2000})
      })
    }
  })
}

const lisResource = () => {
  treeCommonAPI.listResource(folderId.value).then((ok) => {
    nodeList.value = ok.data
  })
}


watch(folderId, () => {
  lisResource()

})

onMounted(() => {

  lisResource()
})
</script>
