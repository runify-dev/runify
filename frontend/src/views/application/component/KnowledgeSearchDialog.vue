<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="t('application.form.knowledgeHeader')"
    :style="{ width: '28rem' }"
  >
    <div class="flex flex-col gap-4">
      <!-- 知识库选择 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('application.form.selectKnowledge') }}</label>
        <MultiSelect
          v-model="form.knowledgeIds"
          :options="knowledgeList"
          optionLabel="name"
          optionValue="id"
          filter
          :placeholder="t('application.form.selectKnowledgePlaceholder')"
          :maxSelectedLabels="3"
          class="w-full"
        />
      </div>

      <!-- 模型选择 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('application.form.selectModel') }}</label>
        <Select
          v-model="form.modelId"
          :options="modelList"
          optionLabel="name"
          optionValue="id"
          :placeholder="t('application.form.selectModelPlaceholder')"
          class="w-full"
        />
      </div>

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

      <!-- 是否匿名 -->
      <div class="flex items-center gap-3">
        <ToggleSwitch v-model="form.allowAnonymousAccess"/>
        <label class="text-sm text-surface-700">{{ t('application.form.allowAnonymous') }}</label>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" severity="secondary" variant="outlined" @click="close"/>
        <Button :label="t('common.create')" @click="submit"/>
      </div>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import {ref, reactive} from 'vue'
import fileAPI from '@/api/file'
import { t } from '@/locales'
import {TreeCommonAPI} from '@/api/tree'
import type {TreeNode} from 'primevue/treenode'
import {getWorkflowCall} from "@/views/application/template";
import {ROOT_FOLDER_ID} from "@/constants/common.ts";
import MultiSelect from 'primevue/multiselect'

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:success'])

const fileInputRef = ref<HTMLInputElement>()
const visible = ref(false)
const current = ref<TreeNode>()

const modelCommonAPI = new TreeCommonAPI('model')
const knowledgeCommonAPI = new TreeCommonAPI('knowledge')
const modelList = ref<Array<any>>([])
const knowledgeList = ref<Array<any>>([])

const form = reactive({
  name: '',
  icon: '',
  desc: '',
  allowAnonymousAccess: false,
  modelId: '',
  knowledgeIds: [] as string[]
})

const resetForm = () => {
  form.name = ''
  form.icon = ''
  form.desc = ''
  form.allowAnonymousAccess = false
  form.modelId = ''
  form.knowledgeIds = []
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
    workflow: getWorkflowCall('search')(form.modelId, form.knowledgeIds)
  }

  props.api
    .createResource(current.value ? current.value.key : 'root', data)
    .then((ok) => {
      emit('create:success', current.value ? current.value.key : undefined, ok.data)
      close()
    })
}

const open = (node?: TreeNode) => {
  current.value = node
  resetForm()
  visible.value = true
  modelCommonAPI.listResource(ROOT_FOLDER_ID).then((ok) => {
    modelList.value = ok.data
  })
  knowledgeCommonAPI.listResource(ROOT_FOLDER_ID).then((ok) => {
    knowledgeList.value = ok.data
  })
}

const close = () => {
  current.value = undefined
  visible.value = false
}

defineExpose({open, close})
</script>
