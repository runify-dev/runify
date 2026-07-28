<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="isEdit ? t('application.form.editHeader') : t('application.form.newHeader')"
    :style="{ width: '28rem' }"
  >
    <div class="flex flex-col gap-4">
      <!-- 应用图标 -->
      <div class="flex flex-col gap-2">
        <label class="text-sm font-medium text-surface-700">{{ t('application.form.appIcon') }}</label>
        <div class="flex items-center gap-3">
          <div
            v-if="!form.icon"
            class="w-16 h-16 rounded-xl border-2 border-dashed border-surface-300 flex items-center justify-center cursor-pointer hover:border-primary-400 hover:bg-primary-50 transition-all duration-200"
            @click="triggerFileInput"
          >
            <i class="pi pi-plus text-surface-400 text-lg"/>
          </div>

          <div v-else class="relative group">
            <div class="w-16 h-16 rounded-xl overflow-hidden border border-surface-200">
              <Image :src="form.icon" alt="icon" width="64" height="64" preview/>
            </div>
            <button
              class="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-red-500 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-150"
              @click="form.icon = ''"
            >
              <i class="pi pi-times text-xs"/>
            </button>
          </div>

          <span class="text-xs text-surface-400">{{ t('application.form.imageFormat') }}</span>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="hidden"
          @change="handleFileChange"
        />
      </div>

      <!-- 应用名称 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('application.form.appName') }}</label>
        <InputText
          v-model="form.name"
          type="text"
          :placeholder="t('application.form.appNamePlaceholder')"
          fluid
          class="!text-sm"
        />
      </div>

      <!-- 应用描述 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('application.form.appDesc') }}</label>
        <Textarea
          v-model="form.desc"
          :placeholder="t('application.form.appDescPlaceholder')"
          rows="3"
          fluid
          class="!text-sm !resize-none"
        />
      </div>

      <!-- 应用类型（仅编辑模式显示） -->
      <div v-if="isEdit" class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('application.form.appType') }}</label>
        <!-- 类型为空：让用户选择 -->
        <Select
          v-if="!originalAppType"
          v-model="form.appType"
          :options="appTypeOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('application.form.selectAppType')"
          class="w-full"
        />
        <!-- 类型已设置：只读显示 -->
        <div v-else class="flex items-center gap-1.5 px-3 py-2 rounded-md bg-surface-100 dark:bg-surface-800 text-sm text-surface-600 dark:text-surface-400">
          <i :class="appTypeIconMap[originalAppType] || 'pi pi-th-large'" class="text-xs"/>
          {{ appTypeLabelMap[originalAppType] || t('application.appLabel') }}
        </div>
      </div>

      <!-- 是否匿名 -->
      <div class="flex items-center gap-3">
        <ToggleSwitch v-model="form.allowAnonymousAccess"/>
        <label class="text-sm text-surface-700">{{ t('application.form.allowAnonymous') }}</label>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" severity="secondary" variant="outlined" @click="close"/>
        <Button :label="isEdit ? t('common.save') : t('common.create')" @click="submit"/>
      </div>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import {ref, reactive, computed} from 'vue'
import fileAPI from '@/api/file'
import { t } from '@/locales'
import applicationAPI from '@/api/application'
import {TreeCommonAPI} from '@/api/tree'
import type {TreeNode} from 'primevue/treenode'
import {getWorkflowCall} from "@/views/application/template";

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:success', 'edit:success'])

const fileInputRef = ref<HTMLInputElement>()
const visible = ref(false)
const current = ref<TreeNode>()
const editData = ref<any>(null)

const isEdit = computed(() => !!editData.value)

const originalAppType = ref<string | null>(null)

const appTypeOptions = [
  { label: t('application.createTypes.agent.label'), value: 'agent' },
  { label: t('application.createTypes.knowledge.label'), value: 'search' },
  { label: t('application.createTypes.custom.label'), value: 'workflow' }
]

const appTypeIconMap: Record<string, string> = {
  'agent': 'pi pi-android',
  'search': 'pi pi-book',
  'workflow': 'pi pi-file'
}

const appTypeLabelMap: Record<string, string> = {
  'agent': t('application.createTypes.agent.label'),
  'search': t('application.createTypes.knowledge.label'),
  'workflow': t('application.createTypes.custom.label')
}

const form = reactive({
  name: '',
  icon: '',
  desc: '',
  allowAnonymousAccess: false,
  appType: '' as string
})

const resetForm = () => {
  form.name = ''
  form.icon = ''
  form.desc = ''
  form.allowAnonymousAccess = false
  form.appType = ''
  originalAppType.value = null
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    const fd = new FormData()
    fd.append('file', file)
    fileAPI.uploadFile(fd).then((ok) => {
      form.icon = `./api/storage/file/${ok.data.id}`
    })
    input.value = ''
  }
}

const submit = () => {
  if (!form.name.trim()) return

  const data = {
    name: form.name,
    desc: form.desc,
    icon: form.icon,
    allowAnonymousAccess: form.allowAnonymousAccess,
    appType: isEdit.value ? (form.appType || undefined) : 'workflow',
    workflow: isEdit.value ? null : getWorkflowCall('customize')()
  }

  if (isEdit.value) {
    applicationAPI.edit(editData.value.id, data).then(() => {
      emit('edit:success', editData.value.id, data)
      close()
    })
  } else {
    props.api
      .createResource(current.value ? current.value.key : 'root', data)
      .then((ok) => {
        emit('create:success', current.value ? current.value.key : undefined, ok.data)
        close()
      })
  }
}

const openCreate = (node?: TreeNode) => {
  editData.value = null
  current.value = node
  resetForm()
  visible.value = true
}

const openEdit = (data: any) => {
  editData.value = data
  current.value = undefined
  form.name = data.name || ''
  form.icon = data.icon || ''
  form.desc = data.desc || ''
  form.allowAnonymousAccess = data.allowAnonymousAccess || false
  form.appType = ''
  originalAppType.value = data.appType || null
  visible.value = true
}

const close = () => {
  current.value = undefined
  editData.value = null
  visible.value = false
}

defineExpose({openCreate, openEdit, close})
</script>
