<template>
  <div class="flex flex-col h-full min-h-0">
    <!-- 未选中或文件夹：子项列表 -->
    <div v-if="isFolderView" class="flex-1 overflow-auto p-4">
      <!-- 搜索栏 + 创建 -->
      <div class="flex items-center justify-between gap-4 mb-6">
        <InputGroup class="max-w-sm">
          <InputGroupAddon>
            <i class="pi pi-search"/>
          </InputGroupAddon>
          <InputText v-model="searchText" :placeholder="t('knowledge.details.searchDocs')"/>
        </InputGroup>
        <Button icon="pi pi-plus" :label="t('knowledge.create')" @click="toggleCreateMenu"/>
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
        <input ref="importInputRef" type="file" class="hidden"
               accept=".pdf,.docx,.doc,.zip,.txt,.md,.markdown,.csv,.json,.xml,.log,.html,.yaml,.yml"
               @change="onImportFileChange"/>
      </div>

      <!-- 卡片网格 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <template v-for="child in filteredChildren" :key="child.id">
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
            @click="selectItem(child)"
          >
            <template #header>
              <div class="flex items-start justify-between p-4 pb-0">
                <div class="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500 text-lg shrink-0">
                  <i :class="child.type === 'folder' ? 'pi pi-folder' : 'pi pi-file-edit'"/>
                </div>
                <div @click.stop>
                  <Button icon="pi pi-ellipsis-v" severity="secondary" variant="text" size="small"
                    class="!w-7 !h-7 !p-0 opacity-0 group-hover:opacity-100 transition-opacity duration-150"
                    @click.stop="toggleChildMenu($event, child)"/>
                </div>
              </div>
            </template>
            <template #content>
              <h3 class="text-sm font-semibold text-surface-900 truncate mb-1">{{ child.name }}</h3>
              <p class="text-xs text-surface-500 leading-relaxed line-clamp-2 min-h-[2.5rem]">
                {{ child.type === 'folder' ? t('knowledge.details.folder') : (child.excerpt || t('knowledge.details.noContent')) }}
              </p>
            </template>
            <template #footer>
              <div class="flex items-center justify-between pt-2.5 border-t" style="border-color: var(--p-content-border-color);">
                <span class="text-[11px] font-medium px-2 py-0.5 rounded-full"
                  :class="child.type === 'folder' ? 'bg-surface-100 text-surface-600' : 'bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-300'">
                  {{ child.type === 'folder' ? t('knowledge.details.folder') : t('knowledge.details.document') }}
                </span>
                <span class="flex items-center gap-1 text-[11px] text-surface-400"><i class="pi pi-clock text-[10px]"/> {{ child.updateTime }}</span>
              </div>
            </template>
          </Card>
        </template>

        <!-- 空状态 -->
        <div v-if="filteredChildren.length === 0"
          class="col-span-full flex flex-col items-center justify-center py-16 text-surface-400">
          <i class="pi pi-inbox text-5xl mb-4 opacity-40"/>
          <p class="text-sm">{{ searchText ? t('knowledge.details.noMatch') : t('knowledge.details.emptyFolder') }}</p>
        </div>
      </div>

      <!-- 子项操作菜单 -->
      <Menu ref="childMenuRef" :model="childMenuItems" popup/>
      <ConfirmDialog/>
    </div>

    <!-- 文档：Markdown 编辑器 -->
    <div v-else class="flex-1 min-h-0 relative">
      <Editor ref="editorRef" @change="onChange"/>
      <button
        class="fixed bottom-10 right-10 z-10 w-12 h-12 rounded-full shadow-lg flex items-center justify-center transition-all duration-300 cursor-pointer border-0"
        :class="saving ? 'bg-surface-400 text-white scale-95' : hasChanged ? 'bg-primary-500 text-white hover:bg-primary-600 hover:scale-105' : 'bg-surface-200 text-surface-500'"
        @click="saveNow"
        :title="saving ? t('knowledge.details.saving') : t('common.save')"
      >
        <i v-if="saving" class="pi pi-spin pi-spinner text-lg"/>
        <i v-else class="pi pi-save text-lg"/>
      </button>
    </div>

    <!-- 创建 Dialog -->
    <CreateFolderDialog ref="createFolderRef" @create:success="onFolderCreated"/>
    <CreateTextDialog ref="createTextRef" @create:success="onTextCreated"/>
  </div>
