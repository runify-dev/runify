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
          :placeholder="t('application.search')"
        />
      </InputGroup>

      <!-- 操作按钮组 -->
      <div class="flex items-center gap-2 shrink-0">
        <input ref="fileInputRef" type="file" accept=".json" class="hidden" @change="handleImport"/>
        <Button icon="pi pi-plus"
                v-if="permissionCreate"
                :label="t('application.create')" @click="toggleCreateMenu"/>
        <Menu ref="createMenuRef" :model="createMenuItems" popup
              :pt="{ item: { class: '!p-0' }, itemContent: { style: 'justify-content: flex-start !important' }, itemLink: { style: 'justify-content: flex-start !important; text-align: left !important' }, list: { class: '!py-1' } }">
          <template #item="{ item, props }">
            <a v-ripple class="flex flex-col gap-0.5 px-3 py-1.5 items-start" v-bind="props.action">
              <div class="flex items-center gap-2">
                <span :class="item.icon" class="w-4 text-sm"/>
                <span class="text-sm font-medium">{{ item.label }}</span>
              </div>
              <span class="text-xs text-surface-400">{{ item.description }}</span>
            </a>
          </template>
        </Menu>
      </div>
    </div>

    <!-- 应用网格 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <template v-for="item in filteredList" :key="item.id">
        <Card
          class="group cursor-pointer hover:-translate-y-0.5 transition-all duration-200"
          :pt="{
            root: {
              class: 'h-full flex flex-col',
              style: 'border: 1px solid var(--p-content-border-color); box-shadow: var(--p-shadow-1); background: var(--p-content-background);'
            },
            body: { class: 'flex-1 flex flex-col gap-3' },
            content: { class: 'flex-1' }
          }"
          @click="handleOpen(item)"
        >
          <template #header>
            <div class="flex items-start justify-between p-4 pb-0">
              <div
                class="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500 text-lg shrink-0 overflow-hidden"
              >
                <img
                  v-if="item.icon"
                  :src="item.icon"
                  alt="icon"
                  class="w-full h-full object-cover"
                />
                <i v-else class="pi pi-th-large"/>
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
          </template>
          <template #content>
            <h3 class="text-sm font-semibold text-surface-900 truncate mb-1">
              {{ item.name }}
            </h3>
            <p class="text-xs text-surface-500 leading-relaxed line-clamp-2 min-h-[2.5rem]">
              {{ item.desc || t('application.noDesc') }}
            </p>
          </template>
          <template #footer>
            <div class="flex items-center justify-between gap-2 pt-2.5 border-t" style="border-color: var(--p-content-border-color);">
              <span
                class="text-[11px] font-medium px-1.5 py-0.5 rounded-full inline-flex items-center gap-0.5 shrink-0"
                :class="getTypeBadgeClass(getAppType(item))"
              >
                <i :class="getTypeIcon(getAppType(item))" class="text-[9px]"/>
                {{ getShortTypeLabel(getAppType(item)) }}
              </span>
              <span class="flex items-center gap-1 text-[11px] text-surface-400 shrink-0 whitespace-nowrap">
                <i class="pi pi-clock text-[10px]"/>
                {{ item.updateTime }}
              </span>
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
        <p class="text-sm">{{ t('application.empty') }}</p>
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
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {type Node} from '@/api/type/node'
import {useConfirm} from 'primevue/useconfirm'
import {useToast} from 'primevue/usetoast'
import bus from '@/bus/index'
import {TreeCommonAPI} from '@/api/tree'
import {PermissionConstants} from "@/permission/data.ts";
import {Role} from "@/permission/common.ts";
import {hasPermission} from "@/permission";
import { t } from '@/locales'

const treeCommonAPI = new TreeCommonAPI('application')
const router = useRouter()
const route = useRoute()
const confirm = useConfirm()
const toast = useToast()

const nodeList = ref<Array<Node>>([])
const folder = ref<Node>()
const searchText = ref<string>('')
const menuRef = ref()
const createMenuRef = ref()
const activeItem = ref<Node | null>(null)
const fileInputRef = ref<HTMLInputElement>()

const filteredList = computed(() =>
  searchText.value
    ? nodeList.value.filter((n) => n.name?.includes(searchText.value))
    : nodeList.value
)
const permissionCreate = computed(() => {
  return hasPermission([PermissionConstants.APPLICATION_CREATE.newResourcePermission(folderId.value), Role.ADMIN], 'OR')
})
const folderId = computed(() => {
  const {
    params: {id}
  } = route as any
  return id
})

const createMenuItems = computed(() => [
  {
    label: t('application.createTypes.agent.label'),
    description: t('application.createTypes.agent.description'),
    icon: 'pi pi-android',
    command: () => openCreateApplication('agent')
  },
  {
    label: t('application.createTypes.knowledge.label'),
    description: t('application.createTypes.knowledge.description'),
    icon: 'pi pi-book',
    command: () => openCreateApplication('search')
  },
  {
    label: t('application.createTypes.custom.label'),
    description: t('application.createTypes.custom.description'),
    icon: 'pi pi-file',
    command: () => openCreateApplication('workflow')
  },
  {
    label: t('application.createTypes.import.label'),
    description: t('application.createTypes.import.description'),
    icon: 'pi pi-upload',
    command: () => fileInputRef.value?.click()
  }
])

