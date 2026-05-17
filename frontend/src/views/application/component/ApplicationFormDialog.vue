<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="isEdit ? '编辑应用' : '新建应用'"
    :style="{ width: '28rem' }"
  >
    <div class="flex flex-col gap-4">
      <!-- 应用图标 -->
      <div class="flex flex-col gap-2">
        <label class="text-sm font-medium text-surface-700">应用图标</label>
        <div class="flex items-center gap-3">
          <div
            v-if="!form.icon"
            class="w-16 h-16 rounded-xl border-2 border-dashed border-surface-300 flex items-center justify-center cursor-pointer hover:border-primary-400 hover:bg-primary-50 transition-all duration-200"
            @click="triggerFileInput"
          >
            <i class="pi pi-plus text-surface-400 text-lg" />
          </div>

          <div v-else class="relative group">
            <div class="w-16 h-16 rounded-xl overflow-hidden border border-surface-200">
              <Image :src="form.icon" alt="icon" width="64" height="64" preview />
            </div>
            <button
              class="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-red-500 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-150"
              @click="form.icon = ''"
            >
              <i class="pi pi-times text-xs" />
            </button>
          </div>

          <span class="text-xs text-surface-400">支持 JPG、PNG 格式</span>
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
        <label class="text-sm font-medium text-surface-700">应用名称</label>
        <InputText
          v-model="form.name"
          type="text"
          placeholder="请输入应用名称"
          fluid
          class="!text-sm"
        />
      </div>

      <!-- 应用描述 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">应用描述</label>
        <Textarea
          v-model="form.desc"
          placeholder="请输入应用描述（选填）"
          rows="3"
          fluid
          class="!text-sm !resize-none"
        />
      </div>

      <!-- 是否匿名 -->
      <div class="flex items-center gap-3">
        <ToggleSwitch v-model="form.allowAnonymousAccess" />
        <label class="text-sm text-surface-700">允许匿名访问</label>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" severity="secondary" variant="outlined" @click="close" />
        <Button :label="isEdit ? '保存' : '创建'" @click="submit" />
      </div>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import fileAPI from '@/api/file'
import applicationAPI from '@/api/application'
import { TreeCommonAPI } from '@/api/tree'
import type { TreeNode } from 'primevue/treenode'

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:success', 'edit:success'])

const fileInputRef = ref<HTMLInputElement>()
const visible = ref(false)
const current = ref<TreeNode>()
const editData = ref<any>(null)

const isEdit = computed(() => !!editData.value)

const form = reactive({
  name: '',
  icon: '',
  desc: '',
  allowAnonymousAccess: false
})

const resetForm = () => {
  form.name = ''
  form.icon = ''
  form.desc = ''
  form.allowAnonymousAccess = false
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
    allowAnonymousAccess: form.allowAnonymousAccess
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
  visible.value = true
}

const close = () => {
  current.value = undefined
  editData.value = null
  visible.value = false
}

defineExpose({ openCreate, openEdit, close })
</script>