</template>
<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import Editor from '@/editor/index.vue'
import useStore from '@/stores'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import CreateFolderDialog from '@/views/knowledge/component/CreateFolderDialog.vue'
import CreateTextDialog from '@/views/knowledge/component/CreateTextDialog.vue'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import knowledgeApi from '@/api/knowledge'
import type { Document } from '@/api/knowledge'
import { ROOT_FOLDER_ID } from "@/constants/common"
import bus from '@/bus'
import { t } from '@/locales'

const route = useRoute()
const router = useRouter()
const confirm = useConfirm()
const toast = useToast()

// ---- 核心状态：只从路由派生 ----
const knowledgeId = computed(() => route.params.id as string)
const isFolderRoute = computed(() => route.name === 'knowledgeDocFolder')
const currentId = computed(() => isFolderRoute.value ? (route.params.folderId as string) : (route.params.documentId as string))

const currentDoc = ref<Document | null>(null)
const folderChildren = ref<Document[]>([])
const allDocs = ref<Document[]>([])
const searchText = ref('')

const isFolderView = computed(() => isFolderRoute.value)
const currentFolderId = computed(() => isFolderRoute.value ? (currentId.value || ROOT_FOLDER_ID) : (currentDoc.value?.parentId || ROOT_FOLDER_ID))

const filteredChildren = computed(() =>
  searchText.value
    ? folderChildren.value.filter(c => c.name?.includes(searchText.value))
    : folderChildren.value
)

// ---- 加载文档树 + 当前视图 ----
const loadTree = () => {
  if (!knowledgeId.value) return
  knowledgeApi.tree(knowledgeId.value).then(res => {
    allDocs.value = res.data
    resolveView()
  })
}

const resolveView = () => {
  const id = currentId.value
  if (isFolderRoute.value) {
    // 文件夹路由
    currentDoc.value = null
    const folderId = (!id || id === ROOT_FOLDER_ID) ? ROOT_FOLDER_ID : id
    folderChildren.value = getDescendants(allDocs.value, folderId)
  } else {
    // 文档路由
    const doc = allDocs.value.find(f => f.id === id)
    if (doc) {
      currentDoc.value = doc
      loadDocumentContent(doc)
    }
  }
}

const getDescendants = (items: Document[], parentId: string): Document[] => {
  const result: Document[] = []
  for (const child of items.filter(f => f.parentId === parentId)) {
    result.push(child)
    if (child.type === 'folder') result.push(...getDescendants(items, child.id))
  }
  return result
}

// ---- 监听路由变化 ----
watch(() => route.name, () => {
  if (route.name === 'knowledgeDocFolder' || route.name === 'knowledgeDocument') {
    resolveView()
  }
})

watch(() => route.params.documentId, () => {
  if (route.name !== 'knowledgeDocument') return
  resolveView()
})

watch(() => route.params.folderId, () => {
  if (route.name !== 'knowledgeDocFolder') return
  resolveView()
})

watch(() => route.params.id, () => {
  if (route.name !== 'knowledgeDocFolder' && route.name !== 'knowledgeDocument' && route.name !== 'knowledgeSetting') return
  loadTree()
})

// ---- 编辑器 ----
let editingDocId: string
const saving = ref(false)
const hasChanged = ref(false)

const editorRef = ref<InstanceType<typeof Editor>>()

const onChange = () => {
  hasChanged.value = true
}

const saveNow = () => {
  if (!editorRef.value || !editingDocId || saving.value) return
  saving.value = true
  knowledgeApi.updateContent(knowledgeId.value, editingDocId, editorRef.value.getEditor().getMarkdown()).then(() => {
    saving.value = false
    hasChanged.value = false
    toast.add({severity: 'success', summary: t('knowledge.details.saved'), life: 1500})
  }).catch(() => { saving.value = false })
}

const loadDocumentContent = (doc: Document) => {
  editingDocId = doc.id
  saving.value = false
  hasChanged.value = false
  knowledgeApi.getDocument(knowledgeId.value, doc.id).then(ok => {
    editorRef.value?.setContent(ok.data.content)
  })
}

// ---- 点击卡片：只切路由 ----
const selectItem = (item: Document) => {
  if (item.type === 'folder') {
    router.push({name: 'knowledgeDocFolder', params: {id: knowledgeId.value, folderId: item.id}})
  } else {
    router.push({name: 'knowledgeDocument', params: {id: knowledgeId.value, documentId: item.id}})
  }
}

// ---- 创建 ----
const createMenuRef = ref()
const createFolderRef = ref<InstanceType<typeof CreateFolderDialog>>()
const createTextRef = ref<InstanceType<typeof CreateTextDialog>>()

const toggleCreateMenu = (event: Event) => {
  createMenuRef.value?.toggle(event)
}

