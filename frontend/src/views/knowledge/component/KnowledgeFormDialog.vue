<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="t('knowledge.form.header')"
    :style="{ width: '28rem' }"
  >
    <div class="flex flex-col gap-4">
      <!-- 图标 -->
      <div class="flex flex-col gap-2">
        <label class="text-sm font-medium text-surface-700">{{ t('knowledge.form.icon') }}</label>
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
          <span class="text-xs text-surface-400">{{ t('knowledge.form.imageFormat') }}</span>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="hidden"
          @change="handleFileChange"
        />
      </div>

      <!-- 名称 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('knowledge.form.name') }}</label>
        <InputText
          v-model="form.name"
          type="text"
          :placeholder="t('knowledge.form.namePlaceholder')"
          fluid
          class="!text-sm"
        />
      </div>

      <!-- 描述 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('knowledge.form.desc') }}</label>
        <Textarea
          v-model="form.desc"
          :placeholder="t('knowledge.form.descPlaceholder')"
          rows="3"
          fluid
          class="!text-sm !resize-none"
        />
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
import {TreeCommonAPI} from '@/api/tree'
import type {TreeNode} from 'primevue/treenode'
import {t} from '@/locales'

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:success'])

const fileInputRef = ref<HTMLInputElement>()
const visible = ref(false)
const current = ref<TreeNode>()

const form = reactive({
  name: '',
  icon: '',
  desc: ''
})

const resetForm = () => {
  form.name = ''
  form.icon = ''
  form.desc = ''
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

const open = (node?: TreeNode) => {
  current.value = node
  resetForm()
  visible.value = true
}

const close = () => {
  current.value = undefined
  visible.value = false
}

const submit = () => {
  if (!form.name.trim()) return
  props.api
    .createResource(current.value ? current.value.key : 'root', {
      name: form.name,
      desc: form.desc,
      icon: form.icon
    })
    .then((ok) => {
      emit('create:success', current.value ? current.value.key : undefined, ok.data)
      close()
    })
}

defineExpose({open, close})
</script>
