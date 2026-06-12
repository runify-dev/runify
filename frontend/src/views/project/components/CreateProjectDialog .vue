<template>
  <Dialog v-model:visible="visible" modal :header="t('project.createProject')" :style="{ width: '28rem' }">
    <Form ref="formRef" :initial-values="{ name: '' }" :resolver="resolver" @submit="submit">
      <div class="flex flex-col gap-4">
        <!-- 项目图标 -->
        <FormField v-slot="$field: any" name="icon">
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium text-surface-700">{{ t('project.projectIcon') }}</label>
            <div class="flex items-center gap-3">
              <!-- 未上传时显示上传区域 -->
              <div
                v-if="!$field.value"
                class="w-16 h-16 rounded-xl border-2 border-dashed border-surface-300 flex items-center justify-center cursor-pointer hover:border-primary-400 hover:bg-primary-50 transition-all duration-200"
                @click="triggerFileInput"
              >
                <i class="pi pi-plus text-surface-400 text-lg" />
              </div>

              <!-- 已上传时显示图片 -->
              <div v-else class="relative group">
                <div class="w-16 h-16 rounded-xl overflow-hidden border border-[var(--p-content-border-color)]">
                  <Image :src="$field.value" alt="icon" width="64" height="64" preview />
                </div>
                <button
                  class="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-red-500 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-150"
                  @click="$field.onChange({ value: '' })"
                >
                  <i class="pi pi-times text-xs" />
                </button>
              </div>

              <span class="text-xs text-surface-400">{{ t('common.imageFormat') }}</span>
            </div>
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleFileChange"
            />
          </div>
        </FormField>

        <!-- 项目名称 -->
        <FormField v-slot="$field" name="name">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">{{ t('project.projectName') }}</label>
            <InputText
              type="text"
              :placeholder="t('project.projectNamePlaceholder')"
              fluid
              class="!text-sm"
            />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>

        <!-- 项目描述 -->
        <FormField v-slot="$field" name="desc">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">{{ t('project.projectDesc') }}</label>
            <Textarea
              :placeholder="t('project.projectDescPlaceholder')"
              rows="3"
              fluid
              class="!text-sm !resize-none"
            />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>

        <!-- 项目路径 -->
        <FormField v-slot="$field" name="path">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">{{ t('project.projectPath') }}</label>
            <div class="flex items-center gap-2">
              <span class="text-sm text-surface-400 shrink-0">/</span>
              <InputText
                type="text"
                :placeholder="t('project.projectPathPlaceholder')"
                fluid
                class="!text-sm"
              />
            </div>
            <p class="text-xs text-surface-400">{{ t('project.projectPathHint') }}</p>
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>
      </div>
    </Form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" severity="secondary" variant="outlined" @click="close" />
        <Button :label="t('common.create')" @click="submit" />
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { t } from '@/locales'
import fileAPI from '@/api/file'
import { TreeCommonAPI } from '@/api/tree'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { TreeNode } from 'primevue/treenode'
import type { FormInstance } from '@primevue/forms'

const emit = defineEmits(['create:resource:success'])

const resolver = ref(
  zodResolver(
    z.object({
      name: z.string().min(1, { message: t('project.projectNameRequired') }),
      path: z
        .string()
        .min(1, { message: t('project.pathRequired') })
        .regex(/^[a-zA-Z0-9_-]+$/, { message: t('project.pathFormat') }),
      desc: z.string().optional(),
      icon: z.string().min(1, { message: t('project.iconRequired') })
    })
  )
)

const treeCommonAPI = new TreeCommonAPI('project')
const formRef = ref<FormInstance>()
const fileInputRef = ref<HTMLInputElement>()
const visible = ref(false)
const current = ref<TreeNode>()

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
      formRef.value?.setFieldValue('icon', `./api/storage/file/${ok.data.id}`)
    })
    input.value = ''
  }
}

const submit = () => {
  formRef.value?.validate().then(({ errors, values }) => {
    if (Object.keys(errors).length === 0) {
      const submitValues = {
        ...values,
        path: `/${values.path}`
      }
      treeCommonAPI
        .createResource(current.value ? current.value.key : 'root', submitValues)
        .then((ok) => {
          emit('create:resource:success', current.value ? current.value.key : undefined, ok.data)
          close()
        })
    }
  })
}

const open = (node: any) => {
  current.value = node
  visible.value = true
}

const close = () => {
  current.value = undefined
  visible.value = false
}

defineExpose({ open, close })
</script>