const createMenuItems = computed(() => [
  {
    label: t('knowledge.details.newFolder'),
    description: t('knowledge.details.newFolderDesc'),
    icon: 'pi pi-folder',
    command: () => createFolderRef.value?.open({knowledgeId: knowledgeId.value, parentId: currentFolderId.value})
  },
  {
    label: t('knowledge.details.newDocument'),
    description: t('knowledge.details.newDocumentDesc'),
    icon: 'pi pi-file-edit',
    command: () => createTextRef.value?.open({knowledgeId: knowledgeId.value, parentId: currentFolderId.value})
  },
  {
    label: t('knowledge.details.importDocument'),
    description: t('knowledge.details.importDocumentDesc'),
    icon: 'pi pi-file-import',
    command: () => importInputRef.value?.click()
  }
])

const importInputRef = ref<HTMLInputElement>()
const importing = ref(false)

const onImportFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importing.value = true
  knowledgeApi.importDocument(knowledgeId.value, currentFolderId.value, file)
    .then(() => {
      loadTree()
      bus.emit('knowledge:document:refresh')
      toast.add({severity: 'success', summary: t('knowledge.createSuccess'), life: 2000})
    })
    .catch((e: any) => {
      toast.add({severity: 'error', summary: e?.message || t('knowledge.details.importFailed'), life: 3000})
    })
    .finally(() => {
      importing.value = false
      input.value = ''
    })
}

const onFolderCreated = (data: {knowledgeId: string; parentId: string; name: string}) => {
  knowledgeApi.createFolder(data.knowledgeId, data.parentId, data.name).then(() => {
    loadTree()
    bus.emit('knowledge:document:refresh')
    toast.add({severity: 'success', summary: t('knowledge.createSuccess'), life: 2000})
  })
}

const onTextCreated = (data: {knowledgeId: string; parentId: string; name: string}) => {
  knowledgeApi.createText(data.knowledgeId, data.parentId, data.name).then(() => {
    loadTree()
    bus.emit('knowledge:document:refresh')
    toast.add({severity: 'success', summary: t('knowledge.createSuccess'), life: 2000})
  })
}

// ---- 子项操作菜单 ----
const childMenuRef = ref()
const childMenuTarget = ref<Document | null>(null)
const childMenuItems = computed(() => [
  {
    label: t('knowledge.rename'),
    icon: 'pi pi-pencil',
    command: () => {
      if (!childMenuTarget.value) return
      knowledgeApi.rename(knowledgeId.value, childMenuTarget.value.id, childMenuTarget.value.name).then(() => {
        loadTree()
        toast.add({severity: 'success', summary: t('knowledge.renameSuccess'), life: 2000})
      })
    }
  },
  {separator: true},
  {
    label: t('knowledge.delete'),
    icon: 'pi pi-trash',
    class: '!text-red-500 [&_.p-menuitem-icon]:!text-red-500',
    command: () => {
      const item = childMenuTarget.value
      if (!item) return
      confirm.require({
        message: t('knowledge.deleteMessage', {name: item.name}),
        header: t('knowledge.deleteConfirm'),
        icon: 'pi pi-exclamation-triangle',
        rejectProps: {label: t('common.cancel'), severity: 'secondary', variant: 'outlined'},
        acceptProps: {label: t('knowledge.delete'), severity: 'danger'},
        accept: () => {
          knowledgeApi.remove(knowledgeId.value, item.id).then(() => {
            loadTree()
            toast.add({severity: 'success', summary: t('knowledge.deleteSuccess'), life: 2000})
          })
        }
      })
    }
  }
])

const toggleChildMenu = (event: Event, item: Document) => {
  childMenuTarget.value = item
  childMenuRef.value?.toggle(event)
}

// ---- 生命周期 ----
loadTree()

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
  if (editorRef.value && editingDocId) {
    const content = editorRef.value.getEditor().getMarkdown()
    if (content) knowledgeApi.updateContent(knowledgeId.value, editingDocId, content)
  }
})

function onBeforeUnload() {
  if (!editorRef.value || !editingDocId) return
  const content = editorRef.value.getEditor().getMarkdown()
  if (!content) return
  const baseURL = window.RUNIFY_APP.admin.baseURL + '/api'
  const { user } = useStore()
  fetch(`${baseURL}/knowledge/resources/${knowledgeId.value}/documents/${editingDocId}/content`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', AUTHORIZATION: `Bearer ${user.getToken()}` },
    body: JSON.stringify({ content }),
    keepalive: true,
  })
}
window.addEventListener('beforeunload', onBeforeUnload)
</script>
