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
          placeholder="搜索应用..."
        />
      </InputGroup>

      <!-- 操作按钮组 -->
      <div class="flex items-center gap-2 shrink-0">
        <input ref="fileInputRef" type="file" accept=".json" class="hidden" @change="handleImport"/>
        <Button icon="pi pi-plus"
                v-if="permissionCreate"
                label="创建" @click="toggleCreateMenu"/>
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
              {{ item.desc || '暂无描述' }}
            </p>
          </template>
          <template #footer>
            <div class="flex items-center justify-between pt-2.5 border-t" style="border-color: var(--p-content-border-color);">
              <span class="text-[11px] font-medium px-2 py-0.5 rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-300">
                应用
              </span>
              <span class="flex items-center gap-1 text-[11px] text-surface-400">
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
        <p class="text-sm">暂无应用，点击「新建应用」开始创建</p>
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

const createMenuItems = [
  {
    label: '智能体应用',
    description: '基于大模型能力，快速构建自主决策的智能体',
    icon: 'pi pi-android',
    command: () => openCreateApplication('agent')
  },
  {
    label: '知识库应用',
    description: '连接专属知识库，打造精准问答助手',
    icon: 'pi pi-book',
    command: () => openCreateApplication('search')
  },
  {
    label: '自定义应用',
    description: '自由编排工作流，灵活定制专属应用',
    icon: 'pi pi-file',
    command: () => openCreateApplication('workflow')
  },
  {
    label: '导入配置',
    description: '通过 JSON 文件一键导入已有应用',
    icon: 'pi pi-upload',
    command: () => fileInputRef.value?.click()
  }
]

const toggleCreateMenu = (event: Event) => {
  createMenuRef.value?.toggle(event)
}

const menuItems = computed(() => [
  {
    label: '打开',
    icon: 'pi pi-arrow-up-right',
    visible: hasPermission([
      PermissionConstants.APPLICATION_READ.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    command: () => activeItem.value && handleOpen(activeItem.value)
  },
  {
    label: '编辑',
    icon: 'pi pi-pencil',
    visible: hasPermission([
      PermissionConstants.APPLICATION_EDIT.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    command: () => activeItem.value && handleEdit(activeItem.value)
  },
  {separator: true},
  {
    label: '删除',
    visible: hasPermission([
      PermissionConstants.APPLICATION_DELETE.newResourcePermission(activeItem.value?.id || ''),
      Role.ADMIN], "OR"),
    icon: 'pi pi-trash',
    class: '!text-red-500 [&_.p-menuitem-icon]:!text-red-500',
    command: () => activeItem.value && handleDelete(activeItem.value)
  },
  {separator: true},
  {
    label: '导出',
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
    message: `确定要删除「${item.name}」吗？此操作不可撤销。`,
    header: '删除确认',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {label: '取消', severity: 'secondary', variant: 'outlined'},
    acceptProps: {label: '删除', severity: 'danger'},
    accept: () => {
      treeCommonAPI.removeResource(item.id).then(() => {
        nodeList.value = nodeList.value.filter((n) => n.id !== item.id)
        bus.emit('tree:remove', item.id)
        toast.add({severity: 'success', summary: '删除成功', life: 2000})
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
    toast.add({severity: 'success', summary: '导入成功', life: 2000})
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