const toggleCreateMenu = (event: Event) => {
  createMenuRef.value?.toggle(event)
}

const menuItems = computed(() => [
  {
    label: t('application.open'),
    icon: 'pi pi-arrow-up-right',
    visible: hasPermission([
      PermissionConstants.APPLICATION_READ.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    command: () => activeItem.value && handleOpen(activeItem.value)
  },
  {
    label: t('application.edit'),
    icon: 'pi pi-pencil',
    visible: hasPermission([
      PermissionConstants.APPLICATION_EDIT.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    command: () => activeItem.value && handleEdit(activeItem.value)
  },
  {separator: true},
  {
    label: t('application.delete'),
    visible: hasPermission([
      PermissionConstants.APPLICATION_DELETE.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    icon: 'pi pi-trash',
    class: '!text-red-500 [&_.p-menuitem-icon]:!text-red-500',
    command: () => activeItem.value && handleDelete(activeItem.value)
  },
  {separator: true},
  {
    label: t('application.export'),
    visible: hasPermission([
      PermissionConstants.APPLICATION_READ.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    icon: 'pi pi-download',
    command: () => activeItem.value && handleExport(activeItem.value)
  }
])

const toggleMenu = (event: Event, item: Node) => {
  activeItem.value = item
  menuRef.value?.toggle(event)
}

// 从应用数据中获取类型（可能为 null/undefined）
const getAppType = (item: Node): string | null => {
  return (item as any).appType || null
}

// 获取应用类型的图标（与新建菜单保持一致）
const getTypeIcon = (appType: string | null) => {
  const iconMap: Record<string, string> = {
    'agent': 'pi pi-android',
    'search': 'pi pi-book',
    'workflow': 'pi pi-file'
  }
  return iconMap[appType || ''] || 'pi pi-th-large'
}

// 获取应用类型的短标签（用于 footer 紧凑显示）
const getShortTypeLabel = (appType: string | null) => {
  const labelMap: Record<string, string> = {
    'agent': t('application.shortTypes.agent'),
    'search': t('application.shortTypes.search'),
    'workflow': t('application.shortTypes.workflow')
  }
  return labelMap[appType || ''] || t('application.appLabel')
}

// 获取应用类型标签的样式类
const getTypeBadgeClass = (appType: string | null) => {
  const classMap: Record<string, string> = {
    'agent': 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-300',
    'search': 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-300',
    'workflow': 'bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-300'
  }
  return classMap[appType || ''] || 'bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-300'
}


const openCreateApplication = (type?: string) => {
  bus.emit('open:create:application:dialog', {id: folderId.value, type})
}

const handleOpen = (item: Node) => {
  router.push({name: 'applicationDetails', params: {id: item.id}})
  bus.emit('sidebar:flip')
}

const handleEdit = (item: Node) => {
  bus.emit('open:edit:application:dialog', item)
}

const handleDelete = (item: Node) => {
  confirm.require({
    message: t('application.deleteMessage', { name: item.name }),
    header: t('application.deleteConfirm'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {label: t('common.cancel'), severity: 'secondary', variant: 'outlined'},
    acceptProps: {label: t('application.delete'), severity: 'danger'},
    accept: () => {
      treeCommonAPI.removeResource(item.id).then(() => {
        nodeList.value = nodeList.value.filter((n) => n.id !== item.id)
        bus.emit('tree:remove', item.id)
        toast.add({severity: 'success', summary: t('application.deleteSuccess'), life: 2000})
      })
    }
  })
}

const handleExport = (item: Node) => {
  treeCommonAPI.exportResource(item.id).then((blob: Blob) => {
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${item.name}.json`
    link.click()
    window.URL.revokeObjectURL(url)
  })
}

const handleImport = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  treeCommonAPI.importResource(folderId.value, file).then(() => {
    lisResource()
    toast.add({severity: 'success', summary: t('application.importSuccess'), life: 2000})
    input.value = ''
  })
}

const lisResource = () => {
  treeCommonAPI.listResource(folderId.value).then((ok) => {
    nodeList.value = ok.data
  })
}

const forderInfo = () => {
  if (!['root', 'shar', 'share'].includes(folderId.value)) {
    treeCommonAPI.getFolder(folderId.value).then((ok) => {
      folder.value = ok.data
    })
  }
}

watch(folderId, () => {
  lisResource()
  forderInfo()
})

onMounted(() => {
  forderInfo()
  lisResource()
  bus.on('application:edit:success', lisResource)
})

onUnmounted(() => {
  bus.off('application:edit:success', lisResource)
})
</script>
